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
    private String adId;
    private String ipAddress;
    private String device;
    private long clickedAt;
    private boolean isDummy; // true: 더미 데이터, false: 실제 클릭
}
