package com.manadoksli.controller.error;


import com.manadoksli.aspect.annotation.LogResponse;
import com.manadoksli.dto.ApiResponse;
import com.manadoksli.exceptions.ApiException;
import com.manadoksli.service.ILocalizationService;
import io.micrometer.common.util.StringUtils;
import jakarta.json.JsonException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures.
 * The error response follows RFC7807 - Problem Details for HTTP APIs (https://tools.ietf.org/html/rfc7807).
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
@LogResponse
@RegisterReflectionForBinding({
        FieldErrorVM.class
})
public class GlobalControllerAdvice {

    public static final String ERROR_INVALID_ARGUMENTS = "error.invalid-arguments";
    public static final String ERROR_MSG_PLACEHOLDER = "{}";

    private final ILocalizationService localizationService;

    private final DataIntegrityViolationMessageParser dataIntegrityMessageParser;


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<FieldErrorVM>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        log.error("[INVALID ARGUMENTS]: {}", ex.getMessage(), ex);

        var errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(f -> {
                    String fieldLabel = humanizeField(f.getField());
                    String constraintMessage = StringUtils.isNotBlank(f.getDefaultMessage())
                            ? f.getDefaultMessage()
                            : f.getCode();

                    return new FieldErrorVM(
                            f.getObjectName().replaceFirst("DTO$", ""),
                            f.getField(),
                            fieldLabel + " " + constraintMessage
                    );
                })
                .toList();

        ApiResponse<List<FieldErrorVM>> response = new ApiResponse<>();
        response.setCode(400);
        response.setMessage(localizationService.getMessage("error.jakarta.validation"));
        response.setFieldErrors(errors);

        return response.toResponseEntity();
    }


    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<FieldErrorVM>>> handleConstraintViolationException(
            ConstraintViolationException ex
    ) {
        log.error("[CONSTRAINT VIOLATION]: {}", ex.getMessage(), ex);

        var errors = ex.getConstraintViolations()
                .stream()
                .map(v -> new FieldErrorVM(
                        v.getRootBeanClass().getSimpleName().replaceFirst("DTO$", ""),
                        v.getPropertyPath().toString(),
                        humanizeField(extractFieldName(v.getPropertyPath().toString())) + " " + v.getMessage()
                ))
                .toList();

        ApiResponse<List<FieldErrorVM>> response = new ApiResponse<>();
        response.setCode(400);
        response.setMessage(localizationService.getMessage("error.jakarta.validation"));
        response.setFieldErrors(errors);

        return response.toResponseEntity();
    }


    @ExceptionHandler({NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<Map<String, String>>> handleNoResourceFoundExceptions(NoResourceFoundException ex) {
        ApiResponse<Map<String, String>> response = new ApiResponse<>();
        response.setCode(404);
        response.setData(Map.of("path", ex.getResourcePath()));
        response.setMessage(localizationService.getMessage("error.path.not-found"));
        return response.toResponseEntity();
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> internalServerError(Exception ex) {
        log.error("[INTERNAL SERVER ERROR]: {}", ex.getMessage(), ex);
        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(500);
        response.setMessage(localizationService.getMessage("error.unknown-error"));

        String causeClassName = Optional.ofNullable(ex.getCause())
                .map(Throwable::getClass)
                .map(Class::getCanonicalName)
                .orElse(null);
        response.setData(causeClassName);
        return response.toResponseEntity();
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<String>> handleBadRequestException(BadRequestException ex) {
        log.error("[BAD REQUEST]: {}", ex.getMessage(), ex);

        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(400);
        response.setMessage(ex.getMessage());
        response.setData(ex.getClass().getCanonicalName());

        return response.toResponseEntity();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<String>> httpMessageNotReadableError(HttpMessageNotReadableException ex) {
        log.error("[MISSING REQUEST BODY]: {}", ex.getMessage(), ex);
        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(400);
        response.setMessage(localizationService.getMessage("error.parsing-error"));

        String causeClassName = Optional.ofNullable(ex.getCause())
                .map(Throwable::getClass)
                .map(Class::getCanonicalName)
                .orElse(null);
        response.setData(causeClassName);
        return response.toResponseEntity();
    }


    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<String>> methodNotSupportedExHandler(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(405);
        response.setMessage(ex.getMessage());
        response.setData(req.getRequestURI());
        return response.toResponseEntity();
    }

    @ExceptionHandler({DataIntegrityViolationException.class})
    public ResponseEntity<ApiResponse<String>> dataIntegrityViolationHandler(DataIntegrityViolationException ex) {
        log.error(ERROR_MSG_PLACEHOLDER, ex.getMessage(), ex);
        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(400);
        response.setData(dataIntegrityMessageParser.parse(ex));
        response.setMessage(localizationService.getMessage("error.data-integrity"));
        return response.toResponseEntity();
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<String>> missingServletRequestParameterException(MissingServletRequestParameterException ex) {
        log.error(ERROR_MSG_PLACEHOLDER, ex.getMessage(), ex);
        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(400);
        response.setMessage(localizationService.getMessage(ERROR_INVALID_ARGUMENTS, new Object[]{ex.getParameterName()}));
        response.setData(ex.getParameterName());

        return response.toResponseEntity();
    }


    @ExceptionHandler({InvalidFormatException.class, JsonException.class, DateTimeParseException.class, InvalidFormatException.class})
    public ResponseEntity<ApiResponse<String>> jsonExceptionHandler(Exception ex) {
        log.error(ERROR_MSG_PLACEHOLDER, ex.getMessage(), ex);
        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(400);
        response.setMessage(localizationService.getMessage("error.parsing-error"));
        response.setData(ex.getClass().getCanonicalName());

        return response.toResponseEntity();
    }

    @ExceptionHandler({ApiException.class})
    public ResponseEntity<ApiResponse<String>> apiExceptionHandler(ApiException ex) {
        log.error(ERROR_MSG_PLACEHOLDER, localizationService.getMessage(ex.getMessage()), ex);
        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(ex.getHttpCode());
        response.setMessage(localizationService.getMessage(ex.getMessage()));
        response.setData(ex.getClass().getCanonicalName());

        return response.toResponseEntity();
    }


    @ExceptionHandler({MaxUploadSizeExceededException.class})
    public ResponseEntity<ApiResponse<String>> fileSizeExceededExceptionHandler(MaxUploadSizeExceededException ex) {
        log.error(ERROR_MSG_PLACEHOLDER, ex.getMessage(), ex);
        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(400);
        response.setMessage(localizationService.getMessage("error.file-size-exceeded"));
        response.setData(ex.getClass().getCanonicalName());

        return response.toResponseEntity();
    }


    @ExceptionHandler({ResponseStatusException.class})
    public ResponseEntity<ApiResponse<String>> responseStatusExceptionHandler(ResponseStatusException ex) {
        log.error(ERROR_MSG_PLACEHOLDER, ex.getMessage(), ex);
        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(ex.getStatusCode().value());
        response.setMessage(ex.getReason());
        response.setData(ex.getMostSpecificCause().getClass().getCanonicalName());

        return response.toResponseEntity();
    }


    private String extractFieldName(String propertyPath) {
        if (propertyPath == null) {
            return null;
        }
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot != -1
                ? propertyPath.substring(lastDot + 1)
                : propertyPath;
    }

    private String humanizeField(String field) {
        try {
            // Handle nested fields: rewards[0].endDate -> End Date
            // Extract the last field name after the last dot
            String fieldName = field;

            if (field.contains(".")) {
                fieldName = field.substring(field.lastIndexOf(".") + 1);
            }

            // Remove array indices if present: rewards[0] -> rewards
            fieldName = fieldName.replaceAll("\\[\\d+]", "");

            // Capitalize each word: end Date -> End Date
            return fieldName.replaceAll("([a-z])([A-Z])", "$1 $2");
        } catch (Exception e) {
            return field;
        }
    }


}
