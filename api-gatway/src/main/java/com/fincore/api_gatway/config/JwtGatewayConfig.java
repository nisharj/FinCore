
package com.fincore.api_gatway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import com.fincore.api_gatway.security.JwtService;

import io.jsonwebtoken.Claims;

import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

@Configuration
public class JwtGatewayConfig {

    private final JwtService jwtService;

    public JwtGatewayConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    // =========================
    // CUSTOMER ROUTES
    // =========================

    @Bean
    public RouterFunction<ServerResponse> customerRoute() {

        RequestPredicate normalCustomerMethods = request -> {

            String method = request.method().name();

            return (method.equals("GET")
                    || method.equals("POST")
                    || method.equals("PUT"))
                    && RequestPredicates.path("/customers/**").test(request);
        };

        return route("customer-service")

                // DELETE → ADMIN ONLY
                .DELETE("/customers/**", http())
                .filter(adminOnly())
                .filter(lb("customer-service"))
                .build()
                .and(
                    route("customer-service-normal")
                        .route(normalCustomerMethods, http())
                        .filter(jwtAuthentication())
                        .filter(lb("customer-service"))
                        .build()
                );
    }

    // =========================
    // ACCOUNT ROUTES
    // =========================

    private RequestPredicate publicAccountPath() {

        return request -> {
            String path = request.path();

            String[] segments = path.split("/");

            if (path.equals("/accounts")) {
                return true;
            }

            if (segments.length == 3
                    && segments[1].equals("accounts")
                    && !segments[2].equals("internal")) {
                return true;
            }

            return segments.length == 4
                    && segments[1].equals("accounts")
                    && segments[2].equals("customer");
        };
    }

    @Bean
    public RouterFunction<ServerResponse> accountRoute() {

        RequestPredicate publicPath = publicAccountPath();

        RequestPredicate normalAccountMethods = request -> {
            String method = request.method().name();

            return (method.equals("GET")
                    || method.equals("POST")
                    || method.equals("PUT"))
                    && publicPath.test(request);
        };

        RequestPredicate adminDeleteMethod = request ->
                request.method().name().equals("DELETE")
                        && publicPath.test(request);

        return route("account-service")

                // DELETE → ADMIN ONLY, PUBLIC PATHS ONLY
                .route(adminDeleteMethod, http())
                .filter(adminOnly())
                .filter(lb("account-service"))
                .build()
                .and(
                    route("account-service-normal")
                        .route(normalAccountMethods, http())
                        .filter(jwtAuthentication())
                        .filter(lb("account-service"))
                        .build()
                );
    }

    // =========================
    // TRANSACTION ROUTES
    // =========================

    @Bean
    public RouterFunction<ServerResponse> transactionRoute() {

        RequestPredicate transactionMethods = request -> {

            String method = request.method().name();

            boolean allowedMethod =
                    method.equals("GET") || method.equals("POST");

            boolean transactionPath =
                    RequestPredicates.path("/transactions/**").test(request);

            return allowedMethod && transactionPath;
        };

        return route("transaction-service")
                .route(transactionMethods, http())
                .filter(jwtAuthentication())
                .filter(lb("transaction-service"))
                .build();
    }

    // =========================
    // JWT AUTHENTICATION
    // =========================

    private HandlerFilterFunction<ServerResponse, ServerResponse> jwtAuthentication() {
        return (request, next) -> {

            String authHeader = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ServerResponse
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = jwtService.validateToken(token);

                String username = claims.getSubject();
                String role = claims.get("role", String.class);

                System.out.println("Authenticated user: " + username + " | Role: " + role);

                return next.handle(request);
            }
            catch (Exception e) {
                return ServerResponse
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid or expired token");
            }
        };
    }

    // =========================
    // ADMIN AUTHORIZATION
    // =========================

    private HandlerFilterFunction<ServerResponse, ServerResponse> adminOnly() {

        return (request, next) -> {

            String authHeader = request.headers()
                    .firstHeader(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ServerResponse
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);

            try {

                Claims claims = jwtService.validateToken(token);

                String username = claims.getSubject();
                String role = claims.get("role", String.class);

                System.out.println(
                        "ADMIN CHECK → User: " + username + " | Role: " + role
                );

                if (!"ADMIN".equals(role)) {
                    return ServerResponse
                            .status(HttpStatus.FORBIDDEN)
                            .body("Admin access required");
                }

                return next.handle(request);

            } catch (Exception e) {
                return ServerResponse
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid or expired token");
            }
        };
    }
}
