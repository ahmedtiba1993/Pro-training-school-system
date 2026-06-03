import {Component, OnInit, signal, computed, effect, inject} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {HttpClient} from '@angular/common/http';
import {Router} from '@angular/router';
import {
  CourseSessionControllerService,
  PromotionControllerService,
  TeacherControllerService,
  SubjectControllerService,
  RoomControllerService,
  ClassGroupControllerService,
  ActiveClassGroupResponse,
  CourseSessionResponse,
  CourseSessionSearchRequest,
  CourseSessionUpdateRequest,
  CourseSessionRequest,
  Pageable,
  PromotionLookupResponse,
  TeacherSimpleResponse,
  SubjectResponse,
  RoomResponse,
  ForceCompleteCommand
} from '../../../../../core/api';
import {PaginationComponent} from '../../../../../shared/components/pagination/pagination';
import {ToastService} from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-course-session',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './course-session.html',
  styleUrl: './course-session.css'
})
export class CourseSession implements OnInit {
  // --- API SERVICES INJECTIONS ---
  private courseSessionService = inject(CourseSessionControllerService);
  private classGroupService = inject(ClassGroupControllerService);
  private teacherService = inject(TeacherControllerService);
  private subjectService = inject(SubjectControllerService);
  private roomService = inject(RoomControllerService);
  private toastService = inject(ToastService);
  private http = inject(HttpClient);
  private router = inject(Router);

  // --- LISTS & BACKEND DATA STATE ---
  sessions = signal<CourseSessionResponse[]>([]);
  isLoading = signal<boolean>(false);

  // Stats
  activeSessionsCount = signal<number>(0);
  pendingAttendanceCount = signal<number>(0);
  canceledSessionsCount = signal<number>(0);

  // --- INPUT FILTERS STATE (INPUT STATES) ---
  inputDateStart = signal<string>('');
  inputDateEnd = signal<string>('');
  inputPromotionId = signal<number | null>(null);
  inputTeacherSearch = signal<string>('');
  inputTeacherId = signal<number | null>(null);
  inputSubjectId = signal<number | null>(null);
  inputStatus = signal<string>('ALL');
  inputAttendanceFilter = signal<string>('ALL');

  // --- APPLIED FILTERS STATE (APPLIED STATES) ---
  appliedDateStart = signal<string>('');
  appliedDateEnd = signal<string>('');
  appliedPromotionId = signal<number | null>(null);
  appliedTeacherId = signal<number | null>(null);
  appliedSubjectId = signal<number | null>(null);
  appliedStatus = signal<string>('ALL');
  appliedAttendanceFilter = signal<string>('ALL');

  // Dropdowns visual toggles
  isStatusDropdownOpen = signal<boolean>(false);
  isTeacherSuggestionsOpen = signal<boolean>(false);

  // --- REFERENCE CODES (LOOKUPS) ---
  activeClasses = signal<ActiveClassGroupResponse[]>([]);
  teachersList = signal<TeacherSimpleResponse[]>([]);
  subjects = signal<SubjectResponse[]>([]);
  rooms = signal<RoomResponse[]>([]);

  // --- PAGINATION STATE ---
  page = signal<number>(0);
  size = signal<number>(20);
  totalElements = signal<number>(0);
  totalPages = signal<number>(0);

  // --- MODALS STATE ---
  isModalOpen = signal<boolean>(false);
  selectedSession = signal<CourseSessionResponse | null>(null);
  isDetailsLoading = signal<boolean>(false);

  // Mode Édition
  isEditMode = signal<boolean>(false);
  isSavingEdit = signal<boolean>(false);

  // Formulaire d'édition
  editSessionDate = signal<string>('');
  editStartTime = signal<string>('');
  editEndTime = signal<string>('');
  editSessionType = signal<string>('');
  editTeacherId = signal<number | null>(null);
  editRoomId = signal<number | null>(null);
  editLessonTitle = signal<string>('');
  editCourseContent = signal<string>('');

  // Mode Création
  isCreateModalOpen = signal<boolean>(false);
  isSavingCreate = signal<boolean>(false);

