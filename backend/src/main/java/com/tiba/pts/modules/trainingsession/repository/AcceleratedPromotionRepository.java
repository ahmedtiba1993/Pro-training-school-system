package com.tiba.pts.modules.trainingsession.repository;

import com.tiba.pts.modules.trainingsession.domain.entity.AcceleratedPromotion;
import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcceleratedPromotionRepository extends JpaRepository<AcceleratedPromotion, Long> {

  // To retrieve with a limit
  Page<AcceleratedPromotion> findByStatus(PromotionStatus status, Pageable pageable);

  // To retrieve all of a status
  List<AcceleratedPromotion> findByStatus(PromotionStatus status, Sort sort);
}
