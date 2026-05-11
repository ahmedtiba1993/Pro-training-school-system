package com.tiba.pts.modules.trainingsession.service;

import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.academicyear.domain.enums.YearStatus;
import com.tiba.pts.modules.academicyear.repository.AcademicYearRepository;
import com.tiba.pts.modules.specialty.domain.entity.Training;
import com.tiba.pts.modules.specialty.domain.enums.TrainingStatus;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import com.tiba.pts.modules.specialty.repository.TrainingRepository;
import com.tiba.pts.modules.trainingsession.domain.entity.AccreditedPromotion;
import com.tiba.pts.modules.trainingsession.domain.enums.PromotionStatus;
import com.tiba.pts.modules.trainingsession.dto.request.AccreditedPromotionRequest;
import com.tiba.pts.modules.trainingsession.dto.request.AccreditedPromotionUpdateRequest;
import com.tiba.pts.modules.trainingsession.dto.response.AccreditedPromotionResponse;
import com.tiba.pts.modules.trainingsession.dto.response.AccreditedPromotionStatsResponse;
import com.tiba.pts.modules.trainingsession.dto.response.OngoingPromotionResponse;
import com.tiba.pts.modules.trainingsession.mapper.AccreditedPromotionMapper;
import com.tiba.pts.modules.trainingsession.repository.AccreditedPromotionRepository;
import com.tiba.pts.modules.trainingsession.repository.PromotionRepository;
import com.tiba.pts.modules.trainingsession.repository.projection.PromotionStatusStatsProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccreditedPromotionService {

  private final AccreditedPromotionRepository accreditedPromotionRepository;
  private final AccreditedPromotionMapper mapper;
  private final TrainingRepository trainingRepository;
  private final AcademicYearRepository academicYearRepository;
  private final PromotionRepository promotionRepository;

  @Transactional
  public Long createAccreditedPromotion(AccreditedPromotionRequest request) {

    // Vérifier si la formation (Training) existe
    Training training =
        trainingRepository
            .findById(request.getTrainingId())
            .orElseThrow(() -> new ResourceNotFoundException("TRAINING_NOT_FOUND"));

    // Vérifier si l'année académique existe[cite: 1]
    AcademicYear academicYear =
        academicYearRepository
            .findById(request.getAcademicYearId())
            .orElseThrow(() -> new ResourceNotFoundException("ACADEMIC_YEAR_NOT_FOUND"));

    // Vérifier le statut de l'année académique
    if (academicYear.getStatus() != YearStatus.PLANNED
        && academicYear.getStatus() != YearStatus.ENROLLMENT
        && academicYear.getStatus() != YearStatus.IN_PROGRESS) {
      throw new BusinessValidationException("ACADEMIC_YEAR_MUST_BE_ACTIVE");
    }

    // Vérifier le type de la formation
    if (training.getTrainingType() != TrainingType.ACCREDITED) {
      throw new BusinessValidationException("TRAINING_MUST_BE_ACCREDITED_TYPE");
    }

    // Vérifier le statut de la formation (doit être ACTIVE)
    if (training.getStatus() != TrainingStatus.ACTIVE) {
      throw new BusinessValidationException("TRAINING_MUST_BE_ACTIVE");
    }

    // Vérifier l'unicité de la combinaison [training_id + academic_year_id]
    if (accreditedPromotionRepository.existsByTrainingIdAndAcademicYearId(
        training.getId(), academicYear.getId())) {
      throw new EntityAlreadyExistsException("ACCREDITED_PROMOTION_ALREADY_EXISTS_FOR_THIS_YEAR");
    }

    // Mapping (Utilisation de MapStruct)
    AccreditedPromotion promotion = mapper.toEntity(request);

    // Attachement des relations
    promotion.setTraining(training);
    promotion.setAcademicYear(academicYear);

    // Génération dynamique du Code
    String yearSuffix = generateYearSuffix(academicYear.getLabel());
    String generatedCode = training.getCode().toUpperCase() + "-" + yearSuffix;
    promotion.setCode(generatedCode);

    // Forcer les valeurs par défaut métier
    promotion.setStatus(PromotionStatus.DRAFT);
    promotion.setEnrollmentCount(0);

    // Sauvegarde
    AccreditedPromotion savedPromotion = accreditedPromotionRepository.save(promotion);

    return savedPromotion.getId();
  }

  @Transactional(readOnly = true)
  public PageResponse<AccreditedPromotionResponse> getAllPaged(int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());
    Page<AccreditedPromotion> pageResult = accreditedPromotionRepository.findAll(pageable);
    return PageResponse.of(pageResult, mapper::toResponse);
  }

  @Transactional(readOnly = true)
  public AccreditedPromotionResponse getById(Long id) {
    AccreditedPromotion promotion =
        accreditedPromotionRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("PROMOTION_NOT_FOUND"));
    return mapper.toResponse(promotion);
  }

  @Transactional
  public void updateAccreditedPromotion(Long id, AccreditedPromotionUpdateRequest request) {

    // Récupération de l'entité
    AccreditedPromotion promotion =
        accreditedPromotionRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("PROMOTION_NOT_FOUND"));

    // LE GARDE-FOU (La règle Read-Only)
    if (promotion.getStatus() == PromotionStatus.COMPLETED
        || promotion.getStatus() == PromotionStatus.CANCELLED) {
      throw new BusinessValidationException("CANNOT_CHANGE");
    }

    // Règle du Prix : Interdit de modifier si inscriptions en cours
    if (promotion.getEnrollmentCount() > 0
        && request.registrationFee().compareTo(promotion.getRegistrationFee()) != 0) {
      throw new BusinessValidationException(
          "CANNOT_CHANGE_REGISTRATION_FEE_WITH_ACTIVE_ENROLLMENTS");
    }
    if (promotion.getEnrollmentCount() > 0
        && request.tuitionFee().compareTo(promotion.getTuitionFee()) != 0) {
      throw new BusinessValidationException("CANNOT_CHANGE_TUITION_FEE_WITH_ACTIVE_ENROLLMENTS");
    }

    // Règle Anti-Expulsion : Capacité
    if (request.capacity() < promotion.getEnrollmentCount()) {
      throw new BusinessValidationException("CAPACITY_CANNOT_BE_LESS_THAN_ENROLLMENT_COUNT");
    }

    // Mise à jour via MapStruct (qui copiera désormais les dates d'inscription)
    mapper.updateEntity(request, promotion);
  }

  @Transactional(readOnly = true)
  public List<AccreditedPromotionResponse> getPromotionsByStatus(
      PromotionStatus status, Integer limit) {
    Sort sort = Sort.by(Sort.Direction.DESC, "startDate");
    List<AccreditedPromotion> promotions;

    if (limit != null && limit > 0) {
      Pageable pageable = PageRequest.of(0, limit, sort);
      promotions = accreditedPromotionRepository.findByStatus(status, pageable).getContent();
    } else {
      promotions = accreditedPromotionRepository.findByStatus(status, sort);
    }

    return promotions.stream().map(mapper::toResponse).toList();
  }

  @Transactional
  public void changeStatus(Long id, PromotionStatus newStatus) {
    AccreditedPromotion promotion =
        accreditedPromotionRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("PROMOTION_NOT_FOUND"));

    PromotionStatus currentStatus = promotion.getStatus();

    if (currentStatus == newStatus) {
      throw new BusinessValidationException("PROMOTION_ALREADY_IN_REQUESTED_STATUS");
    }

    switch (newStatus) {
      case ENROLLMENT:
        if (currentStatus != PromotionStatus.DRAFT) {
          throw new BusinessValidationException("INVALID_TRANSITION_TO_ENROLLMENT");
        }
        break;
      case CANCELLED:
        if (currentStatus != PromotionStatus.DRAFT && currentStatus != PromotionStatus.ENROLLMENT) {
          throw new BusinessValidationException("INVALID_TRANSITION_TO_CANCELLED");
        }
        break;
      default:
        throw new BusinessValidationException("UNKNOWN_STATUS_TRANSITION");
    }
    promotion.setStatus(newStatus);
    accreditedPromotionRepository.save(promotion);
  }

  @Transactional
  public void syncStatusWithAcademicYear(Long yearId, YearStatus newYearStatus) {
    PromotionStatus targetStatus;
    switch (newYearStatus) {
      case ENROLLMENT:
        targetStatus = PromotionStatus.ENROLLMENT;
        break;
      case IN_PROGRESS:
        targetStatus = PromotionStatus.IN_PROGRESS;
        break;
      case CLOSING:
        targetStatus = PromotionStatus.EVALUATION;
      case COMPLETED:
        targetStatus = PromotionStatus.COMPLETED;
        break;
      default:
        return;
    }
    accreditedPromotionRepository.updateStatusByAcademicYearId(targetStatus, yearId);
  }

  @Transactional(readOnly = true)
  public List<AccreditedPromotionStatsResponse> getPromotionStatistics() {

    List<PromotionStatus> targetStatuses =
        List.of(
            PromotionStatus.DRAFT,
            PromotionStatus.ENROLLMENT,
            PromotionStatus.IN_PROGRESS,
            PromotionStatus.EVALUATION);

    // Récupération optimisée en une seule requête
    List<PromotionStatusStatsProjection> projections =
        accreditedPromotionRepository.getStatsByStatuses(targetStatuses);

    // EnumMap pour des performances maximales
    Map<PromotionStatus, PromotionStatusStatsProjection> statsMap =
        new EnumMap<>(PromotionStatus.class);
    for (PromotionStatusStatsProjection proj : projections) {
      statsMap.put(proj.getStatus(), proj);
    }

    // Construction de la réponse finale, avec gestion sécurisée des statuts vides
    return targetStatuses.stream()
        .map(
            status -> {
              PromotionStatusStatsProjection proj = statsMap.get(status);
              long count = (proj != null) ? proj.getPromotionCount() : 0L;
              long enrollments = (proj != null) ? proj.getTotalEnrollments() : 0L;

              return new AccreditedPromotionStatsResponse(status, count, enrollments);
            })
        .toList();
  }

  /**
   * Récupère la liste des promotions avec les statuts ENROLLMENT et IN_PROGRESS. Retourne une
   * version allégée (OngoingPromotionResponse).
   *
   * @param limit Le nombre maximum d'éléments à retourner (peut être null)
   * @return Liste de OngoingPromotionResponse
   */
  @Transactional(readOnly = true)
  public List<OngoingPromotionResponse> getOngoingPromotions(Integer limit) {
    List<PromotionStatus> targetStatuses =
        List.of(PromotionStatus.ENROLLMENT, PromotionStatus.IN_PROGRESS);

    // Tri par date de début décroissante
    Sort sort = Sort.by(Sort.Direction.DESC, "startDate");
    List<AccreditedPromotion> promotions;

    if (limit != null && limit > 0) {
      Pageable pageable = PageRequest.of(0, limit, sort);
      promotions =
          accreditedPromotionRepository.findByStatusIn(targetStatuses, pageable).getContent();
    } else {
      promotions = accreditedPromotionRepository.findByStatusIn(targetStatuses, sort);
    }

    // Utilisation de MapStruct pour transformer en OngoingPromotionResponse
    return promotions.stream().map(mapper::toOngoingResponse).toList();
  }

  private String generateYearSuffix(String academicYearLabel) {
    if (academicYearLabel == null || academicYearLabel.isBlank()) {
      return "YY";
    }
    try {
      String[] parts = academicYearLabel.split("-");
      if (parts.length == 2) {
        String start = parts[0].trim().substring(parts[0].trim().length() - 2);
        String end = parts[1].trim().substring(parts[1].trim().length() - 2);
        return start + end;
      }
    } catch (Exception e) {
      return academicYearLabel.replaceAll("[^0-9]", "");
    }
    return academicYearLabel.replaceAll("[^0-9]", "");
  }
}
