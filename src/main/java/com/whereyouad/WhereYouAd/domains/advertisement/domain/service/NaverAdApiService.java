package com.whereyouad.WhereYouAd.domains.advertisement.domain.service;

import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformConnectionRepository;
import com.whereyouad.WhereYouAd.global.utils.AdApiAuthUtil;
import com.whereyouad.WhereYouAd.global.adapi.dto.AdAuthRequest;
import com.whereyouad.WhereYouAd.infrastructure.client.naver.client.NaverClient;
import com.whereyouad.WhereYouAd.infrastructure.client.naver.dto.NaverDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.AdvertisementHandler;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.code.NaverAdErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverAdApiService {

    private final PlatformConnectionRepository connectionRepository;
    private final AdApiAuthUtil adApiAuthUtil;
    private final NaverClient naverClient;

    // 캠페인 목록 조회
    @Transactional(readOnly = true)
    public List<NaverDTO.CampaignResponse> getCampaigns(Long connectionId) {
        try {
            Map<String, String> headers = adApiAuthUtil.generateAuthHeaders(
                    connectionId, AdAuthRequest.forMethodAndPath("GET", "/ncc/campaigns"));
            return naverClient.getCampaigns(headers);
        } catch (Exception e) {
            log.error("[NAVER] 캠페인 조회 실패 - connectionId={}", connectionId, e);
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_CAMPAIGN_FETCH_FAILED);
        }
    }

    // 광고 그룹 목록 조회
    @Transactional(readOnly = true)
    public List<NaverDTO.AdGroupResponse> getAdGroups(Long connectionId, String nccCampaignId) {
        try {
            Map<String, String> headers = adApiAuthUtil.generateAuthHeaders(
                    connectionId, AdAuthRequest.forMethodAndPath("GET", "/ncc/adgroups"));
            return naverClient.getAdGroups(headers, nccCampaignId);
        } catch (Exception e) {
            log.error("[NAVER] 광고 그룹 조회 실패 - connectionId={}, campaignId={}", connectionId, nccCampaignId, e);
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_AD_GROUP_FETCH_FAILED);
        }
    }

    // 광고(소재) 목록 조회
    @Transactional(readOnly = true)
    public List<NaverDTO.AdResponse> getAds(Long connectionId, String nccAdgroupId) {
        try {
            Map<String, String> headers = adApiAuthUtil.generateAuthHeaders(
                    connectionId, AdAuthRequest.forMethodAndPath("GET", "/ncc/ads"));
            return naverClient.getAds(headers, nccAdgroupId);
        } catch (Exception e) {
            log.error("[NAVER] 광고 소재 조회 실패 - connectionId={}, adGroupId={}", connectionId, nccAdgroupId, e);
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_AD_CONTENT_FETCH_FAILED);
        }
    }

    // 키워드 목록 조회
    @Transactional(readOnly = true)
    public List<NaverDTO.KeywordResponse> getKeywords(Long connectionId, String nccAdgroupId) {
        try {
            Map<String, String> headers = adApiAuthUtil.generateAuthHeaders(
                    connectionId, AdAuthRequest.forMethodAndPath("GET", "/ncc/keywords"));
            return naverClient.getKeywords(headers, nccAdgroupId);
        } catch (Exception e) {
            log.error("[NAVER] 키워드 조회 실패 - connectionId={}, adGroupId={}", connectionId, nccAdgroupId, e);
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_KEYWORD_FETCH_FAILED);
        }
    }

    // AD 리포트 생성
    @Transactional
    public NaverDTO.StatReportResponse requestAdReport(Long connectionId, String statDt) {
        return requestStatReport(connectionId, "AD", statDt);
    }

    // AD_CONVERSION 리포트 생성
    @Transactional
    public NaverDTO.StatReportResponse requestAdConversionReport(Long connectionId, String statDt) {
        return requestStatReport(connectionId, "AD_CONVERSION", statDt);
    }

    // 리포트 조회 private 메서드
    private NaverDTO.StatReportResponse requestStatReport(Long connectionId, String reportTp, String statDt) {
        try {
            Map<String, String> headers = adApiAuthUtil.generateAuthHeaders(
                    connectionId, AdAuthRequest.forMethodAndPath("POST", "/stat-reports"));
            NaverDTO.StatReportRequest request = new NaverDTO.StatReportRequest(reportTp, statDt);
            return naverClient.createStatReport(headers, request);
        } catch (feign.FeignException.BadRequest e) {
            String errorBody = e.contentUTF8();
            if (errorBody != null && errorBody.contains("10004")) {
                log.info("[NAVER] {} 보고서 지표 없음 (코드 10004) - connectionId={}, statDt={}. 리포트 생성을 취소합니다.", reportTp, connectionId, statDt);
                return null;
            }
            log.error("[NAVER] {} 보고서 생성 실패 (BadRequest) - connectionId={}", reportTp, connectionId, e);
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_REPORT_REQUEST_FAILED);
        } catch (Exception e) {
            log.error("[NAVER] {} 보고서 생성 실패 - connectionId={}", reportTp, connectionId, e);
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_REPORT_REQUEST_FAILED);
        }
    }

    // 보고서 상태 조회
    @Transactional(readOnly = true)
    public NaverDTO.StatReportResponse getReportStatus(Long connectionId, String reportJobId) {
        try {
            Map<String, String> headers = adApiAuthUtil.generateAuthHeaders(
                    connectionId, AdAuthRequest.forMethodAndPath("GET", "/stat-reports/" + reportJobId));
            return naverClient.getStatReportStatus(headers, reportJobId);
        } catch (Exception e) {
            log.error("[NAVER] 보고서 상태 조회 실패 - connectionId={}, reportJobId={}", connectionId, reportJobId, e);
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_REPORT_STATUS_CHECK_FAILED);
        }
    }

    private void validateDownloadUrl(String downloadUrl) {
        URI uri;
        try {
            uri = new URI(downloadUrl);
        } catch (URISyntaxException e) {
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_INVALID_DOWNLOAD_URL);
        }
        if (!"https".equals(uri.getScheme())) {
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_INVALID_DOWNLOAD_URL);
        }
        if (uri.getUserInfo() != null) {
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_INVALID_DOWNLOAD_URL);
        }
        String host = uri.getHost();
        if (host == null || !host.endsWith(".naver.com")) {
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_INVALID_DOWNLOAD_URL);
        }
    }

    // 보고서 다운로드 (원문 받아오기)
    @Transactional(readOnly = true)
    public NaverDTO.RawReportResponse downloadReport(Long connectionId, String downloadUrl) {
        validateDownloadUrl(downloadUrl);
        try {
            URI uri = URI.create(downloadUrl);
            // 다운로드 Path를 바탕으로 서명 생성
            Map<String, String> headers = adApiAuthUtil.generateAuthHeaders(
                    connectionId, AdAuthRequest.forMethodAndPath("GET", uri.getPath()));
            feign.Response response = naverClient.downloadStatReport(headers, uri);

            try (InputStream is = response.body().asInputStream()) {
                String rawContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                return new NaverDTO.RawReportResponse(rawContent);
            }
        } catch (Exception e) {
            log.error("[NAVER] 보고서 다운로드 실패 - connectionId={}, url={}", connectionId, downloadUrl, e);
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_REPORT_DOWNLOAD_FAILED);
        }
    }

    // 캠페인 예산 수정
    @Transactional
    public NaverDTO.CampaignResponse updateCampaignBudget(Long connectionId, String campaignId, NaverDTO.UpdateCampaignBudgetRequest request) {

        // 예산이 10의 배수가 아닌 경우 오류(네이버 광고 예산 요청 값 검증)
        if (request.dailyBudget() != null && request.dailyBudget() % 10 != 0) {
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_INVALID_BUDGET_VALUE);
        }
        // API 호출
        try {
            // 헤더 제작
            Map<String, String> headers = adApiAuthUtil.generateAuthHeaders(
                    connectionId, AdAuthRequest.forMethodAndPath("PUT", "/ncc/campaigns/" + campaignId));
            // 요청 body 제작
            NaverDTO.UpdateCampaignBudgetBody body =
                    new NaverDTO.UpdateCampaignBudgetBody(campaignId, request.useDailyBudget(), request.dailyBudget());
            // API 호출
            return naverClient.updateCampaignBudget(headers, campaignId, "budget", body);
        } catch (Exception e) {
            log.error("[NAVER] 캠페인 예산 수정 실패 - connectionId={}, campaignId={}", connectionId, campaignId, e);
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_CAMPAIGN_BUDGET_UPDATE_FAILED);
        }
    }

    // 광고그룹 예산 수정
    @Transactional
    public NaverDTO.AdGroupResponse updateAdGroupBudget(Long connectionId, String adgroupId, NaverDTO.UpdateAdGroupBudgetRequest request) {

        // 예산이 10의 배수가 아닌 경우 오류(네이버 광고 예산 요청 값 검증)
        if (request.dailyBudget() != null && request.dailyBudget() % 10 != 0) {
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_INVALID_BUDGET_VALUE);
        }
        if (request.bidAmt() != null && request.bidAmt() % 10 != 0) {
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_INVALID_BUDGET_VALUE);
        }
        // API 호출
        try {
            // 헤더 제작
            Map<String, String> headers = adApiAuthUtil.generateAuthHeaders(
                    connectionId, AdAuthRequest.forMethodAndPath("PUT", "/ncc/adgroups/" + adgroupId));
            String fields = request.bidAmt() != null ? "budget,bidAmt" : "budget";
            // 요청 body 제작
            NaverDTO.UpdateAdGroupBudgetBody body =
                    new NaverDTO.UpdateAdGroupBudgetBody(adgroupId, request.useDailyBudget(), request.dailyBudget(), request.bidAmt());
            // API 호출
            return naverClient.updateAdGroupBudget(headers, adgroupId, fields, body);
        } catch (Exception e) {
            log.error("[NAVER] 광고그룹 예산 수정 실패 - connectionId={}, adgroupId={}", connectionId, adgroupId, e);
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_AD_GROUP_BUDGET_UPDATE_FAILED);
        }
    }

    // 일별 기본 지표 조회 (/stats, 기본 일 단위)
    @Transactional(readOnly = true)
    public List<NaverDTO.StatResponse> getDailyStats(Long connectionId, String id, String since, String until) {
        try {
            Map<String, String> headers = adApiAuthUtil.generateAuthHeaders(
                    connectionId, AdAuthRequest.forMethodAndPath("GET", "/stats"));

            String fields = "[\"impCnt\",\"clkCnt\",\"salesAmt\",\"ctr\",\"cpc\"]";
            String timeRange = String.format("{\"since\":\"%s\",\"until\":\"%s\"}", since, until);
            NaverDTO.StatListResponse result = naverClient.getStats(headers, id, fields, timeRange, null, null);
            return result != null && result.data() != null ? result.data() : List.of();
        } catch (Exception e) {
            log.error("[NAVER] 일별 통계 조회 실패 - connectionId={}, id={}", connectionId, id, e);
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_HOURLY_STAT_FETCH_FAILED);
        }
    }
}
