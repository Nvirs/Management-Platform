package com.eventplatform.gateway.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

	// User identity resolved from the token, forwarded to downstream services.
	public static final String USER_EMAIL_HEADER = "X-User-Email";

	private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

	private record PublicRoute(HttpMethod method, String pattern) {
		boolean matches(HttpMethod requestMethod, String path) {
			return (method == null || method.equals(requestMethod))
					&& PATH_MATCHER.match(pattern, path);
		}
	}

	private static final List<PublicRoute> PUBLIC_ROUTES = List.of(
			new PublicRoute(HttpMethod.POST, "/api/auth/login"),
			new PublicRoute(HttpMethod.POST, "/api/auth/register"),
			new PublicRoute(HttpMethod.GET, "/api/events"),
			new PublicRoute(HttpMethod.GET, "/api/events/*"),
			new PublicRoute(null, "/actuator/**")
	);

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		ServerHttpRequest stripped = request.mutate()
				.headers(headers -> headers.remove(USER_EMAIL_HEADER))
				.build();

		if (isPublic(stripped)) {
			return chain.filter(exchange.mutate().request(stripped).build());
		}

		String authHeader = stripped.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			return unauthorized(exchange, "Missing or malformed Authorization header");
		}

		Optional<String> subject = jwtService.extractSubject(authHeader.substring(7));
		if (subject.isEmpty()) {
			return unauthorized(exchange, "Invalid or expired token");
		}

		ServerHttpRequest mutated = stripped.mutate()
				.headers(headers -> headers.set(USER_EMAIL_HEADER, subject.get()))
				.build();
		return chain.filter(exchange.mutate().request(mutated).build());
	}

	private boolean isPublic(ServerHttpRequest request) {
		if (request.getMethod() == HttpMethod.OPTIONS) {
			return true;
		}
		String path = request.getPath().pathWithinApplication().value();
		HttpMethod method = request.getMethod();
		return PUBLIC_ROUTES.stream().anyMatch(route -> route.matches(method, path));
	}

	private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(HttpStatus.UNAUTHORIZED);
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

		String body = "{\"timestamp\":\"" + Instant.now()
				+ "\",\"status\":401,\"error\":\"" + message + "\"}";
		DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
		return response.writeWith(Mono.just(buffer));
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
}
