package com.whereyouad.WhereYouAd.domains.advertisement.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.service.adapi.meta.MetaAuthService;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.service.adapi.meta.MetaSyncService;

import com.whereyouad.WhereYouAd.global.adapi.exception.AdApiHandler;
import com.whereyouad.WhereYouAd.global.adapi.exception.code.AdApiErrorCode;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.dto.MetaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetaAdApiService {

    private final MetaAuthService metaAuthService;
    private final MetaSyncService metaSyncService;

    // OAuth 인증 URL 반환 (Controller 통신용)
    public MetaResponse.AuthUrlResponse getAuthorizationUrl(Long userId, Long orgId) {
        return metaAuthService.getAuthorizationUrl(userId, orgId);
    }

    // OAuth 콜백 핸들러 및 전체 동기화
    public MetaResponse.MetaSyncSummary handleCallback(String state, String code) {
        Long orgId;
        Long userId;

        try {
            String[] splitState = state.split("-");
            orgId = Long.parseLong(splitState[0]);
            userId = Long.parseLong(splitState[1]);
        } catch (Exception e) {
            throw new AdApiHandler(AdApiErrorCode.INVALID_API_CREDENTIALS);
        }

        // 인증 콜백 처리 및 Connection DB 저장
        metaAuthService.processAuthCallbackAndSave(orgId, userId, code);
        // 즉시 동기화 실행 (과거 기준 30일)
        String startDate = LocalDate.now().minusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String endDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        return metaSyncService.syncAll(orgId, startDate, endDate);
    }

    // 관리자용 수동 동기화 또는 스케줄러 호출용
    public MetaResponse.MetaSyncSummary syncAll(Long orgId, String startDate, String endDate) {
        return metaSyncService.syncAll(orgId, startDate, endDate);
    }

}
