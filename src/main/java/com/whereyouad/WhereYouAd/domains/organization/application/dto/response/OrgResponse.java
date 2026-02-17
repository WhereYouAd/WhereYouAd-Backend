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

    // 조직 멤버 조회 응답 (무한 스크롤 - Slice 기반)
    public record OrgMemberSliceDTO(
            boolean hasNext,
            String nextCursor,
            List<OrgMemberDTO> members
    ) {}

    public record OrgMemberDTO(
            String name,
            String email,
            String profileImageUrl,
            String role
    ) {}
}
