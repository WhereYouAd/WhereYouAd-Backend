package com.whereyouad.WhereYouAd.domains.organization.domain.service;

import com.whereyouad.WhereYouAd.domains.organization.persistence.repository.OrgInvitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrgInvitationScheduler {

    private final OrgInvitationRepository orgInvitationRepository;

    // 02:00에 만료 기한이 지난 초대 내역 삭제
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredInvitations() {
        log.info("만료된 조직 초대 내역 삭제 스케줄러 실행");
        // 현재 시간을 기준으로 만료된 데이터를 모두 삭제
        orgInvitationRepository.deleteByExpireAtBefore(LocalDateTime.now());
        log.info("만료된 조직 초대 데이터 정리");
    }
}