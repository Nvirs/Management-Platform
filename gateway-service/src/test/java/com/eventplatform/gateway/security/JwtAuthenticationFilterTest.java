package com.eventplatform.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

class JwtAuthenticationFilterTest {

	private static final String SECRET = "test-only-secret-key-for-gateway-service-unit-tests-1234";

	private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(new JwtService(SECRET));

	private ServerWebExchange forwarded;

	private final GatewayFilterChain chain = exchange -> {
		this.forwarded = exchange;
		return Mono.empty();
	};

	private String token(String subject, long ttlMs) {
		SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
		return Jwts.builder()
				.subject(subject)
				.issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + ttlMs))
				.signWith(key)
				.compact();
	}

	@Test
	void protectedRouteWithoutTokenReturns401() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.post("/api/registrations"));

		filter.filter(exchange, chain).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(forwarded).isNull();
	}

	@Test
	void protectedRouteWithInvalidTokenReturns401() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.post("/api/registrations")
						.header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"));

		filter.filter(exchange, chain).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(forwarded).isNull();
	}

	@Test
	void protectedRouteWithExpiredTokenReturns401() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.post("/api/registrations")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token("user@example.com", -1000)));

		filter.filter(exchange, chain).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void protectedRouteWithValidTokenForwardsWithUserHeader() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.post("/api/registrations")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token("user@example.com", 60_000)));

		filter.filter(exchange, chain).block();

		assertThat(forwarded).isNotNull();
		assertThat(forwarded.getRequest().getHeaders()
				.getFirst(JwtAuthenticationFilter.USER_EMAIL_HEADER)).isEqualTo("user@example.com");
	}

	@Test
	void publicLoginRouteSkipsAuth() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.post("/api/auth/login"));

		filter.filter(exchange, chain).block();

		assertThat(forwarded).isNotNull();
	}

	@Test
	void publicEventReadSkipsAuthButParticipantsListDoesNot() {
		MockServerWebExchange listEvents = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/events"));
		filter.filter(listEvents, chain).block();
		assertThat(forwarded).isNotNull();

		forwarded = null;
		MockServerWebExchange participants = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/events/evt-1/registrations"));
		filter.filter(participants, chain).block();

		assertThat(participants.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(forwarded).isNull();
	}

	@Test
	void inboundUserEmailHeaderIsStrippedOnPublicRoute() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/events")
						.header(JwtAuthenticationFilter.USER_EMAIL_HEADER, "admin@evil.example"));

		filter.filter(exchange, chain).block();

		assertThat(forwarded.getRequest().getHeaders()
				.getFirst(JwtAuthenticationFilter.USER_EMAIL_HEADER)).isNull();
	}
}
