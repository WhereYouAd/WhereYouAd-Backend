package com.whereyouad.WhereYouAd.infrastructure.client.openai.dto.request;

import lombok.Builder;

import java.util.List;

public class OpenAIRequest {
    // OpenAI Chat Completions API 요청 바디 DTO
    @Builder
    public record Request (
            // 사용할 OpenAI 모델 (gpt-4o-mini)
            String model,

            // 대화 메시지 목록 (system + user)
            List<Message> messages,

            // 응답 창의성 조절 (0.0 ~ 1.0)
            double temperature
    ) {}
    @Builder
    public record Message (
            // system, user
            String role,
            // 메시지 내용
            String content
    ) {}
}
