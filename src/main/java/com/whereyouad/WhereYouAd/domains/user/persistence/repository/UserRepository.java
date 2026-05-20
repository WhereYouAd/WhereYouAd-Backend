package com.whereyouad.WhereYouAd.domains.user.persistence.repository;

import com.whereyouad.WhereYouAd.domains.user.domain.constant.UserStatus;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("select u from User u where u.email = :email")
    Optional<User> findUserByEmail(@Param("email") String email);

    @Query("select u from User u where u.phoneNumber = :phoneNumber")
    Optional<User> findUserByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    // 이메일 중복 예외 처리를 위한 이메일로 조회 메서드
    boolean existsByEmail(String email);

    // Soft Delete 후 일정 기간이 지난 회원 조회 (Hard Delete 대상)
    @Query("SELECT u FROM User u WHERE u.status = :status AND u.deletedAt <= :threshold")
    List<User> findAllByStatusAndDeletedAtBefore(@Param("status") UserStatus status,
                                                 @Param("threshold") LocalDate threshold);
}
