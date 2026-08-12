package com.whereyouad.WhereYouAd.domains.project.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.BudgetType;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.AdvertisementHandler;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.code.AdvertisementErrorCode;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdCampaignRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdGroupRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdContentRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgStatus;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.handler.OrgHandler;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.domains.platform.persistence.repository.PlatformConnectionRepository;
import com.whereyouad.WhereYouAd.domains.project.application.dto.ProjectQueryDto;
import com.whereyouad.WhereYouAd.domains.project.application.dto.request.ProjectRequest;
import com.whereyouad.WhereYouAd.domains.project.application.dto.response.ProjectResponse;
import com.whereyouad.WhereYouAd.domains.project.application.mapper.ProjectConverter;
import com.whereyouad.WhereYouAd.domains.project.exception.ProjectHandler;
import com.whereyouad.WhereYouAd.domains.project.exception.code.ProjectErrorCode;
import com.whereyouad.WhereYouAd.domains.project.persistence.entity.Project;
import com.whereyouad.WhereYouAd.domains.project.persistence.repository.ProjectRepository;
import com.whereyouad.WhereYouAd.domains.user.exception.code.UserErrorCode;
import com.whereyouad.WhereYouAd.domains.user.exception.handler.UserHandler;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.UserRepository;
import com.whereyouad.WhereYouAd.global.utils.BudgetCalculator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AdCampaignRepository adCampaignRepository;
    private final AdGroupRepository adGroupRepository;
    private final AdContentRepository adContentRepository;
    private final OrgRepository orgRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final MetricFactRepository metricFactRepository;
    private final PlatformConnectionRepository platformConnectionRepository;
    private final BudgetCalculator budgetCalculator;

    @Override
    public ProjectResponse.CreatedResponse createProject(Long userId, Long orgId,ProjectRequest.CreateRequest request) {
        //회원 검증
        userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

        //조직 검증 및 엔티티 추출
        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        //조직 Soft Delete 되어있을 시 예외 발생
        if (organization.getStatus() == OrgStatus.DELETED) {
            throw new OrgHandler(OrgErrorCode.ORG_SOFT_DELETED);
        }

        //해당 회원이 조직에 속해있는지 검증
        Optional<OrgMember> orgMember = orgMemberRepository.findByUserIdAndOrgId(userId, organization.getId());
        if (orgMember.isEmpty()) {
            throw new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND);
        }

        //AdCampaign Id 들 기반 AdCampaign 엔티티 추출
        List<Long> campaignIds = request.campaignIds();

        List<AdCampaign> adCampaigns = new ArrayList<>();

        for (Long campaignId : campaignIds) {
            if (campaignId != null) {
                AdCampaign adCampaign = adCampaignRepository.findById(campaignId)
                        .orElseThrow(() -> new AdvertisementHandler(AdvertisementErrorCode.ADCAMPAIGN_NOT_FOUND));

                //만약 해당 AdCampaign 이 연관된 Project 존재 시, 예외 발생
                if (adCampaign.getProject() != null) {
                    throw new AdvertisementHandler(AdvertisementErrorCode.ADCAMPAIGN_ALREADY_RELATED);
                } else { //연관된 Project 없으면 추가 진행
                    adCampaigns.add(adCampaign);
                }
            }
        }

        //Project 엔티티 생성 및 저장
        Project project = Project.builder()
                .name(request.name())
                .status(Status.ON_GOING)
                .description(request.description())
                .createdBy(userId)
                .organization(organization)
                .adCampaigns(adCampaigns)
                .build();

        projectRepository.save(project);

        for (AdCampaign adCampaign : adCampaigns) {
            adCampaign.relateProject(project);
        }

        return ProjectConverter.toCreatedResponse(project);
    }

    //조직 내 모든 Project 조회 로직
    @Override
    public ProjectResponse.ProjectListResponse getProjects(Long userId, Long orgId) {
        validateProject(userId, orgId);

        // 조직 내 모든 Project 조회
        List<Project> projects = projectRepository.findByOrganizationId(orgId);

        //조직 내 Project 존재하지 않을 시 빈 리스트로 응답값 반환
        if (projects.isEmpty()) {
            return ProjectConverter.toProjectListResponse(Collections.emptyList());
        }

        // Project ID 리스트 추출
        List<Long> projectIds = projects.stream()
                .map(Project::getId)
                .toList();

        // 캠페인 요약 정보 (Provider, Budget) 조회 및 Map으로 그룹화
        List<ProjectQueryDto.CampaignSummary> campaignSummaries =
                adCampaignRepository.findCampaignSummariesByProjectIds(projectIds);

        // projectId -> 해당 프로젝트의 모든 캠페인 요약 정보 리스트
        Map<Long, List<ProjectQueryDto.CampaignSummary>> campaignMap = campaignSummaries.stream()
                .collect(Collectors.groupingBy(ProjectQueryDto.CampaignSummary::projectId));

        // 지출 요약 정보 (총 Spend) 조회 및 Map으로 매핑
        List<ProjectQueryDto.SpendSummary> spendSummaries =
                metricFactRepository.findSpendSummariesByProjectIds(projectIds);

        // projectId -> 총 지출액(totalSpend)
        Map<Long, BigDecimal> spendMap = spendSummaries.stream()
                .collect(Collectors.toMap(
                        ProjectQueryDto.SpendSummary::projectId,
                        ProjectQueryDto.SpendSummary::totalSpend
                ));

        // 최종 응답 DTO 조합
        List<ProjectResponse.SimpleProjectResponse> responseList = projects.stream().map(project -> {
            Long projectId = project.getId();
            List<ProjectQueryDto.CampaignSummary> projectCampaigns = campaignMap.getOrDefault(projectId, Collections.emptyList());

            // Provider 추출
            List<Provider> distinctProviders = extractDistinctProviders(projectCampaigns);

            // 총 지출 (MetricFact spend의 합)
            BigDecimal totalSpend = spendMap.getOrDefault(projectId, BigDecimal.ZERO);

            // 예산 소진 현황 계산 메서드 호출
            double budgetUsageRate = budgetCalculator.calculateBudgetUsageRate(projectCampaigns, totalSpend);

            return ProjectConverter.toSimpleProjectResponse(project, distinctProviders, budgetUsageRate);

        }).toList();

        return ProjectConverter.toProjectListResponse(responseList);
    }

    // 개별 프로젝트 조회
    @Override
    public ProjectResponse.ProjectInfoResponse getProject(Long userId, Long orgId, Long projectId) {
        validateProject(userId, orgId);

        Project project = projectRepository.findByIdAndOrganizationId(projectId, orgId)
                .orElseThrow(() -> new ProjectHandler(ProjectErrorCode.PROJECT_NOT_FOUND));

        // 캠페인 정보 모음
        List<ProjectQueryDto.CampaignSummary> campaignSummaries = adCampaignRepository.findCampaignSummariesByProjectId(project.getId());

        // Provider 추출
        List<Provider> distinctProviders = extractDistinctProviders(campaignSummaries);

        // 총 예산 (캠페인 budget의 합)
        long totalBudget = budgetCalculator.calculateTotalBudget(campaignSummaries);

        // 플랫폼별 남은 예산 (전체(구글/메타)/일일(네이버) 특성에 따라 합산)
        List<ProjectResponse.PlatformBudgetSummary> platformBudgets = buildPlatformBudgetSummaries(userId, project.getId(), distinctProviders);

        return ProjectConverter.toProjectInfoResponse(project, distinctProviders, totalBudget, platformBudgets);
    }

    // 프로젝트(캠페인) 내 캠페인들을 provider 기준으로 묶어, 각 플랫폼이 실제로 사용하는 예산 특성(TOTAL/DAILY)에 맞춰 남은 예산을 계산
    // - TOTAL 타입 캠페인이 하나라도 있으면(구글/메타) TOTAL 타입 캠페인들의 예산 합계 - 누적 지출
    // - TOTAL 타입 캠페인이 없으면(네이버) DAILY 타입 캠페인들의 예산 합계 - 오늘 지출
    private List<ProjectResponse.PlatformBudgetSummary> buildPlatformBudgetSummaries(Long userId, Long projectId, List<Provider> providers) {
        List<ProjectQueryDto.CampaignBudgetInfo> campaignBudgetInfos = adCampaignRepository.findCampaignBudgetInfoByProjectId(projectId);
        Map<Provider, List<ProjectQueryDto.CampaignBudgetInfo>> byProvider = campaignBudgetInfos.stream()
                .collect(Collectors.groupingBy(ProjectQueryDto.CampaignBudgetInfo::provider));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        // provider 루프 안에서 매번 쿼리하지 않도록, TOTAL/DAILY 지출을 provider별로 배치 조회 (N+1 방지)
        Map<Provider, BigDecimal> totalSpendByProvider = metricFactRepository
                .sumSpendsByProjectIdAndBudgetTypeGroupByProvider(projectId, BudgetType.TOTAL).stream()
                .collect(Collectors.toMap(ProjectQueryDto.ProviderSpend::provider, ProjectQueryDto.ProviderSpend::totalSpend));
        Map<Provider, BigDecimal> dailySpendByProvider = metricFactRepository
                .sumSpendsByProjectIdAndBudgetTypeAndPeriodGroupByProvider(projectId, BudgetType.DAILY, todayStart, now).stream()
                .collect(Collectors.toMap(ProjectQueryDto.ProviderSpend::provider, ProjectQueryDto.ProviderSpend::totalSpend));

        List<ProjectResponse.PlatformBudgetSummary> result = new ArrayList<>();
        for (Provider provider : providers) {
            List<ProjectQueryDto.CampaignBudgetInfo> infos = byProvider.getOrDefault(provider, Collections.emptyList());

            boolean hasTotalType = infos.stream().anyMatch(info -> info.budgetType() == BudgetType.TOTAL);
            BudgetType characteristic = hasTotalType ? BudgetType.TOTAL : BudgetType.DAILY;

            List<ProjectQueryDto.CampaignBudgetInfo> matchingInfos = infos.stream()
                    .filter(info -> info.budgetType() == characteristic)
                    .toList();

            long budget = matchingInfos.stream()
                    .mapToLong(info -> info.budget() != null ? info.budget() : 0L)
                    .sum();

            BigDecimal spendDec = (characteristic == BudgetType.TOTAL)
                    ? totalSpendByProvider.get(provider)
                    : dailySpendByProvider.get(provider);

            long spend = (spendDec != null) ? spendDec.longValue() : 0L;
            long remaining = budgetCalculator.calculateRemainingBudget(budget, spend);
            double remainingPercentage = budgetCalculator.calculateRemainingRate(budget, remaining);

            // 예산 수정 요청용 캠페인 식별자
            // - 매칭되는 캠페인이 정확히 1개일 때만 값을 채움 (2개 이상이면 어느 캠페인을 가리키는지 모호하므로 null)
            // - 구글/메타: 내부 PK(adCampaignId)를 그대로 사용
            // - 네이버: 예산 수정 API가 내부 PK가 아닌 connectionId + 외부 캠페인ID를 요구하므로 별도로 조립
            ProjectQueryDto.CampaignBudgetInfo target = (matchingInfos.size() == 1) ? matchingInfos.get(0) : null;

            Long adCampaignId = (target != null && provider != Provider.NAVER) ? target.campaignId() : null;
            ProjectResponse.NaverBudgetTarget naverBudgetTarget = (target != null && provider == Provider.NAVER)
                    ? buildNaverBudgetTarget(userId, target)
                    : null;

            result.add(new ProjectResponse.PlatformBudgetSummary(
                    provider, characteristic, adCampaignId, naverBudgetTarget, budget, spend, remaining, remainingPercentage));
        }
        return result;
    }

    // 네이버 캠페인 예산 수정 요청(PUT /api/naver/{connectionId}/campaigns/{campaignId}/budget) 조립용 식별자 조회
    // externalCampaignId/platformAccountId가 없거나, 해당 유저의 platformAccount 연동 정보가 없으면 null 반환
    private ProjectResponse.NaverBudgetTarget buildNaverBudgetTarget(Long userId, ProjectQueryDto.CampaignBudgetInfo info) {
        if (info.externalCampaignId() == null || info.platformAccountId() == null) {
            return null;
        }
        return platformConnectionRepository.findByUserIdAndPlatformAccountId(userId, info.platformAccountId())
                .map(PlatformConnection::getId)
                .map(connectionId -> new ProjectResponse.NaverBudgetTarget(connectionId, info.externalCampaignId()))
                .orElse(null);
    }

    // Provider 추출 공통 메서드
    private List<Provider> extractDistinctProviders(List<ProjectQueryDto.CampaignSummary> campaignSummaries) {
        return campaignSummaries.stream()
                .map(ProjectQueryDto.CampaignSummary::provider)
                .distinct()
                .sorted(Comparator.comparing(Provider::name))
                .toList();
    }



    @Override
    public void updateAllProjectsStatus(Long userId, Long orgId, Status status) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        if (organization.getStatus() == OrgStatus.DELETED) {
            throw new OrgHandler(OrgErrorCode.ORG_SOFT_DELETED);
        }

        Optional<OrgMember> orgMember = orgMemberRepository.findByUserIdAndOrgId(userId, orgId);
        if (orgMember.isEmpty()) {
            throw new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND);
        }

        adCampaignRepository.updateStatusByOrganizationId(orgId, status);
        adGroupRepository.updateStatusByOrganizationId(orgId, status);
        adContentRepository.updateStatusByOrganizationId(orgId, status);

        projectRepository.updateStatusByOrganizationId(orgId, status);
    }

    public void validateProject (Long userId, Long orgId) {
        //회원 Not Found 예외처리
        userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

        //조직 Not Found 예외처리
        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        //조직 Soft Delete 상태시 예외
        if (organization.getStatus() == OrgStatus.DELETED) {
            throw new OrgHandler(OrgErrorCode.ORG_SOFT_DELETED);
        }

        Optional<OrgMember> orgMember = orgMemberRepository.findByUserIdAndOrgId(userId, orgId);

        //조직에 속하지 않은 회원의 요청일 시 예외
        if (orgMember.isEmpty()) {
            throw new OrgHandler(OrgErrorCode.ORG_MEMBER_NOT_FOUND);
        }
    }
}
