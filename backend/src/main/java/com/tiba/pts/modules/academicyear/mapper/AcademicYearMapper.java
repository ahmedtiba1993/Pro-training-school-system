package com.tiba.pts.modules.academicyear.mapper;

import com.tiba.pts.modules.academicyear.domain.entity.AcademicYear;
import com.tiba.pts.modules.academicyear.domain.entity.ExamSession;
import com.tiba.pts.modules.academicyear.domain.entity.Holiday;
import com.tiba.pts.modules.academicyear.domain.entity.Period;
import com.tiba.pts.modules.academicyear.dto.request.AcademicYearRequest;
import com.tiba.pts.modules.academicyear.dto.response.*;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface AcademicYearMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "isDefault", ignore = true)
  @Mapping(target = "isLocked", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "periods", ignore = true)
  @Mapping(target = "holidays", ignore = true)
  AcademicYear toEntity(AcademicYearRequest request);

  AcademicYearResponse toResponse(AcademicYear entity);

  PublicAcademicYearResponse toPublicResponse(AcademicYear entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "isDefault", ignore = true)
  @Mapping(target = "isLocked", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "periods", ignore = true)
  @Mapping(target = "holidays", ignore = true)
  void updateEntityFromRequest(AcademicYearRequest request, @MappingTarget AcademicYear entity);

  ActiveAcademicYearResponse toActiveResponse(
      AcademicYear year,
      long remainingDaysInYear,
      String currentPeriodLabel,
      int periodProgress,
      long remainingDaysInCurrentPeriod);

  @Mapping(target = "id", source = "year.id")
  @Mapping(target = "label", source = "year.label")
  @Mapping(target = "startDate", source = "year.startDate")
  @Mapping(target = "endDate", source = "year.endDate")
  @Mapping(target = "periods", source = "periods")
  @Mapping(target = "holidays", source = "holidays")
  AcademicCalendarResponse toCalendarResponse(
      AcademicYear year,
      List<Period> periods,
      List<Holiday> holidays);

  CalendarPeriodResponse toCalendarPeriodResponse(Period period);

  CalendarSessionResponse toCalendarSessionResponse(ExamSession session);

  @Mapping(target = "numberOfDays", expression = "java(holiday.getNumberOfDays())")
  CalendarHolidayResponse toCalendarHolidayResponse(Holiday holiday);
}
