package com.tiba.pts.modules.enrollment.mapper;

import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import com.tiba.pts.modules.enrollment.dto.request.ParentDto;
import com.tiba.pts.modules.enrollment.dto.request.StudentDto;
import com.tiba.pts.modules.enrollment.dto.response.EnrollmentResponse;
import com.tiba.pts.modules.person.domain.entity.Parent;
import com.tiba.pts.modules.person.dto.request.ParentRequest;
import com.tiba.pts.modules.person.dto.request.StudentRequest;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface EnrollmentMapper {

  StudentRequest toStudentRequest(StudentDto studentDto);

  ParentRequest toParentRequest(ParentDto parentDto);

  // 1. Déclaration des correspondances (Mapping)
  @Mapping(
      target = "studentFullName",
      expression =
          "java(enrollment.getStudent().getLastName() + ' ' + enrollment.getStudent().getFirstName())")
  @Mapping(target = "dateOfBirth", source = "student.dateOfBirth")
  @Mapping(target = "studentPhoneNumber", source = "student.phoneNumber")
  @Mapping(target = "promotionName", source = "trainingSession.promotionName")
  // @Mapping(target = "enrollmentDate", source = "createdAt")
  @Mapping(target = "status", source = "status")

  // Concaténation "Niveau - Spécialité" (ex: "Licence - Informatique")
  @Mapping(
      target = "training",
      expression =
          "java(enrollment.getTrainingSession().getLevel().getLabel() + '-' + enrollment.getTrainingSession().getSpecialty().getName())")

  // CORRECTION ICI : Appel avec le bon nom de méthode et le bon paramètre !
  @Mapping(
      target = "guardianPhoneNumber",
      expression = "java(extractGuardianPhoneNumber(enrollment.getStudent().getParents()))")
  EnrollmentResponse toResponse(Enrollment enrollment);

  // =========================================================
  // MÉTHODE INTERNE DU MAPPER
  // =========================================================
  default String extractGuardianPhoneNumber(Set<Parent> parents) {

    // Sécurité si l'étudiant n'a pas de parents renseignés
    if (parents == null || parents.isEmpty()) {
      return "Non renseigné";
    }

    // On cherche le parent qui est le tuteur légal (isLegalGuardian = true)
    /* for (Parent parent : parents) {
      if (Boolean.TRUE.equals(parent.getIsLegalGuardian())) {

        // Si le tuteur n'a pas renseigné son téléphone, on renvoie un texte par défaut
        if (parent.getPhoneNumber() == null || parent.getPhoneNumber().isBlank()) {
          return "Non renseigné";
        }

        // Sinon, on renvoie son numéro !
        return parent.getPhoneNumber();
      }
    }*/

    // Si la boucle se termine sans trouver de tuteur, on renvoie un texte par défaut
    return "Non renseigné";
  }
}
