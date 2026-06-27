package com.mockify.backend.exception;

import org.springframework.http.HttpStatus;

public class InvalidOpenApiException extends BaseException{

    public InvalidOpenApiException(String message){
        super(message, HttpStatus.BAD_REQUEST);
    }
}
