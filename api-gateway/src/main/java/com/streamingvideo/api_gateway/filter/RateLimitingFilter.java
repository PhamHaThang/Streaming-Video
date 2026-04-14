package com.streamingvideo.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * Rate Limiting Filter use Redis
 * <p>
 * Mechanism:
 * - Use Redis INCR + EXPIRE to count request in duration time
 * - If request > limit -> return 429 Too Many Requests
 */
@Slf4j
@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("${rate-limit.requests-per-second:10}")
    private int requestsPerSecond;

    @Value("${rate-limit.burst-capacity:20}")
    private int burstCapacity;

    public RateLimitingFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String identifier = extractIdentifier(request);
        String redisKey = "rate_limit:" + identifier;
        return redisTemplate.opsForValue()
                .increment(redisKey)
                .flatMap(count -> {
                            Mono<Boolean> expireMono = Mono.empty();
                            if (count == 1) {
                                expireMono = redisTemplate.expire(redisKey, Duration.ofSeconds(1));
                            }

                            return expireMono.then(
                                    handleRequest(exchange, chain, identifier, count)
                            );
                        }
                );
    }

    private Mono<Void> handleRequest(ServerWebExchange exchange,
                                     GatewayFilterChain chain,
                                     String identifier,
                                     Long count) {
        if (count > burstCapacity) {
            log.warn("Rate limit exceeded for: {} | Count: {}", identifier, count);
            return onRateLimitExceeded(exchange);
        }

        // Add rate limit headers into response
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining",
                String.valueOf(burstCapacity - count));
        exchange.getResponse().getHeaders().add("X-RateLimit-Limit",
                String.valueOf(burstCapacity));

        return chain.filter(exchange);
    }


    private String extractIdentifier(ServerHttpRequest request) {
        // Pass JWT filter -> X-User-Id
        String userId = request.getHeaders().getFirst("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            return "user:" + userId;
        }

        // Fallback: Use IP address
        String clientIp = request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
        return "ip:" + clientIp;
    }

    private Mono<Void> onRateLimitExceeded(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", "application/json");
        response.getHeaders().add("Retry-After", "1");

        String body = "{\"error\": \"Too Many Requests\", \"status\": 429, " +
                "\"message\": \"Bạn đã gửi quá nhiều request. Vui lòng thử lại sau.\"}";

        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(body.getBytes()))
        );
    }

    /**
     * Run after JWT filter
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
