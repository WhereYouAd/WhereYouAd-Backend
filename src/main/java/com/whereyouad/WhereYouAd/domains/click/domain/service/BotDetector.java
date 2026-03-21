package com.whereyouad.WhereYouAd.domains.click.domain.service;

import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 광고 클릭에 대한 봇/어뷰징 판별 컴포넌트
 */
@Component
@RequiredArgsConstructor
public class BotDetector {

    private final RedisUtil redisUtil;

    /**
     * IP 주소와 User-Agent를 기반으로 봇 여부를 판별
     */
    public boolean isSuspect(String ipAddress, String userAgent) {
        boolean isUserAgentBot = isBot(userAgent);
        boolean isAbnormalIp = checkAbnormalIp(ipAddress);
        return isUserAgentBot || isAbnormalIp;
    }

    // User-Agent 기반 봇 판별
    private boolean isBot(String userAgent) {
        if (!StringUtils.hasText(userAgent) || userAgent.length() < 10) {
            return true;
        }
        String lowerAgent = userAgent.toLowerCase();
        return isKnownBot(lowerAgent) || isMaliciousBot(lowerAgent);
    }

    private boolean isKnownBot(String lowerAgent) {
        String[] botKeywords = {
                "bot", "crawler", "spider", "ping", "slurp",
                "lighthouse", "postman", "curl", "kakaotalk-scrap",
                "yeti", "googlebot", "bingbot"
        };
        for (String keyword : botKeywords) {
            if (lowerAgent.contains(keyword)) return true;
        }
        return false;
    }

    private boolean isMaliciousBot(String lowerAgent) {
        String[] scraperKeywords = {
                "python-requests", "python-urllib", "java/", "go-http-client", "axios",
                "node-fetch", "okhttp", "wget", "scrapy", "httpclient", "apache-httpclient"
        };
        for (String keyword : scraperKeywords) {
            if (lowerAgent.contains(keyword)) return true;
        }

        String[] headlessKeywords = {
                "headlesschrome", "phantomjs", "puppeteer", "selenium", "playwright", "cypress"
        };
        for (String keyword : headlessKeywords) {
            if (lowerAgent.contains(keyword)) return true;
        }

        // 일반 브라우저는 mozilla 또는 opera 포함
        return !lowerAgent.contains("mozilla") && !lowerAgent.contains("opera");
    }

    // 동일 IP에서 1분에 20회 초과 클릭 시 봇으로 판별
    private boolean checkAbnormalIp(String ipAddress) {
        if (!StringUtils.hasText(ipAddress)) {
            return false;
        }
        String key = "click_ip_count:" + ipAddress;
        Long count = redisUtil.increment(key);

        if (count != null && count == 1) {
            redisUtil.expire(key, 1, TimeUnit.MINUTES);
        }
        if (count != null && count > 20) {
            redisUtil.expire(key, 10, TimeUnit.MINUTES);
            return true;
        }
        return false;
    }
}
