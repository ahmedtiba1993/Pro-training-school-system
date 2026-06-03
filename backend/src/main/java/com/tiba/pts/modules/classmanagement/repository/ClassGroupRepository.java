package com.tiba.pts.modules.classmanagement.repository;

import com.tiba.pts.modules.classmanagement.domain.entity.ClassGroup;
import com.tiba.pts.modules.classmanagement.domain.enums.ClassStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassGroupRepository extends JpaRepository<ClassGroup, Long> {

  // Strict uniqueness check of the couple [promotionId + name]
  boolean existsByPromotionIdAndNameIgnoreCase(Long promotionId, String name);

  boolean existsByCode(String code);

  /**
   * * Search as a list with optional cumulative filters. Default alphabetical sorting integrated
   * directly into the query. Deep join via EntityGraph to eliminate the N+1 problem.
   */
  @EntityGraph(
      attributePaths = {
        "assignments",
        "promotion",
        "promotion.training",
        "promotion.training.level",
        "promotion.training.specialty"
      })
  @Query(
      "SELECT cg FROM ClassGroup cg "
          + "WHERE (:trainingId IS NULL OR cg.promotion.training.id = :trainingId) "
          + "AND (:levelId IS NULL OR cg.promotion.training.level.id = :levelId) "
          + "AND (:status IS NULL OR cg.status = :status) "
          + "ORDER BY cg.name ASC")
  List<ClassGroup> findAllFiltered(
      @Param("trainingId") Long trainingId,
      @Param("levelId") Long levelId,
      @Param("status") ClassStatus status);

  long countByStatus(ClassStatus status);

  @EntityGraph(
      attributePaths = {
        "promotion",
        "promotion.training",
        "promotion.training.level",
        "promotion.training.specialty"
      })
  Optional<ClassGroup> findById(Long id);

  @Query(
      "SELECT c FROM ClassGroup c "
          + "JOIN FETCH c.promotion p "
          + "JOIN FETCH p.training t "
          + "WHERE c.status = 'ACTIVE'")
  List<ClassGroup> findAllActiveClassesWithTraining();

  @EntityGraph(
      attributePaths = {
        "promotion",
        "promotion.training",
        "promotion.training.level",
        "promotion.training.specialty"
      })
  @Query("SELECT cg FROM ClassGroup cg WHERE cg.id IN :ids")
  List<ClassGroup> findAllByIdsWithPromotionAndTraining(@Param("ids") java.util.Collection<Long> ids);
}
