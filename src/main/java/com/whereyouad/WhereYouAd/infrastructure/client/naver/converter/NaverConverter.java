package com.whereyouad.WhereYouAd.infrastructure.client.naver.converter;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Goal;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import com.whereyouad.WhereYouAd.infrastructure.client.naver.dto.NaverDTO;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class NaverConverter {

    // 캠페인 생성용
    public static AdCampaign toAdCampaignEntity(NaverDTO.CampaignResponse dto, Organization organization, PlatformAccount platformAccount) {
        Goal goal = mapToDomainGoal(dto.campaignTp());
        return AdCampaign.builder()
                .externalCampaignId(dto.nccCampaignId())
                .name(dto.name())
                .provider(Provider.NAVER)
                .status(mapToDomainStatus(dto.status()))
                .organization(organization)
                .platformAccount(platformAccount)
                .budget(dto.useDailyBudget() != null && dto.useDailyBudget() ? dto.dailyBudget() : null)
                .startDate(parseLocalDate(dto.periodStartDt()))
                .endDate(parseLocalDate(dto.periodEndDt()))
                .goal(goal)
                .description(buildCampaignDescription(dto.name(), goal))
                .build();
    }

    // 캠페인 갱신용
    public static void updateAdCampaign(AdCampaign entity, NaverDTO.CampaignResponse dto) {
        Long budget = dto.useDailyBudget() != null && dto.useDailyBudget() ? dto.dailyBudget() : null;
        Goal goal = mapToDomainGoal(dto.campaignTp());
        entity.update(
                dto.name(),
                mapToDomainStatus(dto.status()),
                budget,
                goal,
                parseLocalDate(dto.periodStartDt()),
                parseLocalDate(dto.periodEndDt()),
                entity.getDescription()
        );
    }

    // 광고 그룹 생성용
    public static AdGroup toAdGroupEntity(NaverDTO.AdGroupResponse dto, AdCampaign campaign, java.util.List<NaverDTO.KeywordResponse> keywords) {
        return AdGroup.builder()
                .externalGroupId(dto.nccAdgroupId())
                .name(dto.name())
                .targetingInfo(extractTargetingInfo(keywords))
                .status(mapToDomainStatus(dto.status()))
                .adCampaign(campaign)
                .budget(dto.useDailyBudget() != null && dto.useDailyBudget() ? dto.dailyBudget() : null)
                .bidAmount(dto.bidAmt())
                .build();
    }

    // 광고 그룹 갱신용
    public static void updateAdGroup(AdGroup entity, NaverDTO.AdGroupResponse dto, java.util.List<NaverDTO.KeywordResponse> keywords) {
        entity.update(
                dto.name(),
                mapToDomainStatus(dto.status()),
                extractTargetingInfo(keywords)
        );
        entity.updateBudget(
                dto.useDailyBudget() != null && dto.useDailyBudget() ? dto.dailyBudget() : null,
                dto.bidAmt()
        );
    }

    // 광고 소재 생성용
    public static AdContent toAdContentEntity(NaverDTO.AdResponse dto, AdGroup group) {
        return AdContent.builder()
                .externalAdId(dto.nccAdId())
                .name(dto.ad() != null ? dto.ad().headline() : "알 수 없음")
                .status(mapToDomainStatus(dto.status()))
                .adGroup(group)
                .trackingUrl(dto.ad() != null ? dto.ad().displayUrl() : null)
                .landingUrl(dto.ad() != null ? dto.ad().pcUrl() : null)
                .description(dto.ad() != null ? dto.ad().description() : null)
                .type(dto.type())
                .build();
    }

    // 광고 소재 갱신용
    public static void updateAdContent(AdContent entity, NaverDTO.AdResponse dto) {
        entity.update(
                dto.ad() != null ? dto.ad().headline() : entity.getName(),
                dto.type(),
                mapToDomainStatus(dto.status()),
                dto.ad() != null ? dto.ad().description() : entity.getDescription(),
                dto.ad() != null ? dto.ad().displayUrl() : entity.getTrackingUrl(),
                dto.ad() != null ? dto.ad().pcUrl() : entity.getLandingUrl()
        );
    }

    // Naver의 Status -> Domain Status 변환
    public static Status mapToDomainStatus(String naverStatus) {
        if (naverStatus == null) return Status.PAUSED;
        return switch (naverStatus.toUpperCase()) {
            case "ELIGIBLE", "ON_GOING", "ENABLED" -> Status.ON_GOING; // 진행 중
            case "PAUSED", "USER_PAUSED", "DISABLED" -> Status.PAUSED; // 중지 됨
            case "STOPPED", "DELETED" -> Status.OVER; // 완료 / 삭제 처리 됨
            default -> Status.ON_GOING;
        };
    }

    // 시간 포맷 파싱 유틸 (ZonedDateTime 등 처리)
    private static LocalDate parseLocalDate(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isBlank()) {
            return null;
        }
        try {
            // 우선 ISO_ZONED_DATE_TIME 시도 (예: 2024-04-06T00:00:00Z)
            return ZonedDateTime.parse(dateTimeString).toLocalDate();
        } catch (DateTimeParseException e) {
            try {
                // 실패시 단순 형태 (예: 2024-04-06)
                return LocalDate.parse(dateTimeString, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
    }

    // Naver의 campaignTp -> Domain Goal 변환
    private static Goal mapToDomainGoal(String campaignTp) {
        if (campaignTp == null) return Goal.TRAFFIC;
        return switch (campaignTp.toUpperCase()) {
            case "WEB_SITE" -> Goal.TRAFFIC;
            case "SHOPPING", "POWER_CONTENTS", "BRAND_SEARCH", "PLACE" -> Goal.POPULAR;
            case "APP" -> Goal.DOWNLOAD;
            default -> Goal.TRAFFIC;
        };
    }

    private static String buildCampaignDescription(String name, Goal goal) {
        return String.format("네이버 API 자동 연동 캠페인 (이름: %s, 목표: %s)", name, goalToKorean(goal));
    }

    private static String goalToKorean(Goal goal) {
        if (goal == null) return "알 수 없음";
        return switch (goal) {
            case TRAFFIC -> "트래픽";
            case POPULAR -> "인기도";
            case DOWNLOAD -> "다운로드";
        };
    }

    // 키워드 정보 추출 (/ncc/keywords 응답 기반)
    // null인 경우 null을, 아닌 경우 keyword ,로 연결
    private static String extractTargetingInfo(List<NaverDTO.KeywordResponse> keywords) {
        if (keywords == null || keywords.isEmpty()) return null;
        return keywords.stream()
                .map(NaverDTO.KeywordResponse::keyword)
                .filter(k -> k != null && !k.isBlank())
                .collect(Collectors.joining(","));
    }
}
