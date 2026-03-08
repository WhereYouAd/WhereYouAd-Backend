package com.whereyouad.WhereYouAd.infrastructure.client.openai.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class OpenAIResponse {
    // OpenAI Chat Completions API Feign 응답 원본
    @Getter
    @NoArgsConstructor
    public static class Response {
        // AI 응답 후보 목록 (일반적으로 1개, List 형태로 옴)
        private List<Choice> choices;

        // List 형태에서 꺼내주는 메서드
        public String getFirstContent() {
            // 응답이 없는 경우
            if (choices == null || choices.isEmpty())
                return "";

            Choice first = choices.get(0);

            // 메시지가 없는 경우
            if (first.message() == null)
                return "";

            // 정상 응답이 온 경우 결과 반환
            return first.message().content();
        }
    }

    // 응답 후보 (1건)
    public record Choice (
            Message message
    ) {}

    // AI 메시지 내용 (role + content)
    public record Message (
            String role,
            // 실제 AI 분석 결과 텍스트 (JSON 형식)
            String content
    ) {}
}
