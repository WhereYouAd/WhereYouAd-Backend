package com.whereyouad.WhereYouAd.infrastructure.client.naver.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

public class NaverDTO {

    // 캠페인 응답 원문
    public record CampaignResponse(
            String nccCampaignId,
            Long customerId,
            String campaignTp,
            String name,
            String status,
            String statusReason,
            Boolean useDailyBudget,
            Long dailyBudget,
            Boolean usePeriod,
            String periodStartDt,
            String periodEndDt,
            String deliveryMethod,
            String trackingMode,
            String trackingUrl,
            String trackingUrlCustomParams,
            Boolean userLock,
            Integer numberInUse,

            // 공유 예산 관련
            String sharedBudgetId,
            String sharedBudgetName,
            Long sharedDailyBudget,
            String sharedBudgetDeliveryMethod,
            Long sharedBudgetExpectCost,
            Boolean sharedBudgetLock,

            // 등록/수정 시간
            String regTm,
            String editTm
    ) {}

    // 광고 그룹 응답
    public record AdGroupResponse(
            String nccAdgroupId,
            String nccCampaignId,
            Long customerId,
            String name,
            String status,
            String statusReason,
            Long bidAmt,
            Boolean useDailyBudget,
            Long dailyBudget,
            Boolean userLock,
            String regTm,
            String editTm
    ) {}

    // 광고 소재 응답
    public record AdResponse(
            String nccAdId,
            String nccAdgroupId,
            Long customerId,
            String type,
            String status,
            String statusReason,
            String inspectStatus,
            Boolean userLock,
            AdDetail ad,
            String regTm,
            String editTm
    ) {
        public record AdDetail(
                String headline,
                String description,
                String pcUrl,
                String mobileUrl,
                String displayUrl
        ) {}
    }

    // 키워드 응답 (Ad_Group에서 target_info값)
    public record KeywordResponse(
            String nccKeywordId,
            String nccAdgroupId,
            Long customerId,
            String keyword,
            String status,
            Long bidAmt,
            Boolean useGroupBidAmt,
            String regTm,
            String editTm
    ) {}

    // /stats API 반환 래퍼
    // {“data”: [...]} 형식을 받아옴
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatListResponse(
            List<StatResponse> data
    ) {}

    // /stats 응답의 개별 통계 행
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatResponse(
            @JsonAlias({"statDt", "dateStart"}) String statDt,
            Long impCnt,
            Long clkCnt,
            Long salesAmt,
            Double ctr,
            Double cpc,
            Long ccnt,
            Long convAmt
    ) {}

    // 상세 보고서 응답 (Metric_fact)
    public record StatReportResponse(
            String reportJobId,
            String reportJobType,
            String status,
            String downloadUrl,
            String regTm,
            String updateTm
    ) {}

    // 다운로드한 원본 리포트 본문
    public record RawReportResponse(
            String rawContent
    ) {}


    // Request

    // 대용량 보고서 생성 요청값
    public record StatReportRequest(
            String reportTp,
            String statDt
    ) {}

    // 캠페인 예산 수정 요청 (컨트롤러 입력용, ID X)
    public record UpdateCampaignBudgetRequest(
            Boolean useDailyBudget,
            Long dailyBudget
    ) {}

    // 캠페인 예산 수정 body (네이버 API 전송용, ID O)
    public record UpdateCampaignBudgetBody(
            String nccCampaignId,
            Boolean useDailyBudget,
            Long dailyBudget
    ) {}

    // 광고그룹 예산 수정 요청 (컨트롤러 입력용, ID X)
    public record UpdateAdGroupBudgetRequest(
            Boolean useDailyBudget,
            Long dailyBudget,
            Long bidAmt
    ) {}

    // 광고그룹 예산 수정 body (네이버 API 전송용, ID O)
    public record UpdateAdGroupBudgetBody(
            String nccAdgroupId,
            Boolean useDailyBudget,
            Long dailyBudget,
            Long bidAmt
    ) {}
}
