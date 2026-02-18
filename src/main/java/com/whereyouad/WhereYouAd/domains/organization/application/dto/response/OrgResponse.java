package com.whereyouad.WhereYouAd.domains.organization.application.dto.response;

import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgRole;

import java.time.LocalDateTime;
import java.util.List;

public class OrgResponse {

    public record Create(
            Long orgId,
            LocalDateTime createdAt
    ) {}

    //내 조직 정보 조회를 위한 간략화된 조직 정보 DTO
    public record SimpleInfo(
            Long orgId,
            String name,
            String description,
            String logoUrl,
            OrgRole myRole
    ) {}

    //내 조직 정보는 SimpleInfo 를 List 로 반환하고, 페이징 메타데이터를 함께 반환
    //MyOrganization DTO 내부에 SimpleInfo DTO 가 여러개 포함
    public record MyOrganizations(
            List<SimpleInfo> organizations
    ) {}

    //하나의 조직 세부 정보 반환 DTO
    //OrgDetail DTO 내부에 OrgMembers DTO 가 여러개 포함
    public record OrgDetail (
            Long orgId,
            String name,
            String description,
            String logoUrl,
            LocalDateTime createdAt
    ) {}

    public record Update (
            Long orgId,
            String name,
            String description,
            String logoUrl,
            LocalDateTime updatedAt
    ) {}

    //Soft Delete 복구 시 응답값
    public record Delete (
            Long orgId,
            String message
    ) {}


}
