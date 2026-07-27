package com.whereyouad.WhereYouAd.infrastructure.client.discord.dto;

import java.util.List;

public record DiscordMessage(
        String username,
        List<Embed> embeds
) {
    public record Embed(
            String title,
            String description,
            Integer color
    )
    {}
}
