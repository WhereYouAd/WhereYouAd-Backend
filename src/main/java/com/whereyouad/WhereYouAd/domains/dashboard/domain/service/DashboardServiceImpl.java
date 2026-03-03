package com.whereyouad.WhereYouAd.domains.dashboard.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.AdvertisementException;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.AdvertisementException;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.code.AdvertisementErrorCode;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdCampaignRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AdCampaignRepository adCampaignRepository;
    private final MetricFactRepository metricFactRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final OrgRepository orgRepository;

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

        // 잔액 및 퍼센트
        Long remainingBudget = totalBudget - totalSpend;

        // 예산이 0원이면 0%, 아니면 (소진액 / 총예산 * 100) 값의 소수점 첫째 자리까지 반올림
        Double usagePercentage = (totalBudget == 0) ? 0.0
                : Math.round(((double) totalSpend / totalBudget) * 1000) / 10.0;

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
            throw new AdvertisementException(OrgErrorCode.ORG_NOT_FOUND);
        }

        //해당 회원이 조직에 속하는지 확인
        boolean isMember = orgMemberRepository.existsByUserIdAndOrganizationId(userId, orgId);
        if (!isMember) {
            throw new DashboardException(ProjectErrorCode.ACCESS_FORBIDDEN);
        }

        //DB 내부 Mock data 중 가장 최근의 timeBucket 값 추출
        LocalDateTime latestDate = metricFactRepository.findLatestTimeBucket()
                .orElse(LocalDateTime.now());

        //DB 내부 Mock data 중 가장 최근의 timeBucket 값 기반 한달전, 두달전 기준 정립
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
        BigDecimal currTotalRevenue = toZeroIfNull(currentProjection.getTotalRevenue());
        BigDecimal currTotalSpend = toZeroIfNull(currentProjection.getTotalSpend());
        BigDecimal currentRoasBigDecimal = safePercent(currTotalRevenue, currTotalSpend);

        //한달전 ~ 두달전 ROAS 값 계산 -> revenue / spend * 100
        BigDecimal pastTotalRevenue = toZeroIfNull(pastProjection.getTotalRevenue());
        BigDecimal pastTotalSpend = toZeroIfNull(pastProjection.getTotalSpend());
        BigDecimal pastRoas = safePercent(pastTotalRevenue, pastTotalSpend);

        //전환율(CVR) 계산 -> totalConversions(전환수 합계) / totalClicks(클릭수 합계) * 100
        double rawCurrentCvr = safePercent(currentProjection.getTotalConversions(), currentProjection.getTotalClicks());
        double rawPastCvr = safePercent(pastProjection.getTotalConversions(), pastProjection.getTotalClicks());

        //각각의 지표값 -> 클릭수(totalClicks), 노출수(totalImpressions), 전환율(currentCvr), 광고비 대비 매출(ROAS)
        Long totalClicks = currentProjection.getTotalClicks() != null ? currentProjection.getTotalClicks() : 0L;
        Long totalImpressions = currentProjection.getTotalImpressions() != null ? currentProjection.getTotalImpressions() : 0L;
        double currentCvr = Math.floor(rawCurrentCvr * 100.0) / 100.0; //CVR 소수점 2번째자리까지만 파싱
        double currentRoas = currentRoasBigDecimal.doubleValue(); //ROAS 소수점 2번째자리까지만 파싱된 BigDecimal -> double 로 형변환

        //각 지표값의 변화율 -> 클릭수 변화율, 노출수 변화율, 전환율 변화율, ROAS 변화율
        Double clickChangeRate = calculateChangeRate(currentProjection.getTotalClicks(), pastProjection.getTotalClicks());
        Double impressionChangeRate = calculateChangeRate(currentProjection.getTotalImpressions(), pastProjection.getTotalImpressions());
        Double cvrChangeRate = calculateChangeRate(rawCurrentCvr, rawPastCvr);
        Double roasChangeRate = calculateChangeRate(currentRoasBigDecimal, pastRoas);

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

    //변화율 계산 메서드
    private Double calculateChangeRate(Number current, Number past) {
        // null 방어 및 double 형변환
        double currentVal = (current != null) ? current.doubleValue() : 0.0;
        double pastVal = (past != null) ? past.doubleValue() : 0.0;

        // Divide by Zero 방어 (과거 데이터가 0인 경우)
        if (pastVal == 0.0) {
            if (currentVal > 0.0) {
                // 과거엔 0이었으나 현재 실적이 발생한 경우 (비즈니스 룰에 따라 100% 등으로 설정)
                return 100.0;
            }
            // 둘 다 0이거나 데이터가 아예 없는 경우
            return 0.0;
        }

        // 변화율 계산
        double changeRate = ((currentVal - pastVal) / pastVal) * 100.0;

        // 소수점 둘째 자리까지 버림 처리
        return BigDecimal.valueOf(changeRate)
                .setScale(2, RoundingMode.DOWN)
                .doubleValue();
    }

    //지표 값이 null 일 경우 BigDecimal 의 0 으로 바꿔주는 메서드
    private BigDecimal toZeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    //나눗셈 계산시 분모가 0 일 경우 나눗셈 시행하지 않고 0.00 반환
    private BigDecimal safePercent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() == 0 || numerator == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.DOWN);
        }
        return numerator.divide(denominator, 4, RoundingMode.DOWN)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.DOWN);
    }

    //나눗셈 계산시 분모가 0 일 경우 나눗셈 시행하지 않고 0.00 반환
    private double safePercent(Number numerator, Number denominator) {
        double n = numerator == null ? 0.0 : numerator.doubleValue();
        double d = denominator == null ? 0.0 : denominator.doubleValue();
        if (d == 0.0) return 0.0;
        return (n / d) * 100.0;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse.RankingROASList getRoasRanking(Long userId, Long orgId, LocalDate startDate, LocalDate endDate) {

        // 1. 날짜 유효성 검사
        // 시작일이나 종료일이 미래인 경우
        if (startDate.isAfter(LocalDate.now()) || endDate.isAfter(LocalDate.now())) {
            throw new AdvertisementException(AdvertisementErrorCode.INVALID_DATE_RANGE);
        }
        // 시작일보다 종료일이 더 빠른 경우
        if (startDate.isAfter(endDate)) {
            throw new AdvertisementException(AdvertisementErrorCode.INVALID_DATE_RANGE);
        }

        // 2. 조직 존재 여부 확인
        if (!orgRepository.existsById(orgId)) {
            throw new AdvertisementException(OrgErrorCode.ORG_NOT_FOUND);
        }

        // 3. 요청 멤버의 해당 조직에 대한 접근 권한 체크
        boolean isMember = orgMemberRepository.existsByUserIdAndOrganizationId(userId, orgId);
        if (!isMember) {
            throw new AdvertisementException(ProjectErrorCode.ACCESS_FORBIDDEN);
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
                        proj -> calculateRoas(proj.getTotalRevenue(), proj.getTotalSpend())));

        // 8. 현재 기간 결과를 ROAS 기준 내림차순 정렬
        List<RoasProjection> sorted = current.stream()
                .sorted(Comparator.comparingDouble(
                        (RoasProjection p) -> calculateRoas(p.getTotalRevenue(), p.getTotalSpend())).reversed())
                .toList();

        // 9. DTO 변환 + 순위(rank) + diffRate 계산
        List<DashboardResponse.RankingROAS> rankings = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {

            // i번째(순위) RoasProjection 값
            RoasProjection proj = sorted.get(i);

            // 해당 RoasProjection에 대한 값들
            Double currentRoas = calculateRoas(proj.getTotalRevenue(), proj.getTotalSpend()); // 현재 roas
            Double prevRoas = prevRoasMap.get(proj.getProvider()); // 이전 roas
            Integer diffRate = computeDiffRate(currentRoas, prevRoas); // 변화율 계산
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




    //------------------- private 계산 메서드 --------------------


    // ROAS = (revenue / spend) * 100
    // spend가 null 또는 0이면 0.0 반환 (Division by Zero 방어)
    private double calculateRoas(BigDecimal revenue, BigDecimal spend) {
        // spend가 null이거나 0인 경우 -> 0.0 반환
        if (spend == null || spend.compareTo(BigDecimal.ZERO) == 0)
            return 0.0;

        // 매출이 없는 경우 -> 0.0 반환
        if (revenue == null)
            return 0.0;

        // ROAS 계산 후 반환
        return revenue.divide(spend, 4, RoundingMode.HALF_UP) // 소수점 4째자리 까지 구하고 반올림
                .multiply(BigDecimal.valueOf(100)) // * 100
                .doubleValue(); // double로 형변환
    }

    // 변화율 = ((current - prev) / prev) * 100 (반올림 정수)
    // prevRoas가 null이거나 0이면 비교 불가 -> null 반환
    private Integer computeDiffRate(double currentRoas, Double prevRoas) {
        // 이전 데이터가 없거나 0인 경우 null 반환
        if (prevRoas == null || prevRoas == 0.0)
            return null;

        // 변화율 계산 후 반환
        double rate = ((currentRoas - prevRoas) / prevRoas) * 100.0;
        return (int) Math.round(rate);
    }
}
