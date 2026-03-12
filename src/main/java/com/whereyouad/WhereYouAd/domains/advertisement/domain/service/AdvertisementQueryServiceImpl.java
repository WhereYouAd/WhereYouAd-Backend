package com.whereyouad.WhereYouAd.domains.advertisement.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.AdvertisementResponse;
import com.whereyouad.WhereYouAd.domains.advertisement.application.mapper.AdvertisementConverter;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.AdvertisementHandler;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.code.AdvertisementErrorCode;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdCampaignRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.handler.OrgHandler;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class AdvertisementQueryServiceImpl implements AdvertisementQueryService {

    private final AdContentRepository adContentRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final AdCampaignRepository adCampaignRepository;

    @Override
    public AdvertisementResponse.AdContentInfoResponse readAdContent(Long userId, Long orgId, Long projectId,
            Long adContentId) {
        // 유저가 해당 조직인지 검증 (권한 검증)
        if (!orgMemberRepository.existsByUserIdAndOrganizationId(userId, orgId)) {
            throw new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND);
        }

        // 조직, 프로젝트와의 무결성 검증 및 조회
        AdContent adContent = adContentRepository.findByIdWithValidation(adContentId, projectId, orgId)
                .orElseThrow(() -> new AdvertisementHandler(AdvertisementErrorCode.ADCONTENT_NOT_FOUND));

        AdGroup adGroup = adContent.getAdGroup();

        return AdvertisementConverter.toAdContentInfo(adContent, AdvertisementConverter.toAdGroupInfo(adGroup));
    }

    @Override
    public AdvertisementResponse.AdContentInfosResponse readAdContents(Long userId, Long orgId, Long projectId) {
        // 유저가 해당 조직인지 검증 (권한 검증)
        if (!orgMemberRepository.existsByUserIdAndOrganizationId(userId, orgId)) {
            throw new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND);
        }

        List<AdContent> adContents = adContentRepository.findAllByIdWithValidation(projectId, orgId);

        return AdvertisementConverter.toAdContentsInfo(adContents);
    }

    @Override
    public AdvertisementResponse.AdGroupInfoResponse readAdGroup(Long userId, Long orgId, Long projectId,
            Long adContentId) {
        // 유저가 해당 조직인지 검증 (권한 검증)
        if (!orgMemberRepository.existsByUserIdAndOrganizationId(userId, orgId)) {
            throw new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND);
        }

        // 조직, 프로젝트와의 무결성 검증 및 조회
        AdContent adContent = adContentRepository.findByIdWithValidation(adContentId, projectId, orgId)
                .orElseThrow(() -> new AdvertisementHandler(AdvertisementErrorCode.ADCONTENT_NOT_FOUND));

        AdGroup adGroup = adContent.getAdGroup();

        return AdvertisementConverter.toAdGroupInfo(adGroup);
    }

    //Project 엔티티 생성을 위해, 기존에 Project 와 연관되지 않은 AdCampaign 을 Provider 값과 orgId 기반 조회하는 메서드
    //API 연동시, AdCampaign 내부 organization 필드와 함께 리팩티렁 필요
    @Override
    public AdvertisementResponse.AdCampaignListResponse readAdCampaigns(Long userId, Long orgId, String providerType) {
        if (!orgMemberRepository.existsByUserIdAndOrganizationId(userId, orgId)) {
            throw new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND);
        }

        Provider provider;
        try { //providerType 에 잘못된 값이 입력되지 않았는지 검증
            provider = Provider.valueOf(providerType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AdvertisementHandler(AdvertisementErrorCode.INVALID_PROVIDER_VALUE);
        }

        //orgId, Provider 가 일치하고, project 가 null 인 AdCampaign 리스트 추출
        List<AdCampaign> adCampaigns = adCampaignRepository.findByOrgIdAndProviderWithNullProject(orgId, provider);

        //각각의 AdCampaign 엔티티를 AdCampaignSimpleResponse DTO 로 변환
        List<AdvertisementResponse.AdCampaignSimpleResponse> simpleResponses = adCampaigns.stream()
                .map(AdvertisementConverter::toAdCampaignSimple)
                .toList();

        //최종 응답값으로 변환
        return AdvertisementConverter.toAdCampaignList(simpleResponses);
    }
}
