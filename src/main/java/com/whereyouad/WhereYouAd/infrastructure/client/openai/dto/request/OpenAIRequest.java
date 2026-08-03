package com.whereyouad.WhereYouAd.infrastructure.client.openai.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

public class OpenAIRequest {
    // OpenAI Chat Completions API 요청 바디 DTO
    @Builder
    public record Request (
            // 사용할 OpenAI 모델 (gpt-5.6-terra)
            String model,

            // 대화 메시지 목록 (system + user)
            List<Message> messages,

            // GPT-5 계열 추론 강도
            @JsonProperty("reasoning_effort")
            String reasoningEffort
    ) {}
    @Builder
    public record Message (
            // system, user
            String role,
            // 메시지 내용
            String content
    ) {}
}
