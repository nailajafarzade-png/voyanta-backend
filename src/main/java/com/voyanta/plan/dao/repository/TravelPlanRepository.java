package com.voyanta.plan.dao.repository;


import com.voyanta.plan.dao.entity.TravelPlan;
import com.voyanta.plan.enums.PlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TravelPlanRepository extends JpaRepository<TravelPlan, UUID> {
    Optional<TravelPlan> findByAnonymousSessionId(String anonymousSessionId);
    long countByStatus(PlanStatus status);
    List<TravelPlan> findByUserId(UUID userId);
}