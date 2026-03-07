package com.manadoksli.aspect;


import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;
import tools.jackson.databind.json.JsonMapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class ControllerLoggingAspect {

    @Qualifier("loggingMapper")
    private final JsonMapper mapper;


    @Pointcut("execution(@com.manadoksli.aspect.annotation.LogRequestResponse * *(..))")
    public void methodWithLoggedRequestResponse() {
    }


    @Pointcut("within(@com.manadoksli.aspect.annotation.LogRequestResponse *)")
    public void classWithLogRequestResponse() {
    }

    @Pointcut("within(@com.manadoksli.aspect.annotation.LogResponse *)")
    public void classWithLogResponse() {
    }


    @Around("methodWithLoggedRequestResponse() || classWithLogRequestResponse()")
    public Object logRequestAndResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger logger = LogManager.getLogger(joinPoint.getTarget().getClass());

        String reqId = getTraceId();
        RequestMetadata request = new RequestMetadata((ServletRequestAttributes) RequestContextHolder.getRequestAttributes());


        Object requestPayload = Optional.ofNullable(getRequestBodyParameter(joinPoint))
                .map(this::writeAsString)
                .orElse("");

        String originalPath = getPathPattern();
        // Log Request
        logger.info("[{}@{}] {} | {} | REQUEST: {} QUERY: {}", request.getMethod(), originalPath, request.getPath(), reqId, requestPayload, request.getQueryString());

        // Proceed with the method execution and get the response
        Object response = joinPoint.proceed();
        String responseString = Optional.ofNullable(response).map(this::writeAsString).orElse("");

        // Log Response
        logger.info("[{}@{}] {} | {} | RESPONSE: {}", request.getMethod(), originalPath, request.getPath(), reqId, responseString);
        return response;
    }

    @Around("classWithLogResponse()")
    public Object logResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger logger = LogManager.getLogger(joinPoint.getTarget().getClass());

        String reqId = getTraceId();
        RequestMetadata request = new RequestMetadata((ServletRequestAttributes) RequestContextHolder.getRequestAttributes());

        // Proceed with the method execution and get the response
        Object response = joinPoint.proceed();
        String responseString = Optional.ofNullable(response).map(this::writeAsString).orElse("");

        // Log Response
        logger.info("[{}@{}] {} | {} | RESPONSE: {}", request.getMethod(), getPathPattern(), request.getPath(), reqId, responseString);
        return response;
    }

    private String writeAsString(Object payload) {
        try {
            if (payload instanceof String) {
                return String.valueOf(payload);
            }
            if (payload instanceof ResponseEntity<?> res) {
                return mapper.writeValueAsString(res.getBody());
            }
            return mapper.writeValueAsString(payload);
        } catch (Exception e) {
            // Do Nothing
        }
        return String.valueOf(payload);
    }


    private Object getRequestBodyParameter(ProceedingJoinPoint joinPoint) {
        try {
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            Object[] args = joinPoint.getArgs();


            for (int i = 0; i < args.length; i++) {
                Annotation[] parameterAnnotations = method.getParameterAnnotations()[i];
                for (Annotation annotation : parameterAnnotations) {
                    if ((annotation instanceof RequestBody || annotation instanceof ModelAttribute
                         || annotation instanceof RequestPart) && !(args[i] instanceof MultipartFile)) {
                        return args[i];
                    }

                }
            }
        } catch (Exception e) {
            // Do Nothing
        }
        return null;
    }


    private String getPathPattern() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                Object pattern = attributes.getRequest()
                        .getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
                return Optional.ofNullable(pattern)
                        .map(Object::toString)
                        .orElse("");
            }
        } catch (Exception e) {
            // Ignore
        }
        return "";
    }

    @Getter
    static class RequestMetadata {

        private final String path;
        private final String method;
        private final String queryString;

        public RequestMetadata(ServletRequestAttributes requestAttributes) {

            HttpServletRequest request = Optional.ofNullable(requestAttributes)
                    .map(ServletRequestAttributes::getRequest).orElse(null);

            this.path = Optional.ofNullable(request)
                    .map(HttpServletRequest::getServletPath)
                    .orElse("");

            this.method = Optional.ofNullable(request)
                    .map(HttpServletRequest::getMethod)
                    .map(String::toUpperCase)
                    .orElse("");
            this.queryString = Optional.ofNullable(request)
                    .map(HttpServletRequest::getQueryString)
                    .orElse("");

        }
    }


    private String getTraceId() {
        try {
            return UUID.randomUUID().toString();
        } catch (Exception e) {
            return "";
        }
    }


}
