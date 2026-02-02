package com.whereyouad.WhereYouAd.domains.organization.persistence.entity;

import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgStatus;
import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "organization")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Organization extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "org_id")
    private Long id;

    @Column(name = "name", length = 120, nullable = false)
    private String name; //조직 이름

    @Column(name = "description", length = 1000)
    private String description; //조직 설명

    @Column(name = "logo_url", length = 1024)
    private String logoUrl; //로고 이미지 URL

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId; //생성자 ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "status")
    @ColumnDefault("'ACTIVE'") //기본값 ACTIVE
    private OrgStatus status; //ACTIVE, SUSPENDED, DELETED
}
