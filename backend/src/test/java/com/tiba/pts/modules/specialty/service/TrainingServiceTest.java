package com.tiba.pts.modules.specialty.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.modules.specialty.domain.entity.Level;
import com.tiba.pts.modules.specialty.domain.entity.Specialty;
import com.tiba.pts.modules.specialty.domain.entity.Training;
import com.tiba.pts.modules.specialty.domain.enums.DurationUnit;
import com.tiba.pts.modules.specialty.domain.enums.TrainingStatus;
import com.tiba.pts.modules.specialty.domain.enums.TrainingType;
import com.tiba.pts.modules.specialty.dto.request.TrainingRequest;
import com.tiba.pts.modules.specialty.mapper.TrainingMapper;
import com.tiba.pts.modules.specialty.repository.LevelRepository;
import com.tiba.pts.modules.specialty.repository.SpecialtyRepository;
import com.tiba.pts.modules.specialty.repository.TrainingRepository;
import com.tiba.pts.modules.trainingsession.repository.PromotionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingServiceTest {

  @Mock
  private TrainingRepository trainingRepository;

  @Mock
  private TrainingMapper trainingMapper;

  @Mock
  private LevelRepository levelRepository;

  @Mock
  private SpecialtyRepository specialtyRepository;

  @Mock
  private PromotionRepository promotionRepository;

  @InjectMocks
  private TrainingService trainingService;

  @Test
  @DisplayName("generateTechnicalCode: Format CONTINUOUS type code correctly")
  void generateTechnicalCode_continuous_formatSuccess() {
    String code = trainingService.generateTechnicalCode("btp", "elec", TrainingType.CONTINUOUS);
    assertEquals("BTP-ELEC-CON", code);
  }

  @Test
  @DisplayName("generateTechnicalCode: Format ACCREDITED type code correctly")
  void generateTechnicalCode_accredited_formatSuccess() {
    String code = trainingService.generateTechnicalCode("bts", "info", TrainingType.ACCREDITED);
    assertEquals("BTS-INFO-HOM", code);
  }

  @Test
  @DisplayName("generateTechnicalCode: Format ACCELERATED type code correctly")
  void generateTechnicalCode_accelerated_formatSuccess() {
    String code = trainingService.generateTechnicalCode("cap", "soud", TrainingType.ACCELERATED);
    assertEquals("CAP-SOUD-ACC", code);
  }

  @Test
  @DisplayName("generateTechnicalCode: Should trim and uppercase codes with spaces and mixed case")
  void generateTechnicalCode_withSpacesAndMixedCase_shouldTrimAndUpperCase() {
    String code = trainingService.generateTechnicalCode("  btp  ", " dev-web ", TrainingType.CONTINUOUS);
    assertEquals("BTP-DEV-WEB-CON", code);
  }

  @Test
  @DisplayName("generateTechnicalCode: Null parameters should throw BusinessValidationException")
  void generateTechnicalCode_nullParameters_throwsException() {
    assertThrows(
        BusinessValidationException.class,
        () -> trainingService.generateTechnicalCode(null, "info", TrainingType.ACCREDITED));
    assertThrows(
        BusinessValidationException.class,
        () -> trainingService.generateTechnicalCode("bts", null, TrainingType.ACCREDITED));
    assertThrows(
        BusinessValidationException.class,
        () -> trainingService.generateTechnicalCode("bts", "info", null));
  }

  @Test
  @DisplayName("createTraining: Should generate and set formatted technical code on entity")
  void createTraining_shouldGenerateAndSetTechnicalCode() {
    // Given
    TrainingRequest request =
        new TrainingRequest(
            "Description formation",
            TrainingType.ACCREDITED,
            2,
            DurationUnit.YEARS,
            1L,
            2L);

    Level level = Level.builder().id(1L).code("bts").isActive(true).build();
    Specialty specialty = Specialty.builder().id(2L).code("info").build();
    Training training = Training.builder().build();

    when(levelRepository.findById(1L)).thenReturn(Optional.of(level));
    when(specialtyRepository.findById(2L)).thenReturn(Optional.of(specialty));
    when(trainingRepository.existsByLevelIdAndSpecialtyIdAndTrainingType(
            1L, 2L, TrainingType.ACCREDITED))
        .thenReturn(false);
    when(trainingMapper.toEntity(request)).thenReturn(training);
    when(trainingRepository.save(any(Training.class)))
        .thenAnswer(
            invocation -> {
              Training saved = invocation.getArgument(0);
              saved.setId(100L);
              return saved;
            });

    // When
    Long resultId = trainingService.createTraining(request);

    // Then
    assertEquals(100L, resultId);
    ArgumentCaptor<Training> captor = ArgumentCaptor.forClass(Training.class);
    verify(trainingRepository).save(captor.capture());

    Training savedTraining = captor.getValue();
    assertEquals("BTS-INFO-HOM", savedTraining.getCode());
    assertEquals(TrainingStatus.DRAFT, savedTraining.getStatus());
    assertEquals(level, savedTraining.getLevel());
    assertEquals(specialty, savedTraining.getSpecialty());
  }
}
