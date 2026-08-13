package com.whereyouad.WhereYouAd.domains.notification.domain.service;

import com.whereyouad.WhereYouAd.domains.click.persistence.repository.ClickLogRepository;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.BotClickSummaryData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BotClickSummaryDataLoaderTest {

    @Mock
    private ClickLogRepository clickLogRepository;

    @InjectMocks
    private BotClickSummaryDataLoader dataLoader;

    @Test
    void keepsTheThreeQuerySelectedAdsPerOrganizationInQueryOrder() {
        LocalDate targetDate = LocalDate.of(2026, 8, 10);
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();
        when(clickLogRepository.summarizeSuspectClicksByOrg(start, end)).thenReturn(List.of(
                new Object[]{1L, "Organization 1", 15L, 3L, 3L},
                new Object[]{2L, "Organization 2", 12L, 2L, 3L}
        ));
        when(clickLogRepository.summarizeSuspectAdClicksByOrg(start, end, 3)).thenReturn(List.of(
                new Object[]{BigInteger.ONE, "Ad A", BigInteger.valueOf(7)},
                new Object[]{BigInteger.ONE, "Ad B", BigInteger.valueOf(5)},
                new Object[]{BigInteger.ONE, "Ad C", BigInteger.valueOf(3)},
                new Object[]{BigInteger.TWO, "Ad D", BigInteger.valueOf(6)},
                new Object[]{BigInteger.TWO, "Ad E", BigInteger.valueOf(4)},
                new Object[]{BigInteger.TWO, "Ad F", BigInteger.valueOf(2)}
        ));

        List<BotClickSummaryData> summaries = dataLoader.load(targetDate);

        assertThat(summaries).containsExactly(
                new BotClickSummaryData(1L, "Organization 1", 15L, 3L, 3L, List.of(
                        new BotClickSummaryData.TopAd("Ad A", 7L),
                        new BotClickSummaryData.TopAd("Ad B", 5L),
                        new BotClickSummaryData.TopAd("Ad C", 3L)
                )),
                new BotClickSummaryData(2L, "Organization 2", 12L, 2L, 3L, List.of(
                        new BotClickSummaryData.TopAd("Ad D", 6L),
                        new BotClickSummaryData.TopAd("Ad E", 4L),
                        new BotClickSummaryData.TopAd("Ad F", 2L)
                ))
        );
        verify(clickLogRepository).summarizeSuspectAdClicksByOrg(start, end, 3);
    }
}
