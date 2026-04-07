package com.whereyouad.WhereYouAd.domains.organization.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "org_invitation")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class OrgInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invitation_id")
    private Long id;

    @Column(nullable = false, name = "email")
    private String email;

    @Column(name = "invited_at")
    private LocalDateTime invitedAt; // 초대 시간 및 시간 갱신용

    @Column(name = "expire_at")
    private LocalDateTime expireAt; // 만료 시간

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id")
    private Organization organization;

    public void updateInvitedAt() {
        this.invitedAt = LocalDateTime.now();
        this.expireAt = LocalDateTime.now().plusHours(24);
    }
}
