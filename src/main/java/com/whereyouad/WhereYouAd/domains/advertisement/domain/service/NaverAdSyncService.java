package com.whereyouad.WhereYouAd.domains.advertisement.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Grain;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdCampaignRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdGroupRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.platform.exception.PlatformHandler;
import com.whereyouad.WhereYouAd.domains.platform.exception.code.PlatformErrorCode;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformConnectionRepository;
import com.whereyouad.WhereYouAd.infrastructure.client.naver.converter.NaverConverter;
import com.whereyouad.WhereYouAd.infrastructure.client.naver.dto.NaverDTO;
import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.response.AdvertisementResponse;
import com.whereyouad.WhereYouAd.domains.advertisement.application.mapper.AdvertisementConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.PostConstruct;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverAdSyncService {

    private final NaverAdApiService naverAdApiService;
    private final AdCampaignRepository adCampaignRepository;
    private final AdGroupRepository adGroupRepository;
    private final AdContentRepository adContentRepository;
    private final MetricFactRepository metricFactRepository;
    private final PlatformConnectionRepository platformConnectionRepository;

    // 작은 단위로 트랜잭션을 끊기 위한 템플릿
    private final PlatformTransactionManager transactionManager;
    private TransactionTemplate txTemplate;

    @PostConstruct
    public void init() {
        this.txTemplate = new TransactionTemplate(transactionManager);

        // 광고 하나/광고그룹 하나 단위로 독립 트랜잭션 처리
        // 일부 실패해도 전체 배치가 다 롤백되지 않게 하기 위함
        this.txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    // 광고 정보(캠페인/광고그룹/광고소재) 메타데이터 upsert
    public AdvertisementResponse.NaverMetadataSyncResponse syncAllMetadata(Long connectionId) {
        log.info("NAVER 광고 동기화 시작 - connectionId: {}", connectionId);

        // 1. connectionId로 연동 계정 조회
        PlatformConnection connection = platformConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new PlatformHandler(PlatformErrorCode.PLATFORM_CONNECTION_NOT_FOUND));

        // 2. 연결 계정 기준으로 네이버 캠페인 목록 조회
        List<NaverDTO.CampaignResponse> campaigns = naverAdApiService.getCampaigns(connectionId);

        int campaignCount = 0;
        int groupCount = 0;
        int contentCount = 0;

        for (NaverDTO.CampaignResponse campaignDto : campaigns) {
            try {
                AdCampaign adCampaign = txTemplate.execute(status -> {
                    // 3. 같은 platformAccount 범위(NAVER) 안에서만 캠페인 조회
                    AdCampaign entity = adCampaignRepository
                            .findByExternalCampaignIdAndPlatformAccount(campaignDto.nccCampaignId(),
                                    connection.getPlatformAccount())
                            .map(existing -> {
                                // 있으면 업데이트
                                NaverConverter.updateAdCampaign(existing, campaignDto);
                                return adCampaignRepository.save(existing);
                            })
                            .orElseGet(() -> adCampaignRepository.save(
                                    // 없으면 신규 생성
                                    NaverConverter.toAdCampaignEntity(campaignDto,
                                            connection.getPlatformAccount().getOrganization(),
                                            connection.getPlatformAccount())));
                    return entity;
                });

                // 4. 캠페인 저장 후 하위 광고 그룹 동기화 호출
                if (adCampaign != null) {
                    campaignCount++;
                    int[] subCounts = syncAdGroups(connectionId, connection.getPlatformAccount(), adCampaign);
                    groupCount += subCounts[0];
                    contentCount += subCounts[1];
                }

            } catch (Exception e) {
                // 한 캠페인 실패해도 다음 캠페인은 계속 진행
                log.error("NAVER 광고 캠페인(ID:{}) 동기화 중 오류 발생: {}", campaignDto.nccCampaignId(), e.getMessage());
            }
        }

        log.info("NAVER 광고 동기화 완료 - connectionId: {}, campaigns={}, groups={}, contents={}", 
                connectionId, campaignCount, groupCount, contentCount);
        return new AdvertisementResponse.NaverMetadataSyncResponse(connectionId, campaignCount, groupCount, contentCount);
    }

    private int[] syncAdGroups(Long connectionId,
            PlatformAccount platformAccount,
            AdCampaign adCampaign) {

        // 1. 특정 캠페인 아래 광고그룹 목록 조회
        List<NaverDTO.AdGroupResponse> adGroups = naverAdApiService.getAdGroups(connectionId,
                adCampaign.getExternalCampaignId());

        int count = 0;
        int subContentCount = 0;

        for (NaverDTO.AdGroupResponse groupDto : adGroups) {
            try {
                // 2. 광고그룹에 속한 키워드 목록을 조회해서 targetingInfo 문자열로 압축
                List<NaverDTO.KeywordResponse> keywords = naverAdApiService.getKeywords(connectionId,
                        groupDto.nccAdgroupId());

                log.info("NAVER 광고 그룹 동기화 시도 - groupId={}", groupDto.nccAdgroupId());

                AdGroup adGroup = txTemplate.execute(status -> {

                    // 3. 같은 platformAccount 범위 안에서만 광고그룹 조회
                    return adGroupRepository
                            .findByExternalGroupIdAndPlatformAccount(groupDto.nccAdgroupId(), platformAccount)
                            .map(entity -> {
                                // 있으면 업데이트
                                NaverConverter.updateAdGroup(entity, groupDto, keywords);
                                return adGroupRepository.save(entity);
                            })
                            .orElseGet(() -> adGroupRepository.save(
                                    // 없으면 신규 생성
                                    NaverConverter.toAdGroupEntity(groupDto, adCampaign, keywords)));
                });

                // 4. 광고그룹 저장 후 하위 광고소재 동기화
                if (adGroup != null) {
                    count++;
                    subContentCount += syncAdContents(connectionId, platformAccount, adGroup);
                }

            } catch (Exception e) {
                log.error("NAVER 광고 광고 그룹(ID:{}) 동기화 중 오류 발생: {}", groupDto.nccAdgroupId(), e.getMessage(), e);
            }
        }
        return new int[]{count, subContentCount};
    }

    private int syncAdContents(Long connectionId,
            PlatformAccount platformAccount,
            AdGroup adGroup) {

        // 1. 특정 광고그룹 아래 광고소재 목록 조회
        List<NaverDTO.AdResponse> ads = naverAdApiService.getAds(connectionId, adGroup.getExternalGroupId());

        int count = 0;
        for (NaverDTO.AdResponse adDto : ads) {
            try {
                txTemplate.execute(status -> {

                    // 2. 같은 platformAccount 범위 안에서만 광고소재 조회
                    return adContentRepository.findByExternalAdIdAndPlatformAccount(adDto.nccAdId(), platformAccount)
                            .map(entity -> {
                                // 있으면 업데이트
                                NaverConverter.updateAdContent(entity, adDto);
                                return adContentRepository.save(entity);
                            })
                            .orElseGet(() -> adContentRepository.save(
                                    // 없으면 신규 생성
                                    NaverConverter.toAdContentEntity(adDto, adGroup)));
                });
                count++;
            } catch (Exception e) {
                log.error("NAVER 광고 광고 소재(ID:{}) 동기화 중 오류 발생: {}", adDto.nccAdId(), e.getMessage());
            }
        }
        return count;
    }

    // 시간별/일별 기본 지표 MetricFact upsert
    public AdvertisementResponse.NaverStatSyncResponse syncBasicStats(Long connectionId, String statDate) {
        log.info("NAVER Basic Stats 동기화 시작 - connectionId: {}, date: {}", connectionId, statDate);

        // 1. connectionId로 실제 연동 계정 조회
        PlatformConnection connection = platformConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new PlatformHandler(PlatformErrorCode.PLATFORM_CONNECTION_NOT_FOUND));

        // 2. 이 계정(platformAccount)에 속한 광고소재만 조회
        List<AdContent> adContents = adContentRepository.findAllByPlatformAccount(connection.getPlatformAccount());

        int processedCount = 0;
        for (AdContent adContent : adContents) {
            if (adContent.getExternalAdId() == null)
                continue;

            try {
                // 3. 광고소재 1개에 대해 하루(hourly) 통계 조회
                List<NaverDTO.StatResponse> stats = naverAdApiService.getHourlyStats(connectionId,
                        adContent.getExternalAdId(), statDate, statDate);

                // 4. "00" ~ "23" 키로 빠르게 찾기 위한 Map 구성
                Map<String, NaverDTO.StatResponse> hourMap = stats.stream()
                        .filter(s -> s.hour() != null)
                        .collect(Collectors.toMap(NaverDTO.StatResponse::hour, s -> s, (s1, s2) -> s1));

                txTemplate.execute(status -> {
                    long dailyImp = 0L;
                    long dailyClk = 0L;
                    BigDecimal dailySales = BigDecimal.ZERO;

                    // 5. 24시간을 돌면서 hourly MetricFact upsert
                    for (int i = 0; i < 24; i++) {
                        String hh24 = String.format("%02d", i);
                        LocalDateTime timeBucket = LocalDate.parse(statDate).atTime(i, 0);

                        NaverDTO.StatResponse stat = hourMap.get(hh24);

                        // 응답이 없으면 0으로 채움
                        Long impCnt = stat != null && stat.impCnt() != null ? stat.impCnt() : 0L;
                        Long clkCnt = stat != null && stat.clkCnt() != null ? stat.clkCnt() : 0L;
                        BigDecimal salesAmt = stat != null && stat.salesAmt() != null
                                ? BigDecimal.valueOf(stat.salesAmt())
                                : BigDecimal.ZERO;

                        // daily 합계도 같이 누적
                        dailyImp += impCnt;
                        dailyClk += clkCnt;
                        dailySales = dailySales.add(salesAmt);

                        // 해당 시간대의 hourly MetricFact 조회 or 생성
                        MetricFact metricFact = metricFactRepository
                                .findByAdContentAndTimeBucketAndGrain(adContent, timeBucket, Grain.HOURLY)
                                .orElseGet(() -> AdvertisementConverter.createMetricFact(
                                        adContent, timeBucket, Grain.HOURLY, Provider.NAVER));

                        // 기본 지표(impressions, clicks, spend) 반영
                        metricFact.updateBasicMetrics(impCnt, clkCnt, salesAmt);
                        metricFactRepository.save(metricFact);
                    }

                    // 6. 하루 단위 DAILY MetricFact도 별도로 upsert
                    LocalDateTime dailyTimeBucket = LocalDate.parse(statDate).atStartOfDay();
                    MetricFact dailyFact = metricFactRepository
                            .findByAdContentAndTimeBucketAndGrain(adContent, dailyTimeBucket, Grain.DAILY)
                            .orElseGet(() -> AdvertisementConverter.createMetricFact(
                                    adContent, dailyTimeBucket, Grain.DAILY, Provider.NAVER));

                    dailyFact.updateBasicMetrics(dailyImp, dailyClk, dailySales);
                    metricFactRepository.save(dailyFact);

                    return null;
                });

            } catch (Exception e) {
                log.error("NAVER 광고 소재(ID:{}) 통계(Basic) 동기화 오류: {}", adContent.getExternalAdId(), e.getMessage());
            }
        }

        log.info("NAVER Basic Stats 동기화 완료 - connectionId: {}, date: {}, processed: {}",
                connectionId, statDate, processedCount);
        return new AdvertisementResponse.NaverStatSyncResponse(connectionId, statDate, processedCount);
    }

    // 전환 리포트 요청/다운로드
    public AdvertisementResponse.NaverStatSyncResponse syncConversionReports(Long connectionId, String statDate) {
        log.info("NAVER Conversion Report 동기화 시작 - connectionId: {}, date: {}", connectionId, statDate);
        PlatformConnection connection = platformConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new PlatformHandler(PlatformErrorCode.PLATFORM_CONNECTION_NOT_FOUND));

        try {
            // 1. 전환 리포트 생성 요청
            NaverDTO.StatReportResponse reportInit = naverAdApiService.requestAdConversionReport(connectionId,
                    statDate);

            // 리포트 자체가 없으면 종료 (광고 승인이 아직 안된 경우)
            if (reportInit == null || reportInit.reportJobId() == null) {
                log.info("NAVER 성과 리포트 미발행(지표 없음 등). statDate={}", statDate);
                return new AdvertisementResponse.NaverStatSyncResponse(connectionId, statDate, 0);
            }

            String reportJobId = reportInit.reportJobId();
            String downloadUrl = null;

            // 2. 리포트 생성 완료될 때까지 polling
            for (int i = 0; i < 24; i++) {

                NaverDTO.StatReportResponse statusRes = naverAdApiService.getReportStatus(connectionId, reportJobId);

                if ("BUILT".equals(statusRes.status())) {
                    downloadUrl = statusRes.downloadUrl();
                    break;
                } else if ("ERROR".equals(statusRes.status()) || "NONE".equals(statusRes.status())) {
                    log.error("NAVER 리포트 생성 실패 상태: {}", statusRes.status());
                    return new AdvertisementResponse.NaverStatSyncResponse(connectionId, statDate, 0);
                }

                Thread.sleep(5000);
            }

            if (downloadUrl == null) {
                log.error("NAVER 리포트 대기 시간 초과 - reportJobId: {}", reportJobId);
                return new AdvertisementResponse.NaverStatSyncResponse(connectionId, statDate, 0);
            }

            // 3. 리포트 다운로드 후 파싱/반영
            NaverDTO.RawReportResponse rawReport = naverAdApiService.downloadReport(connectionId, downloadUrl);
            int processedCount = parseAndUpsertConversions(rawReport.rawContent(), statDate, connection.getPlatformAccount());

            return new AdvertisementResponse.NaverStatSyncResponse(connectionId, statDate, processedCount);

        } catch (Exception e) {
            log.error("NAVER Conversion Report 동기화 전체 로직 오류: {}", e.getMessage(), e);
            return new AdvertisementResponse.NaverStatSyncResponse(connectionId, statDate, 0);
        }
    }

    // 다운받은 리포트(TSV 파일)를 파싱해서 전환값 반영
    private int parseAndUpsertConversions(String rawContent, String statDate,
                                          PlatformAccount platformAccount) {
        if (rawContent == null || rawContent.isEmpty())
            return 0;

        // 1. TSV 리포트를 줄 단위로 분리
        String[] lines = rawContent.split("\n");
        if (lines.length <= 1)
            return 0;

        // 2. 헤더 위치 파악
        String[] headers = lines[0].split("\t");
        int adIdIdx = -1, hourIdx = -1, convIdx = -1, revIdx = -1;

        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].trim().replaceAll("\"", "");
            if (header.contains("광고 ID") || header.equalsIgnoreCase("Ad ID"))
                adIdIdx = i;
            if (header.contains("시간") || header.equalsIgnoreCase("Hour"))
                hourIdx = i;
            if (header.contains("전환수") || header.equalsIgnoreCase("Conversions") || header.equalsIgnoreCase("ccnt")
                    || header.equalsIgnoreCase("convCnt"))
                convIdx = i;
            if (header.contains("전환매출액") || header.equalsIgnoreCase("Conversion Amount")
                    || header.equalsIgnoreCase("convAmt") || header.equalsIgnoreCase("Revenue"))
                revIdx = i;
        }

        // 광고 ID, 전환수 컬럼은 필수
        if (adIdIdx == -1 || convIdx == -1) {
            log.warn("NAVER 리포트 필수 헤더 매핑 실패. (adIdIdx={}, convIdx={})", adIdIdx, convIdx);
            return 0;
        }

        // 3. 하루 합계 계산용 Map
        Map<String, Long> dailyConversionsMap = new HashMap<>();
        Map<String, BigDecimal> dailyRevenueMap = new HashMap<>();

        int processedCount = 0;
        // 4. 각 라인을 읽어서 hourly 전환값 반영
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.trim().isEmpty())
                continue;
            String[] cols = line.split("\t");

            try {
                if (cols.length <= adIdIdx)
                    continue;
                String externalAdId = cols[adIdIdx].trim().replaceAll("\"", "");

                String hourStr = hourIdx != -1 && cols.length > hourIdx ? cols[hourIdx].trim().replaceAll("\"", "")
                        : "00";
                if (hourStr.length() == 1)
                    hourStr = "0" + hourStr;
                if (hourStr.isEmpty() || hourStr.equals("-"))
                    hourStr = "00";

                String convStr = cols.length > convIdx ? cols[convIdx].trim().replaceAll("\"", "").replaceAll(",", "")
                        : "0";
                String revStr = revIdx != -1 && cols.length > revIdx
                        ? cols[revIdx].trim().replaceAll("\"", "").replaceAll(",", "")
                        : "0";

                Long conversions = Long.parseLong(convStr.isEmpty() ? "0" : convStr);
                BigDecimal revenue = new BigDecimal(revStr.isEmpty() ? "0" : revStr);

                // 하루 합계 누적
                dailyConversionsMap.put(
                        externalAdId,
                        dailyConversionsMap.getOrDefault(externalAdId, 0L) + conversions);
                dailyRevenueMap.put(
                        externalAdId,
                        dailyRevenueMap.getOrDefault(externalAdId, BigDecimal.ZERO).add(revenue));

                LocalDateTime timeBucket = LocalDate.parse(statDate).atTime(Integer.parseInt(hourStr), 0);

                txTemplate.execute(status -> {
                    // 같은 platformAccount 범위에서 광고소재 찾기
                    adContentRepository.findByExternalAdIdAndPlatformAccount(externalAdId, platformAccount)
                            .ifPresent(adContent -> {
                                metricFactRepository
                                        .findByAdContentAndTimeBucketAndGrain(adContent, timeBucket, Grain.HOURLY)
                                        .ifPresent(metricFact -> {
                                            // 기존 hourly MetricFact가 있을 때만 전환값 반영
                                            metricFact.updateConversionMetrics(conversions, revenue);
                                        });
                            });
                    return null;
                });
                processedCount++;
            } catch (Exception e) {
                log.warn("NAVER 리포트 라인 파싱 오류: {}", e.getMessage());
            }
        }

        // 5. 누적값으로 DAILY 전환값 반영
        LocalDateTime dailyTimeBucket = LocalDate.parse(statDate).atStartOfDay();

        for (String extAdId : dailyConversionsMap.keySet()) {
            final Long dailyConv = dailyConversionsMap.get(extAdId);
            final BigDecimal dailyRev = dailyRevenueMap.get(extAdId);

            try {
                txTemplate.execute(status -> {
                    adContentRepository.findByExternalAdIdAndPlatformAccount(extAdId, platformAccount)
                            .ifPresent(adContent -> {
                                metricFactRepository
                                        .findByAdContentAndTimeBucketAndGrain(adContent, dailyTimeBucket, Grain.DAILY)
                                        .ifPresent(dailyFact -> {
                                            dailyFact.updateConversionMetrics(dailyConv, dailyRev);
                                            metricFactRepository.save(dailyFact);
                                        });
                            });
                    return null;
                });
            } catch (Exception e) {
                log.warn("NAVER Daily 전환 리포트 업데이트 오류 (ID:{}): {}", extAdId, e.getMessage());
            }
        }
        return processedCount;
    }
}
