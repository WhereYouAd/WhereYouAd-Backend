package com.whereyouad.WhereYouAd.domains.advertisement.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.service.adapi.meta.MetaAuthService;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.service.adapi.meta.MetaSyncService;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgRole;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.handler.OrgHandler;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;

import com.whereyouad.WhereYouAd.global.adapi.exception.AdApiHandler;
import com.whereyouad.WhereYouAd.global.adapi.exception.code.AdApiErrorCode;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
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
    private final OrgRepository orgRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final RedisUtil redisUtil;

    // 사용자 갱신 버튼 기본 범위(7일)
    private static final int USER_REFRESH_DEFAULT_DAYS = 7;
    // 조직당 재요청 쿨다운(60초)
    private static final long USER_REFRESH_COOLDOWN_SECONDS = 60L;
    // 중복 실행 방지 락 TTL(300초) - 동기화 평균 수행 시간 + 여유
    private static final long USER_REFRESH_LOCK_TTL_SECONDS = 300L;

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

    // 사용자가 Meta 마케팅 정보에 대해 갱신(refresh) 버튼 클릭시 정보 갱신 메서드 -> 조직 관리자(ADMIN) 만 요청 가능
    public MetaResponse.MetaSyncSummary refreshForUser(Long userId, Long orgId) {

        // 조직 존재 + 요청 사용자가 해당 조직 멤버인지 검증 
        if (!orgRepository.existsById(orgId)) {
            throw new OrgHandler(OrgErrorCode.ORG_NOT_FOUND);
        }
        if (!orgMemberRepository.existsByUserIdAndOrganizationId(userId, orgId)) {
            throw new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND);
        }

        OrgMember orgMember = orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND));

        //조직 내 ADMIN 만 갱신 가능하도록 제약
        if (orgMember.getRole() != OrgRole.ADMIN) {
            throw new OrgHandler(OrgErrorCode.ORG_MEMBER_FORBIDDEN);
        }

        // 쿨다운 체크 - 동일 조직에 대해 과도한 연타 요청 차단
        String cooldownKey = "meta:sync:cooldown:" + orgId;
        if (redisUtil.getData(cooldownKey) != null) {
            throw new AdApiHandler(AdApiErrorCode.SYNC_COOLDOWN);
        }

        // 분산 락 - 동시 실행 1건으로 제한
        String lockKey = "meta:sync:lock:" + orgId;
        Boolean acquired = redisUtil.setIfAbsent(
                lockKey,
                String.valueOf(System.currentTimeMillis()),
                USER_REFRESH_LOCK_TTL_SECONDS
        );

        //이미 동기화 진행중인 경우 오류 처리
        if (!Boolean.TRUE.equals(acquired)) {
            throw new AdApiHandler(AdApiErrorCode.SYNC_IN_PROGRESS);
        }

        try {
            // 기본 동기화 범위(7일) 설정
            // 사용자 요청을 받아 기간 설정 하도록 발전 시 변경 필요
            String startDate = LocalDate.now().minusDays(USER_REFRESH_DEFAULT_DAYS).format(DateTimeFormatter.ISO_LOCAL_DATE);
            String endDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

            // 동기화 진행
            return metaSyncService.syncAll(orgId, startDate, endDate);

        } finally {
            // 락 해제 + 쿨다운 설정
            redisUtil.deleteData(lockKey);
            redisUtil.setDataExpire(cooldownKey, "1", USER_REFRESH_COOLDOWN_SECONDS);
        }
    }

}
