package com.whereyouad.WhereYouAd.domains.organization.persistence.entity;

import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgRole;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "org_member")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class OrgMember { //중간 테이블이므로 BaseEntity 미적용

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "membership_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "role")
    private OrgRole role; //역할은 기본값 미적용

    @Column(name = "joined_at")
    private LocalDateTime joinedAt; //가입시간

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id")
    private Organization organization;
}
