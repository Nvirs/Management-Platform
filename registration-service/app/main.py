from contextlib import asynccontextmanager
from datetime import datetime, timezone

from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.database import Base, engine
from app.exceptions import (
    AlreadyCancelledException,
    AlreadyRegisteredException,
    EventFullException,
    EventNotFoundException,
    NotEventOwnerException,
    NotRegistrationOwnerException,
    RegistrationNotFoundException,
)
from app.event_client import EventServiceUnavailableError
from app.routers.registrations import router as registrations_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    Base.metadata.create_all(bind=engine)
    yield


app = FastAPI(title="Registration Service", lifespan=lifespan)
app.include_router(registrations_router)


def _error_response(status_code: int, message: str) -> JSONResponse:
    return JSONResponse(
        status_code=status_code,
        content={
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "status": status_code,
            "error": message,
        },
    )


@app.exception_handler(EventNotFoundException)
@app.exception_handler(RegistrationNotFoundException)
def handle_not_found(request: Request, exc: Exception) -> JSONResponse:
    return _error_response(404, str(exc))


@app.exception_handler(AlreadyRegisteredException)
@app.exception_handler(EventFullException)
@app.exception_handler(AlreadyCancelledException)
def handle_conflict(request: Request, exc: Exception) -> JSONResponse:
    return _error_response(409, str(exc))


@app.exception_handler(NotRegistrationOwnerException)
@app.exception_handler(NotEventOwnerException)
def handle_forbidden(request: Request, exc: Exception) -> JSONResponse:
    return _error_response(403, str(exc))


@app.exception_handler(EventServiceUnavailableError)
def handle_event_service_unavailable(request: Request, exc: Exception) -> JSONResponse:
    return _error_response(502, str(exc))


@app.exception_handler(HTTPException)
def handle_http_exception(request: Request, exc: HTTPException) -> JSONResponse:
    return _error_response(exc.status_code, str(exc.detail))


@app.exception_handler(RequestValidationError)
def handle_validation_error(request: Request, exc: RequestValidationError) -> JSONResponse:
    first_error = exc.errors()[0]
    field = ".".join(str(part) for part in first_error["loc"] if part != "body")
    return _error_response(400, f"{field}: {first_error['msg']}")
