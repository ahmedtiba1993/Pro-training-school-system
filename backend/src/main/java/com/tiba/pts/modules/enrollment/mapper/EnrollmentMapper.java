package com.tiba.pts.modules.enrollment.mapper;

import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import com.tiba.pts.modules.enrollment.domain.entity.EnrollmentDocumentSubmission;
import com.tiba.pts.modules.enrollment.dto.request.*;
import com.tiba.pts.modules.enrollment.dto.response.*;
import com.tiba.pts.modules.profiles.domain.entity.Student;
import com.tiba.pts.modules.profiles.domain.entity.StudentSibling;
import com.tiba.pts.modules.trainingsession.domain.entity.Promotion;


import com.tiba.pts.modules.profiles.dto.request.ParentRequest;
import com.tiba.pts.modules.profiles.dto.request.StudentParentRequest;
import com.tiba.pts.modules.profiles.dto.request.StudentRequest;
import com.tiba.pts.modules.profiles.dto.request.StudentSiblingRequest;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface EnrollmentMapper {

  // ==============================================================================================
  // ENROLLMENT MAPPING (Request -> Entity -> Response)
  // ==============================================================================================

  @Mapping(target = "enrollmentDocumentSubmissions", source = "enrollmentSubmittedDocuments")
  Enrollment toEntity(EnrollmentRequest request);

  @Mapping(target = "enrollmentSubmittedDocuments", source = "enrollmentDocumentSubmissions")
  EnrollmentResponse toResponse(Enrollment entity);

  @Mapping(target = "document.id", source = "enrollmentDocumentId")
  EnrollmentDocumentSubmission toSubmittedDocument(SubmittedDocumentRequest request);

  @Mapping(target = "enrollmentDocumentId", source = "document.id")
  @Mapping(target = "documentName", source = "document.name")
  SubmittedDocumentResponse toSubmittedDocumentResponse(EnrollmentDocumentSubmission entity);

  // ==============================================================================================
  // SUB-OBJECTS FOR THE RESPONSE (Used automatically by toResponse)
  // ==============================================================================================

  PromotionSummaryResponse toPromotionSummaryResponse(Promotion entity);

  StudentSummaryResponse toStudentSummaryResponse(Student entity);

  SiblingInfoResponse toSiblingInfoResponse(StudentSibling entity);

  StudentParentSummaryResponse toStudentParentSummaryResponse(
      com.tiba.pts.modules.profiles.domain.entity.StudentParent entity);

  ParentSummaryResponse toParentSummaryResponse(
      com.tiba.pts.modules.profiles.domain.entity.Parent entity);

  // ==============================================================================================
  // INTER-MODULE TRANSLATION: Enrollment DTO -> Profiles DTO
  // ==============================================================================================

  // The main method called in EnrollmentService
  StudentRequest toStudentRequest(StudentInfoRequest studentInfo);

  // The methods below are used AUTOMATICALLY by MapStruct
  // to translate the lists (parents, siblings) inside toStudentRequest.
  StudentParentRequest toStudentParentRequest(StudentParentInfoRequest request);

  ParentRequest toParentRequest(ParentInfoRequest request);

  StudentSiblingRequest toStudentSiblingRequest(StudentSiblingInfoRequest request);

  // ==============================================================================================
  // MAPPING FOR PAGINATION (Optimized table)
  // ==============================================================================================

  @Mapping(target = "studentFirstName", source = "student.firstName")
  @Mapping(target = "studentLastName", source = "student.lastName")
  @Mapping(target = "studentBirthDate", source = "student.birthDate")
  @Mapping(target = "studentPhone", source = "student.phone")

  // Deep navigation (Deep Mapping) MapStruct handles this all by itself!
  @Mapping(target = "promotionName", source = "promotion.name")
  @Mapping(target = "levelLabel", source = "promotion.training.level.label")
  @Mapping(target = "specialityLabel", source = "promotion.training.specialty.label")

  // Call to the custom method for the guardian
  @Mapping(target = "guardianPhone", expression = "java(extractGuardianPhone(entity.getStudent()))")
  EnrollmentListResponse toListResponse(Enrollment entity);

  /** Java method to find the legal guardian's phone */
  default String extractGuardianPhone(Student student) {
    if (student == null || student.getParents() == null) {
      return null;
    }
    return student.getParents().stream()
        .filter(com.tiba.pts.modules.profiles.domain.entity.StudentParent::isLegalGuardian)
        .map(sp -> sp.getParent().getPhone())
        .findFirst()
        .orElse(null);
  }
}
