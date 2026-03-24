package com.tiba.pts.modules.enrollment.service;

import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import com.tiba.pts.modules.enrollment.domain.entity.EnrollmentDocument;
import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus;
import com.tiba.pts.modules.enrollment.dto.request.EnrollmentRequest;
import com.tiba.pts.modules.enrollment.dto.request.ProvidedDocumentDto;
import com.tiba.pts.modules.enrollment.dto.response.EnrollmentResponse;
import com.tiba.pts.modules.enrollment.mapper.EnrollmentMapper;
import com.tiba.pts.modules.enrollment.repository.EnrollmentRepository;
import com.tiba.pts.modules.person.domain.entity.Student;
import com.tiba.pts.modules.person.domain.enums.ParentType;
import com.tiba.pts.modules.person.dto.request.StudentRequest;
import com.tiba.pts.modules.person.service.ParentService;
import com.tiba.pts.modules.person.service.StudentService;
import com.tiba.pts.modules.registrationdocuments.domain.entity.RegistrationDocument;
import com.tiba.pts.modules.registrationdocuments.service.RegistrationDocumentService;
import com.tiba.pts.modules.trainingSession.domain.entity.TrainingSession;
import com.tiba.pts.modules.trainingSession.repository.TrainingSessionRepository;
import com.tiba.pts.modules.trainingSession.service.TrainingSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

  private final StudentService studentService;
  private final ParentService parentService;
  private final EnrollmentRepository enrollmentRepository;
  private final EnrollmentMapper enrollmentMapper;
  private final TrainingSessionService trainingSessionService;
  private final RegistrationDocumentService registrationDocumentService;

  @Transactional
  public Long createEnrollment(EnrollmentRequest request) {

    // VALIDATE THE TRAINING SESSION AND ITS RULES
    TrainingSession session =
        trainingSessionService.getValidSessionForEnrollment(request.getTrainingSessionId());

    // MAP AND CREATE THE STUDENT
    Student savedStudent =
        studentService.createStudent(enrollmentMapper.toStudentRequest(request.getStudent()));

    //  MAP AND ASSOCIATE PARENTS
    processParents(request, savedStudent);

    // CREATE THE ENROLLMENT
    Enrollment enrollment = new Enrollment();
    enrollment.setStatus(EnrollmentStatus.PENDING);
    enrollment.setStudent(savedStudent);
    enrollment.setTrainingSession(session);

    // Handle Documents
    processDocuments(request.getDocuments(), enrollment);

    // FINAL SAVE
    return enrollmentRepository.save(enrollment).getId();
  }

  private void processParents(EnrollmentRequest request, Student student) {
    parentService.processParent(
        enrollmentMapper.toParentRequest(request.getFather()), ParentType.FATHER, student);
    parentService.processParent(
        enrollmentMapper.toParentRequest(request.getMother()), ParentType.MOTHER, student);
    parentService.processParent(
        enrollmentMapper.toParentRequest(request.getGuardian()), ParentType.GUARDIAN, student);
  }

  private void processDocuments(List<ProvidedDocumentDto> documentDtos, Enrollment enrollment) {

    if (CollectionUtils.isEmpty(documentDtos)) {
      return;
    }

    List<EnrollmentDocument> documentsList = new ArrayList<>();

    for (ProvidedDocumentDto docDto : documentDtos) {
      RegistrationDocument catalogDoc =
          registrationDocumentService.getRegistrationDocumentById(
              docDto.getRegistrationDocumentId());

      EnrollmentDocument providedDoc = new EnrollmentDocument();
      providedDoc.setRegistrationDocument(catalogDoc);
      providedDoc.setProvided(docDto.getIsProvided());
      providedDoc.setEnrollment(enrollment);

      documentsList.add(providedDoc);
    }

    enrollment.setProvidedDocuments(documentsList);
  }

  @Transactional(readOnly = true)
  public PageResponse<EnrollmentResponse> getAll(int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    Page<Enrollment> pageResult = enrollmentRepository.findAll(pageable);
    return PageResponse.of(pageResult, enrollmentMapper::toResponse);
  }
}
