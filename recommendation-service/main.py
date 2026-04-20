"""
Recommendation Service — FastAPI Application.

Entry point:
1. Khởi tạo Redis, RecommendationEngine, Kafka Consumer
2. Expose REST API cho recommendations
3. Health check endpoint tương thích Spring Actuator format

Lifecycle:
- Startup: Redis → Engine → Kafka Consumer (background task)
- Shutdown: Stop consumer → Close Redis
"""

import asyncio
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from api.routes import router
from config import settings
from services.kafka_consumer import KafkaInteractionConsumer
from services.recommendation_engine import RecommendationEngine
from services.redis_cache import RedisCache

# ═══════════════════════════════════════
# Logging Configuration
# ═══════════════════════════════════════
logging.basicConfig(
    level=logging.DEBUG if settings.DEBUG else logging.INFO,
    format="%(asctime)s | %(levelname)-8s | %(name)s | %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger(__name__)


# ═══════════════════════════════════════
# Application Lifespan
# ═══════════════════════════════════════
@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Modern FastAPI lifecycle (thay @app.on_event).

    Startup:
    1. Khởi tạo RedisCache → test connection
    2. Khởi tạo RecommendationEngine (empty matrix)
    3. Start KafkaInteractionConsumer (background asyncio task)
       → consumer sẽ replay events từ Kafka và rebuild matrix

    Shutdown:
    1. Stop Kafka consumer
    2. Cancel consumer task
    3. Close Redis connection
    """
    # ── Startup ──
    logger.info("═" * 50)
    logger.info(f"  Starting {settings.APP_NAME}")
    logger.info(f"  Port: {settings.APP_PORT}")
    logger.info(f"  Kafka: {settings.KAFKA_BOOTSTRAP_SERVERS}")
    logger.info(f"  Redis: {settings.REDIS_HOST}:{settings.REDIS_PORT}")
    logger.info("═" * 50)

    # 1. Redis
    redis_cache = RedisCache()
    app.state.redis_cache = redis_cache

    # 2. Recommendation Engine
    engine = RecommendationEngine(redis_cache)
    app.state.engine = engine

    # 3. Kafka Consumer (background)
    kafka_consumer = KafkaInteractionConsumer(engine)
    consumer_task = asyncio.create_task(kafka_consumer.start())
    app.state.kafka_consumer = kafka_consumer

    logger.info("All services initialized successfully")

    yield

    # ── Shutdown ──
    logger.info("Shutting down...")
    kafka_consumer.stop()
    consumer_task.cancel()
    try:
        await consumer_task
    except asyncio.CancelledError:
        pass
    redis_cache.close()
    logger.info("Shutdown complete")


# ═══════════════════════════════════════
# FastAPI App
# ═══════════════════════════════════════
app = FastAPI(
    title="Video Recommendation Service",
    description="Video recommendation engine for streaming platform",
    version="1.0.0",
    lifespan=lifespan,
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# API Routes
app.include_router(router, prefix="/api/recommendations")


# Health check
@app.get("/actuator/health")
async def health():
    """
    Health check endpoint.
    """
    return {"status": "UP", "service": settings.APP_NAME}


# ═══════════════════════════════════════
# Run directly (dev mode)
# ═══════════════════════════════════════
if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=settings.APP_PORT,
        reload=settings.DEBUG,
    )