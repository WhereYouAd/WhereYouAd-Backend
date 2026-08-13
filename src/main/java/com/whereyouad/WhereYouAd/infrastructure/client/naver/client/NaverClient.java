package com.whereyouad.WhereYouAd.infrastructure.client.naver.client;

import com.whereyouad.WhereYouAd.infrastructure.client.naver.dto.NaverDTO;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.util.List;
import java.util.Map;

@FeignClient(
        name = "naverClient",
        url = "https://api.searchad.naver.com"
)
public interface NaverClient {

    // 캠페인 목록 조회
    @GetMapping("/ncc/campaigns")
    List<NaverDTO.CampaignResponse> getCampaigns(
            @RequestHeader Map<String, String> headers
    );

    // 광고 그룹 목록 조회
    @GetMapping("/ncc/adgroups")
    List<NaverDTO.AdGroupResponse> getAdGroups(
            @RequestHeader Map<String, String> headers,
            @RequestParam(value = "nccCampaignId", required = false) String nccCampaignId
    );

    // 광고(소재) 목록 조회
    @GetMapping("/ncc/ads")
    List<NaverDTO.AdResponse> getAds(
            @RequestHeader Map<String, String> headers,
            @RequestParam(value = "nccAdgroupId", required = false) String nccAdgroupId
    );

    // 키워드 목록 조회
    @GetMapping("/ncc/keywords")
    List<NaverDTO.KeywordResponse> getKeywords(
            @RequestHeader Map<String, String> headers,
            @RequestParam(value = "nccAdgroupId") String nccAdgroupId
    );

    // 통계 데이터 조회
    @GetMapping("/stats")
    NaverDTO.StatListResponse getStats(
            @RequestHeader Map<String, String> headers,
            @RequestParam("id") String id,
            @RequestParam("fields") String fields,
            @RequestParam(value = "timeRange", required = false) String timeRange,
            @RequestParam(value = "datePreset", required = false) String datePreset,
            @RequestParam(value = "breakdown", required = false) String breakdown
    );

    // 통계 데이터 날짜 범위 조회 (단일 소재 + timeIncrement=1 → 일별 행)
    // 복수 ids + 일별(timeIncrement=1) 조합은 네이버 미지원 (공식 샘플 Stats.java: 일별은 단일 id만)
    @GetMapping("/stats")
    NaverDTO.StatListResponse getStatsByDateRange(
            @RequestHeader Map<String, String> headers,
            @RequestParam("id") String id,
            @RequestParam("fields") String fields,
            @RequestParam("timeRange") String timeRange,
            @RequestParam("timeIncrement") String timeIncrement
    );

    // 대용량 보고서 생성 요청
    @PostMapping("/stat-reports")
    NaverDTO.StatReportResponse createStatReport(
            @RequestHeader Map<String, String> headers,
            @RequestBody NaverDTO.StatReportRequest request
    );

    // 대용량 보고서 상태 조회
    @GetMapping("/stat-reports/{reportJobId}")
    NaverDTO.StatReportResponse getStatReportStatus(
            @RequestHeader Map<String, String> headers,
            @PathVariable("reportJobId") String reportJobId
    );

    // 대용량 보고서 다운로드 (동적 URL)
    @GetMapping
    Response downloadStatReport(
            @RequestHeader Map<String, String> headers, URI baseUri
    );

    // 캠페인 예산 수정
    @PutMapping("/ncc/campaigns/{campaignId}")
    NaverDTO.CampaignResponse updateCampaignBudget(
            @RequestHeader Map<String, String> headers,
            @PathVariable("campaignId") String campaignId,
            @RequestParam("fields") String fields,
            @RequestBody NaverDTO.UpdateCampaignBudgetBody body
    );

    // 광고그룹 예산 수정
    @PutMapping("/ncc/adgroups/{adgroupId}")
    NaverDTO.AdGroupResponse updateAdGroupBudget(
            @RequestHeader Map<String, String> headers,
            @PathVariable("adgroupId") String adgroupId,
            @RequestParam("fields") String fields,
            @RequestBody NaverDTO.UpdateAdGroupBudgetBody body
    );
}
