package com.whereyouad.WhereYouAd.domains.organization.domain.service;

import com.whereyouad.WhereYouAd.domains.organization.application.dto.request.OrgRequest;
import com.whereyouad.WhereYouAd.domains.organization.application.dto.response.OrgResponse;
import com.whereyouad.WhereYouAd.domains.organization.application.mapper.OrgConverter;
import com.whereyouad.WhereYouAd.domains.organization.application.mapper.OrgMemberConverter;
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

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class OrgCRUDService {

    private final OrgRepository orgRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final UserRepository userRepository;

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

    public void modifyOrganization(Long userId, Long orgId, OrgRequest.Update request) {
        //TODO

    }

    public void removeOrganization(Long userId, Long orgId) {
        //TODO

    }
}
