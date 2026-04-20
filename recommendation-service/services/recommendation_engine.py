"""
Recommendation Engine — Lõi tính toán gợi ý video.

Responsibilities:
1. Quản lý ma trận User-Item (in-memory dictionary)
2. Thực thi User-based Collaborative Filtering
3. Thực thi Popularity-Based Ranking
4. Kết hợp Hybrid với adaptive α
5. Tìm Similar Videos (Item-based CF)
6. Cache kết quả vào Redis

Note:
- User-Item matrix nằm trong memory → restart sẽ mất.
  Kafka consumer có auto.offset.reset=earliest nên sẽ replay
  events từ earliest offset còn lưu (tuỳ Kafka retention).
- Với dataset lớn (>100K users), cần chuyển sang batch processing.
"""

import logging
import time
from collections import defaultdict
from typing import Dict, List, Set

import numpy as np
from sklearn.metrics.pairwise import cosine_similarity

from config import settings
from services.redis_cache import RedisCache

logger = logging.getLogger(__name__)


class RecommendationEngine:
    """
    Engine tính toán gợi ý video.

    Architecture:
    1. In-memory User-Item matrix (dict of dicts)
    2. Event-driven: Kafka events → update matrix → trigger refresh
    3. Cache results vào Redis để serve API nhanh
    """

    def __init__(self, redis_cache: RedisCache):
        self.redis_cache = redis_cache

        # ── Hyperparameters ──
        self.top_n = settings.TOP_N_RECOMMENDATIONS
        self.similar_users_k = settings.SIMILAR_USERS_K
        self.min_interactions = settings.MIN_INTERACTIONS
        self.refresh_interval = settings.REFRESH_INTERVAL_SECONDS

        # ── Ma trận User-Item ──
        # { user_id: { video_id: cumulative_weight } }
        self.user_item_matrix: Dict[str, Dict[str, float]] = defaultdict(
            lambda: defaultdict(float)
        )

        # ── Theo dõi thay đổi để quyết định khi nào refresh ──
        self._new_event_count: Dict[str, int] = defaultdict(int)
        self._last_refresh_time: Dict[str, float] = {}

        logger.info("RecommendationEngine initialized")

    # ═══════════════════════════════════════════════
    # PUBLIC API
    # ═══════════════════════════════════════════════

    def add_interaction(self, user_id: str, video_id: str, weight: float) -> None:
        """
        Thêm 1 interaction vào ma trận User-Item.

        Weight được cộng dồn (accumulate):
            VIEW(1.0) + LIKE(3.0) = 4.0
            UNLIKE(-3.0) sẽ giảm weight

        Clamp tối thiểu = 0 để tránh score âm trong ma trận.
        """
        self.user_item_matrix[user_id][video_id] += weight

        # Clamp: không cho weight âm (UNLIKE nhiều lần)
        if self.user_item_matrix[user_id][video_id] < 0:
            self.user_item_matrix[user_id][video_id] = 0.0

        self._new_event_count[user_id] += 1

    def should_refresh(self, user_id: str) -> bool:
        """
        Kiểm tra có nên tính toán lại recommendations không.

        Điều kiện (OR):
        1. Lần đầu tiên VÀ có >= min_interactions events
        2. Có >= min_interactions events MỚI kể từ lần refresh cuối
        3. Đã quá refresh_interval giây từ lần refresh cuối
        """
        now = time.time()

        # Điều kiện 1: Lần đầu tiên
        if user_id not in self._last_refresh_time:
            return self._new_event_count[user_id] >= self.min_interactions

        # Điều kiện 2: Đủ events mới
        if self._new_event_count[user_id] >= self.min_interactions:
            return True

        # Điều kiện 3:  Quá thời gian refresh_interval
        if now - self._last_refresh_time[user_id] >= self.refresh_interval:
            return self._new_event_count[user_id] > 0  # Chỉ refresh nếu có event mới

        return False

    def get_recommendations(self, user_id: str) -> tuple:
        """
        Lấy recommendations cho user.

        Priority:
        1. Redis cache (fast path)
        2. Tính toán mới (nếu có data)
        3. Popular videos (fallback cho cold-start)

        Returns:
            (video_ids, source) — source = "cache" | "hybrid" | "popular"
        """
        # 1. Cache hit
        cached = self.redis_cache.get_recommendations(user_id)
        if cached:
            logger.debug(f"Cache HIT: user={user_id}")
            return cached, "cache"

        # 2. Tính toán mới
        logger.debug(f"Cache MISS: user={user_id}")
        if user_id in self.user_item_matrix:
            video_ids = self.compute_recommendations(user_id)
            return video_ids, "hybrid"

        # 3. Fallback
        popular = self._get_popular_video_ids()
        return popular, "popular"

    def compute_recommendations(self, user_id: str) -> List[str]:
        """
        Pipeline chính: tính toán và cache recommendations.

        Pipeline:
        1. Collaborative Filtering → { video: score }
        2. Popularity-Based → { video: score }
        3. Adaptive Hybrid → { video: combined_score }
        4. Lọc video đã xem
        5. Sort + Top-N
        6. Cache vào Redis
        """
        logger.info(f"Computing recommendations: user={user_id}")
        start_time = time.time()

        try:
            # 1. Collaborative Filtering
            collab_scores = self._collaborative_filtering(user_id)

            # 2. Popularity-Based
            popular_scores = self._popularity_based(exclude_user=user_id)

            # 3. Hybrid combine
            final_scores = self._hybrid_combine(collab_scores, popular_scores, user_id)

            # 4. Lọc video đã xem
            watched = set(self.user_item_matrix.get(user_id, {}).keys())
            final_scores = {
                vid: score for vid, score in final_scores.items()
                if vid not in watched
            }

            # 5. Sort + Top-N
            sorted_videos = sorted(
                final_scores.items(), key=lambda x: x[1], reverse=True
            )[:self.top_n]

            video_ids = [vid for vid, _ in sorted_videos]

            # 6. Cache
            self.redis_cache.cache_recommendations(user_id, video_ids)

            # Reset counters
            self._new_event_count[user_id] = 0
            self._last_refresh_time[user_id] = time.time()

            elapsed = time.time() - start_time
            logger.info(
                f"Recommendations computed: user={user_id}, "
                f"count={len(video_ids)}, time={elapsed:.3f}s"
            )
            return video_ids

        except Exception as e:
            logger.error(f"Failed to compute recs for {user_id}: {e}", exc_info=True)
            return self._get_popular_video_ids()

    def get_similar_videos(self, video_id: str, top_n: int = 10) -> List[str]:
        """
        Tìm video tương tự (Item-based CF).

        Transpose ma trận User-Item → Item-User matrix,
        rồi tính cosine similarity giữa target video và tất cả video.
        """
        # Transpose: { video_id: { user_id: weight } }
        item_users: Dict[str, Dict[str, float]] = defaultdict(
            lambda: defaultdict(float)
        )
        for user, items in self.user_item_matrix.items():
            for vid, weight in items.items():
                item_users[vid][user] = weight

        if video_id not in item_users:
            return []

        # Tạo user index
        all_users = sorted({
            u for users in item_users.values() for u in users.keys()
        })
        user_to_idx = {u: i for i, u in enumerate(all_users)}

        # Tạo item-user matrix (n_videos × n_users)
        all_videos = list(item_users.keys())
        matrix = np.zeros((len(all_videos), len(all_users)))
        for i, vid in enumerate(all_videos):
            for user, weight in item_users[vid].items():
                matrix[i][user_to_idx[user]] = weight

        # Cosine similarity
        target_idx = all_videos.index(video_id)
        target_vector = matrix[target_idx].reshape(1, -1)
        similarities = cosine_similarity(target_vector, matrix)[0]

        # Sort và lấy top-N (loại chính nó)
        result = []
        for idx in np.argsort(similarities)[::-1]:
            if idx != target_idx and similarities[idx] > 0:
                result.append(all_videos[idx])
                if len(result) >= top_n:
                    break

        return result

    # ═══════════════════════════════════════════════
    # PRIVATE: Algorithm
    # ═══════════════════════════════════════════════

    def _collaborative_filtering(self, target_user: str) -> Dict[str, float]:
        """
        User-based Collaborative Filtering.

        1. Chuyển dict → numpy matrix
        2. Cosine Similarity (target vs all users)
        3. Lấy K similar users
        4. Weighted score cho video chưa xem
        5. Normalize → [0, 1]
        """
        if target_user not in self.user_item_matrix:
            return {}

        all_users = list(self.user_item_matrix.keys())
        if len(all_users) < 2:
            return {}

        # Tất cả video IDs duy nhất
        all_videos = sorted({
            vid for items in self.user_item_matrix.values()
            for vid in items.keys()
        })
        if not all_videos:
            return {}

        # ── Tạo numpy matrix (n_users × n_videos) ──
        video_to_idx = {vid: idx for idx, vid in enumerate(all_videos)}
        matrix = np.zeros((len(all_users), len(all_videos)))
        for i, user in enumerate(all_users):
            for video, weight in self.user_item_matrix[user].items():
                matrix[i][video_to_idx[video]] = weight

        # ── Cosine Similarity ──
        target_idx = all_users.index(target_user)
        target_vector = matrix[target_idx].reshape(1, -1)
        similarities = cosine_similarity(target_vector, matrix)[0]

        # ── K similar users (loại chính mình, loại sim ≤ 0) ──
        similar_indices = [
            idx for idx in np.argsort(similarities)[::-1]
            if idx != target_idx and similarities[idx] > 0
        ][:self.similar_users_k]

        # ── Weighted scores ──
        target_videos = set(self.user_item_matrix[target_user].keys())
        scores: Dict[str, float] = {}

        for idx in similar_indices:
            sim = similarities[idx]
            sim_user = all_users[idx]
            for video, weight in self.user_item_matrix[sim_user].items():
                if video not in target_videos:
                    scores[video] = scores.get(video, 0.0) + sim * weight

        # ── Normalize → [0, 1] ──
        if scores:
            max_score = max(scores.values())
            if max_score > 0:
                scores = {v: s / max_score for v, s in scores.items()}

        return scores

    def _popularity_based(self, exclude_user: str = None) -> Dict[str, float]:
        """
        Popularity-Based Ranking.
        Tổng trọng số tương tác từ tất cả users (loại trừ target user).
        Normalize → [0, 1].
        """
        video_scores: Dict[str, float] = defaultdict(float)

        for user, items in self.user_item_matrix.items():
            if user == exclude_user:
                continue
            for video, weight in items.items():
                video_scores[video] += weight

        if video_scores:
            max_score = max(video_scores.values())
            if max_score > 0:
                video_scores = {v: s / max_score for v, s in video_scores.items()}

        return dict(video_scores)

    def _hybrid_combine(
        self,
        collab_scores: Dict[str, float],
        popular_scores: Dict[str, float],
        user_id: str,
    ) -> Dict[str, float]:
        """
        Adaptive Hybrid Combiner.

        Formula: score = α × CF + (1-α) × Popularity
        α tự điều chỉnh theo số unique videos user đã tương tác.

        ┌──────────────────────┬───────┬──────────────────────────┐
        │ Unique Videos        │   α   │ Lý do                    │
        ├──────────────────────┼───────┼──────────────────────────┤
        │ < 3                  │  0.1  │ Quá ít → popularity      │
        │ 3 ~ 9                │  0.3  │ Ít → phần lớn popularity │
        │ 10 ~ 19              │  0.5  │ Vừa đủ → cân bằng       │
        │ 20 ~ 49              │  0.7  │ Nhiều → ưu tiên CF       │
        │ ≥ 50                 │  0.85 │ Rất nhiều → chủ yếu CF   │
        └──────────────────────┴───────┴──────────────────────────┘
        """
        n_interactions = len(self.user_item_matrix.get(user_id, {}))

        if n_interactions < 3:
            alpha = 0.1
        elif n_interactions < 10:
            alpha = 0.3
        elif n_interactions < 20:
            alpha = 0.5
        elif n_interactions < 50:
            alpha = 0.7
        else:
            alpha = 0.85

        logger.debug(
            f"Hybrid alpha={alpha} for user={user_id} "
            f"(n_interactions={n_interactions})"
        )

        all_videos = set(collab_scores.keys()) | set(popular_scores.keys())
        combined: Dict[str, float] = {}
        for video in all_videos:
            cf = collab_scores.get(video, 0.0)
            pop = popular_scores.get(video, 0.0)
            combined[video] = alpha * cf + (1 - alpha) * pop

        return combined

    def _get_popular_video_ids(self) -> List[str]:
        """Fallback: trả về video phổ biến nhất."""
        # Thử cache trước
        cached = self.redis_cache.get_popular_videos()
        if cached:
            return cached

        popular = self._popularity_based()
        sorted_videos = sorted(popular.items(), key=lambda x: x[1], reverse=True)
        result = [vid for vid, _ in sorted_videos[:self.top_n]]

        # Cache lại
        if result:
            self.redis_cache.cache_popular_videos(result)

        return result