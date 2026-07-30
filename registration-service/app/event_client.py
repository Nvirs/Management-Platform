from typing import Optional

import httpx

from app.config import get_settings


class EventClient:
    """Thin HTTP client for event-service's public read endpoints.

    Registration data isn't duplicated locally; event existence/capacity is
    looked up live via event-service's public GET /api/events/{id}.
    """

    def __init__(self, base_url: str, timeout: float = 5.0):
        self._base_url = base_url.rstrip("/")
        self._timeout = timeout

    def get_event(self, event_id: str) -> Optional[dict]:
        try:
            response = httpx.get(
                f"{self._base_url}/api/events/{event_id}", timeout=self._timeout
            )
        except httpx.HTTPError as exc:
            raise EventServiceUnavailableError() from exc

        if response.status_code == 404:
            return None
        if response.status_code != 200:
            raise EventServiceUnavailableError()

        return response.json()


class EventServiceUnavailableError(RuntimeError):
    def __init__(self):
        super().__init__("This event service is currently unavailable, please try again later")


def get_event_client() -> EventClient:
    return EventClient(get_settings().event_service_url)
