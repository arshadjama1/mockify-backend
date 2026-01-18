package com.mockify.backend.exception;

import org.springframework.http.HttpStatus;

public class JwtTokenInvalidException extends BaseException {
    public JwtTokenInvalidException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
