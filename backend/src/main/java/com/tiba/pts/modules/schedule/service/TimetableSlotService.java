package com.tiba.pts.modules.schedule.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.core.service.PdfGeneratorService;
import com.tiba.pts.modules.profiles.domain.entity.Teacher;
import com.tiba.pts.modules.profiles.domain.enums.TeacherStatus;
import com.tiba.pts.modules.profiles.repository.TeacherRepository;
import com.tiba.pts.modules.room.domain.entity.Room;
import com.tiba.pts.modules.room.domain.enums.RoomStatus;
import com.tiba.pts.modules.room.repository.RoomRepository;
import com.tiba.pts.modules.schedule.domain.entity.Schedule;
import com.tiba.pts.modules.schedule.domain.entity.TimeSlotDefinition;
import com.tiba.pts.modules.schedule.domain.entity.TimetableSlot;
import com.tiba.pts.modules.schedule.domain.enums.Periodicity;
import com.tiba.pts.modules.schedule.domain.enums.ScheduleStatus;
import com.tiba.pts.modules.schedule.dto.request.TimetableSlotRequest;
import com.tiba.pts.modules.schedule.dto.response.ScheduleInfoResponse;
import com.tiba.pts.modules.schedule.dto.response.TimeSlotDefinitionResponse;
import com.tiba.pts.modules.schedule.dto.response.TimetableSlotInfoResponse;
import com.tiba.pts.modules.schedule.dto.response.TimetableSlotDetailResponse;
import com.tiba.pts.modules.schedule.dto.response.TimetableTeacherViewResponse;
import com.tiba.pts.modules.schedule.dto.response.TimetableViewResponse;
import com.tiba.pts.modules.schedule.mapper.ScheduleMapper;
import com.tiba.pts.modules.schedule.mapper.TimeSlotDefinitionMapper;
import com.tiba.pts.modules.schedule.mapper.TimetableSlotMapper;
import com.tiba.pts.modules.schedule.repository.ScheduleRepository;
import com.tiba.pts.modules.schedule.repository.TimeSlotDefinitionRepository;
import com.tiba.pts.modules.schedule.repository.TimetableSlotRepository;
import com.tiba.pts.modules.subject.domain.entity.Subject;
import com.tiba.pts.modules.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tiba.pts.modules.schedule.domain.enums.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TimetableSlotService {

  private final TimetableSlotRepository timetableSlotRepository;
  private final ScheduleRepository scheduleRepository;
  private final SubjectRepository subjectRepository;
  private final TeacherRepository teacherRepository;
  private final RoomRepository roomRepository;
  private final TimetableSlotMapper timetableSlotMapper;
  private final TimeSlotDefinitionRepository timeSlotDefinitionRepository;
  private final ScheduleMapper scheduleMapper;
  private final TimeSlotDefinitionMapper timeSlotDefinitionMapper;
  private final PdfGeneratorService pdfGeneratorService;

  @Transactional
  public Long addTimetableSlot(TimetableSlotRequest request) {

    Schedule schedule =
        scheduleRepository
            .findById(request.getScheduleId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Schedule non trouvé avec l'ID: " + request.getScheduleId()));

    Subject subject =
        subjectRepository
            .findById(request.getSubjectId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Subject non trouvé avec l'ID: " + request.getSubjectId()));

    // Verification: the subject must belong to the promotion's program
    Long scheduleTrainingId = schedule.getClassGroup().getPromotion().getTraining().getId();
    Long subjectTrainingId = subject.getTraining().getId();

    if (!scheduleTrainingId.equals(subjectTrainingId)) {
      throw new BusinessValidationException("SUBJECT_NOT_IN_PROMOTION_TRAINING");
    }

    Teacher teacher =
        teacherRepository
            .findById(request.getTeacherId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Teacher non trouvé avec l'ID: " + request.getTeacherId()));

    if (teacher.getStatus() != TeacherStatus.ACTIVE) {
      throw new BusinessValidationException("TEACHER_NOT_ACTIVE");
    }

    Room room =
        roomRepository
            .findById(request.getRoomId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Room non trouvé avec l'ID: " + request.getRoomId()));

    if (room.getStatus() != RoomStatus.ACTIVE) {
      throw new BusinessValidationException("ROOM_NOT_ACTIVE");
    }

    TimeSlotDefinition timeSlotDefinition =
        timeSlotDefinitionRepository
            .findById(request.getTimeSlotDefinitionId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "TimeSlotDefinition non trouvée avec l'ID: "
                            + request.getTimeSlotDefinitionId()));

    // Verification of periodicity collisions
    Periodicity newPeriodicity = request.getPeriodicity();
    DayOfWeek dayOfWeek = request.getDayOfWeek();
    Long timeSlotDefId = timeSlotDefinition.getId();

    // Collision on the same schedule (same class group)
    List<TimetableSlot> scheduleSlots =
        timetableSlotRepository.findByScheduleIdAndDayOfWeekAndTimeSlotDefinitionId(
            schedule.getId(), dayOfWeek, timeSlotDefId);
    for (TimetableSlot existing : scheduleSlots) {
      if (hasPeriodicityConflict(newPeriodicity, existing.getPeriodicity())) {
        throw new BusinessValidationException("SCHEDULE_SLOT_CONFLICT");
      }
    }

    // Teacher collision (same teacher, same slot, across all schedules)
    List<TimetableSlot> teacherSlots =
        timetableSlotRepository.findByTeacherIdAndDayOfWeekAndTimeSlotDefinitionId(
            teacher.getId(), dayOfWeek, timeSlotDefId);
    for (TimetableSlot existing : teacherSlots) {
      if (hasPeriodicityConflict(newPeriodicity, existing.getPeriodicity())) {
        throw new BusinessValidationException("TEACHER_SLOT_CONFLICT");
      }
    }

    // Room collision (same room, same slot, across all schedules)
    List<TimetableSlot> roomSlots =
        timetableSlotRepository.findByRoomIdAndDayOfWeekAndTimeSlotDefinitionId(
            room.getId(), dayOfWeek, timeSlotDefId);
    for (TimetableSlot existing : roomSlots) {
      if (hasPeriodicityConflict(newPeriodicity, existing.getPeriodicity())) {
        throw new BusinessValidationException("ROOM_SLOT_CONFLICT");
      }
    }

    // Direct construction of the object
    TimetableSlot timetableSlot =
        TimetableSlot.builder()
            .dayOfWeek(dayOfWeek)
            .periodicity(newPeriodicity)
            .schedule(schedule)
            .subject(subject)
            .teacher(teacher)
            .room(room)
            .timeSlotDefinition(timeSlotDefinition)
            .build();

    return timetableSlotRepository.save(timetableSlot).getId();
  }

  @Transactional
  public Long updateTimetableSlot(Long id, TimetableSlotRequest request) {
    // Check existing TimetableSlot
    TimetableSlot timetableSlot =
        timetableSlotRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("TimetableSlot non trouvé avec l'ID: " + id));

    // Check current schedule status
    if (timetableSlot.getSchedule().getStatus() == ScheduleStatus.ARCHIVED) {
      throw new BusinessValidationException("CANNOT_UPDATE_SLOT_OF_ARCHIVED_SCHEDULE");
    }

    // Find and validate new Schedule
    Schedule schedule =
        scheduleRepository
            .findById(request.getScheduleId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Schedule non trouvé avec l'ID: " + request.getScheduleId()));

    if (schedule.getStatus() == ScheduleStatus.ARCHIVED) {
      throw new BusinessValidationException("CANNOT_UPDATE_SLOT_WITH_ARCHIVED_SCHEDULE");
    }

    // Find and validate Subject
    Subject subject =
        subjectRepository
            .findById(request.getSubjectId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Subject non trouvé avec l'ID: " + request.getSubjectId()));

    // Verification: the subject must belong to the promotion's program
    Long scheduleTrainingId = schedule.getClassGroup().getPromotion().getTraining().getId();
    Long subjectTrainingId = subject.getTraining().getId();

    if (!scheduleTrainingId.equals(subjectTrainingId)) {
      throw new BusinessValidationException("SUBJECT_NOT_IN_PROMOTION_TRAINING");
    }

    // Find and validate Teacher (must not be Archived/Departed)
    Teacher teacher =
        teacherRepository
            .findById(request.getTeacherId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Teacher non trouvé avec l'ID: " + request.getTeacherId()));

    if (teacher.getStatus() != TeacherStatus.ACTIVE) {
      throw new BusinessValidationException("TEACHER_ARCHIVED");
    }

    // Find and validate Room (must not be Archived)
    Room room =
        roomRepository
            .findById(request.getRoomId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Room non trouvé avec l'ID: " + request.getRoomId()));

    if (room.getStatus() != RoomStatus.ACTIVE) {
      throw new BusinessValidationException("ROOM_ARCHIVED");
    }

    // Find TimeSlotDefinition
    TimeSlotDefinition timeSlotDefinition =
        timeSlotDefinitionRepository
            .findById(request.getTimeSlotDefinitionId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "TimeSlotDefinition non trouvée avec l'ID: "
                            + request.getTimeSlotDefinitionId()));

    // Verification of periodicity collisions (excluding the current slot itself)
    Periodicity newPeriodicity = request.getPeriodicity();
    DayOfWeek dayOfWeek = request.getDayOfWeek();
    Long timeSlotDefId = timeSlotDefinition.getId();

    // Collision on the same schedule
    List<TimetableSlot> scheduleSlots =
        timetableSlotRepository.findByScheduleIdAndDayOfWeekAndTimeSlotDefinitionId(
            schedule.getId(), dayOfWeek, timeSlotDefId);
    for (TimetableSlot existing : scheduleSlots) {
      if (!existing.getId().equals(id)
          && hasPeriodicityConflict(newPeriodicity, existing.getPeriodicity())) {
        throw new BusinessValidationException("SCHEDULE_SLOT_CONFLICT");
      }
    }

    // Teacher collision
    List<TimetableSlot> teacherSlots =
        timetableSlotRepository.findByTeacherIdAndDayOfWeekAndTimeSlotDefinitionId(
            teacher.getId(), dayOfWeek, timeSlotDefId);
    for (TimetableSlot existing : teacherSlots) {
      if (!existing.getId().equals(id)
          && hasPeriodicityConflict(newPeriodicity, existing.getPeriodicity())) {
        throw new BusinessValidationException("TEACHER_SLOT_CONFLICT");
      }
    }

    // Room collision
    List<TimetableSlot> roomSlots =
        timetableSlotRepository.findByRoomIdAndDayOfWeekAndTimeSlotDefinitionId(
            room.getId(), dayOfWeek, timeSlotDefId);
    for (TimetableSlot existing : roomSlots) {
      if (!existing.getId().equals(id)
          && hasPeriodicityConflict(newPeriodicity, existing.getPeriodicity())) {
        throw new BusinessValidationException("ROOM_SLOT_CONFLICT");
      }
    }

    // Update slot details
    timetableSlot.setDayOfWeek(dayOfWeek);
    timetableSlot.setPeriodicity(newPeriodicity);
    timetableSlot.setSchedule(schedule);
    timetableSlot.setSubject(subject);
    timetableSlot.setTeacher(teacher);
    timetableSlot.setRoom(room);
    timetableSlot.setTimeSlotDefinition(timeSlotDefinition);

    return timetableSlotRepository.save(timetableSlot).getId();
  }

  /**
   * Periodicity matrix: NORMAL vs NORMAL → ❌ Conflict, NORMAL vs WEEK_A → ❌ Conflict, NORMAL vs
   * WEEK_B → ❌ Conflict, WEEK_A vs WEEK_A → ❌ Conflict, WEEK_B vs WEEK_B → ❌ Conflict, WEEK_A vs
   * WEEK_B → ✅ Allowed (alternation)
   *
   * <p>Formula: conflict IF (new == NORMAL) OR (existing == NORMAL) OR (new == existing)
   */
  private boolean hasPeriodicityConflict(
      Periodicity newPeriodicity, Periodicity existingPeriodicity) {
    return newPeriodicity == Periodicity.NORMAL
        || existingPeriodicity == Periodicity.NORMAL
        || newPeriodicity == existingPeriodicity;
  }

  @Transactional(readOnly = true)
  public TimetableViewResponse getTimetableView(Long scheduleId) {

    // Retrieval & Mapping of the Schedule Info
    Schedule schedule =
        scheduleRepository
            .findById(scheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("SCHEDULE_NOT_FOUND"));

    ScheduleInfoResponse scheduleInfo = scheduleMapper.toScheduleInfoResponse(schedule);

    // Retrieval & Mapping of Time Slot Definitions (Rows of the schedule)
    List<TimeSlotDefinitionResponse> timeSlotDefinitions =
        timeSlotDefinitionRepository.findAllByOrderByOrderIndexAsc().stream()
            .map(timeSlotDefinitionMapper::toResponse)
            .collect(Collectors.toList());

    // Retrieval & Mapping of the actual sessions (Cells of the schedule)
    List<TimetableSlotInfoResponse> timetableSlots =
        timetableSlotRepository.findAllByScheduleId(scheduleId).stream()
            .map(timetableSlotMapper::toInfoResponse)
            .collect(Collectors.toList());

    // Construction of the global view
    return TimetableViewResponse.builder()
        .scheduleInfo(scheduleInfo)
        .timeSlotDefinitions(timeSlotDefinitions)
        .timetableSlots(timetableSlots)
        .build();
  }

  @Transactional(readOnly = true)
  public TimetableSlotDetailResponse getById(Long id) {
    TimetableSlot slot =
        timetableSlotRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("TimetableSlot non trouvé avec l'ID: " + id));
    return timetableSlotMapper.toDetailResponse(slot);
  }

  @Transactional(readOnly = true)
  public TimetableTeacherViewResponse getTimetableViewByTeacher(Long teacherId) {

    // Check that the teacher exists
    Teacher teacher =
        teacherRepository
            .findById(teacherId)
            .orElseThrow(() -> new ResourceNotFoundException("TEACHER_NOT_FOUND"));

    // Retrieval & Mapping of Time Slot Definitions (Rows of the schedule)
    List<TimeSlotDefinitionResponse> timeSlotDefinitions =
        timeSlotDefinitionRepository.findAllByOrderByOrderIndexAsc().stream()
            .map(timeSlotDefinitionMapper::toResponse)
            .collect(Collectors.toList());

    // Retrieval & Mapping of this teacher's sessions (across all schedules)
    List<TimetableSlotInfoResponse> timetableSlots =
        timetableSlotRepository.findAllByTeacherId(teacherId).stream()
            .map(timetableSlotMapper::toInfoResponse)
            .collect(Collectors.toList());

    // Construction of the teacher view
    return TimetableTeacherViewResponse.builder()
        .teacherId(teacher.getId())
        .teacherName(teacher.getFirstName() + " " + teacher.getLastName())
        .timeSlotDefinitions(timeSlotDefinitions)
        .timetableSlots(timetableSlots)
        .build();
  }

  // ======================== PDF EXPORT ========================

  @Transactional(readOnly = true)
  public byte[] exportTimetablePdfBySchedule(Long scheduleId) {

    // Retrieve the Schedule with its ClassGroup
    Schedule schedule =
        scheduleRepository
            .findById(scheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("SCHEDULE_NOT_FOUND"));

    // Retrieve data
    List<TimeSlotDefinition> timeSlotDefs =
        timeSlotDefinitionRepository.findAllByOrderByOrderIndexAsc();
    List<TimetableSlot> slots = timetableSlotRepository.findAllByScheduleId(scheduleId);

    // Construction of Thymeleaf variables
    String title = "Emploi du Temps - " + schedule.getLabel();
    String subtitle = "Classe : " + schedule.getClassGroup().getName();

    Map<String, Object> variables =
        buildTimetablePdfVariables(title, subtitle, timeSlotDefs, slots);

    // PDF generation
    return pdfGeneratorService.generatePdf("schedule/timetable-view", variables);
  }

  @Transactional(readOnly = true)
  public byte[] exportTimetablePdfByTeacher(Long teacherId) {

    // Check that the teacher exists
    Teacher teacher =
        teacherRepository
            .findById(teacherId)
            .orElseThrow(() -> new ResourceNotFoundException("TEACHER_NOT_FOUND"));

    // Retrieve data
    List<TimeSlotDefinition> timeSlotDefs =
        timeSlotDefinitionRepository.findAllByOrderByOrderIndexAsc();
    List<TimetableSlot> slots = timetableSlotRepository.findAllByTeacherId(teacherId);

    // Construction of Thymeleaf variables
    String title = "Emploi du Temps - Enseignant";
    String subtitle =
        teacher.getFirstName() + " " + teacher.getLastName() + " (" + teacher.getCode() + ")";

    Map<String, Object> variables =
        buildTimetablePdfVariables(title, subtitle, timeSlotDefs, slots);

    // PDF generation
    return pdfGeneratorService.generatePdf("schedule/timetable-view", variables);
  }

  private Map<String, Object> buildTimetablePdfVariables(
      String title,
      String subtitle,
      List<TimeSlotDefinition> timeSlotDefs,
      List<TimetableSlot> slots) {

    // Days displayed in the schedule
    List<String> days = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY");

    Map<String, String> dayLabels =
        Map.of(
            "MONDAY", "Lundi",
            "TUESDAY", "Mardi",
            "WEDNESDAY", "Mercredi",
            "THURSDAY", "Jeudi",
            "FRIDAY", "Vendredi",
            "SATURDAY", "Samedi",
            "SUNDAY", "Dimanche");

    // Build time slot definitions for the template
    List<Map<String, Object>> timeSlotsList =
        timeSlotDefs.stream()
            .map(
                tsd -> {
                  Map<String, Object> map = new HashMap<>();
                  map.put("id", tsd.getId().toString());
                  map.put("label", tsd.getLabel());
                  map.put("startTime", tsd.getStartTime().toString());
                  map.put("endTime", tsd.getEndTime().toString());
                  return map;
                })
            .collect(Collectors.toList());

    // Grid construction: grid[day][timeSlotId] = List<slotInfo>
    Map<String, Map<String, List<Map<String, String>>>> grid = new LinkedHashMap<>();
    for (String day : days) {
      grid.put(day, new HashMap<>());
    }

    for (TimetableSlot slot : slots) {
      String day = slot.getDayOfWeek().name();
      String timeSlotId = slot.getTimeSlotDefinition().getId().toString();

      Map<String, String> slotInfo = new HashMap<>();
      slotInfo.put("subject", slot.getSubject().getName());
      slotInfo.put(
          "teacher", slot.getTeacher().getFirstName() + " " + slot.getTeacher().getLastName());
      slotInfo.put("room", slot.getRoom().getName());
      String periodicityKey = slot.getPeriodicity().name();
      slotInfo.put("periodicity", periodicityKey);
      slotInfo.put("periodicityLabel", getPeriodicityLabel(periodicityKey));

      grid.get(day).computeIfAbsent(timeSlotId, k -> new ArrayList<>()).add(slotInfo);
    }

    // Current date for the footer
    String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

    // Variable assembly
    Map<String, Object> variables = new HashMap<>();
    variables.put("title", title);
    variables.put("subtitle", subtitle);
    variables.put("days", days);
    variables.put("dayLabels", dayLabels);
    variables.put("timeSlots", timeSlotsList);
    variables.put("grid", grid);
    variables.put("currentDate", currentDate);

    return variables;
  }

  /** Translates the periodicity key to French label for the PDF. */
  private String getPeriodicityLabel(String periodicityKey) {
    return switch (periodicityKey) {
      case "WEEK_A" -> "Semaine A";
      case "WEEK_B" -> "Semaine B";
      default -> periodicityKey;
    };
  }
}
