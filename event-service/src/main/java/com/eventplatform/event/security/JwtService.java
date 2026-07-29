package com.eventplatform.event.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Verifies JWTs issued by auth-service. Both services share the same JWT_SECRET
 * so event-service can validate tokens locally without calling back to auth-service.
 */
@Service
public class JwtService {

	private final SecretKey signingKey;

	public JwtService(@Value("${jwt.secret}") String secret) {
		this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	public Optional<String> extractSubject(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(signingKey)
					.build()
					.parseSignedClaims(token)
					.getPayload();
			return Optional.of(claims.getSubject());
		} catch (JwtException | IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}