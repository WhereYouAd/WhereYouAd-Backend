package com.whereyouad.WhereYouAd.domains.click.presentation.scheduler;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import com.whereyouad.WhereYouAd.domains.click.application.dto.ClickDto;
import com.whereyouad.WhereYouAd.domains.click.exception.ClickHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DummyClickProducerTest {

    @Mock
    private KafkaTemplate<String, ClickDto> kafkaTemplate;

    @Mock
    private AdContentRepository adContentRepository;

    private DummyClickProducer producer;

    @BeforeEach
    void setUp() {
        producer = new DummyClickProducer(kafkaTemplate, adContentRepository);
        ReflectionTestUtils.setField(producer, "maxRunningMinutes", 30L);
    }

    @Test
    @DisplayName("비활성화 환경에서는 토글 요청 시 예외가 발생하고 광고를 조회하지 않는다")
    void toggle_disabled_throws() {
        ReflectionTestUtils.setField(producer, "enabled", false);

        assertThatThrownBy(() -> producer.toggle()).isInstanceOf(ClickHandler.class);
        verifyNoInteractions(adContentRepository);
    }

    @Test
    @DisplayName("활성화 환경에서는 토글로 켜고 끌 수 있고, 켜진 동안 더미 클릭을 발행한다")
    void toggle_enabled_runsAndStops() {
        enableWithOneAd();

        assertThat(producer.toggle()).isTrue();
        producer.generateDummyClick();
        verify(kafkaTemplate, atLeastOnce()).send(eq("ad-click-events"), anyString(), any(ClickDto.class));

        assertThat(producer.toggle()).isFalse();
    }

    @Test
    @DisplayName("최대 실행 시간이 지나면 자동으로 꺼지고 더 이상 발행하지 않는다")
    void generate_afterMaxRunningTime_autoStops() {
        enableWithOneAd();
        producer.toggle();
        ReflectionTestUtils.setField(producer, "startedAt", Instant.now().minus(Duration.ofMinutes(31)));

        producer.generateDummyClick();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(ClickDto.class));
        assertThat((boolean) ReflectionTestUtils.getField(producer, "isRunning")).isFalse();
    }

    private void enableWithOneAd() {
        ReflectionTestUtils.setField(producer, "enabled", true);
        List<Object[]> ads = List.<Object[]>of(new Object[]{1L, 10L, Provider.values()[0]});
        when(adContentRepository.findAllActiveAdOrgMappings(Status.ON_GOING)).thenReturn(ads);
    }
}
