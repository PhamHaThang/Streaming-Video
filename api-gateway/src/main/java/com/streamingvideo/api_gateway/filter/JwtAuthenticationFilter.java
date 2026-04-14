package com.streamingvideo.api_gateway.filter;

import com.streamingvideo.api_gateway.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * JWT Authentication Filter validate JWT for any request
 * Flow:
 * 1. Check path in PUBLIC_ENDPOINST?
 * 2. If public -> pass
 * 3. If no public -> extact and validate JWT token
 * 4. If token valid -> add info user into header
 * 5. If token no valid -> 401 Unauthorized
 */
@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    private final JwtUtil jwtUtil;
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh-token",
            "/api/videos/public",
            "/api/videos/trending",
            "/actuator/health");

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("Gateway Filter - Path: {} | Method: {}", path, request.getMethod());

        // --- Step 1: Check Public Endpoint ---
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }
        // --- Step 2: Extract Authorization Header ---
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return onError(exchange, "Missing or invalid Authorization header",
                    HttpStatus.UNAUTHORIZED);
        }
        // --- Step 3: Validate JWT Token ---
        String token = authHeader.substring(7);

        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid or expired JWT token for path: {}", path);
            return onError(exchange, "Invalid or expired token",
                    HttpStatus.UNAUTHORIZED);
        }

        // --- Step 4: Extract info user and add into header ---
        String userId = jwtUtil.extractUserId(token);
        List<String> roles = jwtUtil.extractRoles(token);
        String email = jwtUtil.extractEmail(token);
        log.info("Authenticated user: {} | Roles: {} | Path: {}", userId, roles, path);

        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Email", email != null ? email : "")
                .header("X-User-Roles", roles != null ? String.join(",", roles) : "")
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");

        String body = String.format(
                "{\"error\": \"%s\", \"status\": %d, \"message\": \"%s\"}",
                status.getReasonPhrase(), status.value(), message);

        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(
                enpoint -> path.equals(enpoint) ||
                        path.startsWith(enpoint + "/"));

    }
}
