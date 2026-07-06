package com.whereyouad.WhereYouAd.infrastructure.client.discord;

import com.whereyouad.WhereYouAd.infrastructure.client.discord.dto.DiscordMessage;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;

@FeignClient(name = "discordWebhookClient", url = "https://discord.com")
public interface DiscordWebhookClient {

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    void send(URI webhookUri, @RequestBody DiscordMessage message);
}
