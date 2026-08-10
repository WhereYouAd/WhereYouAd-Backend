package com.whereyouad.WhereYouAd.domains.notification.domain.service;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.BotClickSummaryData;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BotClickSummaryNotificationServiceTest {

    @Mock
    private BotClickSummaryDataLoader dataLoader;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BotClickSummaryNotificationService service;

    private BotClickSummaryData summary(Long orgId, String orgName) {
        return new BotClickSummaryData(orgId, orgName, 342, 17, 5,
                List.of(new BotClickSummaryData.TopAd("광고A", 120)));
    }

    @Test
    @DisplayName("조직 1곳 발송 실패가 다른 조직 발송에 전파되지 않음")
    void sendFailureIsolatedPerOrg() {
        when(dataLoader.load(any(LocalDate.class)))
                .thenReturn(List.of(summary(1L, "조직1"), summary(2L, "조직2")));
        doThrow(new RuntimeException("웹훅 실패"))
                .when(notificationService).sendApiAlarmToOrg(eq(1L), any(), anyString(), anyString());

        service.sendDailyBotSummaries();

        verify(notificationService).sendApiAlarmToOrg(eq(2L), eq(NotificationType.CLICKS), anyString(), anyString());
    }

    @Test
    @DisplayName("봇 클릭 집계 대상 조직이 없으면 발송하지 않음")
    void noSummariesNoSend() {
        when(dataLoader.load(any(LocalDate.class))).thenReturn(List.of());

        service.sendDailyBotSummaries();

        verify(notificationService, never()).sendApiAlarmToOrg(anyLong(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("요약 메시지에 총 클릭수·유니크 IP·상위 광고가 포함됨")
    void messageContainsSummaryFields() {
        when(dataLoader.load(any(LocalDate.class))).thenReturn(List.of(summary(1L, "조직1")));

        service.sendDailyBotSummaries();

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendApiAlarmToOrg(
                eq(1L), eq(NotificationType.CLICKS),
                contains("조직1"),
                messageCaptor.capture());
        assertThat(messageCaptor.getValue())
                .contains("총 의심 클릭 342회")
                .contains("유니크 IP 17개")
                .contains("광고A(120회)");
    }
}