  // Formulaire de création
  createSessionDate = signal<string>('');
  createStartTime = signal<string>('');
  createEndTime = signal<string>('');
  createSessionType = signal<string>('REGULAR');
  createLessonTitle = signal<string>('');
  createCourseContent = signal<string>('');
  createClassGroupId = signal<number | null>(null);
  createSubjectId = signal<number | null>(null);
  createTeacherId = signal<number | null>(null);
  createRoomId = signal<number | null>(null);

  // Action confirmation modal
  isConfirmModalOpen = signal<boolean>(false);
  isConfirmingAction = signal<boolean>(false);
  confirmActionType = signal<'CANCEL' | 'RESTORE' | 'REOPEN' | 'FORCE_COMPLETE' | null>(null);
  confirmCancelReason = signal<string>('');
  confirmForceAttendance = signal<boolean>(false);
  confirmForceLessonTitle = signal<string>('');
  confirmForceCourseContent = signal<string>('');

  // Modification actions on a session (Obsolete but kept for temporary compatibility)
  isCancelling = signal<boolean>(false);
  cancellationReasonInput = signal<string>('');

  // --- LOCALLY CALCULATED STATS (Absences) ---
  statDailyAbsences = computed(() => {
    const todayStr = '2026-06-01'; // Pivot date
    const todays = this.sessions().filter(s => s.sessionDate === todayStr);
    let absences = 0;
    todays.forEach(s => {
      if (s.status === 'COMPLETED' && s.attendanceSubmitted) {
        absences += 2;
      }
    });
    return absences;
  });

  constructor() {
  }

  ngOnInit(): void {
    this.initializeDefaultDates();
    this.loadLookups();
    this.loadStats();
    this.loadCourseSessions();
  }

  // --- INITIALIZATION OF DEFAULT DATES ---
  initializeDefaultDates(): void {
    // Initialize inputs as empty by default
    this.inputDateStart.set('');
    this.inputDateEnd.set('');

    // Also initialize applied filters by default as empty
    this.appliedDateStart.set('');
    this.appliedDateEnd.set('');
  }

  // --- LOOKUPS LOADING ---
  loadLookups(): void {
    this.classGroupService.getAllActiveClasses().subscribe({
      next: (res: any) => {
        if (res.success && res.data) this.activeClasses.set(res.data);
      },
      error: (err: any) => console.error('Erreur chargement classes actives:', err)
    });

    this.teacherService.getActiveTeachers().subscribe({
      next: (res: any) => {
        if (res.success && res.data) this.teachersList.set(res.data);
      },
      error: (err: any) => console.error('Erreur chargement enseignants:', err)
    });

    this.subjectService.getAllSubjects().subscribe({
      next: (res: any) => {
        if (res.success && res.data) this.subjects.set(res.data);
      },
      error: (err: any) => console.error('Erreur chargement matières:', err)
    });

    this.roomService.getActiveRooms().subscribe({
      next: (res: any) => {
        if (res.success && res.data) this.rooms.set(res.data);
      },
      error: (err: any) => console.error('Erreur chargement salles:', err)
    });
  }

