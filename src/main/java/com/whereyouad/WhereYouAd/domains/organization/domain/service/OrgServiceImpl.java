package com.whereyouad.WhereYouAd.domains.organization.domain.service;

import com.whereyouad.WhereYouAd.domains.organization.application.dto.request.OrgRequest;
import com.whereyouad.WhereYouAd.domains.organization.application.dto.response.OrgResponse;
import com.whereyouad.WhereYouAd.domains.organization.application.mapper.OrgConverter;
import com.whereyouad.WhereYouAd.domains.organization.application.mapper.OrgMemberConverter;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgStatus;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.handler.OrgHandler;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.domains.user.domain.service.EmailService;
import com.whereyouad.WhereYouAd.domains.user.exception.code.UserErrorCode;
import com.whereyouad.WhereYouAd.domains.user.exception.handler.UserHandler;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class OrgServiceImpl implements OrgService{

    private final OrgRepository orgRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final UserRepository userRepository;

    private final EmailService emailService;

    //조직(워크스페이스) 생성 메서드
    public OrgResponse.Create createOrganization(Long userId, OrgRequest.Create request) {

        //유저 정보 추출
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

        //만약 해당 User 가 이미 같은 name 을 가진 Organization 에 속해있으면 예외처리
        //해당 User 의 OrgMember 를 모두 추출해서,
        List<OrgMember> orgMemberByUser = orgMemberRepository.findOrgMemberByUser(user);
        for (OrgMember orgMember : orgMemberByUser) {
            //OrgMember 내부 Organization 의 name 이 생성하려는 request 의 name 과 같으면
            if (orgMember.getOrganization().getName().equals(request.name())) {
                throw new OrgHandler(OrgErrorCode.ORG_NAME_DUPLICATE); //예외처리
            }
        }

        //조직 생성
        Organization organization = OrgConverter.toOrganization(userId, request);

        //OrgMember 생성
        OrgMember orgMember = OrgMemberConverter.toOrgMemberADMIN(user, organization);

        orgRepository.save(organization);
        orgMemberRepository.save(orgMember);

        return OrgConverter.toCreatedResponse(organization);
    }

    public OrgResponse.Read getOrganization(Long userId) {
        //TODO
        return null;
    }

    //조직 정보 수정 메서드
    public OrgResponse.Update modifyOrganization(Long userId, Long orgId, OrgRequest.Update request) {
        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        //만약 조직 정보 수정을 요청한 회원이 해당 조직을 생성한 회원이 아니라면,
        if (!organization.getOwnerUserId().equals(userId)) {
            throw new OrgHandler(OrgErrorCode.ORG_FORBIDDEN); //예외처리
        }

        //조직 정보 수정
        organization.modifyInfo(request);

        //변환 된 필드값과 해당 조직의 Id, updatedAt 가 포함된 DTO 로 반환
        return OrgConverter.toUpdatedResponse(organization);
    }

    public OrgResponse.Delete restoreOrganization(Long userId, Long orgId) {
        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        //만약 조직 복구 요청한 회원이 해당 조직을 생성한 회원이 아니라면,
        if (!organization.getOwnerUserId().equals(userId)) {
            throw new OrgHandler(OrgErrorCode.ORG_FORBIDDEN); //예외처리
        }

        //조직이 이미 활성화 상태라면,
        if (organization.getStatus() == OrgStatus.ACTIVE) {
            throw new OrgHandler(OrgErrorCode.ORG_ALREADY_ACTIVE); //예외처리
        }

        organization.restoreDelete(); //조직 Soft Delete 복구

        return OrgConverter.toRestoredResponse(organization);
    }

    //조직 삭제 메서드 -> Hard Delete (DB 에서 완전히 제거)
    public void removeOrganization(Long userId, Long orgId) {
        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        //만약 조직 삭제 요청한 회원이 해당 조직을 생성한 회원이 아니라면,
        if (!organization.getOwnerUserId().equals(userId)) {
            throw new OrgHandler(OrgErrorCode.ORG_FORBIDDEN); //예외처리
        }

        //해당 조직에 가입된 모든 회원들의 가입 정보 삭제
        List<OrgMember> orgMembers = orgMemberRepository.findOrgMemberByOrg(organization);

        orgMemberRepository.deleteAll(orgMembers);

        //조직 실제 삭제
        orgRepository.delete(organization);
    }

    //조직 삭제 메서드 -> Soft Delete (status 만 DELETED 로 변경)
    public void removeOrganizationSoft(Long userId, Long orgId) {
        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        //만약 조직 삭제 요청한 회원이 해당 조직을 생성한 회원이 아니라면,
        if (!organization.getOwnerUserId().equals(userId)) {
            throw new OrgHandler(OrgErrorCode.ORG_FORBIDDEN);
        }

        //조직 status 만 DELETED 로 변경 후 종료
        organization.softDelete();
    }

    @Override
    public OrgResponse.OrgInvitationResponse sendOrgInvitation(Long orgId, String email) {
        Organization organization = orgRepository.findById(orgId).orElseThrow(() ->
            new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 유저 회원 가입 유무
        User user = userRepository.findUserByEmail(email).orElseThrow(() ->
                new UserHandler(UserErrorCode.USER_NOT_FOUND));

        // 회원 가입 미완료 (유저가 아닐 시)
        if (user == null) {
            emailService.sendEmailForOrgInvitation(email);
        }

        // 회원 가입 완료 (이미 존재하는 유저)
        else {
            // 이미 초대 완료
            if (orgMemberRepository.existsByUser(user))
                new OrgHandler(OrgErrorCode.ORG_MEMBER_ALREADY_ACTIVE);

            else emailService.sendEmailForOrgInvitation(email);
        }
        return new OrgResponse.OrgInvitationResponse(orgId, "조직 멤버 초대 이메일을 전송하였습니다.", email);
    }
}
