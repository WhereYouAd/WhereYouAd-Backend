package com.whereyouad.WhereYouAd.domains.project.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.domains.project.persistence.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    @Query("select p from Project p where p.organization.id = :orgId")
    List<Project> findByOrganizationId(@Param(value = "orgId") Long orgId);

    @Query("select p from Project p where p.id = :projectId and p.organization.id = :orgId")
    Optional<Project> findByIdAndOrganizationId(@Param("projectId") Long projectId, @Param("orgId") Long orgId);

    @Modifying
    @Query("UPDATE Project p SET p.status = :status WHERE p.id = :projectId")
    void updateStatusById(@Param("projectId") Long projectId, @Param("status") Status status);

    @Modifying
    @Query("UPDATE Project p SET p.status = :status WHERE p.organization.id = :orgId")
    void updateStatusByOrganizationId(@Param("orgId") Long orgId, @Param("status") Status status);

    // 조직 Hard Delete 시 소속 Project 엔티티 일괄 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Project p WHERE p.organization.id = :orgId")
    void deleteByOrganizationId(@Param("orgId") Long orgId);
}
