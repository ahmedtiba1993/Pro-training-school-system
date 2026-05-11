package com.tiba.pts.modules.trainingsession.repository;

import com.tiba.pts.modules.trainingsession.domain.entity.ContinuousPromotion;
import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContinuousPromotionRepository extends JpaRepository<ContinuousPromotion, Long> {
  Page<ContinuousPromotion> findByStatus(PromotionStatus status, Pageable pageable);

  List<ContinuousPromotion> findByStatus(PromotionStatus status, Sort sort);

  long countByStatus(PromotionStatus status);

  boolean existsByNameIgnoreCase(String name);
}
