"""
Kafka Consumer: Consume UserInteractionEvent từ Interaction Service.

Note:
- Topic: "user-interactions"
- Serialization: json.loads() to parse
- Key: userId (string) — đảm bảo thứ tự per user
- confluent-kafka dùng blocking poll(), cần chạy trong thread riêng
  để không block asyncio event loop
"""

import asyncio
import json
import logging
from typing import Optional

from confluent_kafka import Consumer, KafkaError, KafkaException

from config import settings

logger = logging.getLogger(__name__)


class KafkaInteractionConsumer:
    """
    Kafka Consumer chạy trong background.

    Flow:
    1. Poll message từ Kafka (blocking, timeout=1s)
    2. Parse JSON → dict
    3. Gọi engine.add_interaction()
    4. Kiểm tra should_refresh() → compute nếu cần
    5. asyncio.sleep(0.01) để yield control cho event loop
    """

    def __init__(self, engine):
        """
        Args:
            engine: RecommendationEngine instance
        """
        self.engine = engine
        self._running = False
        self._consumer: Optional[Consumer] = None

    async def start(self) -> None:
        """
        Start consume loop.
        - confluent_kafka.Consumer.poll() là blocking call
        - Với timeout=1.0, nó chỉ block tối đa 1 giây
        - Kết hợp asyncio.sleep(0.01) để không starve event loop
        - auto.offset.reset=earliest: khi group mới → đọc từ đầu
          → giúp rebuild ma trận User-Item sau khi restart
        """
        self._running = True

        conf = {
            "bootstrap.servers": settings.KAFKA_BOOTSTRAP_SERVERS,
            "group.id": settings.KAFKA_GROUP_ID,
            "auto.offset.reset": "earliest",
            "enable.auto.commit": True,
            "auto.commit.interval.ms": 5000,
        }

        self._consumer = Consumer(conf)
        self._consumer.subscribe([settings.KAFKA_TOPIC_INTERACTIONS])

        logger.info(
            f"Kafka Consumer started | "
            f"servers={settings.KAFKA_BOOTSTRAP_SERVERS} | "
            f"topic={settings.KAFKA_TOPIC_INTERACTIONS} | "
            f"group={settings.KAFKA_GROUP_ID}"
        )

        try:
            while self._running:
                # Poll 1 message, timeout 1 giây
                msg = self._consumer.poll(timeout=1.0)

                if msg is None:
                    # Không có message mới
                    await asyncio.sleep(0.1)
                    continue

                if msg.error():
                    if msg.error().code() == KafkaError._PARTITION_EOF:
                        continue
                    else:
                        logger.error(f"Kafka error: {msg.error()}")
                        continue

                # Parse và xử lý
                try:
                    value = msg.value().decode("utf-8")
                    event = json.loads(value)
                    await self._process_event(event)
                except json.JSONDecodeError as e:
                    logger.error(f"Invalid JSON from Kafka: {e}")
                except Exception as e:
                    logger.error(f"Failed to process message: {e}", exc_info=True)

                # Yield control cho asyncio event loop
                await asyncio.sleep(0.01)

        except KafkaException as e:
            logger.error(f"Kafka exception: {e}")
        except asyncio.CancelledError:
            logger.info("Kafka consumer task cancelled")
        finally:
            if self._consumer:
                self._consumer.close()
            logger.info("Kafka Consumer stopped")

    async def _process_event(self, event: dict) -> None:
        """
        Xử lý 1 UserInteractionEvent.

        Expected event format:
        {
            "userId": "uuid-string",
            "videoId": "uuid-string",
            "interactionType": "VIEW|LIKE|UNLIKE|COMPLETE|SHARE|COMMENT|WATCH_TIME|SEARCH",
            "weight": 1.0,
            "metadata": { ... } | null,
            "timestamp": "2024-01-15T10:30:00Z"
        }
        """
        user_id = event.get("userId")
        video_id = event.get("videoId")
        interaction_type = event.get("interactionType", "")
        weight = float(event.get("weight", 1.0))

        if not user_id or not video_id:
            logger.warning(f"Skipping event with missing userId/videoId: {event}")
            return

        # Chuẩn hoá IDs về string (UUID thành string)
        user_id = str(user_id)
        video_id = str(video_id)

        logger.debug(
            f"Event: user={user_id[:8]}..., video={video_id[:8]}..., "
            f"type={interaction_type}, weight={weight}"
        )

        # Cập nhật ma trận User-Item
        self.engine.add_interaction(user_id, video_id, weight)

        # Kiểm tra có cần refresh không
        if self.engine.should_refresh(user_id):
            logger.info(f"Triggering refresh for user: {user_id[:8]}...")
            await asyncio.to_thread(
                self.engine.compute_recommendations, user_id
            )

    def stop(self) -> None:
        """Dừng consumer loop."""
        self._running = False
        logger.info("Kafka Consumer stopping...")
