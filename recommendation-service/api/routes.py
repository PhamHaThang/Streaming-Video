"""
API Routes cho Recommendation Service.

Authentication: JWT được validate bởi API Gateway.
Gateway truyền user ID qua header X-User-Id.

Routes:
    GET  /api/recommendations                 → Personalized recommendations
    GET  /api/recommendations/similar/{id}    → Similar videos
    POST /api/recommendations/refresh         → Force refresh
"""

import logging
from typing import Optional

from fastapi import APIRouter, HTTPException, Header, Query, Request

from models.schemas import RecommendationResponse, SimilarVideosResponse

router = APIRouter()
logger = logging.getLogger(__name__)


def _get_engine(request: Request):
    """Helper: Get RecommendationEngine from app state."""
    return request.app.state.engine


@router.get("", response_model=RecommendationResponse)
async def get_recommendations(
    request: Request,
    x_user_id: str = Header(..., alias="X-User-Id"),
    limit: int = Query(default=20, ge=1, le=50),
):
    """
    GET /api/recommendations

    Get personalized video recommendations.

    Flow:
    1. Đọc từ Redis cache (fast path, < 1ms)
    2. Cache miss → tính toán real-time
    3. Fallback → popular videos (cho user mới)

    Headers:
    - X-User-Id: UUID (set bởi API Gateway sau khi validate JWT)

    Query params:
    - limit: 1-50, default 20
    """
    engine = _get_engine(request)

    try:
        video_ids, source = engine.get_recommendations(x_user_id)
        video_ids = video_ids[:limit]

        logger.info(
            f"GET /recommendations: user={x_user_id[:8]}..., "
            f"count={len(video_ids)}, source={source}"
        )

        return RecommendationResponse(
            user_id=x_user_id,
            video_ids=video_ids,
            count=len(video_ids),
            source=source,
        )

    except Exception as e:
        logger.error(f"Failed to get recommendations: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Lỗi khi lấy gợi ý")


@router.get("/similar/{video_id}", response_model=SimilarVideosResponse)
async def get_similar_videos(
    request: Request,
    video_id: str,
    limit: int = Query(default=10, ge=1, le=30),
):
    """
    GET /api/recommendations/similar/{videoId}

    Lấy danh sách video tương tự (Item-based CF).
    Dùng cho sidebar "Video liên quan" khi xem video.

    No auth.
    """
    engine = _get_engine(request)

    try:
        similar_ids = engine.get_similar_videos(video_id, top_n=limit)

        return SimilarVideosResponse(
            video_id=video_id,
            similar_video_ids=similar_ids,
            count=len(similar_ids),
        )

    except Exception as e:
        logger.error(f"Failed to get similar videos: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Lỗi khi lấy video tương tự")


@router.post("/refresh")
async def refresh_recommendations(
    request: Request,
    x_user_id: str = Header(..., alias="X-User-Id"),
):
    """
    POST /api/recommendations/refresh

    Force refresh recommendations cho user.
    1. Xóa cache cũ
    2. Tính toán lại từ ma trận User-Item hiện tại
    3. Cache kết quả mới
    """
    engine = _get_engine(request)

    try:
        engine.redis_cache.invalidate_user_cache(x_user_id)
        video_ids = engine.compute_recommendations(x_user_id)

        return {
            "success": True,
            "message": "Recommendations refreshed",
            "count": len(video_ids),
        }

    except Exception as e:
        logger.error(f"Failed to refresh: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Lỗi khi refresh gợi ý")
