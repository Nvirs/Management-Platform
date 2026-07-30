from datetime import datetime

from pydantic import BaseModel, ConfigDict


class RegistrationRequest(BaseModel):
    event_id: str


class RegistrationResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: str
    event_id: str
    user_email: str
    status: str
    created_at: datetime
    updated_at: datetime