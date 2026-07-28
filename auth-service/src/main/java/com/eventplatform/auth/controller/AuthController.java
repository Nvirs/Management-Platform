package com.eventplatform.auth.controller;

import com.eventplatform.auth.dto.AuthResponse;
import com.eventplatform.auth.dto.LoginRequest;
import com.eventplatform.auth.dto.RegisterRequest;
import com.eventplatform.auth.dto.UserResponse;
import com.eventplatform.auth.repository.UserRepository;
import com.eventplatform.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final UserRepository userRepository;

	public AuthController(AuthService authService, UserRepository userRepository) {
		this.authService = authService;
		this.userRepository = userRepository;
	}

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@GetMapping("/me")
	public ResponseEntity<UserResponse> me(Authentication authentication) {
		String email = authentication.getName();
		return userRepository.findByEmail(email)
				.map(user -> ResponseEntity.ok(UserResponse.from(user)))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
}
