package com.whereyouad.WhereYouAd.infrastructure.client.openai.service;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.ai.application.dto.response.AIResponse;
import com.whereyouad.WhereYouAd.domains.ai.application.mapper.AIConverter;
import com.whereyouad.WhereYouAd.domains.ai.exception.AIHandler;
import com.whereyouad.WhereYouAd.domains.ai.exception.code.AIErrorCode;
import com.whereyouad.WhereYouAd.infrastructure.client.openai.client.OpenAIClient;
import com.whereyouad.WhereYouAd.infrastructure.client.openai.prompt.PromptBuilder;
import com.whereyouad.WhereYouAd.infrastructure.client.openai.dto.request.OpenAIRequest;
import com.whereyouad.WhereYouAd.infrastructure.client.openai.dto.response.OpenAIResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenApiService {

    private final OpenAIClient openAiClient;
    private final PromptBuilder promptBuilder;

    @Value("${openai.model}")
    private String model;

    // 기간 내 MetricFact 목록을 받아 AI 분석 리포트를 생성
    public AIResponse.AnalysisResponse generateAnalysis(
            String provider,
            LocalDate startDate,
            LocalDate endDate,
            List<MetricFact> metrics) {
        // 1. 일별 MetricFact 원본 데이터를 포함한 프롬프트 생성
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(provider, startDate, endDate, metrics);

        // 2. AIConverter로 요청 DTO 빌드
        OpenAIRequest.Request request = AIConverter.toOpenAiRequest(model, systemPrompt, userPrompt);

        // 3. OpenAI API 호출
        OpenAIResponse.Response response;
        try {
            log.info("[generateAnalysis] OpenAI 호출 시작. provider: {}, 기간: {} ~ {}, 레코드 수: {}",
                    provider, startDate, endDate, metrics.size());
            response = openAiClient.chatCompletions(request);
            log.info("[generateAnalysis] OpenAI 응답 수신 완료.");

        } catch (FeignException.BadRequest e) {
            // 400: 요청 파라미터 오류 (모델명 오류, messages 형식 오류 등)
            log.error("[generateAnalysis] OpenAI 400 Bad Request: {}", extractFeignMessage(e));
            throw new AIHandler(AIErrorCode.INVALID_OPENAI_REQUEST);

        } catch (FeignException.Unauthorized e) {
            // 401: API 키 누락 또는 유효하지 않은 키
            log.error("[generateAnalysis] OpenAI 401 Unauthorized: {}", extractFeignMessage(e));
            throw new AIHandler(AIErrorCode.INVALID_OPENAI_API_KEY);

        } catch (FeignException.TooManyRequests e) {
            // 429: 호출 횟수(Rate limit) 초과
            log.error("[generateAnalysis] OpenAI 429 Rate Limit: {}", extractFeignMessage(e));
            throw new AIHandler(AIErrorCode.OPENAI_RATE_LIMIT);

        } catch (FeignException e) {
            // 그 외 Feign 에러 (5xx 등)
            log.error("[generateAnalysis] OpenAI Feign 오류 (status={}): {}",
                    e.status(), extractFeignMessage(e));
            throw new AIHandler(AIErrorCode.AI_CALL_FAILED);
        }

        // 4. 응답 content 추출 및 빈 값 방어
        String aiContent = response.getFirstContent();
        if (aiContent == null || aiContent.isBlank()) {
            log.error("[generateAnalysis] OpenAI 응답 content가 비어있습니다.");
            throw new AIHandler(AIErrorCode.AI_CALL_FAILED);
        }

        // 5. AIConverter 로 JSON -> AnalysisResponse 파싱
        return AIConverter.toAnalysisResponse(aiContent);
    }

    // 에러 메시지 추출
    private String extractFeignMessage(FeignException e) {
        try {
            return e.contentUTF8();
        } catch (Exception ex) {
            return e.getMessage();
        }
    }
}
