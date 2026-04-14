package com.whereyouad.WhereYouAd.infrastructure.client.naver.client;

import com.whereyouad.WhereYouAd.infrastructure.client.naver.dto.NaverDTO;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    List<NaverDTO.Campaign> getCampaigns(@RequestHeader Map<String, String> headers);
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
}
