package com.whereyouad.WhereYouAd.domains.advertisement.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.AdvertisementResponse;
import com.whereyouad.WhereYouAd.domains.advertisement.application.mapper.AdvertisementConverter;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.AdvertisementHandler;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.code.AdvertisementErrorCode;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.handler.OrgHandler;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class AdvertisementQueryServiceImpl implements AdvertisementQueryService {

    private final AdContentRepository adContentRepository;
    private final OrgMemberRepository orgMemberRepository;

    @Override
    public AdvertisementResponse.AdContentInfoResponse readAdContent(Long userId, Long orgId, Long projectId, Long adContentId) {
        // 유저가 해당 조직인지 검증 (권한 검증)
        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND));

        // 조직, 프로젝트와의 무결성 검증 및 조회
        AdContent adContent = adContentRepository.findByIdWithValidation(adContentId, projectId, orgId)
                .orElseThrow(() -> new AdvertisementHandler(AdvertisementErrorCode.ADCONTENT_NOT_FOUND));

        AdGroup adGroup = adContent.getAdGroup();
        if (adGroup == null)
            throw new AdvertisementHandler(AdvertisementErrorCode.ADGROUP_NOT_FOUND);

        return AdvertisementConverter.toAdContentInfo(adContent, AdvertisementConverter.toAdGroupInfo(adGroup));
    }

    @Override
    public AdvertisementResponse.AdContentInfosResponse readAdContents(Long userId, Long orgId, Long projectId) {
        // 유저가 해당 조직인지 검증 (권한 검증)
        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND));

        List<AdContent> adContents = adContentRepository.findAllByIdWithValidation(projectId, orgId);

        return AdvertisementConverter.toAdContentsInfo(adContents);
    }

    @Override
    public AdvertisementResponse.AdGroupInfoResponse readAdGroup(Long userId, Long orgId, Long projectId, Long adContentId) {
        // 유저가 해당 조직인지 검증 (권한 검증)
        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND));

        // 조직, 프로젝트와의 무결성 검증 및 조회
        AdContent adContent = adContentRepository.findByIdWithValidation(adContentId, projectId, orgId)
                .orElseThrow(() -> new AdvertisementHandler(AdvertisementErrorCode.ADCONTENT_NOT_FOUND));

        AdGroup adGroup = adContent.getAdGroup();
        if (adGroup == null)
            throw new AdvertisementHandler(AdvertisementErrorCode.ADGROUP_NOT_FOUND);

        return AdvertisementConverter.toAdGroupInfo(adGroup);
    }
}
