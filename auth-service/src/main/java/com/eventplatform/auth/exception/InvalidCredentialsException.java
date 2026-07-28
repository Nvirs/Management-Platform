package com.eventplatform.auth.exception;

public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Hibás email cím vagy jelszó");
	}
}
