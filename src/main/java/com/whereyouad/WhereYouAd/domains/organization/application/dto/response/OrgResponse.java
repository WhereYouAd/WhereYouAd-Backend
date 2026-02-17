package com.whereyouad.WhereYouAd.domains.organization.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class OrgResponse {

    public record Create(
            Long orgId,
            LocalDateTime createdAt
    ) {}

    public record Read (
        //TODO
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

    // 조직 멤버 조회 응답 (무한 스크롤 - Slice 기반)
    public record OrgMemberSliceDTO(
            boolean hasNext,
            String nextCursor,
            List<OrgMemberDTO> members
    ) {}

    // 조직 멤버 전체 수 조회 응답
    public record OrgMemberCountDTO(
            int totalCount
    ) {}

    public record OrgMemberDTO(
            String name,
            String email,
            String profileImageUrl,
            String role
    ) {}
}