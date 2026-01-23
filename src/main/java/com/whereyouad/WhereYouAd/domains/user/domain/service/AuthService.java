package com.whereyouad.WhereYouAd.domains.user.domain.service;

import com.whereyouad.WhereYouAd.domains.user.application.dto.request.LoginRequest;
import com.whereyouad.WhereYouAd.domains.user.exception.code.AuthErrorCode;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.RefreshToken;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.RefreshTokenRepository;
import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.security.jwt.CustomUserDetailsService;
import com.whereyouad.WhereYouAd.global.security.jwt.JwtTokenProvider;
import com.whereyouad.WhereYouAd.global.security.jwt.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CustomUserDetailsService customUserDetailsService;

    //최초 로그인을 통해 AccessToken 과 RefreshToken 발급 받는 메서드
    @Transactional
    public TokenResponse login(LoginRequest request) {
        //email, password 기반 Spring Security가 사용할 인증 객체(AuthenticationToken) 생성
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.email(), request.password());

        //실제 password 검증
        // authenticate()가 실행되면 CustomUserDetailsService.loadUserByUsername이 호출되어 DB의 유저 정보와 비교합니다.
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        //인증 정보 기반 JWT 토큰(Access & Refresh) 생성
        TokenResponse tokenResponse = jwtTokenProvider.generateToken(authentication);

        //RefreshToken 저장 -> 없으면 생성, 이미 있으면 update
        RefreshToken refreshToken = refreshTokenRepository.findByKeyId(request.email())
                .map(entity -> entity.updateValue(tokenResponse.refreshToken()))
                .orElse(RefreshToken.builder()
                        .keyId(request.email())
                        .value(tokenResponse.refreshToken()).
                        build());

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
}
