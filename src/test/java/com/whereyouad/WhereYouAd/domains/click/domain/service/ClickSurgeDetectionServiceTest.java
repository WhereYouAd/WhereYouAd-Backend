package com.whereyouad.WhereYouAd.domains.click.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import com.whereyouad.WhereYouAd.domains.click.domain.config.ClickSurgeProperties;
import com.whereyouad.WhereYouAd.domains.click.domain.constant.ClickWindowKeys;
import com.whereyouad.WhereYouAd.domains.click.persistence.entity.ClickAnomalyEvent;
import com.whereyouad.WhereYouAd.domains.click.persistence.entity.ClickBaselineStat;
import com.whereyouad.WhereYouAd.domains.click.persistence.repository.ClickAnomalyEventRepository;
import com.whereyouad.WhereYouAd.domains.click.persistence.repository.ClickBaselineStatRepository;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.NotificationType;
import com.whereyouad.WhereYouAd.domains.notification.domain.service.NotificationService;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClickSurgeDetectionServiceTest {

    @Mock
    private RedisUtil redisUtil;
    @Mock
    private ClickBaselineStatRepository baselineStatRepository;
    @Mock
    private ClickAnomalyEventRepository anomalyEventRepository;
    @Mock
    private AdContentRepository adContentRepository;
    @Mock
    private NotificationService notificationService;

    private ClickSurgeDetectionService service;

    private final ClickSurgeProperties props = new ClickSurgeProperties();
    private final LocalDateTime windowStart = LocalDateTime.of(2026, 8, 10, 14, 0);

    private static final Long AD_ID = 101L;
    private static final Long ORG_ID = 11L;

    @BeforeEach
    void setUp() {
        service = new ClickSurgeDetectionService(
                redisUtil, baselineStatRepository, anomalyEventRepository,
                adContentRepository, notificationService, props);
    }

    private void givenActiveAds(String... members) {
        when(redisUtil.sMembers(ClickWindowKeys.activeAdsKey(windowStart)))
                .thenReturn(Set.of(members));
    }

    private ClickBaselineStat warmedUpStat(Long adId, double mean, double variance) {
        ClickBaselineStat stat = ClickBaselineStat.init(adId, windowStart.getDayOfWeek().getValue(), windowStart.getHour());
        stat.updateStats(mean, variance, 20); // warm-up 완료 상태
        return stat;
    }

    // 분 카운터 multiGet - 요청 키 수만큼 minuteValue 반환
    private void givenMinuteCounts(String minuteValue) {
        lenient().when(redisUtil.multiGetData(any()))
                .thenAnswer(invocation -> {
                    List<String> keys = invocation.getArgument(0);
                    return keys.stream().map(key -> minuteValue).toList();
                });
    }

    @Test
    @DisplayName("급증 감지됐지만 streak 미달(1회)이면 발송하지 않고 이벤트만 기록")
    void streakBelowThresholdDoesNotNotify() {
        givenActiveAds(AD_ID + ":" + ORG_ID);
        givenMinuteCounts("40"); // 5분 합산 200회, baseline 40 → 5배
        when(baselineStatRepository.findByAdContentIdInAndWeekdayAndHourOfDay(anyCollection(), anyInt(), anyInt()))
                .thenReturn(List.of(warmedUpStat(AD_ID, 40, 25)));
        when(redisUtil.getData("click:surge:streak:" + AD_ID)).thenReturn(null); // 첫 감지

        service.detectForWindow(windowStart);

        ArgumentCaptor<ClickAnomalyEvent> eventCaptor = ArgumentCaptor.forClass(ClickAnomalyEvent.class);
        verify(anomalyEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().isNotified()).isFalse();
        verify(redisUtil).setDataExpire(eq("click:surge:streak:" + AD_ID), eq("1"), anyLong());
        verify(notificationService, never()).sendApiAlarmToOrg(anyLong(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("streak K회 도달 + 쿨다운 선점 성공이면 발송 후 notified=true 갱신")
    void streakReachedAndCooldownAcquiredNotifies() {
        givenActiveAds(AD_ID + ":" + ORG_ID);
        givenMinuteCounts("40");
        when(baselineStatRepository.findByAdContentIdInAndWeekdayAndHourOfDay(anyCollection(), anyInt(), anyInt()))
                .thenReturn(List.of(warmedUpStat(AD_ID, 40, 25)));
        when(redisUtil.getData("click:surge:streak:" + AD_ID)).thenReturn("1"); // 직전 윈도우 감지 이력
        when(redisUtil.setIfAbsent(startsWith("notification:cooldown:surge:ad:"), anyString(), anyLong()))
                .thenReturn(true);
        when(adContentRepository.findAllById(any())).thenReturn(List.of());
        when(anomalyEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.detectForWindow(windowStart);

        ArgumentCaptor<ClickAnomalyEvent> eventCaptor = ArgumentCaptor.forClass(ClickAnomalyEvent.class);
        verify(anomalyEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().isNotified()).isTrue();
        verify(notificationService).sendApiAlarmToOrg(eq(ORG_ID), eq(NotificationType.CLICKS), anyString(), anyString());
    }

    @Test
    @DisplayName("알림 발송 실패 시 notified=false 유지")
    void deliveryFailureKeepsNotifiedFalse() {
        givenActiveAds(AD_ID + ":" + ORG_ID);
        givenMinuteCounts("40");
        when(baselineStatRepository.findByAdContentIdInAndWeekdayAndHourOfDay(anyCollection(), anyInt(), anyInt()))
                .thenReturn(List.of(warmedUpStat(AD_ID, 40, 25)));
        when(redisUtil.getData("click:surge:streak:" + AD_ID)).thenReturn("1");
        when(redisUtil.setIfAbsent(startsWith("notification:cooldown:surge:ad:"), anyString(), anyLong()))
                .thenReturn(true);
        when(adContentRepository.findAllById(any())).thenReturn(List.of());
        when(anomalyEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("웹훅 실패"))
                .when(notificationService).sendApiAlarmToOrg(anyLong(), any(), anyString(), anyString());

        service.detectForWindow(windowStart);

        ArgumentCaptor<ClickAnomalyEvent> eventCaptor = ArgumentCaptor.forClass(ClickAnomalyEvent.class);
        verify(anomalyEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().isNotified()).isFalse();
        verify(anomalyEventRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("쿨다운 중이면 streak 도달해도 재발송하지 않음")
    void cooldownSuppressesNotification() {
        givenActiveAds(AD_ID + ":" + ORG_ID);
        givenMinuteCounts("40");
        when(baselineStatRepository.findByAdContentIdInAndWeekdayAndHourOfDay(anyCollection(), anyInt(), anyInt()))
                .thenReturn(List.of(warmedUpStat(AD_ID, 40, 25)));
        when(redisUtil.getData("click:surge:streak:" + AD_ID)).thenReturn("3");
        when(redisUtil.setIfAbsent(startsWith("notification:cooldown:surge:ad:"), anyString(), anyLong()))
                .thenReturn(false); // 쿨다운 선점 실패

        service.detectForWindow(windowStart);

        verify(notificationService, never()).sendApiAlarmToOrg(anyLong(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("미감지 윈도우는 streak 리셋 + EMA 갱신")
    void normalWindowResetsStreakAndUpdatesEma() {
        givenActiveAds(AD_ID + ":" + ORG_ID);
        givenMinuteCounts("8"); // 5분 합산 40회 = 평소 수준
        ClickBaselineStat stat = warmedUpStat(AD_ID, 40, 25);
        when(baselineStatRepository.findByAdContentIdInAndWeekdayAndHourOfDay(anyCollection(), anyInt(), anyInt()))
                .thenReturn(List.of(stat));

        service.detectForWindow(windowStart);

        verify(redisUtil).deleteData("click:surge:streak:" + AD_ID);
        verify(anomalyEventRepository, never()).save(any());
        assertThat(stat.getSampleCount()).isEqualTo(21); // EMA 갱신됨
        verify(baselineStatRepository).saveAll(any());
    }

    @Test
    @DisplayName("같은 조직의 여러 광고가 동시 급증하면 조직당 알림 1건으로 묶어 발송")
    void multipleAdsSameOrgBundledIntoOneNotification() {
        Long adId2 = 102L;
        givenActiveAds(AD_ID + ":" + ORG_ID, adId2 + ":" + ORG_ID);
        givenMinuteCounts("40");
        when(baselineStatRepository.findByAdContentIdInAndWeekdayAndHourOfDay(anyCollection(), anyInt(), anyInt()))
                .thenReturn(List.of(warmedUpStat(AD_ID, 40, 25), warmedUpStat(adId2, 40, 25)));
        when(redisUtil.getData(startsWith("click:surge:streak:"))).thenReturn("1");
        when(redisUtil.setIfAbsent(startsWith("notification:cooldown:surge:ad:"), anyString(), anyLong()))
                .thenReturn(true);
        when(adContentRepository.findAllById(any())).thenReturn(List.of());

        service.detectForWindow(windowStart);

        verify(notificationService, times(1))
                .sendApiAlarmToOrg(eq(ORG_ID), eq(NotificationType.CLICKS), anyString(), anyString());
    }

    @Test
    @DisplayName("드라이런 모드(notify-enabled=false)면 기록만 하고 쿨다운 소모·발송 없이 notified=false 유지")
    void dryRunRecordsButDoesNotNotify() {
        props.setNotifyEnabled(false);
        givenActiveAds(AD_ID + ":" + ORG_ID);
        givenMinuteCounts("40");
        when(baselineStatRepository.findByAdContentIdInAndWeekdayAndHourOfDay(anyCollection(), anyInt(), anyInt()))
                .thenReturn(List.of(warmedUpStat(AD_ID, 40, 25)));
        when(redisUtil.getData("click:surge:streak:" + AD_ID)).thenReturn("1");

        service.detectForWindow(windowStart);

        ArgumentCaptor<ClickAnomalyEvent> eventCaptor = ArgumentCaptor.forClass(ClickAnomalyEvent.class);
        verify(anomalyEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().isNotified()).isFalse();
        verify(redisUtil, never()).setIfAbsent(anyString(), anyString(), anyLong());
        verify(notificationService, never()).sendApiAlarmToOrg(anyLong(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("active set이 비어있으면 아무 작업도 하지 않음")
    void emptyActiveSetDoesNothing() {
        when(redisUtil.sMembers(anyString())).thenReturn(Set.of());

        service.detectForWindow(windowStart);

        verify(baselineStatRepository, never())
                .findByAdContentIdInAndWeekdayAndHourOfDay(any(Collection.class), anyInt(), anyInt());
        verify(notificationService, never()).sendApiAlarmToOrg(anyLong(), any(), anyString(), anyString());
    }
}
