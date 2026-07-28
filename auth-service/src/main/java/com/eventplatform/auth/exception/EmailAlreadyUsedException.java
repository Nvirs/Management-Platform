package com.eventplatform.auth.exception;

public class EmailAlreadyUsedException extends RuntimeException {

	public EmailAlreadyUsedException(String email) {
		super("This email is already in use: " + email);
	}
}
