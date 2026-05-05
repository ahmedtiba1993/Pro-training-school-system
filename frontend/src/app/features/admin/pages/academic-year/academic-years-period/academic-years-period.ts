import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe, NgClass } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastService } from '../../../../../shared/services/toast.service';
import {
  AcademicYearControllerService,
  PeriodControllerService,
  PeriodRequest,
  PeriodResponse,
  AcademicYearResponse,
  ExamSessionControllerService,
  ExamSessionRequest,
  ExamSessionResponse
} from '../../../../../core/api';

@Component({
  selector: 'app-academic-years-period',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, DatePipe, NgClass],
  templateUrl: './academic-years-period.html',
  styleUrl: './academic-years-period.css'
})
export class AcademicYearsPeriod implements OnInit {
  private route = inject(ActivatedRoute);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);

  // --- API SERVICES ---
  private academicYearApi = inject(AcademicYearControllerService);
  private periodApi = inject(PeriodControllerService);
  private examSessionApi = inject(ExamSessionControllerService);

  // --- GLOBAL STATES ---
  academicYearId = signal<number | null>(null);
  academicYear = signal<AcademicYearResponse | null>(null);
  periods = signal<PeriodResponse[]>([]);

  isLoading = signal<boolean>(true);
  isSaving = signal<boolean>(false);
  errorMessage = signal<string>('');

  // --- PERIOD MODAL STATES ---
  showPeriodModal = signal<boolean>(false);
  editingPeriodId = signal<number | null>(null);

  // --- EXAM SESSION MODAL STATES ---
  showSessionModal = signal<boolean>(false);
  selectedPeriodIdForSession = signal<number | null>(null);
  editingSessionId = signal<number | null>(null);

  // --- TOGGLE LOCK (PERIOD) MODAL STATES ---
  showToggleLockModal = signal<boolean>(false);
  selectedPeriodForToggleLock = signal<PeriodResponse | null>(null);
  isTogglingLock = signal<boolean>(false);

  // --- TOGGLE LOCK (SESSION) MODAL STATES (ADDED) ---
  showSessionLockModal = signal<boolean>(false);
  selectedSessionForLock = signal<ExamSessionResponse | null>(null);
  isTogglingSessionLock = signal<boolean>(false);

  // --- FORMS  ---
  sessionForm = this.fb.group({
    label: ['', [Validators.required]],
    sessionType: ['MAIN', [Validators.required]],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]]
  });

  periodForm = this.fb.group({
    label: ['', [Validators.required]],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]]
  });

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.academicYearId.set(Number(idParam));
      this.loadAcademicYear();
      this.loadPeriods();
    } else {
      this.errorMessage.set("ID de l'année académique manquant dans l'URL.");
      this.isLoading.set(false);
    }
  }

  // DATA LOADING
  loadAcademicYear() {
    const yearId = this.academicYearId();
    if (!yearId) return;

    this.academicYearApi.getAcademicYearById(yearId).subscribe({
      next: (response: any) => {
        this.academicYear.set(response.data);
      },
      error: err => {
        console.error("Erreur lors du chargement de l'année académique.", err);
      }
    });
  }

  loadPeriods() {
    const yearId = this.academicYearId();
    if (!yearId) return;

    this.isLoading.set(true);
    this.errorMessage.set('');

    this.periodApi.getPeriodsByAcademicYearId(yearId).subscribe({
      next: (response: any) => {
        this.periods.set(response.data || []);
        this.isLoading.set(false);
      },
      error: err => {
        this.errorMessage.set('Impossible de charger les périodes.');
        this.isLoading.set(false);
      }
    });
  }

  // PERIOD ACTIONS (MODAL)
  openPeriodModal() {
    this.editingPeriodId.set(null);
    this.periodForm.reset();
    this.showPeriodModal.set(true);
  }

  closePeriodModal() {
    this.showPeriodModal.set(false);
  }

  submitPeriodForm() {
    if (this.periodForm.invalid) {
      this.periodForm.markAllAsTouched();
      return;
    }

    const yearId = this.academicYearId();
    if (!yearId) return;

    this.isSaving.set(true);

    const payload: PeriodRequest = {
      label: this.periodForm.value.label!,
      startDate: this.periodForm.value.startDate!,
      endDate: this.periodForm.value.endDate!,
      academicYearId: yearId
    };

    const periodId = this.editingPeriodId();

    if (periodId) {
      // UPDATE MODE
      this.periodApi.updatePeriod(periodId, payload).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.closePeriodModal();
          this.loadPeriods();
          this.toast.success('Période modifiée avec succès !');
        },
        error: err => this.handleBackendError(err)
      });
    } else {
      // CREATION MODE
      this.periodApi.createPeriod(payload).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.closePeriodModal();
          this.loadPeriods();
          this.toast.success('Période ajoutée avec succès !');
        },
        error: err => this.handleBackendError(err)
      });
    }
  }

  editPeriod(period: PeriodResponse) {
    this.editingPeriodId.set(period.id!);
    this.periodForm.patchValue({
      label: period.label,
      startDate: period.startDate,
      endDate: period.endDate
    });
    this.showPeriodModal.set(true);
  }

  // EXAM SESSION ACTIONS (MODAL)
  openSessionModal(periodId: number) {
    this.selectedPeriodIdForSession.set(periodId);
    this.editingSessionId.set(null); // Creation Mode
    this.sessionForm.reset({
      sessionType: 'MAIN'
    });
    this.showSessionModal.set(true);
  }

  editSession(session: ExamSessionResponse, periodId: number) {
    this.selectedPeriodIdForSession.set(periodId);
    this.editingSessionId.set(session.id!);

    this.sessionForm.patchValue({
      label: session.label,
      sessionType: session.sessionType,
      startDate: session.startDate,
      endDate: session.endDate
    });

    this.showSessionModal.set(true);
  }

  closeSessionModal() {
    this.showSessionModal.set(false);
    this.selectedPeriodIdForSession.set(null);
    this.editingSessionId.set(null);
  }

  submitSessionForm() {
    if (this.sessionForm.invalid) {
      this.sessionForm.markAllAsTouched();
      return;
    }

    const periodId = this.selectedPeriodIdForSession();
    if (!periodId) return;

    this.isSaving.set(true);

    const payload: ExamSessionRequest = {
      label: this.sessionForm.value.label!,
      sessionType: this.sessionForm.value.sessionType as ExamSessionRequest.SessionTypeEnum,
      startDate: this.sessionForm.value.startDate!,
      endDate: this.sessionForm.value.endDate!,
      periodId: periodId
    };

    const sessionId = this.editingSessionId();

    if (sessionId) {
      this.examSessionApi.updateExamSession(sessionId, payload).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.closeSessionModal();
          this.loadPeriods();
          this.toast.success('Session modifiée avec succès !');
        },
        error: err => this.handleSessionError(err)
      });
    } else {
      this.examSessionApi.createExamSession(payload).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.closeSessionModal();
          this.loadPeriods();
          this.toast.success('Session ajoutée avec succès !');
        },
        error: err => this.handleSessionError(err)
      });
    }
  }

  // ==========================================
  // ERROR HANDLING
  // ==========================================

  private handleBackendError(err: any) {
    this.isSaving.set(false);
    const errorBody = err?.error;

    if (errorBody?.errors) {
      for (const erreur of errorBody.errors) {
        if (erreur.message === 'END_DATE_MUST_BE_AFTER_START_DATE') {
          this.toast.error('La date de fin doit être postérieure à la date de début.');
        }
      }
    } else if (errorBody?.message === 'PERIOD_LABEL_ALREADY_EXISTS_IN_THIS_YEAR') {
      this.toast.error('Le nom de cette période existe déjà pour cette année.');
    } else if (errorBody?.message === 'PERIOD_DATES_MUST_BE_WITHIN_ACADEMIC_YEAR_DATES') {
      this.toast.error(
        "Les dates de la période doivent être comprises dans celles de l'année scolaire."
      );
    } else if (errorBody?.message === 'PERIOD_DATES_OVERLAP_EXISTING_PERIOD') {
      this.toast.error('Les dates se chevauchent avec une autre période déjà existante.');
    } else {
      this.toast.error('Une erreur inattendue est survenue.');
      console.error('API Error:', err);
    }
  }

  private handleSessionError(err: any) {
    this.isSaving.set(false);
    const errorBody = err?.error;

    // Handling validation errors array (e.g.: END_DATE_MUST_BE_AFTER_START_DATE)
    if (errorBody?.errors) {
      for (const erreur of errorBody.errors) {
        if (erreur.message === 'END_DATE_MUST_BE_AFTER_START_DATE') {
          this.toast.error('La date de fin de session doit être postérieure à la date de début.');
        }
      }
      return;
    }

    // Dictionary of Session-specific errors
    const errorMessages: Record<string, string> = {
      // Period / Year related errors
      CREATION_FORBIDDEN_YEAR_IS_COMPLETED:
        "Impossible d'ajouter une session : l'année scolaire est clôturée.",
      CANNOT_CREATE_MAIN_SESSION_IN_LOCKED_PERIOD:
        "Impossible d'ajouter une session dans une période verrouillée.",

      // Duplication errors
      MAIN_SESSION_ALREADY_EXISTS_FOR_THIS_PERIOD:
        'Une session Principale existe déjà pour cette période.',
      RETAKE_SESSION_ALREADY_EXISTS_FOR_THIS_PERIOD:
        'Une session de Rattrapage existe déjà pour cette période.',
      EXAM_SESSION_LABEL_ALREADY_EXISTS_IN_THIS_PERIOD:
        'Ce nom de session existe déjà dans cette période.',

      // Date errors (Main Session)
      MAIN_SESSION_DATES_MUST_BE_WITHIN_PERIOD_DATES:
        'Les dates de la session Principale doivent être comprises dans celles de la période.',

      // Retake rules
      CANNOT_CREATE_RETAKE_WITHOUT_MAIN_SESSION:
        "Vous devez d'abord créer une session Principale avant d'ajouter un Rattrapage.",
      RETAKE_START_DATE_MUST_BE_AFTER_MAIN_SESSION_END_DATE:
        'Le Rattrapage doit obligatoirement commencer après la fin de la session Principale.'
    };

    // Display the translated message if found in the dictionary
    const errorCode = errorBody?.message;
    if (errorCode && errorMessages[errorCode]) {
      this.toast.error(errorMessages[errorCode]);
    }
    // Default error message
    else {
      this.toast.error("Erreur inattendue lors de l'opération sur la session.");
      console.error('Session API Error:', err);
    }
  }

  // ==========================================
  // LOCK / UNLOCK ACTIONS (PERIOD)
  // ==========================================
  openToggleLockModal(period: PeriodResponse) {
    this.selectedPeriodForToggleLock.set(period);
    this.showToggleLockModal.set(true);
  }

  closeToggleLockModal() {
    this.showToggleLockModal.set(false);
    this.selectedPeriodForToggleLock.set(null);
  }

  confirmToggleLock() {
    const period = this.selectedPeriodForToggleLock();
    if (!period || !period.id) return;

    this.isTogglingLock.set(true);

    this.periodApi.togglePeriodLock(period.id).subscribe({
      next: () => {
        const actionMessage = period.isLocked ? 'déverrouillée' : 'verrouillée';
        this.toast.success(`La période ${period.label} a été ${actionMessage} avec succès.`);
        this.isTogglingLock.set(false);
        this.closeToggleLockModal();
        this.loadPeriods();
      },
      error: err => {
        this.isTogglingLock.set(false);
        this.closeToggleLockModal();

        const errorMessage = err?.error?.message;

        if (errorMessage === 'CANNOT_LOCK_PERIOD_BEFORE_ITS_END_DATE') {
          this.toast.error('Impossible de verrouiller cette période avant sa date de fin.');
        } else if (errorMessage === 'CANNOT_UNLOCK_PERIOD_OF_COMPLETED_YEAR') {
          this.toast.error(
            "Impossible de déverrouiller une période si l'année scolaire est définitivement terminée."
          );
        } else {
          this.toast.error('Erreur lors de la modification du verrouillage de la période.');
        }
        console.error('Erreur API ToggleLock Période:', err);
      }
    });
  }

  // ==========================================
  // LOCK / UNLOCK ACTIONS (SESSION)
  // ==========================================
  openToggleSessionLockModal(session: ExamSessionResponse) {
    this.selectedSessionForLock.set(session);
    this.showSessionLockModal.set(true);
  }

  closeToggleSessionLockModal() {
    this.showSessionLockModal.set(false);
    this.selectedSessionForLock.set(null);
  }

  confirmToggleSessionLock() {
    const session = this.selectedSessionForLock();
    if (!session || !session.id) return;

    this.isTogglingSessionLock.set(true);

    this.examSessionApi.toggleSessionLock(session.id).subscribe({
      next: () => {
        const actionMessage = session.isLocked ? 'déverrouillée' : 'verrouillée';
        this.toast.success(`La session a été ${actionMessage} avec succès.`);
        this.isTogglingSessionLock.set(false);
        this.closeToggleSessionLockModal();
        this.loadPeriods();
      },
      error: err => {
        this.isTogglingSessionLock.set(false);
        this.closeToggleSessionLockModal();

        const errorMessage = err?.error?.message;

        if (errorMessage === 'CANNOT_LOCK_SESSION_BEFORE_ITS_END_DATE') {
          this.toast.error('Impossible de verrouiller cette session avant sa date de fin.');
        } else if (errorMessage === 'CANNOT_MODIFY_LOCK_ON_COMPLETED_YEAR') {
          this.toast.error("Impossible de modifier une session car l'année est terminée.");
        } else {
          this.toast.error('Erreur lors de la modification du verrouillage de la session.');
        }
        console.error('Erreur API ToggleSessionLock:', err);
      }
    });
  }
}
