package com.whereyouad.WhereYouAd.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whereyouad.WhereYouAd.domains.user.exception.code.AuthErrorCode;
import com.whereyouad.WhereYouAd.global.response.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증 실패 시 처리 핸들러 (401 Unauthorized)
 * 역할: 사용자가 인증 없이(혹은 유효하지 않은 자격 증명으로)
 * 보호된 리소스(API)에 접근하려 할 때 동작.
 * SecurityConfig에서 .authenticationEntryPoint() 로 등록하여 사용
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private ObjectMapper objectMapper = new ObjectMapper();

    //인증 실패 시,
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException
    {
        log.error("인증 실패 (EntryPoint): {}", authException.getMessage());

        //응답 헤더 설정
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); //401 상태코드 설정

        //INVALID_TOKEN_FORMAT 으로 응답 생성 및 반환
        AuthErrorCode errorCode = AuthErrorCode.INVALID_TOKEN_FORMAT;
        ErrorResponse errorResponse = ErrorResponse.of(errorCode, request);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
