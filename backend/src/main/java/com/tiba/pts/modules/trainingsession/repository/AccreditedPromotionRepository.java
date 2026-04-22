package com.tiba.pts.modules.trainingsession.repository;

import com.tiba.pts.modules.trainingsession.domain.entity.AccreditedPromotion;
import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccreditedPromotionRepository extends JpaRepository<AccreditedPromotion, Long> {
  // To retrieve with a limit
  Page<AccreditedPromotion> findByStatus(PromotionStatus status, Pageable pageable);

  // To retrieve all of a status
  List<AccreditedPromotion> findByStatus(PromotionStatus status, Sort sort);
}
