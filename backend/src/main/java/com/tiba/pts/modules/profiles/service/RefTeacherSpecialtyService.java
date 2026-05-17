package com.tiba.pts.modules.profiles.service;

import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.profiles.domain.entity.RefTeacherSpecialty;
import com.tiba.pts.modules.profiles.domain.entity.Teacher;
import com.tiba.pts.modules.profiles.domain.enums.TeacherStatus;
import com.tiba.pts.modules.profiles.dto.request.RefTeacherSpecialtyRequest;
import com.tiba.pts.modules.profiles.dto.request.UpdateSpecialtyRequest;
import com.tiba.pts.modules.profiles.dto.response.RefTeacherSpecialtyResponse;
import com.tiba.pts.modules.profiles.dto.response.RefTeacherSpecialtySimpleResponse;
import com.tiba.pts.modules.profiles.dto.response.TeacherSimpleResponse;
import com.tiba.pts.modules.profiles.mapper.RefTeacherSpecialtyMapper;
import com.tiba.pts.modules.profiles.mapper.TeacherMapper;
import com.tiba.pts.modules.profiles.repository.RefTeacherSpecialtyRepository;
import com.tiba.pts.modules.profiles.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RefTeacherSpecialtyService {

  private final RefTeacherSpecialtyRepository specialtyRepository;
  private final RefTeacherSpecialtyMapper specialtyMapper;
  private final TeacherRepository teacherRepository;
  private final TeacherMapper teacherMapper;

  @Transactional
  public Long createSpecialty(RefTeacherSpecialtyRequest request) {
    if (specialtyRepository.existsByCode(request.getCode())) {
      throw new EntityAlreadyExistsException("SPECIALTY_CODE_ALREADY_EXISTS");
    }
    if (specialtyRepository.existsByLabel(request.getLabel())) {
      throw new EntityAlreadyExistsException("SPECIALTY_LABEL_ALREADY_EXISTS");
    }

    RefTeacherSpecialty specialty = specialtyMapper.toEntity(request);
    RefTeacherSpecialty savedSpecialty = specialtyRepository.save(specialty);

    return specialtyMapper.toResponse(savedSpecialty).getId();
  }

  @Transactional(readOnly = true)
  public List<RefTeacherSpecialtyResponse> getAllSpecialtiesWithCount(String keyword) {
    List<RefTeacherSpecialty> specialties;

    // check if a search parameter has been provided
    if (keyword != null && !keyword.isBlank()) {
      specialties = specialtyRepository.findByLabelContainingIgnoreCase(keyword.trim());
    } else {
      specialties = specialtyRepository.findAll();
    }

    return specialtyMapper.toResponseList(specialties);
  }

  @Transactional(readOnly = true)
  public List<RefTeacherSpecialtySimpleResponse> getAllSpecialtiesSimple() {
    List<RefTeacherSpecialty> specialties = specialtyRepository.findAll();
    return specialtyMapper.toSimpleResponseList(specialties);
  }

  @Transactional(readOnly = true)
  public RefTeacherSpecialtyResponse getSpecialtyById(Long id) {
    RefTeacherSpecialty specialty =
        specialtyRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("SPECIALTY_NOT_FOUND"));
    return specialtyMapper.toResponse(specialty);
  }

  @Transactional
  public Long update(Long id, UpdateSpecialtyRequest request) {
    // Verify if the specialty exists
    RefTeacherSpecialty specialty =
        specialtyRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("SPECIALTY_NOT_FOUND"));

    // Verify if the new label already exists on another row
    if (specialtyRepository.existsByLabelAndIdNot(request.getLabel(), id)) {
      throw new EntityAlreadyExistsException("SPECIALTY_LABEL_ALREADY_EXISTS");
    }

    // Mutation of editable fields and saving
    specialty.setLabel(request.getLabel());
    specialty.setDescription(request.getDescription());

    RefTeacherSpecialty updatedSpecialty = specialtyRepository.save(specialty);

    return specialtyMapper.toResponse(updatedSpecialty).getId();
  }

  @Transactional(readOnly = true)
  public List<TeacherSimpleResponse> getActiveTeachersBySpecialty(Long specialtyId) {
    // Verify that the specialty exists before searching
    if (!specialtyRepository.existsById(specialtyId)) {
      throw new ResourceNotFoundException("SPECIALTY_NOT_FOUND");
    }

    // Retrieve only teachers with ACTIVE status for this specialty
    List<Teacher> activeTeachers =
        teacherRepository.findBySpecialtiesIdAndStatus(specialtyId, TeacherStatus.ACTIVE);

    // Map
    return teacherMapper.toSimpleResponseList(activeTeachers);
  }
}
