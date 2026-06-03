package com.tiba.pts.modules.execution.mapper;

import com.tiba.pts.modules.execution.domain.entity.AttendanceRecord;
import com.tiba.pts.modules.execution.dto.response.AttendanceRecordResponse;
import com.tiba.pts.modules.profiles.mapper.RefTeacherSpecialtyMapper;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring",
    uses = {RefTeacherSpecialtyMapper.class},
    builder = @Builder(disableBuilder = true))
public interface AttendanceRecordMapper {

  @Mapping(target = "enrollmentId", source = "enrollment.id")
  @Mapping(
      target = "studentName",
      expression =
          "java(entity.getEnrollment() != null && entity.getEnrollment().getStudent() != null ? entity.getEnrollment().getStudent().getFirstName() + \" \" + entity.getEnrollment().getStudent().getLastName() : null)")
  AttendanceRecordResponse toResponse(AttendanceRecord entity);
}
