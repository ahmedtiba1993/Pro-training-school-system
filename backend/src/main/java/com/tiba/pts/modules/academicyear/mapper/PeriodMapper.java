package com.tiba.pts.modules.academicyear.mapper;

import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.academicyear.domain.entity.ExamSession;
import com.tiba.pts.modules.academicyear.domain.entity.Period;
import com.tiba.pts.modules.academicyear.dto.request.ExamSessionRequest;
import com.tiba.pts.modules.academicyear.dto.request.PeriodRequest;
import com.tiba.pts.modules.academicyear.dto.response.DefaultPeriodResponse;
import com.tiba.pts.modules.academicyear.dto.response.PeriodResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface PeriodMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  @Mapping(target = "sessions", ignore = true)
  @Mapping(target = "academicYear", source = "academicYearId")
  Period toEntity(PeriodRequest request);

  PeriodResponse toResponse(Period period);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "isLocked", ignore = true)
  @Mapping(target = "sessionType", ignore = true) // INTERDIT de transformer un MAIN en RETAKE
  @Mapping(
      target = "period",
      ignore = true) // INTERDIT de déplacer la session dans un autre trimestre
  void updateEntityFromRequest(ExamSessionRequest request, @MappingTarget ExamSession entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "isLocked", ignore = true)
  @Mapping(target = "orderIndex", ignore = true)
  @Mapping(target = "sessions", ignore = true)
  @Mapping(target = "academicYear", ignore = true)
  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "modifiedDate", ignore = true)
  void updatePeriodFromRequest(PeriodRequest request, @MappingTarget Period entity);

  DefaultPeriodResponse toDefaultResponse(Period entity);

  List<DefaultPeriodResponse> toDefaultResponseList(List<Period> entities);

  // Helper for MapStruct: Transform a Long into a Proxy object (avoids a SQL query)
  // @Named("idToAcademicYear")
  default AcademicYear idToAcademicYear(Long id) {
    if (id == null) return null;
    AcademicYear year = new AcademicYear();
    year.setId(id);
    return year;
  }
}
