package com.whereyouad.WhereYouAd.global.security.oauth2.repository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * OAuth2 인증 요청 정보를 HTTP 세션 대신 쿠키에 저장하는 클래스
 *
 * Spring Security의 OAuth2 로그인시 CSRF 방지를 위해 state 값 사용
 * 기본 구현체(HttpSessionOAuth2AuthorizationRequestRepository)는 이 state를 HTTP 세션에 저장하는데,
 * 서버가 SessionCreationPolicy.STATELESS(JWT 방식)로 설정되면 세션을 사용할 수 없어 state 검증에 실패
 *
 * 따라서 state를 HTTP 세션 대신 쿠키에 저장하도록 커스텀 구현
 * 쿠키는 브라우저가 자동으로 들고 다니므로 STATELESS 서버에서도 state 검증이 가능
 *
 * 1. 소셜 로그인 시작 → saveAuthorizationRequest() → state를 쿠키에 저장
 * 2. 플랫폼(구글/네이버/카카오) 로그인 완료 → 콜백 요청에 쿠키가 담겨옴
 * 3. 콜백 처리 → removeAuthorizationRequest() → 쿠키에서 state 꺼내서 검증 후 삭제
 */
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String COOKIE_NAME = "oauth2_auth_request"; // 저장할 쿠키 이름
    private static final int COOKIE_EXPIRE_SECONDS = 180;            // 쿠키 유효시간 3분 (로그인 소요 시간 고려)

    // 쿠키에서 OAuth2 인증 요청 정보를 꺼내서 반환
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return getCookie(request, COOKIE_NAME)
                .map(cookie -> deserialize(cookie.getValue()))
                .orElse(null);
    }

    // OAuth2 인증 요청 정보를 쿠키에 저장 (소셜 로그인 시작 시점에 호출됨)
    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteCookie(request, response, COOKIE_NAME);
            return;
        }
        addCookie(response, COOKIE_NAME, serialize(authorizationRequest), COOKIE_EXPIRE_SECONDS);
    }

    // 쿠키에서 OAuth2 인증 요청 정보를 꺼내고 쿠키를 삭제 (콜백 처리 시점에 호출됨)
    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                  HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        deleteCookie(request, response, COOKIE_NAME);
        return authorizationRequest;
    }

    // 요청에서 특정 이름의 쿠키를 찾아 반환
    private Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
                .filter(c -> c.getName().equals(name))
                .findFirst();
    }

    // 쿠키 추가
    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true); // JS에서 접근 불가 (보안)
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    // 쿠키 삭제 (maxAge를 0으로 세팅하면 브라우저가 즉시 만료시킴)
    private void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return;
        Arrays.stream(cookies)
                .filter(c -> c.getName().equals(name))
                .forEach(c -> {
                    c.setValue("");
                    c.setPath("/");
                    c.setMaxAge(0);
                    response.addCookie(c);
                });
    }

    // OAuth2AuthorizationRequest 객체 → 바이트 배열 → Base64 문자열 (쿠키에 저장 가능한 형태로 변환)
    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(authorizationRequest);
            oos.close();
            return Base64.getUrlEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("OAuth2AuthorizationRequest 직렬화 실패", e);
        }
    }

    // Base64 문자열 → 바이트 배열 → OAuth2AuthorizationRequest 객체로 복원
    private OAuth2AuthorizationRequest deserialize(String value) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(value);
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            return (OAuth2AuthorizationRequest) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("OAuth2AuthorizationRequest 역직렬화 실패", e);
        }
    }
}
