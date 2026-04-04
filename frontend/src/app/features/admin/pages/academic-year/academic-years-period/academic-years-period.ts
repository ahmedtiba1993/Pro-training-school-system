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

  // --- FORMS  ---
  sessionForm = this.fb.group({
    label: ['', [Validators.required]],
    sessionType: ['MAIN', [Validators.required]],
    status: ['OPEN', [Validators.required]],
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
    this.editingSessionId.set(null); // Mode Création
    this.sessionForm.reset({
      sessionType: 'MAIN',
      status: 'OPEN'
    });
    this.showSessionModal.set(true);
  }

  // Opens the modal in edit mode with pre-filled data
  editSession(session: ExamSessionResponse, periodId: number) {
    this.selectedPeriodIdForSession.set(periodId); // Use the periodId passed from the HTML
    this.editingSessionId.set(session.id!); // Edit mode

    this.sessionForm.patchValue({
      label: session.label,
      sessionType: session.sessionType,
      status: session.status,
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
      status: this.sessionForm.value.status as ExamSessionRequest.StatusEnum,
      startDate: this.sessionForm.value.startDate!,
      endDate: this.sessionForm.value.endDate!,
      periodId: periodId
    };

    const sessionId = this.editingSessionId();

    if (sessionId) {
      // UPDATE MODE
      this.examSessionApi.updateExamSession(sessionId, payload).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.closeSessionModal();
          this.loadPeriods();
          this.toast.success('Session modifiée avec succès !');
        },
        error: err => {
          this.isSaving.set(false);
          this.handleSessionError(err);
        }
      });
    } else {
      // CREATION MODE
      this.examSessionApi.createExamSession(payload).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.closeSessionModal();
          this.loadPeriods();
          this.toast.success('Session ajoutée avec succès !');
        },
        error: err => {
          this.isSaving.set(false);
          this.handleSessionError(err);
        }
      });
    }
  }

  // ==========================================
  // ERROR HANDLING
  // ==========================================
  private handleBackendError(err: any) {
    this.isSaving.set(false);
    const errorBody = err?.error;
    if (errorBody && errorBody.errorCode === 'ENTITY_ALREADY_EXISTS') {
      if (errorBody.message === 'PERIOD_LABEL_ALREADY_EXISTS_IN_THIS_YEAR') {
        this.toast.error('Le nom de cette période existe déjà pour cette année.');
      }
    } else if (errorBody?.message === 'END_DATE_MUST_BE_AFTER_START_DATE') {
      this.toast.error('La date de fin doit être après la date de début.');
    } else {
      this.toast.error('Une erreur inattendue est survenue.');
    }
  }

  private handleSessionError(err: any) {
    const message = err?.error?.message;
    if (message === 'EXAM_SESSION_TYPE_ALREADY_EXISTS_IN_THIS_PERIOD') {
      this.toast.error('Une session de ce type existe déjà pour cette période.');
    } else if (message === 'AN_OPEN_SESSION_ALREADY_EXISTS_IN_THIS_PERIOD') {
      this.toast.error(
        "Il y a déjà une session ouverte. Veuillez la fermer avant d'en ouvrir une nouvelle."
      );
    } else if (message === 'EXAM_SESSION_LABEL_ALREADY_EXISTS_IN_THIS_PERIOD') {
      this.toast.error('Ce nom de session existe déjà dans cette période.');
    } else {
      this.toast.error("Erreur lors de l'opération sur la session.");
    }
  }
}
