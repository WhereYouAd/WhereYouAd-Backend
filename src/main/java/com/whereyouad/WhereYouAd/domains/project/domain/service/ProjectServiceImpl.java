package com.whereyouad.WhereYouAd.domains.project.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.exception.AdvertisementHandler;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.code.AdvertisementErrorCode;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdCampaignRepository;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgStatus;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.handler.OrgHandler;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.domains.project.application.dto.request.ProjectRequest;
import com.whereyouad.WhereYouAd.domains.project.application.dto.response.ProjectResponse;
import com.whereyouad.WhereYouAd.domains.project.application.mapper.ProjectConverter;
import com.whereyouad.WhereYouAd.domains.project.persistence.entity.Project;
import com.whereyouad.WhereYouAd.domains.project.persistence.repository.ProjectRepository;
import com.whereyouad.WhereYouAd.domains.user.exception.code.UserErrorCode;
import com.whereyouad.WhereYouAd.domains.user.exception.handler.UserHandler;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectServiceImpl implements ProjectService{

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AdCampaignRepository adCampaignRepository;
    private final OrgRepository orgRepository;
    private final OrgMemberRepository orgMemberRepository;

    @Override
    public ProjectResponse.CreatedResponse createProject(Long userId, Long orgId,ProjectRequest.CreateRequest request) {
        //회원 검증
        userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

        //조직 검증 및 엔티티 추출
        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        //조직 Soft Delete 되어있을 시 예외 발생
        if (organization.getStatus() == OrgStatus.DELETED) {
            throw new OrgHandler(OrgErrorCode.ORG_SOFT_DELETED);
        }

        //해당 회원이 조직에 속해있는지 검증
        Optional<OrgMember> orgMember = orgMemberRepository.findByUserIdAndOrgId(userId, organization.getId());
        if (orgMember.isEmpty()) {
            throw new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND);
        }

        //AdCampaign Id 들 기반 AdCampaign 엔티티 추출
        List<Long> campaignIds = request.campaignIds();

        List<AdCampaign> adCampaigns = new ArrayList<>();

        for (Long campaignId : campaignIds) {
            if (campaignId != null) {
                AdCampaign adCampaign = adCampaignRepository.findById(campaignId)
                        .orElseThrow(() -> new AdvertisementHandler(AdvertisementErrorCode.ADCAMPAIGN_NOT_FOUND));

                //만약 해당 AdCampaign 이 연관된 Project 존재 시, 예외 발생
                if (adCampaign.getProject() != null) {
                    throw new AdvertisementHandler(AdvertisementErrorCode.ADCAMPAIGN_ALREADY_RELATED);
                } else { //연관된 Project 없으면 추가 진행
                    adCampaigns.add(adCampaign);
                }
            }
        }

        //Project 엔티티 생성 및 저장
        Project project = Project.builder()
                .name(request.name())
                .description(request.description())
                .createdBy(userId)
                .organization(organization)
                .adCampaigns(adCampaigns)
                .build();

        projectRepository.save(project);

        for (AdCampaign adCampaign : adCampaigns) {
            adCampaign.relateProject(project);
        }

        return ProjectConverter.toCreatedResponse(project);
    }
}
