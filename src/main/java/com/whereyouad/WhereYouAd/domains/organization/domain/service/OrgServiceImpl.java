package com.whereyouad.WhereYouAd.domains.organization.domain.service;

import com.whereyouad.WhereYouAd.domains.ai.persistence.repository.AIInsightReportRepository;
import com.whereyouad.WhereYouAd.domains.organization.application.dto.request.OrgRequest;
import com.whereyouad.WhereYouAd.domains.organization.application.dto.response.OrgResponse;
import com.whereyouad.WhereYouAd.domains.organization.application.mapper.OrgConverter;
import com.whereyouad.WhereYouAd.domains.organization.application.mapper.OrgMemberConverter;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgRole;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgStatus;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.handler.OrgHandler;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgInvitation;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgInvitationRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformConnectionRepository;
import com.whereyouad.WhereYouAd.domains.timeline.persistence.repository.TimelineRepository;
import com.whereyouad.WhereYouAd.domains.user.domain.service.EmailService;
import com.whereyouad.WhereYouAd.domains.user.exception.code.UserErrorCode;
import com.whereyouad.WhereYouAd.domains.user.exception.handler.UserHandler;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.UserRepository;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import com.whereyouad.WhereYouAd.infrastructure.client.aws.s3.S3UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class OrgServiceImpl implements OrgService {

    private final OrgRepository orgRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final OrgInvitationRepository orgInvitationRepository;
    private final TimelineRepository timelineRepository;
    private final AIInsightReportRepository aiInsightReportRepository;
    private final UserRepository userRepository;
    private final PlatformConnectionRepository platformConnectionRepository;

    private final RedisUtil redisUtil;
    private final EmailService emailService;
    private final S3UploadService s3UploadService;


    // 조직(워크스페이스) 생성 메서드
    public OrgResponse.Create createOrganization(Long userId, OrgRequest.Create request, MultipartFile imageFile) {

        // 유저 정보 추출
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

        // 만약 해당 User 가 이미 같은 name 을 가진 Organization 에 속해있으면 예외처리
        // 해당 User 의 OrgMember 를 모두 추출해서,
        List<OrgMember> orgMemberByUser = orgMemberRepository.findOrgMemberByUser(user);
        for (OrgMember orgMember : orgMemberByUser) {
            // OrgMember 내부 Organization 의 name 이 생성하려는 request 의 name 과 같으면
            if (orgMember.getOrganization().getName().equals(request.name())) {
                throw new OrgHandler(OrgErrorCode.ORG_NAME_DUPLICATE); // 예외처리
            }
        }

        //추가 : 로고 이미지 처리
        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = s3UploadService.uploadImage(imageFile);
        }

        try {
            // 조직 생성
            Organization organization = OrgConverter.toOrganization(userId, request, imageUrl);

            // OrgMember 생성
            OrgMember orgMember = OrgMemberConverter.toOrgMemberADMIN(user, organization);

            orgRepository.save(organization);
            orgMemberRepository.save(orgMember);

            return OrgConverter.toCreatedResponse(organization);
        } catch (Exception e) { //조직 생성 중 오류 발생 시
            log.error("조직 생성 실패: {}", e.getMessage(), e);
            if (imageUrl != null) { //로고 이미지 S3 에서 삭제 진행 (orphan 방지)
                try {
                    s3UploadService.deleteImageFromUrl(imageUrl);
                } catch (Exception deleteException) {
                    log.warn("조직 생성 실패 후 S3 이미지 삭제 실패: {}", imageUrl, deleteException);
                }
            }

            throw new OrgHandler(OrgErrorCode.ORG_CREATE_FAILED);
        }
    }

    //로그인한 회원이 속한 조직 모두 조회 메서드
    public OrgResponse.MyOrganizations getMyOrganizations(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new UserHandler(UserErrorCode.USER_NOT_FOUND));
        Long currentOrgId = user.getCurrentOrgId();

        //회원 id 로 OrgMember 모두 조회 -> DB 조회에서 OrgStatus.ACTIVE 인 Organization 만 포함하는 OrgMember 만 조회해 온다.
        List<OrgMember> orgMembers = orgMemberRepository.findOrgMemberByUserId(userId);

        //각각의 OrgMember 에서 SimpleInfo DTO 로 매핑
        List<OrgResponse.SimpleInfo> infos = orgMembers.stream()
                .map( m -> OrgConverter.toOrgSimpleInfo(m, currentOrgId))
                .toList();

        //마지막 반환 DTO 로 변환
        return OrgConverter.toMyOrganizations(infos);
    }

    @Override
    public OrgResponse.CurrentWorkspace setCurrentWorkspace(Long userId, Long orgId) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new UserHandler(UserErrorCode.USER_NOT_FOUND));

        orgMemberRepository.findByUserIdAndOrgId(userId, orgId).orElseThrow(() ->
                new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND));

        user.setCurrentOrgId(orgId);

        return new OrgResponse.CurrentWorkspace(orgId);
    }

    @Override
    public OrgResponse.CurrentWorkspace getCurrentWorkspace(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new UserHandler(UserErrorCode.USER_NOT_FOUND));

        Long currentOrgId = user.getCurrentOrgId();

        // 현재 설정한 워크스페이스가 없다면 null 반환
        return new OrgResponse.CurrentWorkspace(currentOrgId);
    }

    //하나의 조직에 대한 세부 사항(ID, 이름, 설명, logoUrl, createdAt)
    public OrgResponse.OrgDetail getOrganizationDetail(Long orgId) {
        //해당 조직 id 로 Organization 조회
        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        //Soft Delete 된 조직이면 예외처리
        if (organization.getStatus() == OrgStatus.DELETED) {
            throw new OrgHandler(OrgErrorCode.ORG_SOFT_DELETED);
        }

        //DTO 로 변환
        return OrgConverter.toOrgDetail(organization);
    }

    public OrgResponse.MyOrganizations getSoftDeletedOrgs(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

        //userId 일치하고, Organization 의 status 가 DELETED 인 OrgMember 만 조회
        List<OrgMember> orgMembers = orgMemberRepository.findOrgMemberByUserIdSoftDeleted(userId);

        //Soft Deleted 된 조직 없으면 빈 리스트 반환
        if (orgMembers.isEmpty()) {
            return OrgConverter.toMyOrganizations(Collections.emptyList());
        }

        //각각의 OrgMember 에서 SimpleInfo DTO 로 매핑
        List<OrgResponse.SimpleInfo> infos = orgMembers.stream()
                .map(OrgConverter::toOrgSimpleInfo)
                .toList();

        return OrgConverter.toMyOrganizations(infos);
    }

    // 조직 정보 수정 메서드
    public OrgResponse.Update modifyOrganization(Long userId, Long orgId, OrgRequest.Update request, MultipartFile imageFile) {
        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 만약 조직 정보 수정을 요청한 회원이 해당 조직을 생성한 회원이 아니라면,
        if (!organization.getOwnerUserId().equals(userId)) {
            throw new OrgHandler(OrgErrorCode.ORG_FORBIDDEN); // 예외처리
        }

        //조직 로고 이미지 처리 추가
        String oldLogoUrl = organization.getLogoUrl();
        //기본적으로는 기존 이미지 유지
        String finalLogoImageUrl = oldLogoUrl;

        //로고 이미지를 기본 공백 이미지로 하는 거라면
        if (request.isImageDeleted()) {
            finalLogoImageUrl = null; //URL 을 null 처리

            //기존 로고 이미지 존재 시 삭제
            if (oldLogoUrl != null) {
                try {
                    s3UploadService.deleteImageFromUrl(oldLogoUrl);
                } catch (Exception e) {
                    log.warn("조직 정보 수정 진행간에 S3 이미지 삭제 실패: {}", oldLogoUrl, e);
                }
            }

        } else if (imageFile != null && !imageFile.isEmpty()) { //이미지 변경이라면,
            //이미지 업로드
            finalLogoImageUrl = s3UploadService.uploadImage(imageFile);

            //기존 로고 이미지 존재 시 삭제
            if (oldLogoUrl != null) {
                try {
                    s3UploadService.deleteImageFromUrl(oldLogoUrl);
                } catch (Exception e) {
                    log.warn("조직 정보 수정 진행간에 S3 이미지 삭제 실패: {}", oldLogoUrl, e);
                }

            }

        }

        // 조직 정보 수정
        organization.modifyInfo(request);
        organization.modifyLogoImage(finalLogoImageUrl);

        // 변환 된 필드값과 해당 조직의 Id, updatedAt 가 포함된 DTO 로 반환
        return OrgConverter.toUpdatedResponse(organization);
    }

    public OrgResponse.Delete restoreOrganization(Long userId, Long orgId) {
        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 만약 조직 복구 요청한 회원이 해당 조직을 생성한 회원이 아니라면,
        if (!organization.getOwnerUserId().equals(userId)) {
            throw new OrgHandler(OrgErrorCode.ORG_FORBIDDEN); // 예외처리
        }

        // 조직이 이미 활성화 상태라면,
        if (organization.getStatus() == OrgStatus.ACTIVE) {
            throw new OrgHandler(OrgErrorCode.ORG_ALREADY_ACTIVE); // 예외처리
        }

        organization.restoreDelete(); // 조직 Soft Delete 복구

        return OrgConverter.toRestoredResponse(organization);
    }

    // 조직 삭제 메서드 -> Hard Delete (DB 에서 완전히 제거)
    public void removeOrganization(Long userId, Long orgId) {
        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 만약 조직 삭제 요청한 회원이 해당 조직을 생성한 회원이 아니라면,
        if (!organization.getOwnerUserId().equals(userId)) {
            throw new OrgHandler(OrgErrorCode.ORG_FORBIDDEN); // 예외처리
        }

        // 조직에 연동된 플랫폼 계정이 존재할 경우 오류
        // 별도 API 를 통해 사용자가 명시적 삭제 진행해야함
        if (!platformConnectionRepository.findByUserIdAndOrgId(userId, orgId).isEmpty()) {
            throw new OrgHandler(OrgErrorCode.ORG_PLATFORM_CONNECTED);
        }

        timelineRepository.deleteByOrganizationId(orgId);
        aiInsightReportRepository.deleteByOrganizationId(orgId);
        orgInvitationRepository.deleteByOrganizationId(orgId);

        String logoUrl = organization.getLogoUrl();

        // 해당 조직에 가입된 모든 회원들의 가입 정보 삭제
        List<OrgMember> orgMembers = orgMemberRepository.findOrgMemberByOrg(organization);

        // 현재 워크스페이스가 삭제되는 조직인 멤버들의 currentOrgId를 null로 초기화
        for (OrgMember member : orgMembers) {
            if (Objects.equals(member.getUser().getCurrentOrgId(), orgId)) {
                member.getUser().setCurrentOrgId(null);
            }
        }

        orgMemberRepository.deleteAll(orgMembers);

        // 조직 실제 삭제
        orgRepository.delete(organization);

        //추가 : 조직 로고 이미지 존재 시, 이미지를 S3 에서 삭제하는 로직 추가
        //조직 삭제 이후 이미지 삭제하여 이미지 삭제 실패 시 조직 삭제 실패 방지
        if (logoUrl != null) {
            try {
                s3UploadService.deleteImageFromUrl(logoUrl);
            } catch (Exception e) {
                log.warn("조직 삭제 간에 S3 이미지 삭제 실패: {}", logoUrl, e);
            }

        }
    }

    // 조직 삭제 메서드 -> Soft Delete (status 만 DELETED 로 변경)
    public void removeOrganizationSoft(Long userId, Long orgId) {
        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 만약 조직 삭제 요청한 회원이 해당 조직을 생성한 회원이 아니라면,
        if (!organization.getOwnerUserId().equals(userId)) {
            throw new OrgHandler(OrgErrorCode.ORG_FORBIDDEN);
        }

        // 조직에 연동된 플랫폼 계정이 존재할 경우 오류
        // 별도 API 를 통해 사용자가 명시적 삭제 진행해야함
        if (!platformConnectionRepository.findByUserIdAndOrgId(userId, orgId).isEmpty()) {
            throw new OrgHandler(OrgErrorCode.ORG_PLATFORM_CONNECTED);
        }

        // 현재 워크스페이스가 삭제되는 조직인 멤버들의 currentOrgId를 null로 초기화
        List<OrgMember> orgMembers = orgMemberRepository.findOrgMemberByOrg(organization);
        for (OrgMember member : orgMembers) {
            if (Objects.equals(member.getUser().getCurrentOrgId(), orgId)) {
                member.getUser().setCurrentOrgId(null);
            }
        }

        // 조직 status 만 DELETED 로 변경
        organization.softDelete();
    }

    // User Hard Delete 정리용 - 해당 User 가 owner 인 Soft Deleted Organization 들을 Hard Delete
    // (Soft Delete 시 'owner + 다른 멤버 존재' 케이스는 차단되므로, 여기서는 본인 1명만 속한 조직만 존재)
    @Override
    public void removeOrganizationsOwnedBySoftDeletedUser(Long userId) {
        List<Organization> targetOrganizations =
                orgRepository.findAllByOwnerUserIdAndStatus(userId, OrgStatus.DELETED);

        for (Organization organization : targetOrganizations) {
            Long orgId = organization.getId();
            String logoUrl = organization.getLogoUrl();

            // 조직 소속 OrgMember 전부 Hard Delete (탈퇴 회원 본인의 OrgMember 포함)
            List<OrgMember> orgMembers = orgMemberRepository.findOrgMemberByOrg(organization);
            orgMemberRepository.deleteAll(orgMembers);

            timelineRepository.deleteByOrganizationId(orgId);
            aiInsightReportRepository.deleteByOrganizationId(orgId);
            orgInvitationRepository.deleteByOrganizationId(orgId);

            // 조직 Hard Delete
            orgRepository.delete(organization);

            // 로고 이미지 S3 삭제 (실패 시 로그만 남기고 진행)
            if (logoUrl != null) {
                try {
                    s3UploadService.deleteImageFromUrl(logoUrl);
                } catch (Exception e) {
                    log.warn("탈퇴 회원 조직 Hard Delete - S3 로고 이미지 삭제 실패: {}", logoUrl, e);
                }
            }
        }
    }

    public void removeMemberFromOrg(Long userId, Long orgId, Long memberId) {

        // 0. 본인은 삭제 불가
        if(Objects.equals(userId, memberId)){
            throw new OrgHandler(OrgErrorCode.ORG_CANNOT_KICK_SELF);
        }

        // 1. 조직 존재 여부 확인
        orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 2. 요청자가 해당 조직의 ADMIN인지 확인
        OrgMember requester = orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND));

        if (requester.getRole() != OrgRole.ADMIN) {
            throw new OrgHandler(OrgErrorCode.ORG_MEMBER_FORBIDDEN);
        }

        // 3. 삭제 대상 멤버가 해당 조직에 존재하는지 확인
        OrgMember targetMember = orgMemberRepository.findByUserIdAndOrgId(memberId, orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND));

        // 4. 대상 맴버가 ADMIN이라면 추방 불가
        if (targetMember.getRole() == OrgRole.ADMIN) {
            throw new OrgHandler(OrgErrorCode.ORG_CANNOT_KICK_ADMIN);
        }

        // 추방되는 멤버의 현재 워크스페이스가 해당 조직이라면 null로 초기화
        if (Objects.equals(targetMember.getUser().getCurrentOrgId(), orgId)) {
            targetMember.getUser().setCurrentOrgId(null);
        }

        // 5. 중간 테이블에서 해당 멤버 삭제
        orgMemberRepository.delete(targetMember);
    }

    public OrgResponse.OrgMemberDTO updateOrgMembersRole(Long userId, Long orgId, Long memberId,
            OrgRequest.UpdateRole dto) {

        // 0. 본인 권한 변경 불가
        if (Objects.equals(userId, memberId)) {
            throw new OrgHandler(OrgErrorCode.ORG_CANNOT_ROLE_CHANGE_SELF);
        }

        // 1. 조직 존재 여부 확인
        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 2. 요청자가 해당 조직의 ADMIN인지 확인
        OrgMember requester = orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND));

        if (requester.getRole() != OrgRole.ADMIN) {
            throw new OrgHandler(OrgErrorCode.ORG_MEMBER_FORBIDDEN);
        }

        // 3. 권한 변경 대상 멤버 조회
        OrgMember orgMember = orgMemberRepository.findByUserIdAndOrgId(memberId, orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND));

        // 4. ADMIN -> MEMBER 강등 시: 조직 내 ADMIN이 2명 이상이어야만 허용
        // 해당 멤버 ADMIN, 요청 역할 MEMBER인 경우
        boolean isDemoting = orgMember.getRole() == OrgRole.ADMIN && dto.orgRole() == OrgRole.MEMBER;
        if (isDemoting) {
            long adminCount = orgMemberRepository.countByOrganizationIdAndRole(orgId, OrgRole.ADMIN);
            if (adminCount < 2) {
                throw new OrgHandler(OrgErrorCode.ORG_LAST_ADMIN);
            }
        }

        // 역할 변경 (더티체킹)
        orgMember.updateRole(dto.orgRole());

        // 변경된 멤버 정보를 DTO 로 반환
        return OrgConverter.toOrgMemberDTO(orgMember);
    }

    @Override
    // 조직 초대 이메일 보내기
    public OrgResponse.OrgInvitationResponse sendOrgInvitation(Long userId, Long orgId, String email) {
        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 초대자와 조직 관계 검증 (초대자가 조직의 멤버인지 확인)
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

        if (!orgMemberRepository.existsByUserAndOrganization(sender, organization)) {
            // 초대자가 조직 멤버가 아님 -> 권한 없음
            throw new OrgHandler(OrgErrorCode.ORG_FORBIDDEN);
        }

        // 초대 완료 여부 확인, 가입 여부에 상관 없이 이메일 발송
        userRepository.findUserByEmail(email).ifPresent(user -> {
            if (orgMemberRepository.existsByUserAndOrganization(user, organization)) {
                throw new OrgHandler(OrgErrorCode.ORG_MEMBER_ALREADY_ACTIVE);
            }
        });

        // 이미 동일한 이메일로 대기 중인 초대가 있는지 확인
        Optional<OrgInvitation> existingInvitation = orgInvitationRepository.findByEmailAndOrganization(email, organization);
        if (existingInvitation.isPresent()) {

            LocalDateTime inviteAt = existingInvitation.get().getInvitedAt();
            LocalDateTime now = LocalDateTime.now();

            // 초대 간격 5분 설정
            if(Duration.between(inviteAt, now).toMinutes() < 5){
                throw new OrgHandler(OrgErrorCode.ORG_ALREADY_INVITE);
            }
            // 5분 이상의 중복 초대의 경우 갱신만 진행(만료 시간도 같이 갱신)
            existingInvitation.get().updateInvitedAt();
        } else {
            // 신규 초대 테이블 저장
            OrgInvitation newInvitation = OrgConverter.toOrgInvitation(email, organization);
            orgInvitationRepository.save(newInvitation);
        }

        // Redis key = 임의의 UUID 토큰(조직 초대 이메일 내 링크를 구별)
        String token = UUID.randomUUID().toString();
        // Redis value = 조직 아이디와 이메일의 조합
        String value = orgId + ":" + email;
        redisUtil.setDataExpire("INVITE:" + token, value, 3600 * 24L);

        emailService.sendEmailForOrgInvitation(token, email, organization.getName());

        return new OrgResponse.OrgInvitationResponse(orgId, "조직 멤버 초대 이메일을 전송하였습니다.", email);
    }

    @Override
    // 조직 초대 수락 (이메일 내 링크 클릭 시)
    public OrgResponse.OrgInvitationResponse acceptOrgInvitation(Long userId, String token) {
        // 링크 만료 또는 유효하지 않을 시
        if (token == null)
            throw new OrgHandler(OrgErrorCode.ORG_INVITATION_INVALID);

        // Redis 내 UUID(key)에 대한 email(value) 비교
        String value = redisUtil.getData("INVITE:" + token);
        if (value == null) {
            throw new OrgHandler(OrgErrorCode.ORG_INVITATION_INVALID);
        }

        String[] valueForSplit = value.split(":");
        String email = valueForSplit[1];

        // 로그인한 사용자 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

        // 초대된 이메일과 현재 로그인한 사용자의 이메일이 일치하는지 확인
        if (!user.getEmail().equals(email)) {
            throw new OrgHandler(OrgErrorCode.ORG_INVITATION_FORBIDDEN_USER);
        }

        Organization organization = orgRepository.findById(Long.parseLong(valueForSplit[0]))
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 이미 멤버인지 중복 체크
        if (orgMemberRepository.existsByUserAndOrganization(user, organization))
            throw new OrgHandler(OrgErrorCode.ORG_MEMBER_ALREADY_ACTIVE);

        // OrgInvitation 에서 해당 초대 내역이 있는지 확인 후 삭제 (대기열 제거)
        orgInvitationRepository.findByEmailAndOrganization(email, organization)
                .ifPresent(orgInvitationRepository::delete);

        // 멤버 편입
        orgMemberRepository.save(OrgMemberConverter.toOrgMemberMEMBER(user, organization));

        // Redis 사용 토큰 삭제
        redisUtil.deleteData("INVITE:" + token);

        return new OrgResponse.OrgInvitationResponse(organization.getId(), "조직 멤버 초대 이메일을 수락하였습니다.", email);
    }

    // 조직 생성자(ownerUserId) 양도 메서드
    public void changeOwner(Long userId, Long orgId, OrgRequest.ChangeOwner request) {

        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 만약 생성자 양도를 요청한 회원이 해당 조직의 생성자 아닐 경우 오류
        if (!Objects.equals(organization.getOwnerUserId(), userId)) {
            throw new OrgHandler(OrgErrorCode.ORG_FORBIDDEN);
        }

        // 생성자 양도를 요청한 기존 조직 생성자(userId)와, 새로운 생성자(request.newOwnerUserId()) 가 동일할 경우 오류
        if (Objects.equals(userId, request.newOwnerUserId())) {
            throw new OrgHandler(OrgErrorCode.ORG_OWNER_SAME_AS_BEFORE);
        }

        // 새로운 생성자가 될 회원의 OrgMember 조회
        OrgMember newOwner = orgMemberRepository.findByUserIdAndOrgId(request.newOwnerUserId(), orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND));

        // 새로운 생성자는 ADMIN 만 가능
        if (newOwner.getRole() != OrgRole.ADMIN) {
            throw new OrgHandler(OrgErrorCode.ORG_MEMBER_FORBIDDEN);
        }

        // 생성자 양도 진행
        organization.changeOwner(request.newOwnerUserId());
    }
}
