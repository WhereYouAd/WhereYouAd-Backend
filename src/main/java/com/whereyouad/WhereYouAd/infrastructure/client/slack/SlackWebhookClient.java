package com.whereyouad.WhereYouAd.infrastructure.client.slack;

import com.whereyouad.WhereYouAd.infrastructure.client.slack.dto.SlackMessage;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;

@FeignClient(name = "slackWebhookClient", url = "https://hooks.slack.com")
public interface SlackWebhookClient {

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    void send(URI webhookUri, @RequestBody SlackMessage message);
}
