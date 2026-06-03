package com.tiba.pts.modules.execution.service;

import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.enrollment.domain.entity.Enrollment;
import com.tiba.pts.modules.enrollment.repository.EnrollmentRepository;
import com.tiba.pts.modules.execution.domain.entity.AttendanceRecord;
import com.tiba.pts.modules.execution.domain.entity.CourseSession;
import com.tiba.pts.modules.execution.domain.enums.SessionStatus;
import com.tiba.pts.modules.execution.dto.request.AttendanceRequest;
import com.tiba.pts.modules.execution.dto.response.AttendanceRecordResponse;
import com.tiba.pts.modules.execution.mapper.AttendanceRecordMapper;
import com.tiba.pts.modules.execution.repository.AttendanceRecordRepository;
import com.tiba.pts.modules.execution.repository.CourseSessionRepository;
import jakarta.persistence.EntityNotFoundException;
import com.tiba.pts.modules.classmanagement.repository.ClassAssignmentRepository;
import com.tiba.pts.modules.classmanagement.domain.entity.ClassAssignment;
import com.tiba.pts.modules.enrollment.domain.enums.EnrollmentStatus;
import com.tiba.pts.modules.execution.domain.enums.AttendanceStatus;
import com.tiba.pts.modules.execution.dto.response.AttendanceStatsResponse;
import com.tiba.pts.modules.execution.dto.response.StudentAttendanceResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class AttendanceRecordService {

  private final CourseSessionRepository courseSessionRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final AttendanceRecordRepository attendanceRecordRepository;
  private final ClassAssignmentRepository classAssignmentRepository;

  @Transactional
  public Long submitAttendance(AttendanceRequest request) {
    // Verify courseSessionId exists
    CourseSession session =
        courseSessionRepository
            .findById(request.courseSessionId())
            .orElseThrow(() -> new EntityNotFoundException("COURSE_SESSION_NOT_FOUND"));

    if (session.getStatus() == SessionStatus.COMPLETED) {
      throw new BusinessValidationException("CANNOT_SUBMIT_ATTENDANCE_FOR_COMPLETED_SESSION");
    }

    // Extract enrollment IDs
    List<Long> enrollmentIds =
        request.items().stream().map(item -> item.enrollmentId()).distinct().toList();

    // Fetch all enrollments in one batch and verify count
    List<Enrollment> enrollments = enrollmentRepository.findAllById(enrollmentIds);
    if (enrollments.size() != enrollmentIds.size()) {
      throw new ResourceNotFoundException("ENROLLMENT_NOT_FOUND");
    }

    // Also verify if these enrollments are in the same class
    long countInClass =
        enrollmentRepository.countByIdInAndClassGroupId(
            enrollmentIds, session.getClassGroup().getId());
    if (countInClass != enrollmentIds.size()) {
      throw new BusinessValidationException("ENROLLMENTS_NOT_IN_SESSION_CLASS_GROUP");
    }

    // Delete old absences from this session (Drop & Replace)
    session.clearAttendances();

    // 5. Save new records
    List<AttendanceRecord> newRecords =
        request.items().stream()
            .map(
                item -> {
                  // Hibernate creates a fake object (Proxy) without hitting the database
                  Enrollment proxyEnrollment =
                      enrollmentRepository.getReferenceById(item.enrollmentId());

                  return AttendanceRecord.builder()
                      .courseSession(session)
                      .enrollment(proxyEnrollment)
                      .status(item.status())
                      .build();
                })
            .toList();

    session.getAttendances().addAll(newRecords);

    // session.setAttendanceSubmitted(true)
    if (request.isDraft()) {
      session.setStatus(SessionStatus.IN_PROGRESS);
      session.setAttendanceSubmitted(false);
    } else {
      session.setStatus(SessionStatus.COMPLETED);
      session.setAttendanceSubmitted(true);
    }

    return courseSessionRepository.save(session).getId();
  }

  @Transactional(readOnly = true)
  public AttendanceStatsResponse getAttendanceStats(Long courseSessionId) {
    CourseSession session =
        courseSessionRepository
            .findById(courseSessionId)
            .orElseThrow(() -> new EntityNotFoundException("COURSE_SESSION_NOT_FOUND"));

    long totalEffectif =
        classAssignmentRepository.countByClassGroupIdAndEnrollmentStatusIn(
            session.getClassGroup().getId(),
            List.of(EnrollmentStatus.VALIDATED, EnrollmentStatus.CONDITIONALLY_VALIDATED));
    long totalAbsents = attendanceRecordRepository.countByCourseSessionId(courseSessionId);
    long totalPresents = totalEffectif - totalAbsents;

    return AttendanceStatsResponse.builder()
        .totalEffectif(totalEffectif)
        .totalAbsents(totalAbsents)
        .totalPresents(totalPresents)
        .build();
  }

  @Transactional(readOnly = true)
  public List<StudentAttendanceResponse> getStudentsAttendanceForSession(Long courseSessionId) {

    // Find the session
    CourseSession session =
        courseSessionRepository
            .findById(courseSessionId)
            .orElseThrow(() -> new EntityNotFoundException("COURSE_SESSION_NOT_FOUND"));

    // Find eligible students in a SINGLE QUERY (Filtered by date and status!)
    List<StudentAttendanceResponse> students =
        attendanceRecordRepository.findEligibleStudentsForCallSheet(
            session.getClassGroup().getId(), session.getSessionDate());

    // Retrieve the absences of this session
    List<AttendanceRecord> attendanceRecords =
        attendanceRecordRepository.findByCourseSessionId(courseSessionId);

    // Create the ultra-fast Map (Your excellent idea)
    Map<Long, String> attendanceMap =
        attendanceRecords.stream()
            .collect(
                Collectors.toMap(
                    record -> record.getEnrollment().getId(), record -> record.getStatus().name()));

    // Apply absences to the list of students
    for (StudentAttendanceResponse student : students) {
      String dbStatus = attendanceMap.get(student.getEnrollmentId());
      if (dbStatus != null) {
        student.setStatus(dbStatus);
      }
    }

    return students;
  }
}
