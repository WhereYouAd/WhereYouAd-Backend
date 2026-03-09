package com.whereyouad.WhereYouAd.domains.project.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.AdvertisementHandler;
import com.whereyouad.WhereYouAd.domains.advertisement.exception.code.AdvertisementErrorCode;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.AdCampaignRepository;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.MetricFactRepository;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgStatus;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.handler.OrgHandler;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.domains.project.application.dto.ProjectQueryDto;
import com.whereyouad.WhereYouAd.domains.project.application.dto.request.ProjectRequest;
import com.whereyouad.WhereYouAd.domains.project.application.dto.response.ProjectResponse;
import com.whereyouad.WhereYouAd.domains.project.application.mapper.ProjectConverter;
import com.whereyouad.WhereYouAd.domains.project.persistence.entity.Project;
import com.whereyouad.WhereYouAd.domains.project.persistence.repository.ProjectRepository;
import com.whereyouad.WhereYouAd.domains.user.exception.code.UserErrorCode;
import com.whereyouad.WhereYouAd.domains.user.exception.handler.UserHandler;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectServiceImpl implements ProjectService{

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AdCampaignRepository adCampaignRepository;
    private final OrgRepository orgRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final MetricFactRepository metricFactRepository;

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
    public ProjectResponse.ProjectListResponse getProjects(Long userId, Long orgId) {

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
            List<Provider> distinctProviders = projectCampaigns.stream()
                    .map(ProjectQueryDto.CampaignSummary::provider)
                    .distinct()
                    .sorted(Comparator.comparing(Provider::name))
                    .toList();

            // 총 지출 (MetricFact spend의 합)
            BigDecimal totalSpend = spendMap.getOrDefault(projectId, BigDecimal.ZERO);

            // 예산 소진 현황 계산 메서드 호출
            double budgetUsageRate = calculateBudgetUsageRate(projectCampaigns, totalSpend);

            return ProjectConverter.toSimpleProjectResponse(project, distinctProviders, budgetUsageRate);

        }).toList();

        return ProjectConverter.toProjectListResponse(responseList);
    }

    //예산 소진 현황 계산 메서드
    private double calculateBudgetUsageRate(List<ProjectQueryDto.CampaignSummary> projectCampaigns, BigDecimal totalSpend) {
        // 총 예산 (캠페인 budget의 합)
        long totalBudget = projectCampaigns.stream()
                .mapToLong(summary -> summary.budget() != null ? summary.budget() : 0L)
                .sum();

        // 예산이 없거나 지출이 없으면 0.0 반환
        if (totalBudget <= 0 || totalSpend == null) {
            return 0.0;
        }

        // 소진율 계산 (비용 / 예산 * 100), 소수점 첫째 자리 이후로는 버림 처리
        return totalSpend
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalBudget), 1, RoundingMode.DOWN)
                .doubleValue();
    }
}
