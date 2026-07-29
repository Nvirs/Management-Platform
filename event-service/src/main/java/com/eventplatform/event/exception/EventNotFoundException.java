package com.eventplatform.event.exception;

import java.util.UUID;

public class EventNotFoundException extends RuntimeException {

	public EventNotFoundException(UUID id) {
		super("Nincs ilyen esemény: " + id);
	}
}