import json
import logging

import pika

from app.config import get_settings
from app.models import Registration

logger = logging.getLogger(__name__)

EXCHANGE = "events"
EXCHANGE_TYPE = "topic"


class EventPublisher:
    """Thin AMQP publisher for domain events, backed by pika.

    A connection is opened and closed per publish rather than kept alive
    across requests, since pika's BlockingConnection isn't safe to share
    across the threads FastAPI uses for sync endpoints.
    """

    def __init__(self, url: str):
        self._url = url

    def publish_registration_confirmed(self, registration: Registration) -> None:
        payload = {
            "registrationId": registration.id,
            "eventId": registration.event_id,
            "userEmail": registration.user_email,
            "confirmedAt": registration.created_at.isoformat(),
        }
        self._publish("registration.confirmed", payload)

    def _publish(self, routing_key: str, payload: dict) -> None:
        connection = pika.BlockingConnection(pika.URLParameters(self._url))
        try:
            channel = connection.channel()
            channel.exchange_declare(
                exchange=EXCHANGE, exchange_type=EXCHANGE_TYPE, durable=True
            )
            channel.basic_publish(
                exchange=EXCHANGE,
                routing_key=routing_key,
                body=json.dumps(payload).encode("utf-8"),
                properties=pika.BasicProperties(
                    content_type="application/json", delivery_mode=2
                ),
            )
        finally:
            connection.close()


def get_event_publisher() -> EventPublisher:
    return EventPublisher(get_settings().rabbitmq_url)
