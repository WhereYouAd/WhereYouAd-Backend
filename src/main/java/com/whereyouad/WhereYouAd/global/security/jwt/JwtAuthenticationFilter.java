package com.whereyouad.WhereYouAd.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.user.exception.code.AuthErrorCode;
import com.whereyouad.WhereYouAd.domains.user.exception.handler.UserHandler;
import com.whereyouad.WhereYouAd.global.response.ErrorResponse;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailService; //DB 에서 User 정보 가져오는 Service 클래스
    private final ObjectMapper objectMapper;
    private final RedisUtil redisUtil;

    //들어오는 로직에 대한 JWT 토큰 기반 실제 필터링 메서드
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException
    {
        //Request 헤더에서 JWT Token(AccessToken) 추출
        String token = resolveToken(request);
        try {

            //AccessToken 공백 확인
            if (StringUtils.hasText(token)) {

                //AccessToken 유효성 확인
                jwtTokenProvider.validateToken(token); //예외 발생 가능 구간 -> ExpiredJwtException 등

                //블랙리스트(로그아웃 처리된 토큰) 확인 — 만료 전이라도 차단
                //AuthService.logout 에서 해시 처리된 값과 동일하게 AccessToken 값 해시해서 조회
                if (redisUtil.getData(BLACKLIST_PREFIX + sha256(token)) != null) {
                    setErrorResponse(response, AuthErrorCode.INVALID_TOKEN_FORMAT, request);
                    return;
                }

                //토큰에서 email 값 추출
                String email = jwtTokenProvider.getSubject(token);
                String providerStr = jwtTokenProvider.getProvider(token);
                //& email 값으로 DB 내 해당 email 로 가입한 회원 존재하는지 확인
                CustomUserDetails userDetails = (CustomUserDetails) customUserDetailService.loadUserByUsername(email);

                CustomUserDetails finalUserDetails = new CustomUserDetails(
                        userDetails.getUser(),
                        Provider.valueOf(providerStr)
                );

                //Spring Security 가 인식 가능한 인증 객체(Authentication) 생성
                //이미 인증된 상태에서 Security 가 인식 가능하게 만드는 것 임으로 비밀번호(credentials) 필드는 null
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(finalUserDetails, null, finalUserDetails.getAuthorities());

                //SecurityContextHolder 에 인증 객체 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (ExpiredJwtException e) { //토큰 만료시 예외 처리
            setErrorResponse(response, AuthErrorCode.TOKEN_EXPIRED, request);
            return;
        } catch (JwtException | IllegalArgumentException e) { //토큰 위조 or 손상 시 예외 처리
            setErrorResponse(response, AuthErrorCode.INVALID_TOKEN_FORMAT, request);
            return;
        } catch (UserHandler e) { // 로그아웃 블랙리스트 조회시 sha256 암호화 관련 오류 발생시 예외 처리 -> 사실상 발생 확률 적음
            setErrorResponse(response, AuthErrorCode.TOKEN_HASH_FAILED, request);
            return;
        }

        filterChain.doFilter(request, response);
    }

    //Request Header 에서 토큰 정보를 꺼내오는 메서드
    //Authorization: Bearer {token} 형태 파싱하는 메서드
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    //JSON 에러 응답을 직접 작성하는 메서드
    //Filter 에서 발생한 예외는 GlobalHandler 로 처리 불가 -> 여기서 예외 응답 처리
    private void setErrorResponse(HttpServletResponse response, AuthErrorCode errorCode, HttpServletRequest request) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        ErrorResponse errorResponse = ErrorResponse.of(errorCode, request);

        // 객체를 JSON 문자열로 변환하여 출력
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    // AuthService.logout 의 블랙리스트 등록 키와 동일한 방식
    private String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) { //SHA-256이 JVM 에서 지원안할시 발생 -> 사실상 발생확률 적음
            throw new UserHandler(AuthErrorCode.TOKEN_HASH_FAILED);
        }
    }
}
