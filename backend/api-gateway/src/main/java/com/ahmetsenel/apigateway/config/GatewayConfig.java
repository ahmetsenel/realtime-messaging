package com.ahmetsenel.apigateway.config;

import com.ahmetsenel.apigateway.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()

                .route("auth-service", r -> r
                        .path("/api/auth/**",
                                "/api/users/**")
                        .uri("lb://AUTH-SERVICE"))

                .route("chat-service", r -> r
                        .path("/api/chat/**")
                        .filters(f ->
                                f.filter(jwtAuthFilter.apply(
                                        new JwtAuthFilter.Config())
                                )
                        )
                        .uri("lb://CHAT-SERVICE"))

                .route("chat-service-ws", r -> r
                        .path("/ws/**")
                        .uri("lb://CHAT-SERVICE"))

                .build();
    }

}
