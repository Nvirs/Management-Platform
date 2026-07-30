from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.event_client import EventClient, get_event_client
from app.exceptions import (
    AlreadyCancelledException,
    AlreadyRegisteredException,
    EventFullException,
    EventNotFoundException,
    NotEventOwnerException,
    NotRegistrationOwnerException,
    RegistrationNotFoundException,
)
from app.models import CANCELLED, REGISTERED, Registration
from app.schemas import RegistrationRequest, RegistrationResponse
from app.security import get_current_user_email

router = APIRouter(tags=["registrations"])


@router.post(
    "/api/registrations",
    response_model=RegistrationResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_registration(
    request: RegistrationRequest,
    user_email: str = Depends(get_current_user_email),
    db: Session = Depends(get_db),
    event_client: EventClient = Depends(get_event_client),
):
    event = event_client.get_event(request.event_id)
    if event is None:
        raise EventNotFoundException(request.event_id)

    already_registered = (
        db.query(Registration)
        .filter(
            Registration.event_id == request.event_id,
            Registration.user_email == user_email,
            Registration.status == REGISTERED,
        )
        .first()
        is not None
    )
    if already_registered:
        raise AlreadyRegisteredException()

    capacity = event.get("capacity")
    if capacity is not None:
        current_total = (
            db.query(Registration)
            .filter(
                Registration.event_id == request.event_id,
                Registration.status == REGISTERED,
            )
            .count()
        )
        if current_total >= capacity:
            raise EventFullException()

    registration = Registration(event_id=request.event_id, user_email=user_email)
    db.add(registration)
    db.commit()
    db.refresh(registration)
    return registration


@router.get("/api/registrations/me", response_model=list[RegistrationResponse])
def list_my_registrations(
    user_email: str = Depends(get_current_user_email),
    db: Session = Depends(get_db),
):
    return (
        db.query(Registration)
        .filter(Registration.user_email == user_email)
        .order_by(Registration.created_at.desc())
        .all()
    )


@router.delete("/api/registrations/{registration_id}", status_code=status.HTTP_204_NO_CONTENT)
def cancel_registration(
    registration_id: str,
    user_email: str = Depends(get_current_user_email),
    db: Session = Depends(get_db),
):
    registration = db.get(Registration, registration_id)
    if registration is None:
        raise RegistrationNotFoundException(registration_id)
    if registration.user_email != user_email:
        raise NotRegistrationOwnerException()
    if registration.status != REGISTERED:
        raise AlreadyCancelledException()

    registration.status = CANCELLED
    db.commit()


@router.get(
    "/api/events/{event_id}/registrations",
    response_model=list[RegistrationResponse],
)
def list_event_registrations(
    event_id: str,
    user_email: str = Depends(get_current_user_email),
    db: Session = Depends(get_db),
    event_client: EventClient = Depends(get_event_client),
):
    event = event_client.get_event(event_id)
    if event is None:
        raise EventNotFoundException(event_id)
    if event.get("organizerEmail") != user_email:
        raise NotEventOwnerException()

    return (
        db.query(Registration)
        .filter(Registration.event_id == event_id)
        .order_by(Registration.created_at.desc())
        .all()
    )
