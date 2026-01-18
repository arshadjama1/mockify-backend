package com.mockify.backend.exception;

import org.springframework.http.HttpStatus;

public class JwtTokenExpiredException extends BaseException {
    public JwtTokenExpiredException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
