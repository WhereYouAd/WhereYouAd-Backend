package com.whereyouad.WhereYouAd.infrastructure.client.naver.client;

import com.whereyouad.WhereYouAd.infrastructure.client.naver.dto.NaverDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

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
}
