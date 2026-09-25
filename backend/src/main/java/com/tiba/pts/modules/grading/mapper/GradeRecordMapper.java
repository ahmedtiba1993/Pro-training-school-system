package com.tiba.pts.modules.grading.mapper;

import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import com.tiba.pts.modules.grading.domain.entity.GradeRecord;
import com.tiba.pts.modules.grading.dto.response.GradeRecordResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface GradeRecordMapper {

  @Mapping(target = "id", source = "gradeRecord.id")
  @Mapping(target = "enrollmentId", source = "enrollment.id")
  @Mapping(target = "matricule", source = "enrollment.student.studentCode")
  @Mapping(target = "nom", source = "enrollment.student.lastName")
  @Mapping(target = "prenom", source = "enrollment.student.firstName")
  @Mapping(target = "presence", source = "gradeRecord.attendanceStatus")
  @Mapping(target = "note", source = "gradeRecord.score")
  @Mapping(target = "commentaire", source = "gradeRecord.teacherComment")
  GradeRecordResponse toResponse(Enrollment enrollment, GradeRecord gradeRecord);

  @Mapping(target = "id", source = "id")
  @Mapping(target = "enrollmentId", source = "enrollment.id")
  @Mapping(target = "matricule", source = "enrollment.student.studentCode")
  @Mapping(target = "nom", source = "enrollment.student.lastName")
  @Mapping(target = "prenom", source = "enrollment.student.firstName")
  @Mapping(target = "presence", source = "attendanceStatus")
  @Mapping(target = "note", source = "score")
  @Mapping(target = "commentaire", source = "teacherComment")
  GradeRecordResponse toResponse(GradeRecord gradeRecord);

  List<GradeRecordResponse> toResponseList(List<GradeRecord> gradeRecords);
}
