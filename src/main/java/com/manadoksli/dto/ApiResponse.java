package com.manadoksli.dto;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Optional;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class ApiResponse<T> implements Serializable {

    public static final String DEFAULT_SUCCESS_MSG = "Success";
    @Serial
    private static final long serialVersionUID = -661399947346496786L;

    @JsonIgnore
    @Builder.Default
    private Integer code = 200;


    private String message;
    private T data;


    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime timestamp;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean success;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorCode;


    public static <U> ApiResponse<U> setResponse(U data, String message, int code) {
        return ApiResponse.<U>builder()
                .code(code)
                .data(data)
                .message(message)
                .build();
    }


    public static ApiResponse<Void> setSuccessWithMessage(String message) {
        return setResponse(null, message, 200);
    }

    public static <U> ApiResponse<U> setDefaultSuccess() {
        return setResponse(null, DEFAULT_SUCCESS_MSG, 200);
    }

    public static <U> ApiResponse<U> setCreated() {
        return setCreated(null);
    }

    public static <U> ApiResponse<U> setCreated(U data) {
        return setResponse(data, DEFAULT_SUCCESS_MSG, 201);
    }

    public static <U> ApiResponse<U> setResponse(U data, int code) {
        return setResponse(data, DEFAULT_SUCCESS_MSG, code);
    }

    public static <U> ApiResponse<U> setSuccess(U data) {
        return setResponse(data, DEFAULT_SUCCESS_MSG, 200);
    }

    public static <U> ApiResponse<U> setInternalError() {
        return setResponse(null, "INTERNAL SERVER ERROR", 500);
    }


    @JsonIgnore
    public HttpStatus getHttpStatus() {
        return HttpStatus.valueOf(this.code);
    }

    public ApiResponse<T> setCode(int code) {
        this.code = code;
        return this;
    }

    @JsonIgnore
    public boolean isSuccess() {
        return Optional.ofNullable(code)
                .map(it -> it >= 200 && it <= 399)
                .orElse(false);
    }

    @JsonIgnore
    public boolean is5xxError() {
        return Optional.ofNullable(code)
                .map(it -> it >= 500)
                .orElse(false);
    }

    @JsonIgnore
    public boolean is4xxError() {
        return Optional.ofNullable(code)
                .map(it -> it >= 500)
                .orElse(false);
    }


    @JsonIgnore
    public ResponseEntity<ApiResponse<T>> toResponseEntity() {
        return ResponseEntity.status(getHttpStatus()).body(this);
    }
}

