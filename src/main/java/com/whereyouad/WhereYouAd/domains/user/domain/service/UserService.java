package com.whereyouad.WhereYouAd.domains.user.domain.service;

import com.whereyouad.WhereYouAd.domains.user.exception.UserErrorCode;
import com.whereyouad.WhereYouAd.domains.user.domain.constant.UserStatus;
import com.whereyouad.WhereYouAd.domains.user.application.mapper.UserConverter;
import com.whereyouad.WhereYouAd.domains.user.application.dto.request.SignUpRequest;
import com.whereyouad.WhereYouAd.domains.user.application.dto.response.SignUpResponse;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.UserRepository;
import com.whereyouad.WhereYouAd.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    //회원가입 메서드
    public SignUpResponse signUpUser(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) { //이미 이메일로 만든 계정이 존재할 시
            throw new CustomException(UserErrorCode.USER_EMAIL_DUPLICATE); //이메일 중복 예외
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
                .isEmailVerified(false)
                .build();

        User savedUser = userRepository.save(user);

        //Response DTO 로 변환 및 반환
        return UserConverter.toSignInResponse(savedUser);
    }
}
