package com.whereyouad.WhereYouAd.global.exception;

import com.whereyouad.WhereYouAd.domains.user.exception.code.AuthErrorCode;
import com.whereyouad.WhereYouAd.global.response.ErrorResponse;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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
        ErrorResponse errorResponse = ErrorResponse.of(e.getErrorCode(), request);
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(errorResponse);
    }

    //@Valid 검사 실패(필수 파라미터가 null 또는 공백) 인 경우 예외를 잡는 핸들러
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        // 예시 결과: "email: 이메일 형식이 아닙니다, password: 비밀번호는 필수입니다"
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.error("MethodArgumentNotValidException 발생: {}", errorMessage);
        log.error("에러가 발생한 지점 {}, {}", request.getMethod(), request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INVALID_PARAMETER,
                request
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
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
}
