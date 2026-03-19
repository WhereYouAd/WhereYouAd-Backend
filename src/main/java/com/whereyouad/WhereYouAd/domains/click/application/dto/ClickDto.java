package com.whereyouad.WhereYouAd.domains.click.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClickDto {
    private String adId;
    private String ipAddress;
    private String device;
    private long clickedAt;
}
