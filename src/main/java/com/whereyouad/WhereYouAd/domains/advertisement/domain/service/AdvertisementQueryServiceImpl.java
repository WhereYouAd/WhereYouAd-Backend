package com.whereyouad.WhereYouAd.domains.advertisement.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.AdvertisementResponse;
import com.whereyouad.WhereYouAd.domains.advertisement.application.mapper.AdvertisementConverter;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.AdvertisementHandler;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.code.AdvertisementErrorCode;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdGroupRepository;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.handler.OrgHandler;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.domains.project.exception.ProjectHandler;
import com.whereyouad.WhereYouAd.domains.project.exception.code.ProjectErrorCode;
import com.whereyouad.WhereYouAd.domains.project.persistence.entity.Project;
import com.whereyouad.WhereYouAd.domains.project.persistence.repository.ProjectRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class AdvertisementQueryServiceImpl implements AdvertisementQueryService {

    private final OrgRepository orgRepository;
    private final ProjectRepository projectRepository;
    private final AdContentRepository adContentRepository;

    @Override
    public AdvertisementResponse.AdContentInfoResponse readAdContent(Long orgId, Long projectId, Long adContentId) {
        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        Project project = projectRepository.findByIdandOrganizationId(projectId, orgId)
                .orElseThrow(() -> new ProjectHandler(ProjectErrorCode.PROJECT_NOT_FOUND));

        AdContent adContent = adContentRepository.findById(adContentId)
                .orElseThrow(() -> new AdvertisementHandler(AdvertisementErrorCode.ADCONTENT_NOT_FOUND));

        AdGroup adGroup = adContent.getAdGroup();

        return AdvertisementConverter.toAdContentInfo(adContent, AdvertisementConverter.toAdGroupInfo(adGroup));
    }

    @Override
    public AdvertisementResponse.AdContentInfosResponse readAdContents(Long orgId, Long projectId) {
        return null;
    }

    @Override
    public AdvertisementResponse.AdGroupInfoResponse readAdGroup(Long orgId, Long projectId, Long adContentId) {
        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        Project project = projectRepository.findByIdandOrganizationId(projectId, orgId)
                .orElseThrow(() -> new ProjectHandler(ProjectErrorCode.PROJECT_NOT_FOUND));

        AdContent adContent = adContentRepository.findById(adContentId)
                .orElseThrow(() -> new AdvertisementHandler(AdvertisementErrorCode.ADCONTENT_NOT_FOUND));

        AdGroup adGroup = adContent.getAdGroup();

        return AdvertisementConverter.toAdGroupInfo(adGroup);
    }
}
