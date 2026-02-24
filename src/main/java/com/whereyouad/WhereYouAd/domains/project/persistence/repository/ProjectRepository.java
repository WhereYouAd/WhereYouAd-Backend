package com.whereyouad.WhereYouAd.domains.project.persistence.repository;

import com.whereyouad.WhereYouAd.domains.project.persistence.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
