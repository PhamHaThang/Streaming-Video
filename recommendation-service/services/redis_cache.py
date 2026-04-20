"""
Redis Cache Service
- Luu tru ket qua recommendations da tinh toan
- Giup serve API nhanh hon ma khong can tinh toan lai moi lan co request moi
Redis Key Pattern:
    rec:user:{userId}     → JSON array of video IDs (TTL = RECOMMENDATION_TTL)
    rec:popular           → JSON array of popular video IDs (TTL = RECOMMENDATION_TTL)
"""
import json
import logging
from typing import List, Optional

import redis

from config import settings

logger = logging.getLogger(__name__)
class RedisCache:
    """Quản lý cache recommendations bằng Redis."""

    def __init__(self):
        """
        Khởi tạo Redis connection.
        """
        self._client = redis.Redis(
            host=settings.REDIS_HOST,
            port=settings.REDIS_PORT,
            password=settings.REDIS_PASSWORD or None,
            db=settings.REDIS_DB,
            decode_responses=True,
            socket_connect_timeout=5,
            retry_on_timeout=True,
        )

        # Test connection
        try:
            self._client.ping()
            logger.info(
                f"Redis connected: {settings.REDIS_HOST}:{settings.REDIS_PORT}"
            )
        except redis.ConnectionError as e:
            logger.warning(f"Redis connection failed (will retry): {e}")

    def cache_recommendations(self, user_id: str, video_ids: List[str]) -> None:
        """
        Cache danh sách video gợi ý cho user.
        Key: rec:user:{userId}
        Value: JSON array of video_id strings
        TTL: RECOMMENDATION_TTL (default 3600s = 1 giờ)
        """
        key = f"rec:user:{user_id}"
        try:
            self._client.setex(
                key,
                settings.RECOMMENDATION_TTL,
                json.dumps(video_ids),
            )
            logger.debug(f"Cached {len(video_ids)} recs for user {user_id}")
        except Exception as e:
            logger.error(f"Failed to cache recommendations: {e}")

    def get_recommendations(self, user_id: str) -> Optional[List[str]]:
        """
        Đọc recommendations từ cache.
        Trả về None nếu cache miss hoặc expired.
        """
        key = f"rec:user:{user_id}"
        try:
            data = self._client.get(key)
            if data:
                return json.loads(data)
            return None
        except Exception as e:
            logger.error(f"Failed to read cache: {e}")
            return None

    def invalidate_user_cache(self, user_id: str) -> None:
        """Xóa cache của user (force refresh)."""
        key = f"rec:user:{user_id}"
        try:
            self._client.delete(key)
            logger.debug(f"Invalidated cache for user {user_id}")
        except Exception as e:
            logger.error(f"Failed to invalidate cache: {e}")

    def cache_popular_videos(self, video_ids: List[str]) -> None:
        """Cache danh sách video phổ biến (global fallback)."""
        try:
            self._client.setex(
                "rec:popular",
                settings.RECOMMENDATION_TTL,
                json.dumps(video_ids),
            )
            logger.debug(f"Cached {len(video_ids)} popular videos")
        except Exception as e:
            logger.error(f"Failed to cache popular videos: {e}")

    def get_popular_videos(self) -> Optional[List[str]]:
        """Lấy danh sách video phổ biến từ cache."""
        try:
            data = self._client.get("rec:popular")
            if data:
                return json.loads(data)
            return None
        except Exception as e:
            logger.error(f"Failed to read popular cache: {e}")
            return None

    def close(self) -> None:
        """Đóng kết nối Redis."""
        try:
            self._client.close()
            logger.info("Redis connection closed")
        except Exception as e:
            logger.error(f"Failed to close Redis connection: {e}")