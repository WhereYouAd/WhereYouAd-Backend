package com.whereyouad.WhereYouAd.domains.click.application.mapper;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.click.application.dto.ClickDto;
import com.whereyouad.WhereYouAd.domains.click.domain.constant.ClickWindowKeys;
import com.whereyouad.WhereYouAd.domains.click.domain.constant.DeviceType;
import com.whereyouad.WhereYouAd.domains.click.persistence.entity.ClickLog;

import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;

public class ClickConverter {

    public static ClickLog toClickLog(AdContent adContent, ClickDto event, boolean isSuspect) {
        DeviceType deviceType = extractDeviceType(event.getUserAgent());
        // clickedAt은 KST 고정 - 일일 요약 등 조회 경계(ClickWindowKeys.ZONE_ID)와 같은 존이어야 한다
        LocalDateTime clickedAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(event.getClickedAt()), ClickWindowKeys.ZONE_ID);

        return ClickLog.builder()
                .adContent(adContent)
                .ipAddress(event.getIpAddress())
                .device(deviceType)
                .clickedAt(clickedAt)
                .isSuspect(isSuspect)
                .build();
    }

    private static DeviceType extractDeviceType(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return DeviceType.UNKNOWN;
        }
        String lowerAgent = userAgent.toLowerCase();
        if (lowerAgent.contains("mobi") || lowerAgent.contains("android") || lowerAgent.contains("iphone")) {
            return DeviceType.MOBILE;
        }
        return DeviceType.PC;
    }
}
