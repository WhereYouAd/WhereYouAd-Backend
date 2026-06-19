package com.whereyouad.WhereYouAd.domains.click.application.dto;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
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
    private Long orgId;
    private Provider provider;
    private String ipAddress;
    private String userAgent;
    private long clickedAt;
    private boolean isDummy;
}
