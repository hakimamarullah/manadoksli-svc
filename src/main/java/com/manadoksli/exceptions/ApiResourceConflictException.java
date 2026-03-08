package com.manadoksli.exceptions;

import org.springframework.http.HttpStatus;

public class ApiResourceConflictException extends ApiException {

    public ApiResourceConflictException(String message) {
        super(message);
        this.httpCode = HttpStatus.CONFLICT.value();
    }

    public ApiResourceConflictException() {
        this.httpCode = HttpStatus.CONFLICT.value();
    }
}
