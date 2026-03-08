package com.manadoksli.exceptions;

import org.springframework.http.HttpStatus;

public class ApiBadRequestException extends ApiException {


    public ApiBadRequestException(String message) {
        super(message);
        this.httpCode = HttpStatus.BAD_REQUEST.value();
    }

    public ApiBadRequestException() {
        this.httpCode = HttpStatus.BAD_REQUEST.value();
    }
}
