package com.whereyouad.WhereYouAd.domains.ai.domain.service;

import com.whereyouad.WhereYouAd.domains.ai.application.dto.request.AIRequest;
import com.whereyouad.WhereYouAd.domains.ai.application.dto.response.AIResponse;

public interface AIService {

    // request 내 정해진 기간동안의 MetricFact를 넘겨 AI에게 분석 요청하는 메서드
    // 요청 내용을 PENDING 상태로 db에 바로 저장 후 accessToken 반환
    String requestAnalysis(Long userId, Long projectId, AIRequest.PeriodRequest request);

    // accessToken에 해당하는 분석 리포트 반환 메서드
    AIResponse.ReportStatusResponse getReportByAccessToken(Long userId, String accessToken);

    // 공유 상태 변경 메서드
    void updateShareStatus(Long userId, String accessToken, boolean isShared);
}
