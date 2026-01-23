package com.whereyouad.WhereYouAd.global.security.jwt;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
// 권한 관련 (ADMIN / USER) 비인가 접근 시도시 발생하는 예외를 처리하는 Handler
// 1차 MVP 에서는 권한 구분을 하지 않으므로 1차에서는 해당 Handler 동작 X -> 추후 확장을 위한 코드


    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException
    {
        log.warn("Forbidden Error: {}", accessDeniedException.getMessage());

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        // JSON 응답 작성
        response.getWriter().write(
                "{" +
                        "\"status\": 403," +
                        "\"error\": \"Forbidden\"," +
                        "\"message\": \"해당 리소스에 접근할 권한이 없습니다.\"," +
                        "\"path\": \"" + request.getRequestURI() + "\"" +
                        "}"
        );
    }
}
