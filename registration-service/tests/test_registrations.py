def test_create_registration_requires_auth(client, events):
    events.add_event("event-1", organizer_email="organizer@example.com")

    response = client.post("/api/registrations", json={"event_id": "event-1"})

    assert response.status_code == 401


def test_create_registration_unknown_event_returns_404(client, auth_header):
    response = client.post(
        "/api/registrations",
        json={"event_id": "missing-event"},
        headers=auth_header("attendee@example.com"),
    )

    assert response.status_code == 404


def test_create_registration_success(client, events, auth_header):
    events.add_event("event-1", organizer_email="organizer@example.com")

    response = client.post(
        "/api/registrations",
        json={"event_id": "event-1"},
        headers=auth_header("attendee@example.com"),
    )

    assert response.status_code == 201
    body = response.json()
    assert body["event_id"] == "event-1"
    assert body["user_email"] == "attendee@example.com"
    assert body["status"] == "REGISTERED"


def test_duplicate_registration_returns_409(client, events, auth_header):
    events.add_event("event-1", organizer_email="organizer@example.com")
    headers = auth_header("attendee@example.com")
    client.post("/api/registrations", json={"event_id": "event-1"}, headers=headers)

    response = client.post("/api/registrations", json={"event_id": "event-1"}, headers=headers)

    assert response.status_code == 409


def test_full_event_returns_409(client, events, auth_header):
    events.add_event("event-1", organizer_email="organizer@example.com", capacity=1)
    client.post(
        "/api/registrations",
        json={"event_id": "event-1"},
        headers=auth_header("first@example.com"),
    )

    response = client.post(
        "/api/registrations",
        json={"event_id": "event-1"},
        headers=auth_header("second@example.com"),
    )

    assert response.status_code == 409


def test_list_my_registrations_only_returns_own(client, events, auth_header):
    events.add_event("event-1", organizer_email="organizer@example.com")
    client.post(
        "/api/registrations", json={"event_id": "event-1"}, headers=auth_header("me@example.com")
    )
    client.post(
        "/api/registrations",
        json={"event_id": "event-1"},
        headers=auth_header("someone-else@example.com"),
    )

    response = client.get("/api/registrations/me", headers=auth_header("me@example.com"))

    assert response.status_code == 200
    body = response.json()
    assert len(body) == 1
    assert body[0]["user_email"] == "me@example.com"


def test_cancel_registration_as_owner(client, events, auth_header):
    events.add_event("event-1", organizer_email="organizer@example.com")
    headers = auth_header("attendee@example.com")
    create_response = client.post(
        "/api/registrations", json={"event_id": "event-1"}, headers=headers
    )
    registration_id = create_response.json()["id"]

    response = client.delete(f"/api/registrations/{registration_id}", headers=headers)

    assert response.status_code == 204

    second_cancel = client.delete(f"/api/registrations/{registration_id}", headers=headers)
    assert second_cancel.status_code == 409


def test_cancel_registration_as_non_owner_forbidden(client, events, auth_header):
    events.add_event("event-1", organizer_email="organizer@example.com")
    create_response = client.post(
        "/api/registrations",
        json={"event_id": "event-1"},
        headers=auth_header("attendee@example.com"),
    )
    registration_id = create_response.json()["id"]

    response = client.delete(
        f"/api/registrations/{registration_id}", headers=auth_header("intruder@example.com")
    )

    assert response.status_code == 403


def test_cancel_unknown_registration_returns_404(client, auth_header):
    response = client.delete(
        "/api/registrations/does-not-exist", headers=auth_header("attendee@example.com")
    )

    assert response.status_code == 404


def test_list_event_registrations_as_organizer(client, events, auth_header):
    events.add_event("event-1", organizer_email="organizer@example.com")
    client.post(
        "/api/registrations",
        json={"event_id": "event-1"},
        headers=auth_header("attendee@example.com"),
    )

    response = client.get(
        "/api/events/event-1/registrations", headers=auth_header("organizer@example.com")
    )

    assert response.status_code == 200
    assert len(response.json()) == 1


def test_list_event_registrations_as_non_organizer_forbidden(client, events, auth_header):
    events.add_event("event-1", organizer_email="organizer@example.com")

    response = client.get(
        "/api/events/event-1/registrations", headers=auth_header("someone-else@example.com")
    )

    assert response.status_code == 403
