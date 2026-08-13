package com.whereyouad.WhereYouAd.domains.notification.persistence.repository;

import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.UserNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    // 알림 기록 커서 조회 (안 읽은 알림 우선, 각 그룹 내에서는 최신순)
    @Query("SELECT un FROM UserNotification un " +
            "JOIN FETCH un.notification n " +
            "WHERE un.user.id = :userId " +
            "AND n.organization.id = :orgId " +
            "AND (:cursorId IS NULL " +
            "     OR (:cursorIsRead = false AND un.isRead = true) " +
            "     OR (un.isRead = :cursorIsRead " +
            "         AND (n.createdAt < :cursorCreatedAt " +
            "              OR (n.createdAt = :cursorCreatedAt AND un.id < :cursorId)))) " +
            "ORDER BY un.isRead ASC, n.createdAt DESC, un.id DESC")
    Slice<UserNotification> findHistoryWithCursor(
            @Param("userId") Long userId,
            @Param("orgId") Long orgId,
            @Param("cursorIsRead") Boolean cursorIsRead,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);


    // 커서가 가리키는 행 조회 (정렬 기준값 isRead / createdAt 확보용, 본인 소유만)
    @Query("SELECT un FROM UserNotification un " +
            "JOIN FETCH un.notification n " +
            "WHERE un.id = :cursorId " +
            "AND un.user.id = :userId " +
            "AND n.organization.id = :orgId")
    Optional<UserNotification> findCursorAnchor(@Param("cursorId") Long cursorId,
                                                @Param("userId") Long userId,
                                                @Param("orgId") Long orgId
                                                );

    // 조직 내 회원의 안 읽은 알림 일괄 읽음 처리
    // 벌크 UPDATE는 조인을 쓸 수 없어 조직 조건만 서브쿼리로 분리
    @Modifying
    @Query("UPDATE UserNotification un " +
            "SET un.isRead = true, un.readAt = :now " +
            "WHERE un.user.id = :userId " +
            "AND un.isRead = false " +
            "AND un.notification.id IN (SELECT n.id FROM Notification n WHERE n.organization.id = :orgId)")
    int markAllAsRead(@Param("userId") Long userId,
                      @Param("orgId") Long orgId,
                      @Param("now") LocalDateTime now);

}
