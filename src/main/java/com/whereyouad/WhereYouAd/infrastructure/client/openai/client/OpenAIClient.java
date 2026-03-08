package com.whereyouad.WhereYouAd.infrastructure.client.openai.client;

import com.whereyouad.WhereYouAd.global.config.OpenAIFeignConfig;
import com.whereyouad.WhereYouAd.infrastructure.client.openai.dto.request.OpenAIRequest;
import com.whereyouad.WhereYouAd.infrastructure.client.openai.dto.response.OpenAIResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "openAiClient",
        url = "${openai.base-url}",
        configuration = OpenAIFeignConfig.class
)

public interface OpenAIClient {

    @PostMapping("/v1/chat/completions")
    OpenAIResponse.Response chatCompletions(@RequestBody OpenAIRequest.Request request);
}
