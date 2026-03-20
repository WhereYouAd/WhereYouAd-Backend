package com.whereyouad.WhereYouAd.domains.click.application.mapper;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.click.application.dto.ClickDto;
import com.whereyouad.WhereYouAd.domains.click.domain.constant.DeviceType;
import com.whereyouad.WhereYouAd.domains.click.persistence.entity.ClickLog;

import org.springframework.util.StringUtils;

public class ClickConverter {

    public static ClickLog toClickLog(AdContent adContent, ClickDto event, boolean isSuspect) {
        DeviceType deviceType = extractDeviceType(event.getUserAgent());

        return ClickLog.builder()
                .adContent(adContent)
                .ipAddress(event.getIpAddress())
                .device(deviceType)
                .clickedAt(event.getClickedAt())
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
