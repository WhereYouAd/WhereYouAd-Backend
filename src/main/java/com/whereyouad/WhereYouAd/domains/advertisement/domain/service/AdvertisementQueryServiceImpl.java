package com.whereyouad.WhereYouAd.domains.advertisement.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.AdvertisementResponse;
import com.whereyouad.WhereYouAd.domains.advertisement.application.mapper.AdvertisementConverter;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.AdvertisementHandler;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.code.AdvertisementErrorCode;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class AdvertisementQueryServiceImpl implements AdvertisementQueryService {

    private final AdContentRepository adContentRepository;

    @Override
    public AdvertisementResponse.AdContentInfoResponse readAdContent(Long orgId, Long projectId, Long adContentId) {
        AdContent adContent = adContentRepository.findByIdWithValidation(adContentId, projectId, orgId)
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
        AdContent adContent = adContentRepository.findByIdWithValidation(adContentId, projectId, orgId)
                .orElseThrow(() -> new AdvertisementHandler(AdvertisementErrorCode.ADCONTENT_NOT_FOUND));

        AdGroup adGroup = adContent.getAdGroup();

        return AdvertisementConverter.toAdGroupInfo(adGroup);
    }
}
