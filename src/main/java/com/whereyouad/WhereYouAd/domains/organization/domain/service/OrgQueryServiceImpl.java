package com.whereyouad.WhereYouAd.domains.organization.domain.service;

import com.whereyouad.WhereYouAd.domains.organization.application.dto.response.OrgResponse;
import com.whereyouad.WhereYouAd.domains.organization.application.mapper.OrgConverter;
import com.whereyouad.WhereYouAd.domains.organization.exception.code.OrgErrorCode;
import com.whereyouad.WhereYouAd.domains.organization.exception.handler.OrgHandler;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgMemberRepository;
import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgRepository;
import com.whereyouad.WhereYouAd.domains.user.domain.constant.UserStatus;
import com.whereyouad.WhereYouAd.global.utils.cursor.CursorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrgQueryServiceImpl implements OrgQueryService{

    private final OrgRepository orgRepository;
    private final OrgMemberRepository orgMemberRepository;

    // 조직 멤버 조회 (커서 기반 무한 스크롤 - Slice + CursorUtil)
    @Override
    public OrgResponse.OrgMemberSliceDTO getOrgMembers(Long orgId, String encodedCursor, Integer size) {

        // 조직 존재 여부 확인
        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 페이지 크기 기본값 설정 (기본 20개)
        int pageSize = (size != null && size > 0) ? size : 20;

        // 커서 디코딩 (null이면 첫 페이지)
        Long cursor = null;
        if (encodedCursor != null && !encodedCursor.isBlank()) {
            cursor = CursorUtil.decodeToId(encodedCursor);
        }

        // Slice 조회 (자동으로 hasNext 계산)
        Slice<OrgMember> slice = orgMemberRepository.findByOrganizationIdWithCursor(
                orgId,
                UserStatus.ACTIVE,
                cursor,
                PageRequest.of(0, pageSize));

        // nextCursor 인코딩 (다음 페이지가 있으면)
        String nextCursor = null;
        if (slice.hasNext() && !slice.getContent().isEmpty()) {
            Long lastId = slice.getContent().get(slice.getContent().size() - 1).getId();
            nextCursor = CursorUtil.encode(lastId);
        }

        return OrgConverter.toOrgMemberSliceDTO(slice.hasNext(), nextCursor, slice.getContent());
    }

    // 조직 전체 멤버 수 조회
    @Override
    public OrgResponse.OrgMemberCountDTO getOrgMembersCount(Long orgId) {
        // 조직 존재 여부 확인
        Organization organization = orgRepository.findById(orgId)
                .orElseThrow(() -> new OrgHandler(OrgErrorCode.ORG_NOT_FOUND));

        // 전체 멤버 수 조회
        int totalCount = orgMemberRepository.countByOrganizationIdAndUserStatus(orgId, UserStatus.ACTIVE);

        return new OrgResponse.OrgMemberCountDTO(totalCount);
    }
}
