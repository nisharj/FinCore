
package com.fincore.account_service.exception;

import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

        public record ApiErrorResponse(Instant timestamp, int status, String error, String message, String path) {
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleNotFound(
                        ResourceNotFoundException ex,
                        HttpServletRequest request) {

                return buildResponse(
                                HttpStatus.NOT_FOUND,
                                ex.getMessage(),
                                request.getRequestURI());
        }

        @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
                        ResponseStatusException ex,
                        HttpServletRequest request) {

                HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

                return buildResponse(
                                status,
                                ex.getReason() != null ? ex.getReason() : status.getReasonPhrase(),
                                request.getRequestURI());
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
                        Exception ex,
                        HttpServletRequest request) {

                ex.printStackTrace();

                return buildResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "An unexpected error occurred",
                                request.getRequestURI());
        }

        private ResponseEntity<ApiErrorResponse> buildResponse(
                        HttpStatus status,
                        String message,
                        String path) {

                ApiErrorResponse body = new ApiErrorResponse(
                                Instant.now(),
                                status.value(),
                                status.getReasonPhrase(),
                                message,
                                path);

                return ResponseEntity.status(status).body(body);
        }
}
