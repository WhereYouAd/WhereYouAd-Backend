package com.whereyouad.WhereYouAd.domains.dashboard.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.AdvertisementHandler;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.code.AdvertisementErrorCode;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.BudgetFieldType;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.BudgetHistory;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdCampaignRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.BudgetHistoryRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.projection.MetricSumProjection;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.projection.RoasProjection;
import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;
import com.whereyouad.WhereYouAd.domains.dashboard.application.mapper.DashboardConverter;
import com.whereyouad.WhereYouAd.domains.dashboard.exception.DashboardException;
import com.whereyouad.WhereYouAd.domains.dashboard.exception.code.DashboardErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgStatus;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.domains.project.exception.code.ProjectErrorCode;
import com.whereyouad.WhereYouAd.global.utils.BudgetCalculator;
import com.whereyouad.WhereYouAd.global.utils.MetricCalculator;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AdCampaignRepository adCampaignRepository;
    private final MetricFactRepository metricFactRepository;
    private final BudgetHistoryRepository budgetHistoryRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final OrgRepository orgRepository;
    private final BudgetCalculator budgetCalculator;
    private final MetricCalculator metricCalculator;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse.BudgetSummaryResponse getBudgetSummary(Long userId, Long orgId, String providerType) {

        // 조직 존재 여부 확인
        orgRepository.findById(orgId)
                .orElseThrow(() -> new DashboardException(OrgErrorCode.ORG_NOT_FOUND));

        // 유저가 해당 조직인지 검증
        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new DashboardException(OrgErrorCode.ORG_MEMBER_NOT_FOUND));

        Long totalBudget;
        BigDecimal totalSpendDec;

        // 통합 대시보드
        if (providerType == null || providerType.trim().isEmpty()) {
            totalBudget = adCampaignRepository.sumAllBudgetsByUserIdAndOrgId(userId, orgId);
            totalSpendDec = metricFactRepository.sumAllSpendsByUserIdAndOrgId(userId, orgId);
            providerType = "ALL";
        }
        // 플랫폼 대시보드
        else {
            Provider provider = Provider.valueOf(providerType.toUpperCase());
            totalBudget = adCampaignRepository.sumBudgetsByUserIdAndOrgIdAndProvider(userId, orgId, provider);
            totalSpendDec = metricFactRepository.sumSpendsByUserIdAndOrgIdAndProvider(userId, orgId, provider);
            providerType = provider.name();
        }

        // Null 방지
        totalBudget = (totalBudget != null) ? totalBudget : 0L;
        Long totalSpend = (totalSpendDec != null) ? totalSpendDec.longValue() : 0L;

        // 잔액 및 퍼센트 (BudgetCalculator에 위임)
        Long remainingBudget = budgetCalculator.calculateRemainingBudget(totalBudget, totalSpend);
        Double usagePercentage = budgetCalculator.calculateUsageRate(totalBudget, BigDecimal.valueOf(totalSpend));

        return new DashboardResponse.BudgetSummaryResponse(
                providerType,
                usagePercentage,
                totalBudget,
                totalSpend,
                remainingBudget);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse.AggregatedSummaryResponse getAggregatedMetrics(Long userId, Long orgId, String providerType) {
        //Organization 존재 확인
        if (!orgRepository.existsById(orgId)) {
            throw new AdvertisementHandler(OrgErrorCode.ORG_NOT_FOUND);
        }

        //해당 회원이 조직에 속하는지 확인
        boolean isMember = orgMemberRepository.existsByUserIdAndOrganizationId(userId, orgId);
        if (!isMember) {
            throw new DashboardException(ProjectErrorCode.ACCESS_FORBIDDEN);
        }

        //DB 내부 Mock data 중 조직별로 가장 최근의 timeBucket 값 추출
        LocalDateTime latestDate = metricFactRepository.findLatestTimeBucketByOrgId(orgId)
                .orElse(LocalDateTime.now());

        //DB 내부 조직별 Mock data 중 가장 최근의 timeBucket 값 기반 한달전, 두달전 기준 정립
        LocalDateTime oneMonthAgo = latestDate.minusMonths(1);

        LocalDateTime twoMonthsAgo = latestDate.minusMonths(2);

        MetricSumProjection currentProjection;
        MetricSumProjection pastProjection;

        if (providerType == null || providerType.trim().isEmpty()) { //providerType 입력 안됐으면, 조직 내 모든 데이터 집계
            //시간값들과 회원이 속한 project 리스트 기반 projection 으로 DB 에서
            //TotalImpressions, TotalClicks, TotalConversions, TotalSpend, TotalRevenue 를 집계해서 가져오기
            currentProjection = metricFactRepository.findMetricsSumByOrgIdAndDateRange(
                    orgId, oneMonthAgo, latestDate, OrgStatus.ACTIVE, Status.ON_GOING
            ); //가장 최근 ~ 한달 전의 집계 projection

            pastProjection = metricFactRepository.findMetricsSumByOrgIdAndDateRange(
                    orgId, twoMonthsAgo, oneMonthAgo, OrgStatus.ACTIVE, Status.ON_GOING
            ); //한달전 ~ 두달전의 집계 projection

        } else { //providerType 이 있다면, 해당 provider 데이터 집계

            Provider provider;
            try { //providerType 에 잘못된 값이 입력되지 않았는지 검증
                provider = Provider.valueOf(providerType.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new DashboardException(DashboardErrorCode.PROVIDER_NOT_VALID);
            }
            currentProjection = metricFactRepository.findMetricsSumByOrgIdAndProvider(
                    orgId, provider, oneMonthAgo, latestDate, OrgStatus.ACTIVE, Status.ON_GOING);
            pastProjection = metricFactRepository.findMetricsSumByOrgIdAndProvider(
                    orgId, provider, twoMonthsAgo, oneMonthAgo, OrgStatus.ACTIVE, Status.ON_GOING);
        }

        //최근 ~ 한달전 ROAS 값 계산하기 -> revenue / spend * 100
        BigDecimal currTotalRevenue = metricCalculator.toZeroIfNull(currentProjection.getTotalRevenue());
        BigDecimal currTotalSpend = metricCalculator.toZeroIfNull(currentProjection.getTotalSpend());
        BigDecimal currentRoasBigDecimal = metricCalculator.safePercent(currTotalRevenue, currTotalSpend);

        //한달전 ~ 두달전 ROAS 값 계산 -> revenue / spend * 100
        BigDecimal pastTotalRevenue = metricCalculator.toZeroIfNull(pastProjection.getTotalRevenue());
        BigDecimal pastTotalSpend = metricCalculator.toZeroIfNull(pastProjection.getTotalSpend());
        BigDecimal pastRoas = metricCalculator.safePercent(pastTotalRevenue, pastTotalSpend);

        //전환율(CVR) 계산 -> totalConversions(전환수 합계) / totalClicks(클릭수 합계) * 100
        double rawCurrentCvr = metricCalculator.safePercent(currentProjection.getTotalConversions(), currentProjection.getTotalClicks());
        double rawPastCvr = metricCalculator.safePercent(pastProjection.getTotalConversions(), pastProjection.getTotalClicks());

        //각각의 지표값 -> 클릭수(totalClicks), 노출수(totalImpressions), 전환율(currentCvr), 광고비 대비 매출(ROAS)
        Long totalClicks = currentProjection.getTotalClicks() != null ? currentProjection.getTotalClicks() : 0L;
        Long totalImpressions = currentProjection.getTotalImpressions() != null ? currentProjection.getTotalImpressions() : 0L;
        double currentCvr = Math.floor(rawCurrentCvr * 100.0) / 100.0; //CVR 소수점 2번째자리까지만 파싱
        double currentRoas = currentRoasBigDecimal.doubleValue(); //ROAS 소수점 2번째자리까지만 파싱된 BigDecimal -> double 로 형변환

        //각 지표값의 변화율 -> 클릭수 변화율, 노출수 변화율, 전환율 변화율, ROAS 변화율
        Double clickChangeRate = metricCalculator.calculateChangeRate(currentProjection.getTotalClicks(), pastProjection.getTotalClicks());
        Double impressionChangeRate = metricCalculator.calculateChangeRate(currentProjection.getTotalImpressions(), pastProjection.getTotalImpressions());
        Double cvrChangeRate = metricCalculator.calculateChangeRate(rawCurrentCvr, rawPastCvr);
        Double roasChangeRate = metricCalculator.calculateChangeRate(currentRoasBigDecimal, pastRoas);

        return DashboardConverter.toAggregatedSummary(
                totalClicks,
                clickChangeRate,
                totalImpressions,
                impressionChangeRate,
                currentCvr,
                cvrChangeRate,
                currentRoas,
                roasChangeRate
                );
    }

    @Override
    @Transactional(readOnly = true)
    // 특정 조직의 전체 광고에 대한 기간별 ROAS 성과 순위 조회
    public DashboardResponse.RankingROASList getRoasRanking(Long userId, Long orgId, LocalDate startDate, LocalDate endDate) {

        // 1. 날짜 유효성 검사
        // 시작일이나 종료일이 미래인 경우
        if (startDate.isAfter(LocalDate.now()) || endDate.isAfter(LocalDate.now())) {
            throw new AdvertisementHandler(AdvertisementErrorCode.INVALID_DATE_RANGE);
        }
        // 시작일보다 종료일이 더 빠른 경우
        if (startDate.isAfter(endDate)) {
            throw new AdvertisementHandler(AdvertisementErrorCode.INVALID_DATE_RANGE);
        }

        // 2. 조직 존재 여부 확인
        if (!orgRepository.existsById(orgId)) {
            throw new AdvertisementHandler(OrgErrorCode.ORG_NOT_FOUND);
        }

        // 3. 요청 멤버의 해당 조직에 대한 접근 권한 체크
        boolean isMember = orgMemberRepository.existsByUserIdAndOrganizationId(userId, orgId);
        if (!isMember) {
            throw new AdvertisementHandler(ProjectErrorCode.ACCESS_FORBIDDEN);
        }

        // 4. 현재 기간 성과 조회 (해당 조직의 모든 프로젝트 포함)
        List<RoasProjection> current = metricFactRepository.findRoasByOrgAndPeriod(
                orgId,
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59));

        // 해당 기간에 데이터 없는 경우 -> 빈 목록 반환
        if (current.isEmpty()) {
            return new DashboardResponse.RankingROASList(startDate, endDate, List.of());
        }

        // 5. 이전 기간 날짜 구하기 (startDate, endDate만큼의 기간을 구하여 뺌)
        // 조회한 기간의 총 일수 구하기 (시작일 포함이므로 +1)
        long periodDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        // 직전 동일 기간 구하기 (각각 총 일수만큼 그대로 빼기)
        LocalDate prevStart = startDate.minusDays(periodDays);
        LocalDate prevEnd = endDate.minusDays(periodDays);

        // 6. 이전 동일 기간 성과 조회 (diffRate 계산을 위함)
        List<RoasProjection> previous = metricFactRepository.findRoasByOrgAndPeriod(
                orgId,
                prevStart.atStartOfDay(),
                prevEnd.atTime(23, 59, 59));

        // 7. 이전 기간 결과를 Map<String(provider), Double(roas)> 로 변환
        Map<String, Double> prevRoasMap = previous.stream()
                .collect(Collectors.toMap(
                        RoasProjection::getProvider,
                        proj -> metricCalculator.calculateRoas(proj.getTotalRevenue(), proj.getTotalSpend())));

        // 8. 현재 기간 결과를 ROAS 기준 내림차순 정렬
        List<RoasProjection> sorted = current.stream()
                .sorted(Comparator.comparingDouble(
                        (RoasProjection p) -> metricCalculator.calculateRoas(p.getTotalRevenue(), p.getTotalSpend())).reversed())
                .toList();

        // 9. DTO 변환 + 순위(rank) + diffRate 계산
        List<DashboardResponse.RankingROAS> rankings = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {

            // i번째(순위) RoasProjection 값
            RoasProjection proj = sorted.get(i);

            // 해당 RoasProjection에 대한 값들
            Double currentRoas = metricCalculator.calculateRoas(proj.getTotalRevenue(), proj.getTotalSpend()); // 현재 roas
            Double prevRoas = prevRoasMap.get(proj.getProvider()); // 이전 roas
            Integer diffRate = metricCalculator.computeDiffRate(currentRoas, prevRoas); // 변화율 계산
            Provider provider = Provider.valueOf(proj.getProvider()); // String -> Enum 변환

            rankings.add(DashboardConverter.toRankingROAS(
                    i + 1,
                    provider,
                    currentRoas,
                    diffRate,
                    proj.getTotalRevenue(),
                    proj.getTotalSpend()
            ));
        }

        return new DashboardResponse.RankingROASList(startDate, endDate, rankings);
    }

    @Override
    @Transactional(readOnly = true)
    // 지정된 날짜(startDate ~ endDate)동안의 진행 중(ON_GOING) 상태인 광고 개수를 찾는 메서드
    public DashboardResponse.OngoingPlatformAdCountResponse getOngoingAdCountByProvider(
            Long userId, Long orgId, LocalDate startDate, LocalDate endDate) {

        // 1. 날짜 유효성 검사
        // 시작일이나 종료일이 미래인 경우
        if (startDate.isAfter(LocalDate.now()) || endDate.isAfter(LocalDate.now())) {
            throw new DashboardException(DashboardErrorCode.INVALID_DATE_RANGE);
        }
        // 시작일보다 종료일이 더 빠른 경우
        if (startDate.isAfter(endDate)) {
            throw new DashboardException(DashboardErrorCode.INVALID_DATE_RANGE);
        }

        // 2. 조직 존재 여부 확인
        orgRepository.findById(orgId)
                .orElseThrow(() -> new DashboardException(OrgErrorCode.ORG_NOT_FOUND));

        // 3. 유저가 해당 조직 멤버인지 검증
        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new DashboardException(DashboardErrorCode.ACCESS_FORBIDDEN));

        // 4. 진행 중인 광고 개수 세기
        List<DashboardResponse.OngoingPlatformAdCount> providerCount = adCampaignRepository
                .countOngoingAdsByProvider(orgId, Status.ON_GOING, startDate, endDate);

        // 5. 변환 후 반환
        return DashboardConverter.toOngoingPlatformAdCountResponse(providerCount, startDate, endDate);
    }




    @Override
    @Transactional(readOnly = true)
    public DashboardResponse.PlatformMetricFactSummaryResponse getPlatformMetricFacts(
            Long userId, Long orgId, String providerType, Integer days) {

        // 1. 조직 접근 권한 검증
        orgRepository.findById(orgId)
                .orElseThrow(() -> new DashboardException(OrgErrorCode.ORG_NOT_FOUND));
        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new DashboardException(DashboardErrorCode.ACCESS_FORBIDDEN));

        // 2. 파라미터 및 조회 기간 설정
        Provider provider;
        try {
            provider = Provider.valueOf(providerType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DashboardException(DashboardErrorCode.PROVIDER_NOT_VALID);
        }

        int targetDays = (days != null && days > 0) ? days : 7;
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(targetDays - 1); // targetDays가 7이면 당일 포함 7일

        // 3. MetricFact 조회
        List<MetricFact> metricFacts = metricFactRepository.findMetricFactsByOrgAndProviderAndPeriod(
                orgId,
                provider,
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59)
        );

        // 4. 일자별 그룹화 및 합산
        Map<LocalDate, List<MetricFact>> factsByDate = metricFacts.stream()
                .collect(Collectors.groupingBy(mf -> mf.getTimeBucket().toLocalDate()));

        List<DashboardResponse.DailyMetricFactResponse> dailyMetrics = new ArrayList<>();

        long sumImpressions = 0L;
        long sumClicks = 0L;
        long sumConversions = 0L;
        BigDecimal sumSpend = BigDecimal.ZERO;
        BigDecimal sumRevenue = BigDecimal.ZERO;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<MetricFact> dailyFacts = factsByDate.getOrDefault(date, List.of());

            long dImpressions = 0L;
            long dClicks = 0L;
            long dConversions = 0L;
            BigDecimal dSpend = BigDecimal.ZERO;
            BigDecimal dRevenue = BigDecimal.ZERO;

            for (MetricFact mf : dailyFacts) {
                dImpressions += (mf.getImpressions() != null) ? mf.getImpressions() : 0L;
                dClicks += (mf.getClicks() != null) ? mf.getClicks() : 0L;
                dConversions += (mf.getConversions() != null) ? mf.getConversions() : 0L;
                if (mf.getSpend() != null) dSpend = dSpend.add(mf.getSpend());
                if (mf.getRevenue() != null) dRevenue = dRevenue.add(mf.getRevenue());
            }

            sumImpressions += dImpressions;
            sumClicks += dClicks;
            sumConversions += dConversions;
            sumSpend = sumSpend.add(dSpend);
            sumRevenue = sumRevenue.add(dRevenue);

            dailyMetrics.add(new DashboardResponse.DailyMetricFactResponse(
                    date,
                    dImpressions,
                    dClicks,
                    dSpend.longValue(),
                    dConversions,
                    dRevenue.longValue(),
                    metricCalculator.calculateCtr(dClicks, dImpressions),
                    metricCalculator.calculateCpa(dSpend, dConversions),
                    metricCalculator.calculateRoas(dRevenue, dSpend)
            ));
        }

        // 5. 합계 지표 객체 생성
        DashboardResponse.DailyMetricFactResponse totalMetric = new DashboardResponse.DailyMetricFactResponse(
                null,
                sumImpressions,
                sumClicks,
                sumSpend.longValue(),
                sumConversions,
                sumRevenue.longValue(),
                metricCalculator.calculateCtr(sumClicks, sumImpressions),
                metricCalculator.calculateCpa(sumSpend, sumConversions),
                metricCalculator.calculateRoas(sumRevenue, sumSpend)
        );

        // 6. 결과 반환
        return new DashboardResponse.PlatformMetricFactSummaryResponse(
                provider.name(),
                startDate,
                endDate,
                totalMetric,
                dailyMetrics
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse.BudgetHistoryListResponse getBudgetHistory(
            Long userId, Long orgId, LocalDate startDate, LocalDate endDate) {

        orgRepository.findById(orgId)
                .orElseThrow(() -> new DashboardException(OrgErrorCode.ORG_NOT_FOUND));
        orgMemberRepository.findByUserIdAndOrgId(userId, orgId)
                .orElseThrow(() -> new DashboardException(DashboardErrorCode.ACCESS_FORBIDDEN));

        List<BudgetHistory> histories = budgetHistoryRepository.findByOrgAndPeriod(
                orgId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );

        List<DashboardResponse.BudgetHistoryItem> items = DashboardConverter.toBudgetHistoryItems(histories);

        return new DashboardResponse.BudgetHistoryListResponse(startDate, endDate, items);
    }
}