package com.eventplatform.event.exception;

public class NotEventOwnerException extends RuntimeException {

	public NotEventOwnerException() {
		super("Csak a saját eseményeidet módosíthatod vagy törölheted");
	}
}