package com.whereyouad.WhereYouAd.domains.click.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClickDto {
    private Long adContentId;
    private String ipAddress;
    private String userAgent;
    private long clickedAt;
    private boolean isDummy;
}
