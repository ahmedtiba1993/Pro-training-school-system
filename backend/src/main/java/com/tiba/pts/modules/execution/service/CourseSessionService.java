package com.tiba.pts.modules.execution.service;

import com.tiba.pts.core.dto.PageResponse;
import com.tiba.pts.core.exception.BusinessValidationException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.classmanagement.domain.entity.ClassGroup;
import com.tiba.pts.modules.classmanagement.domain.enums.ClassStatus;
import com.tiba.pts.modules.classmanagement.repository.ClassGroupRepository;
import com.tiba.pts.modules.execution.domain.entity.CourseSession;
import com.tiba.pts.modules.execution.domain.enums.SessionStatus;
import com.tiba.pts.modules.execution.dto.request.CourseSessionRequest;
import com.tiba.pts.modules.execution.dto.request.CourseSessionSearchRequest;
import com.tiba.pts.modules.execution.dto.request.CourseSessionUpdateRequest;
import com.tiba.pts.modules.execution.dto.response.CourseSessionResponse;
import com.tiba.pts.modules.execution.mapper.CourseSessionMapper;
import com.tiba.pts.modules.execution.repository.CourseSessionRepository;
import com.tiba.pts.modules.profiles.domain.entity.Teacher;
import com.tiba.pts.modules.profiles.domain.enums.TeacherStatus;
import com.tiba.pts.modules.profiles.repository.TeacherRepository;
import com.tiba.pts.modules.room.domain.entity.Room;
import com.tiba.pts.modules.room.domain.enums.RoomStatus;
import com.tiba.pts.modules.room.repository.RoomRepository;
import com.tiba.pts.modules.subject.domain.entity.Subject;
import com.tiba.pts.modules.subject.domain.enums.SubjectStatus;
import com.tiba.pts.modules.subject.repository.SubjectRepository;
import com.tiba.pts.modules.execution.dto.request.ForceCompleteCommand;
import com.tiba.pts.modules.execution.dto.response.CourseSessionStatsResponse;
import java.time.LocalDate;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseSessionService {

  private final CourseSessionRepository courseSessionRepository;
  private final CourseSessionMapper courseSessionMapper;
  private final ClassGroupRepository classGroupRepository;
  private final SubjectRepository subjectRepository;
  private final TeacherRepository teacherRepository;
  private final RoomRepository roomRepository;

  @Transactional
  public Long createCourseSession(CourseSessionRequest request) {
    //  Time Range Validation
    if (request.startTime().isAfter(request.endTime())
        || request.startTime().equals(request.endTime())) {
      throw new BusinessValidationException("START_TIME_MUST_BE_BEFORE_END_TIME");
    }

    // Existence & Active Check for ClassGroup
    ClassGroup classGroup =
        classGroupRepository
            .findById(request.classGroupId())
            .orElseThrow(() -> new EntityNotFoundException("CLASS_GROUP_NOT_FOUND"));
    if (classGroup.getStatus() != ClassStatus.ACTIVE) {
      throw new BusinessValidationException("CLASS_GROUP_NOT_ACTIVE");
    }

    // Existence & Active Check for Subject
    Subject subject =
        subjectRepository
            .findById(request.subjectId())
            .orElseThrow(() -> new EntityNotFoundException("SUBJECT_NOT_FOUND"));
    if (subject.getStatus() != SubjectStatus.ACTIVE) {
      throw new BusinessValidationException("SUBJECT_NOT_ACTIVE");
    }

    // Existence & Active Check for Teacher
    Teacher teacher =
        teacherRepository
            .findById(request.teacherId())
            .orElseThrow(() -> new EntityNotFoundException("TEACHER_NOT_FOUND"));
    if (teacher.getStatus() != TeacherStatus.ACTIVE) {
      throw new BusinessValidationException("TEACHER_NOT_ACTIVE");
    }

    // Existence & Active Check for Room (Optional)
    Room room = null;
    if (request.roomId() != null) {
      room =
          roomRepository
              .findById(request.roomId())
              .orElseThrow(() -> new EntityNotFoundException("ROOM_NOT_FOUND"));
      if (room.getStatus() != RoomStatus.ACTIVE) {
        throw new BusinessValidationException("ROOM_NOT_ACTIVE");
      }
    }

    // Collision Checking
    List<CourseSession> collidingSessions =
        courseSessionRepository.findCollidingSessions(
            request.sessionDate(),
            request.startTime(),
            request.endTime(),
            request.teacherId(),
            request.classGroupId(),
            request.roomId());

    for (CourseSession collision : collidingSessions) {
      if (collision.getTeacher().getId().equals(request.teacherId())) {
        throw new BusinessValidationException("TEACHER_COLLISION");
      }
      if (collision.getClassGroup().getId().equals(request.classGroupId())) {
        throw new BusinessValidationException("CLASS_GROUP_COLLISION");
      }
      if (request.roomId() != null
          && collision.getRoom() != null
          && request.roomId().equals(collision.getRoom().getId())) {
        throw new BusinessValidationException("ROOM_COLLISION");
      }
    }

    // Save Entity
    CourseSession courseSession = courseSessionMapper.toEntity(request);
    courseSession.setClassGroup(classGroup);
    courseSession.setSubject(subject);
    courseSession.setTeacher(teacher);
    courseSession.setRoom(room);
    courseSession.setStatus(SessionStatus.PLANNED);
    courseSession.setAttendanceSubmitted(false);

    return courseSessionRepository.save(courseSession).getId();
  }

  @Transactional
  public CourseSessionResponse updateCourseSession(Long id, CourseSessionUpdateRequest request) {
    CourseSession session =
        courseSessionRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("COURSE_SESSION_NOT_FOUND"));

    // Case 1: Session is CANCELED (Terminal state, immutable)
    if (session.getStatus() == SessionStatus.CANCELED) {
      throw new BusinessValidationException("CANCELED_SESSION_CANNOT_BE_MODIFIED");
    }

    // Case 2: Session is PLANNED (Almost all rights, full collision verification)
    if (session.getStatus() == SessionStatus.PLANNED) {
      // Validate time coherence
      if (request.startTime().isAfter(request.endTime())
          || request.startTime().equals(request.endTime())) {
        throw new BusinessValidationException("START_TIME_MUST_BE_BEFORE_END_TIME");
      }

      // Check active state of references
      ClassGroup classGroup =
          classGroupRepository
              .findById(request.classGroupId())
              .orElseThrow(() -> new EntityNotFoundException("CLASS_GROUP_NOT_FOUND"));
      if (classGroup.getStatus() != ClassStatus.ACTIVE) {
        throw new BusinessValidationException("CLASS_GROUP_NOT_ACTIVE");
      }

      Subject subject =
          subjectRepository
              .findById(request.subjectId())
              .orElseThrow(() -> new EntityNotFoundException("SUBJECT_NOT_FOUND"));
      if (subject.getStatus() != SubjectStatus.ACTIVE) {
        throw new BusinessValidationException("SUBJECT_NOT_ACTIVE");
      }

      Teacher teacher =
          teacherRepository
              .findById(request.teacherId())
              .orElseThrow(() -> new EntityNotFoundException("TEACHER_NOT_FOUND"));
      if (teacher.getStatus() != TeacherStatus.ACTIVE) {
        throw new BusinessValidationException("TEACHER_NOT_ACTIVE");
      }

      Room room = null;
      if (request.roomId() != null) {
        room =
            roomRepository
                .findById(request.roomId())
                .orElseThrow(() -> new EntityNotFoundException("ROOM_NOT_FOUND"));
        if (room.getStatus() != RoomStatus.ACTIVE) {
          throw new BusinessValidationException("ROOM_NOT_ACTIVE");
        }
      }

      // Run collision check
      List<CourseSession> collidingSessions =
          courseSessionRepository.findCollidingSessionsExcludingId(
              id,
              request.sessionDate(),
              request.startTime(),
              request.endTime(),
              request.teacherId(),
              request.classGroupId(),
              request.roomId());

      for (CourseSession collision : collidingSessions) {
        if (collision.getTeacher().getId().equals(request.teacherId())) {
          throw new BusinessValidationException("TEACHER_COLLISION");
        }
        if (collision.getClassGroup().getId().equals(request.classGroupId())) {
          throw new BusinessValidationException("CLASS_GROUP_COLLISION");
        }
        if (request.roomId() != null
            && collision.getRoom() != null
            && request.roomId().equals(collision.getRoom().getId())) {
          throw new BusinessValidationException("ROOM_COLLISION");
        }
      }

      // Apply modifications
      courseSessionMapper.updatePlannedEntity(request, session);
      session.setClassGroup(classGroup);
      session.setSubject(subject);
      session.setTeacher(teacher);
      session.setRoom(room);
    }
    // Case 3: Session is IN_PROGRESS (Only Room can be modified)
    else if (session.getStatus() == SessionStatus.IN_PROGRESS) {
      Room room = null;
      if (request.roomId() != null) {
        room =
            roomRepository
                .findById(request.roomId())
                .orElseThrow(() -> new EntityNotFoundException("ROOM_NOT_FOUND"));
        if (room.getStatus() != RoomStatus.ACTIVE) {
          throw new BusinessValidationException("ROOM_NOT_ACTIVE");
        }
      }

      // Validate collision for the new room
      List<CourseSession> collidingSessions =
          courseSessionRepository.findCollidingSessionsExcludingId(
              id,
              session.getSessionDate(),
              session.getStartTime(),
              session.getEndTime(),
              session.getTeacher().getId(),
              session.getClassGroup().getId(),
              request.roomId());

      for (CourseSession collision : collidingSessions) {
        if (request.roomId() != null
            && collision.getRoom() != null
            && request.roomId().equals(collision.getRoom().getId())) {
          throw new BusinessValidationException("ROOM_COLLISION");
        }
      }

      courseSessionMapper.updateInProgressEntity(request, session);
      session.setRoom(room);
    }
    // Case 4: Session is COMPLETED (Audit mode, only lessonTitle and courseContent can be
    // corrected)
    else if (session.getStatus() == SessionStatus.COMPLETED) {
      courseSessionMapper.updateCompletedEntity(request, session);
    }

    CourseSession updatedSession = courseSessionRepository.save(session);
    return courseSessionMapper.toResponse(updatedSession);
  }

  @Transactional(readOnly = true)
  public PageResponse<CourseSessionResponse> getCourseSessionsPaged(
      CourseSessionSearchRequest request, Pageable pageable) {

    // Execution of the optimized query
    Page<CourseSession> page =
        courseSessionRepository.findAllFiltered(
            request.getStartTime(),
            request.getEndTime(),
            request.getPromotionId(),
            request.getTeacherId(),
            request.getSubjectId(),
            request.getStatus(),
            request.getIsAttendanceSubmitted(),
            request.getType(),
            pageable);

    // Transformation from Entity to DTO (Mapping)
    List<CourseSessionResponse> dtoList =
        page.getContent().stream().map(courseSessionMapper::toResponse).toList();

    // Return the paginated response
    return new PageResponse<>(
        dtoList,
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isLast());
  }

  @Transactional
  public CourseSessionResponse cancelSession(Long sessionId, String reason) {
    CourseSession session =
        courseSessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("COURSE_SESSION_NOT_FOUND"));

    if (session.getStatus() == SessionStatus.COMPLETED) {
      throw new BusinessValidationException("CANNOT_CANCEL_COMPLETED_SESSION");
    }

    if (reason == null || reason.trim().isEmpty()) {
      throw new BusinessValidationException("CANCELLATION_REASON_REQUIRED");
    }

    session.setStatus(SessionStatus.CANCELED);
    session.setCancellationReason(reason.trim());

    CourseSession saved = courseSessionRepository.save(session);
    return courseSessionMapper.toResponse(saved);
  }

  @Transactional
  public CourseSessionResponse reopenSession(Long sessionId) {
    CourseSession session =
        courseSessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("COURSE_SESSION_NOT_FOUND"));

    if (session.getStatus() != SessionStatus.COMPLETED) {
      throw new BusinessValidationException("SESSION_NOT_COMPLETED");
    }

    session.setStatus(SessionStatus.IN_PROGRESS);

    CourseSession saved = courseSessionRepository.save(session);
    return courseSessionMapper.toResponse(saved);
  }

  @Transactional
  public CourseSessionResponse forceCompleteSession(Long sessionId, ForceCompleteCommand cmd) {
    CourseSession session =
        courseSessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("COURSE_SESSION_NOT_FOUND"));

    if (cmd.isAttendanceSubmitted() == null || !cmd.isAttendanceSubmitted()) {
      throw new BusinessValidationException("ATTENDANCE_MUST_BE_SUBMITTED");
    }

    String content = cmd.courseContent();
    if (content == null || content.trim().isEmpty()) {
      content = "Saisie administrative : Contenu non renseigné par l'enseignant";
    } else {
      content = content.trim();
    }

    if (cmd.lessonTitle() != null) {
      session.setLessonTitle(cmd.lessonTitle().trim());
    }
    session.setCourseContent(content);
    session.setStatus(SessionStatus.COMPLETED);
    session.setAttendanceSubmitted(true);

    CourseSession saved = courseSessionRepository.save(session);

    return courseSessionMapper.toResponse(saved);
  }

  @Transactional
  public CourseSessionResponse restoreCanceledSession(Long sessionId) {
    CourseSession session =
        courseSessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("COURSE_SESSION_NOT_FOUND"));

    if (session.getStatus() != SessionStatus.CANCELED) {
      throw new BusinessValidationException("SESSION_NOT_CANCELED");
    }

    // Anti-collision engine check
    List<CourseSession> collidingSessions =
        courseSessionRepository.findCollidingSessionsExcludingId(
            sessionId,
            session.getSessionDate(),
            session.getStartTime(),
            session.getEndTime(),
            session.getTeacher().getId(),
            session.getClassGroup().getId(),
            session.getRoom() != null ? session.getRoom().getId() : null);

    for (CourseSession collision : collidingSessions) {
      if (collision.getTeacher().getId().equals(session.getTeacher().getId())) {
        throw new BusinessValidationException("TEACHER_COLLISION_ON_RESTORE");
      }
      if (collision.getClassGroup().getId().equals(session.getClassGroup().getId())) {
        throw new BusinessValidationException("CLASS_GROUP_COLLISION_ON_RESTORE");
      }
      if (session.getRoom() != null
          && collision.getRoom() != null
          && session.getRoom().getId().equals(collision.getRoom().getId())) {
        throw new BusinessValidationException("ROOM_COLLISION_ON_RESTORE");
      }
    }

    session.setStatus(SessionStatus.PLANNED);
    session.setCancellationReason(null);

    CourseSession saved = courseSessionRepository.save(session);
    return courseSessionMapper.toResponse(saved);
  }

  @Transactional(readOnly = true)
  public CourseSessionStatsResponse getTodayStats() {
    LocalDate today = LocalDate.now();
    long activeCount =
        courseSessionRepository.countBySessionDateAndStatusNot(today, SessionStatus.CANCELED);
    long pendingCount =
        courseSessionRepository.countBySessionDateAndStatusNotAndIsAttendanceSubmittedFalse(
            today, SessionStatus.CANCELED);
    long canceledCount =
        courseSessionRepository.countBySessionDateAndStatus(today, SessionStatus.CANCELED);

    return CourseSessionStatsResponse.builder()
        .activeSessionsCount(activeCount)
        .pendingAttendanceCount(pendingCount)
        .canceledSessionsCount(canceledCount)
        .build();
  }

  @Transactional(readOnly = true)
  public CourseSessionResponse getCourseSessionById(Long id) {
    CourseSession session =
        courseSessionRepository
            .findByIdWithRelations(id)
            .orElseThrow(() -> new EntityNotFoundException("COURSE_SESSION_NOT_FOUND"));
    return courseSessionMapper.toResponse(session);
  }
}
