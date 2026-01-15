package com.whereyouad.WhereYouAd.domains.user.persistence.repository;

import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("select u from User u where u.email = :email")
    Optional<User> findUserByEmail(@Param("email") String email);
}
