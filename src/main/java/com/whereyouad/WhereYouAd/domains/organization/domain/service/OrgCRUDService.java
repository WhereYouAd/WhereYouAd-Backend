package com.whereyouad.WhereYouAd.domains.organization.domain.service;

import com.whereyouad.WhereYouAd.domains.organization.application.dto.request.OrgRequest;
import com.whereyouad.WhereYouAd.domains.organization.application.dto.response.OrgResponse;
import com.whereyouad.WhereYouAd.domains.organization.application.mapper.OrgConverter;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgRole;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgStatus;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.handler.OrgHandler;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.domains.user.exception.code.UserErrorCode;
import com.whereyouad.WhereYouAd.domains.user.exception.handler.UserHandler;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class OrgCRUDService {

    private final OrgRepository orgRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final UserRepository userRepository;

    public OrgResponse.Create createOrganization(Long userId, OrgRequest.Create request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

        //만약 해당 User 가 이미 같은 name 을 가진 Organization 에 속해있으면 예외처리
        List<OrgMember> orgMemberByUser = orgMemberRepository.findOrgMemberByUser(user);
        for (OrgMember orgMember : orgMemberByUser) {
            if (orgMember.getOrganization().getName().equals(request.name())) {
                throw new OrgHandler(OrgErrorCode.ORG_NAME_DUPLICATE);
            }
        }

        Organization organization = Organization.builder()
                .name(request.name())
                .description(request.description())
                .logoUrl(request.logoUrl())
                .ownerUserId(userId)
                .status(OrgStatus.ACTIVE)
                .build();

        OrgMember orgMember = OrgMember.builder()
                .user(user)
                .organization(organization)
                .joinedAt(LocalDateTime.now())
                .role(OrgRole.ADMIN)
                .build();

        orgRepository.save(organization);
        orgMemberRepository.save(orgMember);

        return OrgConverter.toCreatedResponse(organization);
    }

    public OrgResponse.Read getOrganization(Long userId) {
        //TODO
        return null;
    }

    public void modifyOrganization(Long userId, Long orgId, OrgRequest.Update request) {
        //TODO

    }

    public void removeOrganization(Long userId, Long orgId) {
        //TODO

    }
}
