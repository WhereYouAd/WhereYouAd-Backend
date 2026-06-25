package com.whereyouad.WhereYouAd.infrastructure.client.meta.converter;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.*;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.Currency;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.dto.MetaDTO;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.dto.MetaResponse;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class MetaConverter {
    // MetaDTO -> MetricFactResponse 변환

    // Meta Campaign → AdCampaign
    public static AdCampaign toCampaign(MetaDTO.Campaign src, Organization organization, PlatformAccount platformAccount) {

        Currency currency = (platformAccount != null) ? platformAccount.getCurrency() : null;
        BudgetInfo budgetInfo = resolveBudget(src.lifetimeBudget(), src.dailyBudget(), currency);

        //메타 마케팅 API 에서는 따로 캠페인에 설명 설정 안함 -> 임의 문구 삽입
        String description = "메타 API 자동 연동 캠페인 (이름 : " + src.name() + " 목표: " + src.objective() + ")";

        return AdCampaign.builder()
                .externalCampaignId(src.id())
                .name(src.name())
                .description(description)
                .provider(Provider.META)
                .status(mapStatus(src.status()))
                .budget(budgetInfo.amount)
                .budgetType(budgetInfo.type)
                .goal(mapObjective(src.objective()))
                .startDate(parseDate(src.startTime()))
                .endDate(parseDate(src.stopTime()))
                .organization(organization)
                .platformAccount(platformAccount)
                .build();
    }

    // Meta AdSet -> AdGroup
    public static AdGroup toAdGroup(MetaDTO.AdSet src, AdCampaign campaign, PlatformAccount platformAccount) {

        String targetingInfo = null;
        if (src.targeting() != null) {
            targetingInfo = buildTargetingInfo(src.targeting());
        }

        Currency currency = (platformAccount != null) ? platformAccount.getCurrency() : null;
        BudgetInfo budgetInfo = resolveBudget(src.lifetimeBudget(), src.dailyBudget(), currency);

        return AdGroup.builder()
                .externalGroupId(src.id())
                .name(src.name())
                .status(mapStatus(src.status()))
                .targetingInfo(targetingInfo)
                .budget(budgetInfo.amount)
                .budgetType(budgetInfo.type)
                .adCampaign(campaign)
                .build();
    }

    //Meta Ad -> AdContent
    public static AdContent toAdContent(MetaDTO.Ad src, AdGroup adGroup) {

        // 본문(description) : 존재할 경우 해당 본문을, 비어있을 경우 기존 식별자를 대체값으로 사용
        String fallbackDescription = "Meta 광고 ID: " + src.id();
        if (src.creative() != null && src.creative().body() != null && !src.creative().body().trim().isEmpty()) {
            fallbackDescription = src.creative().body();
        }

        // type 매핑
        String mappedType = "BANNER"; // 기본값 (PHOTO)
        if (src.creative() != null && src.creative().objectType() != null) {
            String objType = src.creative().objectType();

            if ("VIDEO".equalsIgnoreCase(objType)) {
                mappedType = "VIDEO";
            }
            else if ("SHARE".equalsIgnoreCase(objType) || "CAROUSEL".equalsIgnoreCase(objType)) {
                mappedType = "NATIVE";
            }
            // 그 외 (PHOTO 등)은 자연스럽게 BANNER 처리
        }

        return AdContent.builder()
                .name(src.name())
                .externalAdId(src.id())
                .type(mappedType)
                .status(mapStatus(src.status()))
                .description(fallbackDescription)
                .adGroup(adGroup)
                .build();
    }

    //Meta Insight -> MetricFact
    public static MetricFact toMetricFact(MetaDTO.Insight src,
                                          AdContent adContent,
                                          AdCampaign adCampaign,
                                          PlatformAccount platformAccount) {
        LocalDateTime timeBucket;
        if (src.dateStart() != null) {
            timeBucket = LocalDate.parse(src.dateStart(), DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } else {
            timeBucket = LocalDate.now().atStartOfDay();
        }
        long conversions = 0L;
        BigDecimal revenue = BigDecimal.ZERO;
        if (src.actions() != null) {
            for (MetaDTO.Action action : src.actions()) {
                if ("purchase".equals(action.actionType())
                        || "offsite_conversion.fb_pixel_purchase".equals(action.actionType())) {
                    try {
                        if (action.value() != null) {
                            conversions += (long) Double.parseDouble(action.value());
                        }
                    } catch (NumberFormatException e) {
                        log.warn("[META] 전환 값 파싱 실패 (건너뜀) - value: {}", action.value());
                    }
                }
            }
        }
        return MetricFact.builder()
                .grain(Grain.DAILY)
                .timeBucket(timeBucket)
                .impressions(parseLong(src.impressions()))
                .clicks(parseLong(src.clicks()))
                .conversions(conversions)
                .spend(parseBigDecimal(src.spend()))
                .revenue(revenue)
                .provider(Provider.META)
                .adContent(adContent)
                .adCampaign(adCampaign)
                .platformAccount(platformAccount)
                .build();
    }

    //유틸 메서드 -> 상태(Status) 매핑
    private static Status mapStatus(String metaStatus) {

        if (metaStatus == null) return Status.ON_GOING;

        return switch (metaStatus.toUpperCase()) {
            case "ACTIVE" -> Status.ON_GOING;
            case "PAUSED" -> Status.PAUSED;
            case "DELETED", "ARCHIVED" -> Status.OVER;
            default -> Status.ON_GOING;
        };
    }

    //유틸 메서드 -> 목표(Goal) 매핑
    private static Goal mapObjective(String objective) {
        if (objective == null) return null;

        return switch (objective.toUpperCase()) {
            case "OUTCOME_AWARENESS", "BRAND_AWARENESS", "REACH" -> Goal.POPULAR;
            case "OUTCOME_TRAFFIC", "LINK_CLICKS", "TRAFFIC" -> Goal.TRAFFIC;
            case "OUTCOME_APP_PROMOTION", "APP_INSTALLS",
                 "OUTCOME_SALES", "CONVERSIONS" -> Goal.DOWNLOAD;
            default -> Goal.TRAFFIC;
        };
    }

    //유틸 메서드 -> 타게팅 정보 매핑
    private static String buildTargetingInfo(MetaDTO.Targeting targeting) {
        StringBuilder sb = new StringBuilder();

        if (targeting.ageMin() != null || targeting.ageMax() != null) {
            sb.append("age:");
            if (targeting.ageMin() != null) sb.append(targeting.ageMin());
            if (targeting.ageMax() != null) sb.append("-").append(targeting.ageMax());
        }

        if (targeting.geoLocations() != null && targeting.geoLocations().countries() != null) {
            sb.append(" / geo:");
            String countries = String.join(",", targeting.geoLocations().countries());
            sb.append(countries);
        }

        return sb.toString();
    }

    //Long, BigDecimal 파싱 메서드
    private static long parseLong(String value) {
        if (value == null || value.isEmpty()) return 0L;
        try {
            return (long) Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
    private static BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            log.warn("[META] BigDecimal 파싱 실패 - value: {}", value);
            return BigDecimal.ZERO;
        }
    }

    // 페이스북 날짜 문자열(예: 2024-03-01T00:00:00-0800) 파싱 메서드
    private static LocalDate parseDate(String isoString) {
        if (isoString == null || isoString.isEmpty()) return null;
        try {
            // ISO 타임존 양식 파싱 시도
            return java.time.OffsetDateTime.parse(isoString, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")).toLocalDate();
        } catch (Exception e) {
            try {
                // 실패 시 순수 앞 10자리(YYYY-MM-DD)만 잘라서 파싱 (안전 백업)
                return LocalDate.parse(isoString.substring(0, 10));
            } catch (Exception ex) {
                return null;
            }
        }
    }

    // MetaAdApiController DTO 변환 (필드 -> DTO)
    public static MetaResponse.AuthUrlResponse toAuthUrlResponse(String authUrl) {
        return new MetaResponse.AuthUrlResponse(authUrl);
    }

    public static MetaResponse.MetaSyncSummary toSyncSummary(int campaignCount, int adGroupCount, int adContentCount, int metricCount) {
        return new MetaResponse.MetaSyncSummary(campaignCount, adGroupCount, adContentCount, metricCount, java.util.List.of());
    }

    public static MetaResponse.MetaSyncSummary toSyncSummary(int campaignCount, int adGroupCount, int adContentCount, int metricCount,
                                                             java.util.List<String> failedAccountIds) {
        return new MetaResponse.MetaSyncSummary(campaignCount, adGroupCount, adContentCount, metricCount, failedAccountIds);
    }

    // AdSet 예산(문자열, Meta 최소 단위) → 로컬 budget 단위 변환. 파싱 실패 시 null
    private static Long parseMinorBudget(String raw, Currency currency) {
        try {
            long minorBudget = Long.parseLong(raw);
            return fromMetaBudget(minorBudget, currency);
        } catch (NumberFormatException e) {
            log.warn("[META] 예산 파싱 실패 - value: {}", raw);
            return null;
        }
    }

    // 예산 추출 결과 — 로컬 통화 단위 금액 + 예산 유형
    private record BudgetInfo(Long amount, BudgetType type) {}

    // lifetime/daily 중 설정된 예산을 로컬 통화 단위 금액 + 유형으로 변환 (Campaign·AdSet 공통)
    private static BudgetInfo resolveBudget(String lifetimeBudget, String dailyBudget, Currency currency) {
        if (isActiveBudget(lifetimeBudget)) {
            return new BudgetInfo(parseMinorBudget(lifetimeBudget, currency), BudgetType.TOTAL);
        }
        if (isActiveBudget(dailyBudget)) {
            return new BudgetInfo(parseMinorBudget(dailyBudget, currency), BudgetType.DAILY);
        }
        return new BudgetInfo(null, null);
    }

    // Meta 는 사용하지 않는 예산 필드를 "0" 으로 응답 → 양수일 때만 실제 설정된 예산으로 간주
    private static boolean isActiveBudget(String raw) {
        if (raw == null || raw.isBlank()) return false;
        try {
            return Long.parseLong(raw.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // 로컬 budget(원 단위) → Meta 의 통화 단위로 변환
    // Meta 는 금액을 최소 단위로 받는다 ex) USD 10$ -> 1000(센트) , KRW 1000 -> 1000
    public static Long toMetaBudget(Long budget, Currency currency) {
        if (budget == null) {
            return null;
        }

        if (currency == null || currency == Currency.KRW) {
            return budget;

        }

        return budget * 100;   // USD 등 소수점 통화는 센트 단위로 전송
    }

    // Meta 통화 단위 → 로컬 budget 단위 변환
    private static Long fromMetaBudget(Long minorBudget, Currency currency) {
        if (minorBudget == null) {
            return null;
        }

        if (currency == null || currency == Currency.KRW) {
            return minorBudget;
        }

        if (minorBudget % 100 != 0) {
            log.warn("[META] 비 KRW 예산 단위가 100 의 배수가 아닙니다. (반올림 처리): {} {}", minorBudget, currency);
        }

        return Math.round(minorBudget / 100.0);
    }

}
