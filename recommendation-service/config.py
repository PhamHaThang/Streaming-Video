"""
Cấu hình cho Recommendation Service.
Đọc từ biến môi trường hoặc file .env, fallback về giá trị mặc định.
- KAFKA_BOOTSTRAP_SERVERS: Trong Docker dùng "kafka:9092", local dùng "localhost:29092"
- REDIS_HOST: Trong Docker dùng "redis", local dùng "localhost"
"""

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Application configuration."""

    # ── Application ──
    APP_NAME: str = "Recommendation Service"
    APP_PORT: int = 8085
    DEBUG: bool = True

    # ── Kafka ──
    # Local: localhost:29092 (mapped port trong docker-compose)
    # Docker: kafka:9092 (internal network)
    KAFKA_BOOTSTRAP_SERVERS: str = "localhost:29092"
    KAFKA_GROUP_ID: str = "recommendation-group"
    KAFKA_TOPIC_INTERACTIONS: str = "user-interactions"

    # ── Redis ──
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379
    REDIS_PASSWORD: str = ""
    REDIS_DB: int = 0
    RECOMMENDATION_TTL: int = 3600  # Cache TTL: 1 giờ (giây)

    # ── Recommendation Algorithm Settings ──
    TOP_N_RECOMMENDATIONS: int = 20         # Số video gợi ý tối đa
    SIMILAR_USERS_K: int = 10               # K nearest neighbors cho CF
    MIN_INTERACTIONS: int = 5               # Tối thiểu events mới để trigger refresh
    REFRESH_INTERVAL_SECONDS: int = 300     # Auto-refresh mỗi 5 phút

    # ── External Services ──
    VIDEO_SERVICE_URL: str = "http://localhost:8082"

    class Config:
        env_file = ".env"
        extra = "ignore"


settings = Settings()