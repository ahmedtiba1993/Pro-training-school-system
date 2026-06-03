package com.tiba.pts.modules.execution.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.modules.classmanagement.domain.entity.ClassAssignment;
import com.tiba.pts.modules.classmanagement.domain.entity.ClassGroup;
import com.tiba.pts.modules.classmanagement.repository.ClassAssignmentRepository;
import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus;
import com.tiba.pts.modules.enrollment.repository.EnrollmentRepository;
import com.tiba.pts.modules.execution.domain.entity.AttendanceRecord;
import com.tiba.pts.modules.execution.domain.entity.CourseSession;
import com.tiba.pts.modules.execution.domain.enums.AttendanceStatus;
import com.tiba.pts.modules.execution.domain.enums.SessionStatus;
import com.tiba.pts.modules.execution.dto.request.AttendanceItemRequest;
import com.tiba.pts.modules.execution.dto.request.AttendanceRequest;
import com.tiba.pts.modules.execution.dto.response.AttendanceStatsResponse;
import com.tiba.pts.modules.execution.dto.response.StudentAttendanceResponse;
import com.tiba.pts.modules.execution.mapper.AttendanceRecordMapper;
import com.tiba.pts.modules.execution.repository.AttendanceRecordRepository;
import com.tiba.pts.modules.execution.repository.CourseSessionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AttendanceRecordServiceTest {

  @Mock
  private CourseSessionRepository courseSessionRepository;

  @Mock
  private EnrollmentRepository enrollmentRepository;

  @Mock
  private AttendanceRecordRepository attendanceRecordRepository;

  @Mock
  private ClassAssignmentRepository classAssignmentRepository;

  @Mock
  private AttendanceRecordMapper attendanceRecordMapper;

  @InjectMocks
  private AttendanceRecordService attendanceRecordService;

  private CourseSession session;
  private ClassGroup classGroup;
  private Enrollment enrollment1;
  private Enrollment enrollment2;

  @BeforeEach
  void setUp() {
    classGroup = new ClassGroup();
    classGroup.setId(20L);

    session = CourseSession.builder()
        .id(1L)
        .sessionDate(LocalDate.now())
        .startTime(LocalTime.of(8, 0))
        .endTime(LocalTime.of(10, 0))
        .status(SessionStatus.PLANNED)
        .classGroup(classGroup)
        .isAttendanceSubmitted(false)
        .attendances(new ArrayList<>())
        .build();

    enrollment1 = new Enrollment();
    enrollment1.setId(100L);

    enrollment2 = new Enrollment();
    enrollment2.setId(101L);
  }

  @Test
  void submitAttendance_success() {
    AttendanceRequest request = new AttendanceRequest(
        1L,
        false, // isDraft
        List.of(
            new AttendanceItemRequest(100L, AttendanceStatus.ABSENT),
            new AttendanceItemRequest(101L, AttendanceStatus.PRESENT)
        )
    );

    when(courseSessionRepository.findById(1L)).thenReturn(Optional.of(session));
    when(enrollmentRepository.findAllById(List.of(100L, 101L))).thenReturn(List.of(enrollment1, enrollment2));
    when(enrollmentRepository.countByIdInAndClassGroupId(List.of(100L, 101L), 20L)).thenReturn(2L);

    when(courseSessionRepository.save(any(CourseSession.class))).thenAnswer(inv -> inv.getArgument(0));

    Long responseId = attendanceRecordService.submitAttendance(request);

    assertNotNull(responseId);
    assertEquals(1L, responseId);
    assertTrue(session.isAttendanceSubmitted());
    assertEquals(SessionStatus.COMPLETED, session.getStatus());
    verify(courseSessionRepository, times(2)).save(session);
  }

  @Test
  void submitAttendance_shouldThrowException_whenSessionNotFound() {
    AttendanceRequest request = new AttendanceRequest(1L, false, List.of(new AttendanceItemRequest(100L, AttendanceStatus.ABSENT)));
    when(courseSessionRepository.findById(1L)).thenReturn(Optional.empty());

    EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
      attendanceRecordService.submitAttendance(request);
    });

    assertEquals("COURSE_SESSION_NOT_FOUND", exception.getMessage());
  }

  @Test
  void submitAttendance_shouldThrowException_whenEnrollmentNotFound() {
    AttendanceRequest request = new AttendanceRequest(1L, false, List.of(new AttendanceItemRequest(100L, AttendanceStatus.ABSENT)));
    when(courseSessionRepository.findById(1L)).thenReturn(Optional.of(session));
    when(enrollmentRepository.findAllById(List.of(100L))).thenReturn(List.of());

    EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
      attendanceRecordService.submitAttendance(request);
    });

    assertEquals("ENROLLMENT_NOT_FOUND", exception.getMessage());
  }

  @Test
  void submitAttendance_shouldThrowException_whenEnrollmentNotInSessionClassGroup() {
    AttendanceRequest request = new AttendanceRequest(1L, false, List.of(new AttendanceItemRequest(100L, AttendanceStatus.ABSENT)));
    when(courseSessionRepository.findById(1L)).thenReturn(Optional.of(session));
    when(enrollmentRepository.findAllById(List.of(100L))).thenReturn(List.of(enrollment1));
    when(enrollmentRepository.countByIdInAndClassGroupId(List.of(100L), 20L)).thenReturn(0L);

    BusinessValidationException exception = assertThrows(BusinessValidationException.class, () -> {
      attendanceRecordService.submitAttendance(request);
    });

    assertEquals("ENROLLMENTS_NOT_IN_SESSION_CLASS_GROUP", exception.getMessage());
  }

  @Test
  void getAttendanceStats_success() {
    when(courseSessionRepository.findById(1L)).thenReturn(Optional.of(session));
    when(classAssignmentRepository.countByClassGroupIdAndEnrollmentStatusIn(
        20L, List.of(EnrollmentStatus.VALIDATED, EnrollmentStatus.CONDITIONALLY_VALIDATED))).thenReturn(2L);
    when(attendanceRecordRepository.countByCourseSessionId(1L)).thenReturn(1L);

    AttendanceStatsResponse stats = attendanceRecordService.getAttendanceStats(1L);

    assertNotNull(stats);
    assertEquals(2L, stats.getTotalEffectif());
    assertEquals(1L, stats.getTotalAbsents());
    assertEquals(1L, stats.getTotalPresents());
  }

  @Test
  void getAttendanceStats_shouldThrowException_whenSessionNotFound() {
    when(courseSessionRepository.findById(1L)).thenReturn(Optional.empty());

    EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
      attendanceRecordService.getAttendanceStats(1L);
    });

    assertEquals("COURSE_SESSION_NOT_FOUND", exception.getMessage());
  }

  @Test
  void getStudentsAttendanceForSession_success() {
    session.setSessionDate(LocalDate.of(2026, 6, 2));

    // Student 1: Validated, created before session -> Included (Absent)
    enrollment1.setStatus(EnrollmentStatus.VALIDATED);
    enrollment1.setCreatedDate(LocalDate.of(2026, 6, 1).atStartOfDay());
    com.tiba.pts.modules.profiles.domain.entity.Student s1 = com.tiba.pts.modules.profiles.domain.entity.Student.builder()
        .firstName("Jean")
        .lastName("Dupont")
        .studentCode("STU01")
        .build();
    enrollment1.setStudent(s1);

    // Student 2: Conditionally Validated, created before session -> Included (Present by default)
    enrollment2.setStatus(EnrollmentStatus.CONDITIONALLY_VALIDATED);
    enrollment2.setCreatedDate(LocalDate.of(2026, 5, 30).atStartOfDay());
    com.tiba.pts.modules.profiles.domain.entity.Student s2 = com.tiba.pts.modules.profiles.domain.entity.Student.builder()
        .firstName("Marie")
        .lastName("Curie")
        .studentCode("STU02")
        .build();
    enrollment2.setStudent(s2);

    // Student 3: Pre-enrolled -> Excluded
    Enrollment enrollment3 = new Enrollment();
    enrollment3.setId(102L);
    enrollment3.setStatus(EnrollmentStatus.PRE_ENROLLED);
    enrollment3.setCreatedDate(LocalDate.of(2026, 6, 1).atStartOfDay());

    // Student 4: Validated, but created on/after session date -> Excluded
    Enrollment enrollment4 = new Enrollment();
    enrollment4.setId(103L);
    enrollment4.setStatus(EnrollmentStatus.VALIDATED);
    enrollment4.setCreatedDate(LocalDate.of(2026, 6, 2).atStartOfDay());

    ClassAssignment assignment1 = ClassAssignment.builder().classGroup(classGroup).enrollment(enrollment1).build();
    ClassAssignment assignment2 = ClassAssignment.builder().classGroup(classGroup).enrollment(enrollment2).build();
    ClassAssignment assignment3 = ClassAssignment.builder().classGroup(classGroup).enrollment(enrollment3).build();
    ClassAssignment assignment4 = ClassAssignment.builder().classGroup(classGroup).enrollment(enrollment4).build();

    AttendanceRecord record1 = AttendanceRecord.builder()
        .enrollment(enrollment1)
        .status(AttendanceStatus.ABSENT)
        .build();

    when(courseSessionRepository.findById(1L)).thenReturn(Optional.of(session));
    when(classAssignmentRepository.findByClassGroupId(20L)).thenReturn(List.of(assignment1, assignment2, assignment3, assignment4));
    when(attendanceRecordRepository.findByCourseSessionId(1L)).thenReturn(List.of(record1));

    List<StudentAttendanceResponse> result = attendanceRecordService.getStudentsAttendanceForSession(1L);

    assertNotNull(result);
    assertEquals(2, result.size());

    // Check Student 1
    StudentAttendanceResponse res1 = result.stream().filter(r -> r.getEnrollmentId().equals(100L)).findFirst().orElseThrow();
    assertEquals("Jean", res1.getFirstName());
    assertEquals("STU01", res1.getStudentCode());
    assertEquals("ABSENT", res1.getStatus());

    // Check Student 2
    StudentAttendanceResponse res2 = result.stream().filter(r -> r.getEnrollmentId().equals(101L)).findFirst().orElseThrow();
    assertEquals("Marie", res2.getFirstName());
    assertEquals("STU02", res2.getStudentCode());
    assertEquals("PRESENT", res2.getStatus());
  }

  @Test
  void submitAttendance_shouldThrowException_whenSessionIsCompleted() {
    AttendanceRequest request = new AttendanceRequest(1L, false, List.of(new AttendanceItemRequest(100L, AttendanceStatus.ABSENT)));
    session.setStatus(SessionStatus.COMPLETED);
    when(courseSessionRepository.findById(1L)).thenReturn(Optional.of(session));

    BusinessValidationException exception = assertThrows(BusinessValidationException.class, () -> {
      attendanceRecordService.submitAttendance(request);
    });

    assertEquals("CANNOT_SUBMIT_ATTENDANCE_FOR_COMPLETED_SESSION", exception.getMessage());
  }
}
