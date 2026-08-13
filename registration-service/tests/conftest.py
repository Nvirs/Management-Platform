from typing import Optional

import jwt
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.config import get_settings
from app.database import Base, get_db
from app.event_client import get_event_client
from app.event_publisher import get_event_publisher
from app.main import app

engine = create_engine(
    "sqlite:///:memory:",
    connect_args={"check_same_thread": False},
    poolclass=StaticPool,
)
TestSessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)


def override_get_db():
    db = TestSessionLocal()
    try:
        yield db
    finally:
        db.close()


class FakeEventClient:
    """In-memory stand-in for event-service, so tests don't need it running."""

    def __init__(self):
        self.events: dict[str, dict] = {}

    def add_event(self, event_id: str, organizer_email: str, capacity: Optional[int] = None):
        self.events[event_id] = {
            "id": event_id,
            "organizerEmail": organizer_email,
            "capacity": capacity,
        }

    def get_event(self, event_id: str) -> Optional[dict]:
        return self.events.get(event_id)


class FakeEventPublisher:
    """In-memory stand-in for the RabbitMQ publisher, so tests don't need a broker."""

    def __init__(self):
        self.published: list[tuple[str, dict]] = []
        self.raise_on_publish = False

    def publish_registration_confirmed(self, registration):
        if self.raise_on_publish:
            raise RuntimeError("RabbitMQ unavailable")
        self.published.append(
            (
                "registration.confirmed",
                {
                    "registrationId": registration.id,
                    "eventId": registration.event_id,
                    "userEmail": registration.user_email,
                },
            )
        )


fake_event_client = FakeEventClient()
fake_event_publisher = FakeEventPublisher()

app.dependency_overrides[get_db] = override_get_db
app.dependency_overrides[get_event_client] = lambda: fake_event_client
app.dependency_overrides[get_event_publisher] = lambda: fake_event_publisher


@pytest.fixture(autouse=True)
def _reset_state():
    Base.metadata.create_all(bind=engine)
    fake_event_client.events.clear()
    fake_event_publisher.published.clear()
    fake_event_publisher.raise_on_publish = False
    yield
    Base.metadata.drop_all(bind=engine)


@pytest.fixture
def client():
    return TestClient(app)


@pytest.fixture
def events():
    return fake_event_client


@pytest.fixture
def publisher():
    return fake_event_publisher


def make_token(email: str) -> str:
    return jwt.encode({"sub": email}, get_settings().jwt_secret, algorithm="HS384")


@pytest.fixture
def auth_header():
    def _make(email: str):
        return {"Authorization": f"Bearer {make_token(email)}"}

    return _make
