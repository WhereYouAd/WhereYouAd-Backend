package com.whereyouad.WhereYouAd.domains.advertisement.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.application.mapper.AdvertisementConverter;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdCampaignRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdGroupRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.BudgetHistoryRepository;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgRole;
import com.whereyouad.WhereYouAd.domains.organization.exception.handler.OrgHandler;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
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
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverAdApiService {

    // /stats 조회 대상 지표 필드
    private static final String STAT_FIELDS = "[\"impCnt\",\"clkCnt\",\"salesAmt\",\"ctr\",\"cpc\",\"ccnt\",\"convAmt\"]";
    // 네이버 API rate limit 대응 호출 간격
    private static final long STATS_CALL_INTERVAL_MS = 400L;

    private final PlatformConnectionRepository connectionRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final AdApiAuthUtil adApiAuthUtil;
    private final NaverClient naverClient;
    private final AdGroupRepository adGroupRepository;
    private final AdCampaignRepository adCampaignRepository;
    private final BudgetHistoryRepository budgetHistoryRepository;

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
    public NaverDTO.CampaignResponse updateCampaignBudget(Long userId, Long connectionId, String campaignId, NaverDTO.UpdateCampaignBudgetRequest request) {
        PlatformConnection connection = validateAdminOwnership(userId, connectionId);
        if (request.dailyBudget() != null) {
            if (request.dailyBudget() % 10 != 0) {
                throw new AdvertisementHandler(NaverAdErrorCode.NAVER_INVALID_BUDGET_VALUE);
            }
            if (request.dailyBudget() < 50 || request.dailyBudget() > 1_000_000_000) {
                throw new AdvertisementHandler(NaverAdErrorCode.NAVER_INVALID_BUDGET_RANGE);
            }
        }
        Optional<AdCampaign> campaignOpt = adCampaignRepository
                .findByPlatformAccountAndExternalCampaignId(connection.getPlatformAccount(), campaignId);
        campaignOpt.ifPresent(campaign -> {
            if (request.dailyBudget() != null && request.dailyBudget().equals(campaign.getBudget())) {
                throw new AdvertisementHandler(NaverAdErrorCode.NAVER_SAME_BUDGET_VALUE);
            }
        });
        try {
            Map<String, String> headers = adApiAuthUtil.generateAuthHeaders(
                    connectionId, AdAuthRequest.forMethodAndPath("PUT", "/ncc/campaigns/" + campaignId));
            NaverDTO.UpdateCampaignBudgetBody body =
                    new NaverDTO.UpdateCampaignBudgetBody(campaignId, request.useDailyBudget(), request.dailyBudget());
            NaverDTO.CampaignResponse result = naverClient.updateCampaignBudget(headers, campaignId, "budget", body);

            campaignOpt.ifPresent(campaign -> {
                Long previousBudget = campaign.getBudget();
                campaign.updateBudget(request.dailyBudget());
                budgetHistoryRepository.save(AdvertisementConverter.toCampaignBudgetHistory(
                        campaign, previousBudget, request.dailyBudget(), userId, Provider.NAVER));
            });

            return result;
        } catch (Exception e) {
            log.error("[NAVER] 캠페인 예산 수정 실패 - connectionId={}, campaignId={}", connectionId, campaignId, e);
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_CAMPAIGN_BUDGET_UPDATE_FAILED);
        }
    }

    // 광고그룹 예산 수정
    @Transactional
    public NaverDTO.AdGroupResponse updateAdGroupBudget(Long userId, Long connectionId, String adgroupId, NaverDTO.UpdateAdGroupBudgetRequest request) {
        PlatformConnection connection = validateAdminOwnership(userId, connectionId);
        if (request.dailyBudget() != null) {
            if (request.dailyBudget() % 10 != 0) {
                throw new AdvertisementHandler(NaverAdErrorCode.NAVER_INVALID_BUDGET_VALUE);
            }
            if (request.dailyBudget() < 50 || request.dailyBudget() > 1_000_000_000) {
                throw new AdvertisementHandler(NaverAdErrorCode.NAVER_INVALID_BUDGET_RANGE);
            }
        }
        if (request.bidAmt() != null) {
            if (request.bidAmt() % 10 != 0) {
                throw new AdvertisementHandler(NaverAdErrorCode.NAVER_INVALID_BUDGET_VALUE);
            }
            if (request.bidAmt() < 70 || request.bidAmt() > 100_000) {
                throw new AdvertisementHandler(NaverAdErrorCode.NAVER_INVALID_BID_AMOUNT_RANGE);
            }
        }
        Optional<AdGroup> adGroupOpt = adGroupRepository
                .findByAdCampaign_PlatformAccountAndExternalGroupId(connection.getPlatformAccount(), adgroupId);
        adGroupOpt.ifPresent(adGroup -> {
            boolean budgetUnchanged = request.dailyBudget() == null
                    || request.dailyBudget().equals(adGroup.getBudget());
            boolean bidAmtUnchanged = request.bidAmt() == null
                    || request.bidAmt().equals(adGroup.getBidAmount());
            if (budgetUnchanged && bidAmtUnchanged) {
                throw new AdvertisementHandler(NaverAdErrorCode.NAVER_SAME_BUDGET_VALUE);
            }
        });
        try {
            Map<String, String> headers = adApiAuthUtil.generateAuthHeaders(
                    connectionId, AdAuthRequest.forMethodAndPath("PUT", "/ncc/adgroups/" + adgroupId));
            // 네이버 API 제약: budget과 bidAmt는 fields에 함께 넣어도 bidAmt가 무시됨 → 각각 별도 호출 필요
            NaverDTO.AdGroupResponse result = null;
            if (request.dailyBudget() != null || request.useDailyBudget() != null) {
                result = naverClient.updateAdGroupBudget(headers, adgroupId, "budget",
                        new NaverDTO.UpdateAdGroupBudgetBody(adgroupId, request.useDailyBudget(), request.dailyBudget(), null));
            }
            if (request.bidAmt() != null) {
                result = naverClient.updateAdGroupBudget(headers, adgroupId, "bidAmt",
                        new NaverDTO.UpdateAdGroupBudgetBody(adgroupId, null, null, request.bidAmt()));
            }

            adGroupOpt.ifPresent(adGroup -> {
                        if (request.dailyBudget() != null) {
                            Long previousBudget = adGroup.getBudget();
                            budgetHistoryRepository.save(AdvertisementConverter.toAdGroupBudgetHistory(
                                    adGroup, previousBudget, request.dailyBudget(), userId, Provider.NAVER));
                        }
                        if (request.bidAmt() != null) {
                            Long previousBidAmount = adGroup.getBidAmount();
                            budgetHistoryRepository.save(AdvertisementConverter.toBidAmountHistory(
                                    adGroup, previousBidAmount, request.bidAmt(), userId, Provider.NAVER));
                        }
                        adGroup.updateBudget(request.dailyBudget(), request.bidAmt());
                    });

            return result;
        } catch (Exception e) {
            log.error("[NAVER] 광고그룹 예산 수정 실패 - connectionId={}, adgroupId={}", connectionId, adgroupId, e);
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_AD_GROUP_BUDGET_UPDATE_FAILED);
        }
    }

    @Transactional(readOnly = true)
    private PlatformConnection validateAdminOwnership(Long userId, Long connectionId) {
        PlatformConnection connection = connectionRepository.findWithAccountAndOrgById(connectionId)
                .orElseThrow(() -> new AdvertisementHandler(NaverAdErrorCode.NAVER_CONNECTION_NOT_FOUND));
        Long orgId = connection.getPlatformAccount().getOrganization().getId();
        OrgMember orgMember = orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND));
        if (orgMember.getRole() != OrgRole.ADMIN) {
            throw new OrgHandler(OrgErrorCode.ORG_MEMBER_FORBIDDEN);
        }
        return connection;
    }

    // 일별 기본 지표 조회 (/stats, 기본 일 단위)
    @Transactional(readOnly = true)
    public List<NaverDTO.StatResponse> getDailyStats(Long connectionId, String id, String since, String until) {
        try {
            Map<String, String> headers = adApiAuthUtil.generateAuthHeaders(
                    connectionId, AdAuthRequest.forMethodAndPath("GET", "/stats"));

            String timeRange = String.format("{\"since\":\"%s\",\"until\":\"%s\"}", since, until);
            NaverDTO.StatListResponse result = naverClient.getStats(headers, id, STAT_FIELDS, timeRange, null, null);
            return result != null && result.data() != null ? result.data() : List.of();
        } catch (Exception e) {
            log.error("[NAVER] 일별 통계 조회 실패 - connectionId={}, id={}", connectionId, id, e);
            throw new AdvertisementHandler(NaverAdErrorCode.NAVER_HOURLY_STAT_FETCH_FAILED);
        }
    }

    // 일별 기본 지표 날짜 범위 배치 조회
    // 네이버 /stats는 일별(timeIncrement=1) 조회 시 단일 id만 지원하므로 소재당 1회씩 범위 전체를 조회
    // 반환: 소재 externalAdId → 해당 소재의 일별 통계 행 목록 (API 호출 실패한 소재는 키 미포함)
    public Map<String, List<NaverDTO.StatResponse>> getDailyStatsBatch(
            Long connectionId, List<String> adIds, LocalDate since, LocalDate until) {
        if (adIds.isEmpty()) {
            return Map.of();
        }

        String timeRange = String.format("{\"since\":\"%s\",\"until\":\"%s\"}", since, until);
        Map<String, List<NaverDTO.StatResponse>> statsByAdId = new LinkedHashMap<>();

        for (int i = 0; i < adIds.size(); i++) {
            String adId = adIds.get(i);
            try {
                Map<String, String> headers = adApiAuthUtil.generateAuthHeaders(
                        connectionId, AdAuthRequest.forMethodAndPath("GET", "/stats"));
                NaverDTO.StatListResponse result =
                        naverClient.getStatsByDateRange(headers, adId, STAT_FIELDS, timeRange, "1");
                statsByAdId.put(adId,
                        result != null && result.data() != null ? result.data() : List.of());
            } catch (Exception e) {
                log.error("[NAVER] 소재(ID:{}) 통계 범위 조회 실패 - connectionId={}", adId, connectionId, e);
            }

            if (i < adIds.size() - 1) {
                try {
                    Thread.sleep(STATS_CALL_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return statsByAdId;
    }
}
