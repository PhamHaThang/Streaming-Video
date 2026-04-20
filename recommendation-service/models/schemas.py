"""
Pydantic models cho API request/response.

All responses follow the platform's standard format.
"""

from typing import List, Optional
from pydantic import BaseModel, Field


class RecommendationResponse(BaseModel):
    """Response cho GET /api/recommendations."""
    user_id: str = Field(description="UUID của user")
    video_ids: List[str] = Field(description="Danh sách UUID video gợi ý")
    count: int = Field(description="Số lượng video trả về")
    source: str = Field(description="Source: hybrid | popular | cache")


class SimilarVideosResponse(BaseModel):
    """Response cho GET /api/recommendations/similar/{videoId}."""
    video_id: str = Field(description="UUID video gốc")
    similar_video_ids: List[str] = Field(description="Danh sách video tương tự")
    count: int = Field(description="Số lượng")


class HealthResponse(BaseModel):
    """Response cho health check."""
    status: str = "UP"
    service: str = "Recommendation Service"