  // --- STATISTICS LOADING ---
  loadStats(): void {
    this.courseSessionService.getTodayStats().subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.activeSessionsCount.set(res.data.activeSessionsCount ?? 0);
          this.pendingAttendanceCount.set(res.data.pendingAttendanceCount ?? 0);
          this.canceledSessionsCount.set(res.data.canceledSessionsCount ?? 0);
        }
      },
      error: (err: any) => console.error('Erreur stats backend:', err)
    });
  }

  // --- SESSIONS LOADING ---
  loadCourseSessions(): void {
    this.isLoading.set(true);

    const searchRequest: CourseSessionSearchRequest = {};

    if (this.appliedDateStart()) {
      searchRequest.startTime = this.appliedDateStart();
    }
    if (this.appliedDateEnd()) {
      searchRequest.endTime = this.appliedDateEnd();
    }
    if (this.appliedPromotionId() !== null) {
      searchRequest.promotionId = this.appliedPromotionId()!;
    }
    if (this.appliedTeacherId() !== null) {
      searchRequest.teacherId = this.appliedTeacherId()!;
    }
    if (this.appliedSubjectId() !== null) {
      searchRequest.subjectId = this.appliedSubjectId()!;
    }
    if (this.appliedStatus() !== 'ALL') {
      searchRequest.status = this.appliedStatus() as CourseSessionSearchRequest.StatusEnum;
    }
    if (this.appliedAttendanceFilter() === 'MARKED') {
      searchRequest.isAttendanceSubmitted = true;
    } else if (this.appliedAttendanceFilter() === 'MISSING') {
      searchRequest.isAttendanceSubmitted = false;
    }

    const pageable: Pageable = {
      page: this.page(),
      size: this.size(),
      sort: ['sessionDate,asc', 'startTime,asc']
    };

    this.courseSessionService.getAllCourseSessions(searchRequest, pageable).subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          this.sessions.set(res.data.content || []);
          this.totalElements.set(res.data.totalElements || 0);
          this.totalPages.set(res.data.totalPages || 0);
        }
        this.isLoading.set(false);
      },
      error: (err: any) => {
        console.error('Erreur chargement des séances:', err);
        this.isLoading.set(false);
        this.sessions.set([]);
      }
    });
  }

  // --- AUTO-COMPLETION ENSEIGNANTS LOCALEMENT ---
  matchedTeachers = computed(() => {
    const query = this.inputTeacherSearch().toLowerCase().trim();
    if (!query) return [];
    return this.teachersList().filter(t =>
      `${t.firstName} ${t.lastName}`.toLowerCase().includes(query)
    );
  });

  onTeacherInput(): void {
    this.isTeacherSuggestionsOpen.set(true);
    if (!this.inputTeacherSearch().trim()) {
      this.inputTeacherId.set(null);
    }
  }

  selectTeacher(teacher: TeacherSimpleResponse): void {
    this.inputTeacherSearch.set(`${teacher.firstName} ${teacher.lastName}`);
    this.inputTeacherId.set(teacher.id ?? null);
    this.isTeacherSuggestionsOpen.set(false);
  }

  clearTeacherSearch(): void {
    this.inputTeacherSearch.set('');
    this.inputTeacherId.set(null);
    this.isTeacherSuggestionsOpen.set(false);
  }

  // --- GESTION DU TOGGLE STATUT UNIQUE ---
  inputStatusLabel = computed(() => {
    const status = this.inputStatus();
    if (status === 'ALL') return 'Tous les statuts';
    if (status === 'PLANNED') return 'Planifié';
    if (status === 'IN_PROGRESS') return 'En cours';
    if (status === 'COMPLETED') return 'Dispensé';
    if (status === 'CANCELED') return 'Annulé';
    return status;
  });

  toggleStatusDropdown(): void {
    this.isStatusDropdownOpen.update(val => !val);
  }

  selectStatus(status: string): void {
    this.inputStatus.set(status);
    this.isStatusDropdownOpen.set(false);
  }

  setAttendanceFilter(mode: string): void {
    this.inputAttendanceFilter.set(mode);
  }

  // --- CLIC SUR LE BOUTON RECHERCHER (APPLICATIONS DES FILTRES) ---
  applyFilters(): void {
    this.appliedDateStart.set(this.inputDateStart());
    this.appliedDateEnd.set(this.inputDateEnd());
    this.appliedPromotionId.set(this.inputPromotionId());
    this.appliedTeacherId.set(this.inputTeacherId());
    this.appliedSubjectId.set(this.inputSubjectId());
    this.appliedStatus.set(this.inputStatus());
    this.appliedAttendanceFilter.set(this.inputAttendanceFilter());
    this.page.set(0); // Reset pagination
    this.loadCourseSessions();
  }

  // --- CLICK ON RESET BUTTON ---
  resetFilters(): void {
    this.initializeDefaultDates();
    this.inputPromotionId.set(null);
    this.inputTeacherSearch.set('');
    this.inputTeacherId.set(null);
    this.inputSubjectId.set(null);
    this.inputStatus.set('ALL');
    this.inputAttendanceFilter.set('ALL');

    // Immediately apply the reset empty filters
    this.applyFilters();
  }

  onPageChange(newPage: number): void {
    this.page.set(newPage);
    this.loadCourseSessions();
  }

  // --- MODALS MANAGEMENT ---
  showSessionDetails(session: CourseSessionResponse): void {
    if (!session.id) return;

    this.selectedSession.set(null);
    this.isModalOpen.set(true);
    this.isDetailsLoading.set(true);
    this.isCancelling.set(false);
    this.cancellationReasonInput.set('');

    const apiService = this.courseSessionService as any;
    if (typeof apiService.getCourseSessionById === 'function') {
      apiService.getCourseSessionById(session.id).subscribe({
        next: (res: any) => {
          if (res && res.success && res.data) {
            this.selectedSession.set(res.data);
          } else {
            this.selectedSession.set(session);
          }
          this.isDetailsLoading.set(false);
        },
        error: (err: any) => {
          console.error('Erreur lors du chargement des détails via service:', err);
          this.selectedSession.set(session);
          this.isDetailsLoading.set(false);
        }
      });
    } else {
      // Direct fallback with HttpClient if the API has not yet been regenerated locally
      this.http.get<any>(`/api/v1/course-sessions/${session.id}`).subscribe({
        next: (res: any) => {
          if (res && res.data) {
            this.selectedSession.set(res.data);
          } else {
            this.selectedSession.set(session);
          }
          this.isDetailsLoading.set(false);
        },
        error: (err: any) => {
          console.error('Erreur lors du chargement des détails via HTTP:', err);
          this.selectedSession.set(session);
          this.isDetailsLoading.set(false);
        }
      });
    }
  }

  closeSessionModal(): void {
    this.isModalOpen.set(false);
    setTimeout(() => {
      this.selectedSession.set(null);
      this.isCancelling.set(false);
      this.cancellationReasonInput.set('');
      this.isEditMode.set(false);
    }, 200);
  }

  // --- BACKEND ACTIONS ON SESSIONS AND CONFIRMATION MODALS ---
  openConfirmModal(type: 'CANCEL' | 'RESTORE' | 'REOPEN' | 'FORCE_COMPLETE'): void {
    this.confirmActionType.set(type);
    this.confirmCancelReason.set('');
    this.confirmForceAttendance.set(false);
    this.confirmForceLessonTitle.set('');
    this.confirmForceCourseContent.set('');
    this.isConfirmingAction.set(false);
    this.isConfirmModalOpen.set(true);
  }

  closeConfirmModal(): void {
    if (this.isConfirmingAction()) return;
    this.isConfirmModalOpen.set(false);
    this.confirmActionType.set(null);
  }

  executeConfirmAction(): void {
    const session = this.selectedSession();
    if (!session || !session.id) return;

    const type = this.confirmActionType();
    if (!type) return;

    this.isConfirmingAction.set(true);

    const handleError = (err: any, msg: string) => {
      console.error(err);
      this.toastService.error(msg);
      this.isConfirmingAction.set(false);
    };

    if (type === 'CANCEL') {
      const reason = this.confirmCancelReason().trim();
      if (!reason) {
        this.toastService.error("Veuillez spécifier un motif d'annulation.");
        this.isConfirmingAction.set(false);
        return;
      }
      this.courseSessionService.cancelSession(session.id, reason).subscribe({
        next: (res: any) => {
          this.toastService.success('La séance a été annulée.');
          this.onActionSuccess(res?.data || res);
        },
        error: (err: any) => handleError(err, "Erreur lors de l'annulation de la séance.")
      });
    } else if (type === 'RESTORE') {
      this.courseSessionService.restoreCanceledSession(session.id).subscribe({
        next: (res: any) => {
          this.toastService.success('La séance a été restaurée.');
          this.onActionSuccess(res?.data || res);
        },
        error: (err: any) => handleError(err, 'Erreur lors de la restauration.')
      });
    } else if (type === 'REOPEN') {
      this.courseSessionService.reopenSession(session.id).subscribe({
        next: (res: any) => {
          this.toastService.success('La séance a été réouverte.');
          this.onActionSuccess(res?.data || res);
        },
        error: (err: any) => handleError(err, 'Erreur lors de la réouverture.')
      });
    } else if (type === 'FORCE_COMPLETE') {
      const cmd: ForceCompleteCommand = {
        isAttendanceSubmitted: this.confirmForceAttendance(),
        lessonTitle: this.confirmForceLessonTitle().trim() || undefined,
        courseContent: this.confirmForceCourseContent().trim() || undefined
      };
      this.courseSessionService.forceCompleteSession(session.id, cmd).subscribe({
        next: (res: any) => {
          this.toastService.success('La séance a été clôturée avec succès.');
          this.onActionSuccess(res?.data || res);
        },
        error: (err: any) => handleError(err, 'Erreur lors de la complétion forcée.')
      });
    }
  }

  private onActionSuccess(updatedData?: any): void {
    this.isConfirmingAction.set(false);
    this.closeConfirmModal();
    this.loadStats();

    if (updatedData) {
      // Met à jour instantanément les données du popup de détails sans faire d'appel GET supplémentaire
      this.selectedSession.set(updatedData);
      this.loadCourseSessions();
    } else {
      // Fallback in case the API does not return the data
      const currentSession = this.selectedSession();
      if (currentSession && currentSession.id) {
        const apiService = this.courseSessionService as any;
        const id = currentSession.id;
        this.isDetailsLoading.set(true);

        const refreshCallback = (res: any) => {
          if (res && res.data) {
            this.selectedSession.set(res.data);
          } else if (res && res.success === undefined) {
            this.selectedSession.set(res);
          }
          this.isDetailsLoading.set(false);
          this.loadCourseSessions();
        };

        if (typeof apiService.getCourseSessionById === 'function') {
          apiService.getCourseSessionById(id).subscribe({
            next: refreshCallback,
            error: () => {
              this.isDetailsLoading.set(false);
              this.loadCourseSessions();
            }
          });
        } else {
          this.http.get<any>(`/api/v1/course-sessions/${id}`).subscribe({
            next: refreshCallback,
            error: () => {
              this.isDetailsLoading.set(false);
              this.loadCourseSessions();
            }
          });
        }
      } else {
        this.closeSessionModal();
        this.loadCourseSessions();
      }
    }
  }

  // --- BACKEND ACTIONS ON SESSIONS (Obsolete - Kept for compatibility) ---
  cancelSession(id: number): void {
    this.openConfirmModal('CANCEL');
  }

  restoreSession(id: number): void {
    this.openConfirmModal('RESTORE');
  }

  reopenSession(id: number): void {
    this.openConfirmModal('REOPEN');
  }

  startCancellation(): void {
    this.openConfirmModal('CANCEL');
  }

  cancelCancellation(): void {
  }

  // --- EDITION / MODIFICATION LOGIC ---
  isFieldEditable(fieldName: string): boolean {
    const session = this.selectedSession();
    if (!session || !this.isEditMode()) return false;

    const status = session.status;
    if (status === 'PLANNED') {
      return ['sessionDate', 'startTime', 'endTime', 'teacherId', 'roomId'].includes(fieldName);
    }
    if (status === 'IN_PROGRESS') {
      return fieldName === 'roomId';
    }
    if (status === 'COMPLETED') {
      return ['lessonTitle', 'courseContent'].includes(fieldName);
    }
    return false;
  }

  startEditMode(): void {
    const session = this.selectedSession();
    if (!session) return;

    this.editSessionDate.set(session.sessionDate || '');
    this.editStartTime.set(session.startTime || '');
    this.editEndTime.set(session.endTime || '');
    this.editSessionType.set(session.sessionType || 'REGULAR');
    this.editTeacherId.set(session.teacherId ?? null);
    this.editRoomId.set(session.roomId ?? null);
    this.editLessonTitle.set(session.lessonTitle || '');
    this.editCourseContent.set(session.courseContent || '');

    this.isEditMode.set(true);
  }

  cancelEditMode(): void {
    this.isEditMode.set(false);
  }

  saveCourseSession(): void {
    const session = this.selectedSession();
    if (!session || !session.id) return;

    if (!this.editSessionDate()) {
      this.toastService.error("La date de séance est obligatoire.");
      return;
    }
    if (!this.editStartTime() || !this.editEndTime()) {
      this.toastService.error("Les heures de début et de fin sont obligatoires.");
      return;
    }
    if (!this.editTeacherId()) {
      this.toastService.error("L'enseignant est obligatoire.");
      return;
    }

    this.isSavingEdit.set(true);

    const request: CourseSessionUpdateRequest = {
      sessionDate: this.editSessionDate(),
      startTime: this.editStartTime(),
      endTime: this.editEndTime(),
      sessionType: this.editSessionType() as CourseSessionUpdateRequest.SessionTypeEnum,
      lessonTitle: this.editLessonTitle().trim() || undefined,
      courseContent: this.editCourseContent().trim() || undefined,
      classGroupId: session.classGroupId!,
      subjectId: session.subjectId!,
      teacherId: this.editTeacherId()!,
      roomId: this.editRoomId() ?? undefined
    };

    this.courseSessionService.updateCourseSession(session.id, request).subscribe({
      next: (res: any) => {
        this.toastService.success('La séance a été modifiée avec succès.');
        this.isSavingEdit.set(false);
        this.isEditMode.set(false);
        this.onActionSuccess(res?.data || res);
      },
      error: (err: any) => {
        console.error(err);
        this.toastService.error("Erreur lors de la modification de la séance.");
        this.isSavingEdit.set(false);
      }
    });
  }

  openCreateModal(): void {
    const today = new Date().toISOString().split('T')[0];
    this.createSessionDate.set(today);
    this.createStartTime.set('');
    this.createEndTime.set('');
    this.createSessionType.set('REGULAR');
    this.createLessonTitle.set('');
    this.createCourseContent.set('');
    this.createClassGroupId.set(null);
    this.createSubjectId.set(null);
    this.createTeacherId.set(null);
    this.createRoomId.set(null);

    this.isCreateModalOpen.set(true);
  }

  closeCreateModal(): void {
    this.isCreateModalOpen.set(false);
  }

  saveNewCourseSession(): void {
    if (!this.createSessionDate()) {
      this.toastService.error("La date de séance est obligatoire.");
      return;
    }
    if (!this.createStartTime() || !this.createEndTime()) {
      this.toastService.error("Les heures de début et de fin sont obligatoires.");
      return;
    }
    if (!this.createSessionType()) {
      this.toastService.error("Le type de séance est obligatoire.");
      return;
    }
    if (!this.createClassGroupId()) {
      this.toastService.error("La classe est obligatoire.");
      return;
    }
    if (!this.createSubjectId()) {
      this.toastService.error("La matière est obligatoire.");
      return;
    }
    if (!this.createTeacherId()) {
      this.toastService.error("L'enseignant est obligatoire.");
      return;
    }

    this.isSavingCreate.set(true);

    let startTimeVal = this.createStartTime();
    if (startTimeVal && startTimeVal.length === 5) {
      startTimeVal += ':00';
    }
    let endTimeVal = this.createEndTime();
    if (endTimeVal && endTimeVal.length === 5) {
      endTimeVal += ':00';
    }

    const request: CourseSessionRequest = {
      sessionDate: this.createSessionDate(),
      startTime: startTimeVal,
      endTime: endTimeVal,
      sessionType: this.createSessionType() as CourseSessionRequest.SessionTypeEnum,
      lessonTitle: this.createLessonTitle().trim() || undefined,
      courseContent: this.createCourseContent().trim() || undefined,
      classGroupId: this.createClassGroupId()!,
      subjectId: this.createSubjectId()!,
      teacherId: this.createTeacherId()!,
      roomId: this.createRoomId() ?? undefined
    };

    this.courseSessionService.createCourseSession(request).subscribe({
      next: () => {
        this.toastService.success('La séance de cours a été créée avec succès.');
        this.isSavingCreate.set(false);
        this.closeCreateModal();
        this.loadStats();
        this.loadCourseSessions();
      },
      error: (err: any) => {
        console.error(err);
        this.toastService.error("Erreur lors de la création de la séance de cours.");
        this.isSavingCreate.set(false);
      }
    });
  }

  goToAbsences(sessionId: number): void {
    this.closeSessionModal();
    this.router.navigate(['/admin/execution/course-sessions', sessionId, 'absences']);
  }

  printReport(): void {
    window.print();
  }
}
