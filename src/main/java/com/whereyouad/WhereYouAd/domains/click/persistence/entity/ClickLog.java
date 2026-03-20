package com.whereyouad.WhereYouAd.domains.click.persistence.entity;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.click.domain.constant.DeviceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "click_log")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class ClickLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "click_log_id")
    private Long id;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "device", length = 512)
    private DeviceType device;

    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt;

    @Column(name = "is_suspect", nullable = false)
    private Boolean isSuspect;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_content_id", nullable = false)
    private AdContent adContent;
}
