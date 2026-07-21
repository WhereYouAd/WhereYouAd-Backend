package com.whereyouad.WhereYouAd.infrastructure.client.openai.service;

import com.whereyouad.WhereYouAd.infrastructure.client.openai.client.OpenAIClient;
import com.whereyouad.WhereYouAd.infrastructure.client.openai.dto.request.OpenAIRequest;
import com.whereyouad.WhereYouAd.infrastructure.client.openai.dto.response.OpenAIResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryableOpenAiCaller {

    private final OpenAIClient openAiClient;

    /**
     * 일시적 오류(429, 5xx)에 한해 최대 3회 재시도 (1s → 2s → 4s 지수 백오프).
     * 400·401 같은 영구 오류는 retryFor 목록에 없으므로 즉시 전파된다.
     */
    @Retryable(
            retryFor = {
                    FeignException.TooManyRequests.class,
                    FeignException.InternalServerError.class,
                    FeignException.ServiceUnavailable.class,
                    FeignException.BadGateway.class,
                    FeignException.GatewayTimeout.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public OpenAIResponse.Response call(OpenAIRequest.Request request) {
        return openAiClient.chatCompletions(request);
    }
}
