package com.whereyouad.WhereYouAd.domains.user.domain.service;

import com.whereyouad.WhereYouAd.domains.user.application.dto.request.LoginRequest;
import com.whereyouad.WhereYouAd.domains.user.exception.code.AuthErrorCode;
import com.whereyouad.WhereYouAd.domains.user.exception.handler.UserHandler;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.RefreshToken;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.RefreshTokenRepository;
import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.security.jwt.CustomUserDetailsService;
import com.whereyouad.WhereYouAd.global.security.jwt.JwtTokenProvider;
import com.whereyouad.WhereYouAd.global.security.jwt.dto.TokenResponse;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CustomUserDetailsService customUserDetailsService;
    private final RedisUtil redisUtil;

    //최초 로그인을 통해 AccessToken 과 RefreshToken 발급 받는 메서드
    //Transactional 어노테이션 제거 -> 메서드 전체에 Transactional 을 걸 시 DB 커넥션 풀 고갈 가능
    public TokenResponse login(LoginRequest request) {
        //email, password 기반 Spring Security가 사용할 인증 객체(AuthenticationToken) 생성
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.email(), request.password());

        //실제 password 검증
        // authenticate()가 실행되면 CustomUserDetailsService.loadUserByUsername이 호출되어 DB의 유저 정보와 비교합니다.
        // CustomUserDetailsService 내부적으로만 짧게 DB 커넥션을 사용하고 반납
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        //인증 정보 기반 JWT 토큰(Access & Refresh) 생성 (DB 커넥션 없이 순수 CPU 연산으로 진행)
        TokenResponse tokenResponse = jwtTokenProvider.generateToken(authentication);

        //RefreshToken 저장 -> 없으면 생성, 이미 있으면 update
        //Spring Data JPA의 findBy... 와 save 메서드는 자체적으로 트랜잭션이 적용되어 있어 Transactional 어노테이션 없어도 안전
        RefreshToken refreshToken = refreshTokenRepository.findByKeyId(request.email())
                .map(entity -> entity.updateValue(tokenResponse.refreshToken()))
                .orElse(RefreshToken.builder()
                        .keyId(request.email())
                        .value(tokenResponse.refreshToken()).
                        build());

        // RefreshToken save 에도 짧게 커넥션을 다시 맺고 데이터를 저장 후 반환
        refreshTokenRepository.save(refreshToken);

        return tokenResponse;
    }

    //기존 AccessToken 만료 시 RefreshToken을 통해 AccessToken & RefreshToken 을 재발급 받는 메서드
    @Transactional
    public TokenResponse reIssue(String refreshToken) {
        jwtTokenProvider.validateToken(refreshToken); //refreshToken 자체에 문제가 있을 시 해당 부분에서 예외 발생

        String email = jwtTokenProvider.getSubject(refreshToken); //refreshToken 에서 사용자 email 값 추출

        //기존에 해당 email의 RefreshToken 이 있는지 조회
        RefreshToken savedRefreshToken = refreshTokenRepository.findByKeyId(email)
                .orElseThrow(() -> new AppException(AuthErrorCode.TOKEN_EXPIRED));

        //해당 저장된 RefreshToken 과 입력받은 RefreshToken 이 동일한지 확인
        if (!savedRefreshToken.getValue().equals(refreshToken)) {
            throw new AppException(AuthErrorCode.INVALID_TOKEN_FORMAT);
        }

        //새로운 토큰 생성을 위해 유저 최신 정보 조회
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
        // RefreshToken이 남아 있어도 탈퇴·정지된 회원이면 새 JWT를 발급하지 않는다.
        if (!userDetails.isEnabled()) {
            throw new AppException(AuthErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        // Spring Security 인증 객체 생성
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        //새로운 Access & RefreshToken 생성 및 저장
        TokenResponse tokenResponse = jwtTokenProvider.generateToken(authentication);
        savedRefreshToken.updateValue(tokenResponse.refreshToken());
        refreshTokenRepository.save(savedRefreshToken);

        //새로운 Access & RefreshToken 반환
        return tokenResponse;
    }

    // 로그아웃 — AccessToken 블랙리스트 등록 + RefreshToken DB에서 삭제
    // 이메일/소셜 로그인 모두 email 을 key 로 동일한 RefreshToken row 를 공유하므로 분기 없이 처리
    public void logout(String accessToken) {

        // AccessToken 없을 시 return 처리 - 관대한 로그아웃 방식
        // SecurityConfig 에서 이미 인증 요구하므로 토큰값 존재 보장되긴 하지만, 안전을 위해 return 처리
        if (!StringUtils.hasText(accessToken)) {
            return;
        }

        // AccessToken 남은 TTL 동안 Redis 에 블랙리스트로 등록 (만료/손상 토큰은 0 반환 -> 등록 생략)
        // 원문 JWT 가 외부에 노출되지 않도록 SHA-256 해시값을 키로 사용
        long remainingMillis = jwtTokenProvider.getRemainingExpirationMillis(accessToken);
        if (remainingMillis > 0) { //SecurityConfig 에서 만료 아님이 보장되지만 안전을 위해 확인
            long remainingSeconds = Math.max((remainingMillis + 999) / 1000, 1);
            redisUtil.setDataExpire(BLACKLIST_PREFIX + sha256(accessToken), "logout", remainingSeconds);
        }

        // RefreshToken 삭제 — 토큰에서 email(subject) 추출 실패 시에도 로그아웃 응답은 계속 진행
        try {
            String email = jwtTokenProvider.getSubject(accessToken);
            if (StringUtils.hasText(email)) {
                refreshTokenRepository.deleteById(email);
            }
        } catch (JwtException | IllegalArgumentException e) {
            //SecurityConfig 에서 손상 아님이 보장되지만 안전을 위해 catch 처리
            log.error("로그아웃 실행 중 JWT 토큰 손상 감지: {}", e);
        }
    }

    // AccessToken 해쉬화 메서드
    // JwtAuthenticationFilter 의 블랙리스트 조회 키와 동일한 방식
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
