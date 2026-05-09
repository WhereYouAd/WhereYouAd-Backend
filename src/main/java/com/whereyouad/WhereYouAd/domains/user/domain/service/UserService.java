package com.whereyouad.WhereYouAd.domains.user.domain.service;

import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.user.application.dto.request.UserInfoModifyRequest;
import com.whereyouad.WhereYouAd.domains.user.application.dto.response.MyOrgResponse;
import com.whereyouad.WhereYouAd.domains.user.application.dto.response.MyPageResponse;
import com.whereyouad.WhereYouAd.domains.user.application.dto.response.UserInfoModifiedResponse;
import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.user.exception.handler.UserHandler;
import com.whereyouad.WhereYouAd.domains.user.exception.code.UserErrorCode;
import com.whereyouad.WhereYouAd.domains.user.domain.constant.UserStatus;
import com.whereyouad.WhereYouAd.domains.user.application.mapper.UserConverter;
import com.whereyouad.WhereYouAd.domains.user.application.dto.request.SignUpRequest;
import com.whereyouad.WhereYouAd.domains.user.application.dto.response.SignUpResponse;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.UserRepository;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import com.whereyouad.WhereYouAd.infrastructure.client.aws.s3.S3UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisUtil redisUtil;
    private final S3UploadService s3UploadService;

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
    @Transactional(readOnly = true)
    public MyPageResponse getMyPage(Long userId, String provider) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

        //추가 : 사용자가 속한 조직의 정보(Id, name, OrgRole) 함께 반환
        List<OrgMember> orgMembers = orgMemberRepository.findOrgMemberByUserId(userId);

        List<MyOrgResponse> orgResponses = new ArrayList<>();

        for (OrgMember orgMember : orgMembers) {
            MyOrgResponse myOrgResponse = UserConverter.toMyOrgResponse(orgMember);
            orgResponses.add(myOrgResponse);
        }

        return UserConverter.toMyPageResponse(user, provider, orgResponses);
    }

    //회원 정보(이름, 프로필 이미지, 비밀번호 변경)
    public UserInfoModifiedResponse modifyUserInfo(Long userId, Provider provider, UserInfoModifyRequest request, MultipartFile image) {
        // 회원 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

        // 최종적으로 DB에 저장될 이름 (기본값: 기존 이름)
        String finalName = user.getName();
        if (request.name() != null && !request.name().isBlank()) {
            finalName = request.name();
        }

        // 최종적으로 DB에 저장될 비밀번호 (기본값: 기존 비밀번호)
        String finalEncodedPassword = user.getPassword();

        if (provider == Provider.EMAIL) {
            // 이메일 로그인 : 비밀번호 변경을 시도했는지 확인 (새 비밀번호 값이 들어왔는지)
            boolean isPwdChangeRequested = request.newPassword() != null && !request.newPassword().isBlank();

            if (isPwdChangeRequested) {
                // 새 비밀번호는 입력했는데, 기존 비밀번호를 안 적은 경우 오류
                if (request.oldPassword() == null || request.oldPassword().isBlank()) {
                    throw new UserHandler(UserErrorCode.USER_OLD_PASSWORD_REQUIRED);
                }

                // 기존 비밀번호 일치 확인
                if (!passwordEncoder.matches(request.oldPassword(), finalEncodedPassword)) {
                    throw new UserHandler(UserErrorCode.USER_PASSWORD_NOT_CORRECT);
                }

                // 기존 비밀번호와 새 비밀번호가 같은지 확인
                if (request.oldPassword().equals(request.newPassword())) {
                    throw new UserHandler(UserErrorCode.USER_PASSWORD_SAME_AS_OLD);
                }

                // 모든 검증 통과 시 새 비밀번호 암호화해서 덮어쓰기
                finalEncodedPassword = passwordEncoder.encode(request.newPassword());
            }
            // 비밀번호 변경 의도가 없다면(isPasswordChangeRequested == false)
            // finalEncodedPassword는 맨 위에서 가져온 기존 비밀번호 그대로 유지

        } else {
            // 소셜 로그인 : 비밀번호 관련 값이 하나라도 들어왔다면 에러 발생
            boolean hasPasswordRequest = (request.newPassword() != null && !request.newPassword().isBlank()) ||
                    (request.oldPassword() != null && !request.oldPassword().isBlank());

            if (hasPasswordRequest) {
                throw new UserHandler(UserErrorCode.SOCIAL_USER_PASSWORD_CANNOT_MODIFY);
            }
        }

        // 프로필 이미지 변경 로직
        String oldProfileImageUrl = user.getProfileImageUrl();
        String finalImageUrl = oldProfileImageUrl; // 기본값은 기존 이미지 유지

        //이미지를 기본 프로필 사진 바꾸는 거라면(프로필 사진 삭제 요청이라면)
        if (request.isImageDeleted()) {
            finalImageUrl = null; //최종 프로필 사진 URL 값을 null 로 지정

            if (oldProfileImageUrl != null) { //기존 프로필 사진 존재했다면 삭제
                s3UploadService.deleteImageFromUrl(oldProfileImageUrl);
            }

        } else if (image != null && !image.isEmpty()) { //새 프로필 이미지 등록이라면,
            finalImageUrl = s3UploadService.uploadImage(image); //새 이미지를 S3 업로드 하고 이미지 URL 받기

            //기존 이미지 존재 시 삭제
            if (oldProfileImageUrl != null) {
                s3UploadService.deleteImageFromUrl(oldProfileImageUrl);
            }
        }

        // 엔티티 수정 (이름, 이미지, 비밀번호)
        user.modifyInfo(finalName, finalImageUrl, finalEncodedPassword);

        return UserConverter.toUserInfoResponse(user.getId(), user.getName(), user.getProfileImageUrl());
    }
}
