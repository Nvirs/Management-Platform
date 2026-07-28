package com.eventplatform.auth.service;

import com.eventplatform.auth.dto.AuthResponse;
import com.eventplatform.auth.dto.LoginRequest;
import com.eventplatform.auth.dto.RegisterRequest;
import com.eventplatform.auth.exception.EmailAlreadyUsedException;
import com.eventplatform.auth.exception.InvalidCredentialsException;
import com.eventplatform.auth.model.User;
import com.eventplatform.auth.repository.UserRepository;
import com.eventplatform.auth.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new EmailAlreadyUsedException(request.email());
		}

		User user = User.builder()
				.email(request.email())
				.password(passwordEncoder.encode(request.password()))
				.build();
		userRepository.save(user);

		return AuthResponse.bearer(jwtService.generateToken(user.getEmail()));
	}

	public AuthResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.email())
				.orElseThrow(InvalidCredentialsException::new);

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new InvalidCredentialsException();
		}

		return AuthResponse.bearer(jwtService.generateToken(user.getEmail()));
	}
}
