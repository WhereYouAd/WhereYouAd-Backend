package com.whereyouad.WhereYouAd.domains.notification.persistence.repository;

import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    Optional<PushSubscription> findByOrgMember_IdAndEndpoint(Long membershipId, String endpoint);

    List<PushSubscription> findAllByOrgMember_IdIn(Collection<Long> membershipIds);

    void deleteByOrgMember_IdAndEndpoint(Long membershipId, String endpoint);

    // 조직 내 멤버들에 매핑된 구독 일괄 조회 (발송 대상 로딩용)
    @Query("SELECT ps FROM PushSubscription ps " +
            "WHERE ps.orgMember.id IN :membershipIds")
    List<PushSubscription> findAllByMembershipIds(@Param("membershipIds") Collection<Long> membershipIds);
}
