package com.whereyouad.WhereYouAd.global.security;

import com.whereyouad.WhereYouAd.global.security.jwt.JwtAccessDeniedHandler;
import com.whereyouad.WhereYouAd.global.security.jwt.JwtAuthenticationEntryPoint;
import com.whereyouad.WhereYouAd.global.security.jwt.JwtAuthenticationFilter;
import com.whereyouad.WhereYouAd.global.security.oauth2.handler.OAuth2AuthenticationSuccessHandler;
import com.whereyouad.WhereYouAd.global.security.oauth2.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) //CSRF 보호 비활성화
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) //세션 관리 정책을 STATELESS -> JWT 사용하므로
                )
                .exceptionHandling(exception -> exception //예외 처리
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint) //인증 실패 시(로그인 미진행, 토큰 만료 등)
                        .accessDeniedHandler(jwtAccessDeniedHandler) //인가 실패 시(권한 부족등)(현재 로직에서는 동작 X -> 모두 ROLE_USER 이므로)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll() //swagger 접근 허용
                        .requestMatchers("/api/users/signup", "/api/auth/**").permitAll() //로그인, 회원가입 접근 허용
                        .anyRequest().authenticated() //이외 접근은 인증 필요
                )
                //Spring Security 의 기본 UsernamePasswordAuthenticationFilter 앞에 JwtAuthenticationFilter 등록
                //Spring Security 가 기본 로그인을 수행하기 전에, JWT 토큰을 먼저 검사해서 유효하면 바로 인증 처리 하기 위해
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // OAuth2 소셜 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
                                .userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler));

        return http.build();
    }

    @Bean  //회원 비밀번호 BCrypt 암호화를 위한 Bean 등록
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}