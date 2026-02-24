package com.whereyouad.WhereYouAd.domains.user.domain.service;

import com.whereyouad.WhereYouAd.domains.user.application.dto.response.MyPageResponse;
import com.whereyouad.WhereYouAd.domains.user.exception.handler.UserHandler;
import com.whereyouad.WhereYouAd.domains.user.exception.code.UserErrorCode;
import com.whereyouad.WhereYouAd.domains.user.domain.constant.UserStatus;
import com.whereyouad.WhereYouAd.domains.user.application.mapper.UserConverter;
import com.whereyouad.WhereYouAd.domains.user.application.dto.request.SignUpRequest;
import com.whereyouad.WhereYouAd.domains.user.application.dto.response.SignUpResponse;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.UserRepository;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisUtil redisUtil;

    //회원가입 메서드
    public SignUpResponse signUpUser(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) { //이미 이메일로 만든 계정이 존재할 시
            throw new UserHandler(UserErrorCode.USER_EMAIL_DUPLICATE); //이메일 중복 예외
        }

        //추가 : 이메일 인증되었는지 확인 -> 악의적 공격자가 이메일 인증을 건너뛰고 회원가입 URL 등으로 바로 들어왔을 경우
        //Redis 에 해당 이메일 인증이 완료되었는지 값을 꺼내보기
        String isEmailVerified = redisUtil.getData("VERIFIED:" + request.email());

        //인증이 안되었다면,
        if (isEmailVerified == null || !isEmailVerified.equals("TRUE")) {
            throw new UserHandler(UserErrorCode.USER_EMAIL_NOT_VERIFIED); //예외 발생(UNAUTHORIZED)
        }

        //비밀번호 암호화 -> SecurityConfig 클래스 내 에서 BCryptPasswordEncoder 를 Bean 등록한거로 사용
        String encodedPwd = passwordEncoder.encode(request.password());

        //User 엔티티 생성
        User user = User.builder()
                .email(request.email())
                .password(encodedPwd)
                .name(request.name())
                .phoneNumber(request.phoneNumber())
                .profileImageUrl(null)
                .status(UserStatus.ACTIVE)
                .isEmailVerified(true) //회원가입 하는 사용자는 모두 이메일 인증 완료된 것
                .build();

        User savedUser = userRepository.save(user);

        //Response DTO 로 변환 및 반환
        return UserConverter.toSignInResponse(savedUser);
    }

    //이미 회원가입 된 회원의 비밀번호 재설정 메서드
    public void passwordReset(String email, String password) {
        //이메일 인증이 되어있는지 확인
        String isEmailVerified = redisUtil.getData("VERIFIED:" + email);

        //인증이 안되었다면,
        if (isEmailVerified == null || !isEmailVerified.equals("TRUE")) {
            throw new UserHandler(UserErrorCode.USER_EMAIL_NOT_VERIFIED); //예외 발생
        }

        //기존 비밀번호와 새 비밀번호가 일치할 시 예외 발생
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

        String oldPassword = user.getPassword();

        if (passwordEncoder.matches(password, oldPassword)) { //새로운 비밀번호 == 이전 비밀번호이면
            throw new UserHandler(UserErrorCode.USER_PASSWORD_SAME_AS_OLD); //예외 발생
        }

        //새 비밀번호 암호화 & 저장
        String newPassword = passwordEncoder.encode(password);
        //비밀번호 변경 (JPA Dirty Checking)
        user.resetPassword(newPassword);

        redisUtil.deleteData("VERIFIED:" + email);
    }

    /**
     * 마이페이지 조회
     * 파라미터 추가: userId 외에 'provider'(로그인 유형) 도 받기
     * 캐시 키 수정: key = "#userId + ':' + #provider"
     * 같은 유저(userId=1)라도 '구글'로 로그인했을 때와 '이메일'로 로그인했을 때
     * 응답 데이터(MyPageResponse의 provider 필드)가 다르므로 캐시를 구분해야 합니다.
     * 예) user:profile::1:GOOGLE / user:profile::1:EMAIL 로 따로 저장됨.
     */
    @Cacheable(value = "user:profile", key = "#userId + ':' + #provider", unless = "#result == null")
    @Transactional(readOnly = true)
    public MyPageResponse getMyPage(Long userId, String provider) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

        return UserConverter.toMyPageResponse(user, provider);
    }
}
