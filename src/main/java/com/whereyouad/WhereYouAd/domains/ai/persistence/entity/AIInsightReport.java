package com.whereyouad.WhereYouAd.domains.ai.persistence.entity;

import com.whereyouad.WhereYouAd.domains.ai.domain.constant.AIStatus;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "ai_insight_report")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AIInsightReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_insight_report_id")
    private Long id;

    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;


    @Column(name = "access_token", unique = true, nullable = false)
    private String accessToken;

    @Builder.Default
    @Column(name = "is_shared", nullable = false)
    private boolean isShared = false;

    @Column(name = "payload_json", columnDefinition = "JSON")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AIStatus status;

    // 연관 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id")
    private Organization organization;

    // 상태 변경 메서드
    public void updateIsShared(boolean isShared) {
        this.isShared = isShared;
    }

    // ai 응답 성공 시 status 변경, 결과 저장
    public void updateSuccess(String payloadJson) {
        this.status = AIStatus.SUCCESS;
        this.payloadJson = payloadJson;
    }

    // ai 응답 실패 시 status 변경
    public void updateFailed() {
        this.status = AIStatus.FAILED;
    }
}
