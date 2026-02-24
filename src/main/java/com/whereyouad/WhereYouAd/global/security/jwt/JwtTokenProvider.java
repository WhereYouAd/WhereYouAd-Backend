package com.whereyouad.WhereYouAd.global.security.jwt;

import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;
import com.whereyouad.WhereYouAd.global.security.jwt.dto.TokenResponse;
import com.whereyouad.WhereYouAd.global.security.oauth2.dto.CustomOAuth2User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    //JWT 토큰 내 권한 정보를 담을 때 사용하는 key 값
    private static final String AUTHORITIES_KEY = "auth";
    private static final String PROVIDER_KEY = "provider";
    //HTTP 헤더에 붙일 타입(Bearer {token})
    private static final String BEARER_TYPE = "Bearer";
    //AccessToken 만료 시간
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 30; //30분
    //RefreshToken 만료 시간
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 7; //7일

    //암,복호화에 사용하는 키 값
    private final Key key;

    //application.yml 에 jwt.secret 값 설정 필요
    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    //AccessToken, RefreshToken 생성 메서드
    public TokenResponse generateToken(Authentication authentication) {

        // 로그인 종류에 따라 이메일(식별자) 추출
        String email;
        String provider;

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomOAuth2User) {
            CustomOAuth2User oAuth2User = (CustomOAuth2User) principal;
            // 소셜 로그인: CustomOAuth2User에서 이메일 추출
            email = oAuth2User.getEmail();

            provider = oAuth2User.getProvider().name();
        } else if (principal instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) principal;
            // 일반 로그인: UserDetails의 username(email) 추출
            email = userDetails.getUsername();

            provider = userDetails.getProvider().name();
        } else {
            // 그 외의 경우 (기본값)
            email = authentication.getName();
            provider = Provider.EMAIL.name();
        }
        //사용자 권한(ROLE_USER) 가져와서 문자열로 반환
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = new Date().getTime();
        Date accessTokenExpireIn = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);

        //AccessToken 생성
        String accessToken = Jwts.builder()
                .setSubject(email) // Payload "sub": 유저의 이메일(ID)
                .claim(AUTHORITIES_KEY, authorities)  // Payload "auth": "ROLE_USER"
                .claim(PROVIDER_KEY, provider)
                .setExpiration(accessTokenExpireIn) // Payload "exp": 만료 시간
                .signWith(key, SignatureAlgorithm.HS512) // Header "alg": HS512 알고리즘으로 서명
                .compact();

        //RefreshToken 생성
        Date refreshTokenExpireIn = new Date(now + REFRESH_TOKEN_EXPIRE_TIME);
        //RefreshToken 은 권한 정보(claims) 는 담지 않고, 누구인지 구별하기 위한 Subject(email) 만 추가
        String refreshToken = Jwts.builder()
                .setSubject(email) //sub: email
                .setExpiration(refreshTokenExpireIn)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        return TokenResponse.builder()
                .grantType(BEARER_TYPE)
                .accessToken(accessToken)
                .accessTokenExpireIn(accessTokenExpireIn.getTime())
                .refreshToken(refreshToken)
                .build();
    }

    //토큰 정보 검증 메서드
    public void validateToken(String token) {
        try {
            //서명 키(Key) 통한 토큰 복호화
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
        } catch (ExpiredJwtException e) {
            // 만료된 토큰인 경우: 재발급(reissue)을 위해 구체적인 예외 던지기
            throw e;

        } catch (UnsupportedJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
            // 그 외 잘못된 토큰 형식은 모두 "잘못된 토큰 형식 입니다." 으로 통칭하여 예외 발생
            throw new JwtException("잘못된 토큰 형식 입니다.");
        }
    }

    //AccessToken 또는 RefreshToken 을 받아 복호화 하여 Subject 인 이메일 값 추출
    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    //토큰 복호화 하여 Claims 부분을 반환
    //만료된 토큰이라도 정보 꺼낼 수 있도록 처리
    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            //만료 토큰이라도 재발급(reissue) 시에는 누구인지 알아야 한다.
            return e.getClaims();
        }
    }

    public String getProvider(String token) {
        Claims claims = parseClaims(token);
        String provider = claims.get(PROVIDER_KEY, String.class);

        if (provider == null || provider.isEmpty()) {
            return Provider.EMAIL.name();
        }

        return provider;
    }
}
