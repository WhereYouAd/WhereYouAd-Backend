package com.whereyouad.WhereYouAd.domains.project.persistence.repository;

import com.whereyouad.WhereYouAd.domains.project.persistence.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    @Query("select p from Project p where p.organization.id = :orgId")
    List<Project> findByOrganizationId(@Param(value = "orgId") Long orgId);
}
