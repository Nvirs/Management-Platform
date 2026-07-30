class EventNotFoundException(Exception):
    def __init__(self, event_id: str):
        super().__init__(f"This event was not found: {event_id}")


class EventFullException(Exception):
    def __init__(self):
        super().__init__("This event is full, there are no more available spots")


class AlreadyRegisteredException(Exception):
    def __init__(self):
        super().__init__("You are already registered for this event")


class RegistrationNotFoundException(Exception):
    def __init__(self, registration_id: str):
        super().__init__(f"This registration was not found: {registration_id}")


class NotRegistrationOwnerException(Exception):
    def __init__(self):
        super().__init__("You can only cancel your own registration")


class NotEventOwnerException(Exception):
    def __init__(self):
        super().__init__("You can only view registrations for your own events")


class AlreadyCancelledException(Exception):
    def __init__(self):
        super().__init__("This registration is already cancelled")
