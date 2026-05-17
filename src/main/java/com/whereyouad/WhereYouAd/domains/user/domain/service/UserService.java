package com.whereyouad.WhereYouAd.domains.user.domain.service;

import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformAccountRepository;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformConnectionRepository;
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
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.AuthProviderAccount;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.AuthProviderAccountRepository;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.UserRepository;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import com.whereyouad.WhereYouAd.infrastructure.client.aws.s3.S3UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final AuthProviderAccountRepository authProviderAccountRepository;
    private final PlatformConnectionRepository platformConnectionRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisUtil redisUtil;
    private final S3UploadService s3UploadService;
    private final OrgRepository orgRepository;

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
     * 조직 정보 (Id, name, OrgRole) 함께 출력
     */
    @Transactional(readOnly = true)
    public MyPageResponse getMyPage(Long userId, String provider) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

        //추가 : 사용자가 속한 조직의 정보(Id, name, OrgRole) 함께 반환
        List<OrgMember> orgMembers = orgMemberRepository.findOrgMemberByUserId(userId);

        List<MyOrgResponse> orgResponses = orgMembers.stream()
                .map(UserConverter::toMyOrgResponse)
                .toList();

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

    // 회원 탈퇴
    // 만약 탈퇴하려는 회원이 Organization 의 owner (organization.getOwnerUserId()) 면 탈퇴 불가 -> 양도 먼저 진행 필요
    public void deleteUser(Long userId) {
        // 회원 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

        // 회원 프로필 이미지 S3 URL 추출
        String profileImageUrl = user.getProfileImageUrl();

        // 회원이 속한 조직 List 조회
        List<OrgMember> orgMembers = orgMemberRepository.findOrgMemberByUserId(userId);

        List<Organization> organizations = orgMembers.stream()
                .map(OrgMember::getOrganization)
                .toList();

        // 회원이 속한 모든 조직 중에서
        for (Organization organization : organizations) {
            if (Objects.equals(organization.getOwnerUserId(), userId)) { // 만약 회원이 생성자인 조직이 있으면
                // 해당 조직의 회원 수를 조회
                long memberCount = orgMemberRepository.countByOrganizationId(organization.getId());

                if (memberCount > 1) { // 회원 수가 1 초과이면 (본인 제외 다른 회원이 조직에 속해있으면)
                    // 탈퇴 불가(조직 생성자 양도부터 먼저 진행해야한다.)
                    throw new UserHandler(UserErrorCode.USER_OWNS_ORGANIZATION);
                }

                // 회원 수가 1 이면 본인만 속한 조직(별도 회원이 없는 조직) 이므로 Soft Delete 진행
                organization.softDelete();
            }
        }

        // User 를 참조하는 OrgMember 제거 (Soft Delete 된 조직 멤버십까지 포함)
        orgMemberRepository.deleteAll(orgMemberRepository.findOrgMemberByUser(user));

        // 소셜 로그인 사용자라면 연결된 AuthProviderAccount 도 제거
        // 논의점: 해당 회원의 이메일로 연동된 소셜로그인 정보 삭제를 어떻게 처리할지?
        List<AuthProviderAccount> authProviderAccounts = authProviderAccountRepository.findByUserEmail(user.getEmail());
        if (!authProviderAccounts.isEmpty()) {
            authProviderAccountRepository.deleteAll(authProviderAccounts);
        }

        // 광고 플랫폼 연동(PlatformConnection) 및 함께 연관된 PlatformAccount 제거
        // 광고 플랫폼 연동 정보 존재 시 예외처리
        // TODO : 광고 플랫폼 연동 정보 삭제 & 관련된 광고 엔티티 (AdCampaign, AdGroup, AdContent, MetricFact) 삭제 API 추가
        List<PlatformConnection> platformConnections = platformConnectionRepository.findByUser_Id(userId);
        if (!platformConnections.isEmpty()) {
            throw new UserHandler(UserErrorCode.USER_HAS_PLATFORM_CONNECTION);
        }

        // 회원 삭제
        userRepository.deleteById(userId);

        // 회원 삭제 이후 S3 에서 이미지 삭제 진행
        try {
            s3UploadService.deleteImageFromUrl(profileImageUrl);
        } catch (Exception e) {
            // 이미지 삭제에 실패하더라도 로그 처리만 하고 탈퇴는 정상 진행
            log.error("회원 탈퇴 과정에서 S3 이미지 삭제 오류 발생");
        }
    }
}
