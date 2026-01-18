package com.whereyouad.WhereYouAd.domain.example.persistence.repository;

import com.whereyouad.WhereYouAd.domain.example.persistence.entity.ExampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExampleRepository extends JpaRepository<ExampleEntity, Long> {
}
