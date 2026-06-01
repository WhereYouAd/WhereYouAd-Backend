package com.whereyouad.WhereYouAd.global.exception;

import com.whereyouad.WhereYouAd.domains.dashboard.exception.code.DashboardErrorCode;
import com.whereyouad.WhereYouAd.domains.user.exception.code.AuthErrorCode;
import com.whereyouad.WhereYouAd.global.response.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // NPE — 주로 DTO 검증 누락이나 비즈니스 로직에서 발생
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("NullPointerException 발생 — 파일: {}, 라인: {}",
                e.getStackTrace()[0].getFileName(), e.getStackTrace()[0].getLineNumber());
        log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());

        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, request));
    }

    // 처리되지 않은 모든 예외를 잡는 핸들러
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllException(Exception e, HttpServletRequest request) {
        log.error("처리되지 않은 예외 발생: ", e);
        log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());
        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INTERNAL_SERVER_ERROR,
                request
        );
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppCustomException(AppException e, HttpServletRequest request) {
        log.error("AppException 발생: {}", e.getErrorCode().getMessage());
        log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());
        ErrorResponse errorResponse = ErrorResponse.of(e.getErrorCode(), request, e.getBind());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(errorResponse);
    }

    //@Valid 검사 실패(필수 파라미터가 null 또는 공백) 인 경우 예외를 잡는 핸들러
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> errors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage(),
                        (existing, duplicate) -> existing
                ));

        log.error("MethodArgumentNotValidException 발생: {}", errors);
        log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INVALID_PARAMETER,
                request,
                errors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    //reissue API 호출 시 쿠키 값이 아예 없는 경우(삭제된 경우) 예외처리
    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ErrorResponse> handleMissingCookieException(MissingRequestCookieException e, HttpServletRequest request) {
        log.error("reissue 요청에 쿠키 누락: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                AuthErrorCode.TOKEN_NOT_FOUND,
                request
        );

        return ResponseEntity
                .status(AuthErrorCode.TOKEN_NOT_FOUND.getHttpStatus())
                .body(errorResponse);

    }

    //reissue API 호출 시 만료된 refreshToken 값으로 접근 시도한 경우 예외 처리
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwtException(ExpiredJwtException e, HttpServletRequest request) {
        log.warn("만료된 JWT refreshToken 입니다: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                AuthErrorCode.TOKEN_EXPIRED, // 만료 전용 에러 코드 사용
                request
        );

        return ResponseEntity
                .status(AuthErrorCode.TOKEN_EXPIRED.getHttpStatus())
                .body(errorResponse);
    }

    /**
     * 로그인 실패 처리 (비밀번호 틀림, 이메일 없음 등)
     * Spring Security에서 발생하는 BadCredentialsException을 잡아서
     * AUTH_401_3 에러 코드로 반환
     */
    @ExceptionHandler({BadCredentialsException.class, InternalAuthenticationServiceException.class})
    public ResponseEntity<ErrorResponse> handleLoginException(Exception e, HttpServletRequest request) {
        log.error("로그인 실패: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                AuthErrorCode.LOGIN_FAILED,
                request
        );

        return ResponseEntity
                .status(AuthErrorCode.LOGIN_FAILED.getHttpStatus())
                .body(errorResponse);
    }

    /**
     * JWT 토큰 유효성 검사 실패 처리 (reissue 과정에서 조작된 토큰 등)
     * SignatureException, MalformedJwtException, UnsupportedJwtException 등을
     * JwtTokenProvider에서 catch하여 JwtException("Invalid Token")으로 던지고 있음
     */
    @ExceptionHandler({JwtException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleJwtException(Exception e, HttpServletRequest request) {
        log.warn("유효하지 않은 JWT 토큰입니다: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                AuthErrorCode.INVALID_TOKEN_FORMAT,
                request
        );

        return ResponseEntity
                .status(AuthErrorCode.INVALID_TOKEN_FORMAT.getHttpStatus())
                .body(errorResponse);
    }

    /**
     * RequestParam 으로 전달된 값이 Enum 타입 등으로 변환되지 못할 때 발생하는 예외 처리 (ex. @RequestParam Provider provider)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.error("타입 변환 실패 오류 발생: 파라미터명 '{}', 입력값 '{}'", e.getName(), e.getValue());

        // Provider Enum 변환 실패인 경우
        if (e.getRequiredType() != null && e.getRequiredType().isEnum()) {

            ErrorResponse errorResponse = ErrorResponse.of(
                    DashboardErrorCode.PROVIDER_NOT_VALID,
                    request
            );

            return ResponseEntity
                    .status(DashboardErrorCode.PROVIDER_NOT_VALID.getHttpStatus())
                    .body(errorResponse);
        }

        // Enum 변환 실패가 아닌 일반적인 타입 매스매치(예: Long 타입에 문자열 입력)인 경우
        Map<String, String> errors = new HashMap<>();
        errors.put(e.getName(), e.getValue() != null ? e.getValue().toString() : "null");

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.PARAMETER_MISMATCH,
                request,
                errors
        );

        return ResponseEntity
                .status(ErrorCode.PARAMETER_MISMATCH.getHttpStatus())
                .body(errorResponse);
    }

    // @Valid 쿼리 파라미터 유효성 검사 실패
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(
            HandlerMethodValidationException e,
            HttpServletRequest request
    ) {

        Map<String, String> errors = new HashMap<>();
        e.getParameterValidationResults().forEach(result ->
                errors.put(
                        result.getMethodParameter().getParameterName(),
                        result.getResolvableErrors().get(0).getDefaultMessage()
                )
        );

        log.error("HandlerMethodValidationException 발생: {}", errors);
        log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INVALID_PARAMETER,
                request,
                errors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    // 필수 @RequestParam 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e,
            HttpServletRequest request
    ) {

        Map<String, String> errors = new HashMap<>();
        errors.put(e.getParameterName(), "필수 파라미터입니다.");

        log.error("MissingServletRequestParameterException 발생: 누락된 파라미터 '{}'", e.getParameterName());
        log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INVALID_PARAMETER,
                request,
                errors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    // @Validated 제약 조건 위반
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException e,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new HashMap<>();
        e.getConstraintViolations().forEach(violation -> {
            String propertyPath = violation.getPropertyPath().toString();
            String fieldName = propertyPath.contains(".")
                    ? propertyPath.substring(propertyPath.lastIndexOf(".") + 1)
                    : propertyPath;
            errors.put(fieldName, violation.getMessage());
        });

        log.error("ConstraintViolationException 발생: {}", errors);
        log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INVALID_PARAMETER,
                request,
                errors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    // Request Body JSON 파싱 실패 또는 Body 누락
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {

        // 특정 필드의 타입 변환 실패 (예: String 필드에 숫자 타입 등)
        if (e.getCause() instanceof InvalidFormatException cause) {
            Map<String, String> errors = new HashMap<>();
            String fieldName = cause.getPath().get(0).getFieldName();

            if (cause.getValue().toString().isEmpty()) {
                errors.put(fieldName, "빈칸을 변환할 수 없습니다.");
            } else {
                errors.put(fieldName, "'" + cause.getValue() + "'을(를) 변환할 수 없습니다.");
            }

            log.error("InvalidFormatException 발생: {}", errors);
            log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());

            return ResponseEntity
                    .status(ErrorCode.JSON_PARSE_FAIL.getHttpStatus())
                    .body(ErrorResponse.of(ErrorCode.JSON_PARSE_FAIL, request, errors));
        }

        // JSON 문법 자체가 잘못된 경우 (중괄호 누락, 따옴표 오류 등)
        if (e.getMessage() != null && e.getMessage().contains("JSON parse error")) {
            log.error("HttpMessageNotReadableException 발생: JSON 파싱 실패 - {}", e.getMessage());
            log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());

            return ResponseEntity
                    .status(ErrorCode.JSON_PARSE_FAIL.getHttpStatus())
                    .body(ErrorResponse.of(ErrorCode.JSON_PARSE_FAIL, request));
        }

        // Request Body 자체가 없는 경우
        log.error("HttpMessageNotReadableException 발생: Request Body 없음");
        log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());

        return ResponseEntity
                .status(ErrorCode.NOT_FOUND_REQUEST_BODY.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.NOT_FOUND_REQUEST_BODY, request));
    }

    // 지원하지 않는 HTTP 메서드
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.error("HttpRequestMethodNotSupportedException 발생: 지원하지 않는 메서드 '{}'", e.getMethod());
        log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());

        Map<String, String[]> errors = new HashMap<>();
        errors.put("지원하는 메서드", e.getSupportedMethods());

        return ResponseEntity
                .status(ErrorCode.NOT_SUPPORT_HTTP_METHOD.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.NOT_SUPPORT_HTTP_METHOD, request, errors));
    }

    // 존재하지 않는 URI
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException e, HttpServletRequest request) {
        log.error("NoResourceFoundException 발생: 존재하지 않는 URI '{}'", request.getRequestURI());

        return ResponseEntity
                .status(ErrorCode.NOT_FOUND_URI.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.NOT_FOUND_URI, request));
    }

    // 지원하지 않는 Content-Type
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        log.error("HttpMediaTypeNotSupportedException 발생: 지원하지 않는 Content-Type '{}'", e.getContentType());
        log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());

        Map<String, String> errors = new HashMap<>();
        if (e.getContentType() != null) {
            errors.put("요청한 Content-Type", e.getContentType().toString());
        }

        return ResponseEntity
                .status(ErrorCode.NOT_SUPPORT_CONTENT_TYPE.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.NOT_SUPPORT_CONTENT_TYPE, request, errors));
    }
}
