import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  AccreditedPromotionControllerService,
  AccreditedPromotionResponse,
  AccreditedPromotionRequest,
  PromotionControllerService,
  TrainingControllerService,
  TrainingResponse,
  AcademicYearControllerService
} from '../../../../../core/api';
import { PaginationComponent } from '../../../../../shared/components/pagination/pagination';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-accredited',
  standalone: true,
  imports: [CommonModule, DatePipe, PaginationComponent, ReactiveFormsModule],
  templateUrl: './accredited.html',
  styleUrl: './accredited.css'
})
export class Accredited implements OnInit {
  private accreditedService = inject(AccreditedPromotionControllerService);
  private promotionService = inject(PromotionControllerService);
  private trainingService = inject(TrainingControllerService);
  private academicYearService = inject(AcademicYearControllerService);
  private fb = inject(FormBuilder);
  private toastService = inject(ToastService);

  sessions = signal<AccreditedPromotionResponse[]>([]);
  activeSessions = signal<AccreditedPromotionResponse[]>([]);
  trainings = signal<TrainingResponse[]>([]);
  academicYears = signal<any[]>([]);
  isLoading = signal<boolean>(true);

  isModalOpen = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);
  isEditMode = signal<boolean>(false);
  editingSessionId = signal<number | null>(null);

  statistics = signal({
    activeSessionsCount: 0,
    plannedSessionsCount: 0,
    closedSessionsCount: 0,
    activeLearnersCount: 0
  });

  currentPage = signal<number>(0);
  pageSize = signal<number>(3);
  totalElements = signal<number>(0);

  isDetailsModalOpen = signal<boolean>(false);
  selectedSession = signal<AccreditedPromotionResponse | null>(null);

  isActiveSessionsModalOpen = signal<boolean>(false);
  allActiveSessions = signal<AccreditedPromotionResponse[]>([]);
  isLoadingAllActive = signal<boolean>(false);

  isConfirmStatusModalOpen = signal<boolean>(false);
  pendingStatus = signal<'PLANNED' | 'ENROLLMENT_OPEN' | 'IN_PROGRESS' | 'CLOSED' | null>(null);
  isChangingStatus = signal<boolean>(false);

  sessionForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    code: ['', [Validators.required]],
    trainingId: [null as number | null, [Validators.required]],
    academicYearId: [null as number | null, [Validators.required]],
    registrationOpeningDate: ['', [Validators.required]],
    registrationDeadline: ['', [Validators.required]],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]],
    fee: [null as number | null, [Validators.required, Validators.min(0)]],
    status: ['PLANNED', [Validators.required]]
  });

  ngOnInit(): void {
    this.loadSessions();
    this.loadStatistics();
    this.loadTrainings();
    this.loadAcademicYears();
    this.loadActiveSessions();
    this.setupAcademicYearListener();
  }

  // Listen for changes on the Academic Year to auto-fill dates
  setupAcademicYearListener(): void {
    this.sessionForm.get('academicYearId')?.valueChanges.subscribe(yearId => {
      if (yearId) {
        const selectedYear = this.academicYears().find(y => y.id === yearId);
        if (selectedYear) {
          // Formatting YYYY-MM-DD
          const start = selectedYear.startDate
            ? new Date(selectedYear.startDate).toISOString().split('T')[0]
            : '';
          const end = selectedYear.endDate
            ? new Date(selectedYear.endDate).toISOString().split('T')[0]
            : '';

          this.sessionForm.patchValue({
            startDate: start,
            endDate: end
          });
        }
      } else {
        // Reset if no year is selected
        this.sessionForm.patchValue({
          startDate: '',
          endDate: ''
        });
      }
    });
  }

  loadAcademicYears(): void {
    this.academicYearService.getAllAcademicYears().subscribe({
      next: (response: any) => {
        if (response.success) {
          this.academicYears.set(response.data.content || response.data);
        }
      },
      error: err => console.error('Erreur chargement des années de formation', err)
    });
  }

  loadActiveSessions(): void {
    this.accreditedService.getPromotionsByStatus1('IN_PROGRESS', 4).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.activeSessions.set(response.data);
        }
      },
      error: err => console.error('Erreur chargement des sessions actives en vedette', err)
    });
  }

  openActiveSessionsModal(): void {
    this.isActiveSessionsModalOpen.set(true);
    this.isLoadingAllActive.set(true);

    this.accreditedService.getPromotionsByStatus1('IN_PROGRESS', undefined as any).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.allActiveSessions.set(response.data);
        }
        this.isLoadingAllActive.set(false);
      },
      error: err => {
        console.error('Erreur chargement de toutes les sessions actives', err);
        this.isLoadingAllActive.set(false);
      }
    });
  }

  closeActiveSessionsModal(): void {
    this.isActiveSessionsModalOpen.set(false);
  }

  loadStatistics(): void {
    this.promotionService.getPromotionStatistics().subscribe({
      next: (response: any) => {
        if (response.success && response.data) {
          this.statistics.set(response.data);
        }
      },
      error: err => console.error('Erreur stats', err)
    });
  }

  loadSessions(): void {
    this.isLoading.set(true);
    this.accreditedService
      .getAllAccreditedPromotions(this.currentPage(), this.pageSize())
      .subscribe({
        next: (response: any) => {
          if (response.success) {
            this.sessions.set(response.data.content);
            this.totalElements.set(response.data?.totalElements || 0);
          }
          this.isLoading.set(false);
        },
        error: err => {
          console.error('Erreur', err);
          this.isLoading.set(false);
        }
      });
  }

  loadTrainings(): void {
    this.trainingService.getAllActiveTrainings('ACCREDITED').subscribe({
      next: (response: any) => {
        if (response.success) {
          this.trainings.set(response.data.content || response.data);
        }
      },
      error: err => console.error('Erreur formations', err)
    });
  }

  onPageChange(newPage: number) {
    this.currentPage.set(newPage);
    this.loadSessions();
  }

  openModal(session?: AccreditedPromotionResponse) {
    if (session) {
      this.isEditMode.set(true);
      this.editingSessionId.set(session.id || null);

      const formattedOpeningDate = (session as any).registrationOpeningDate
        ? new Date((session as any).registrationOpeningDate).toISOString().split('T')[0]
        : '';
      const formattedDeadline = (session as any).registrationDeadline
        ? new Date((session as any).registrationDeadline).toISOString().split('T')[0]
        : '';
      const formattedStartDate = session.startDate
        ? new Date(session.startDate).toISOString().split('T')[0]
        : '';
      const formattedEndDate = session.endDate
        ? new Date(session.endDate).toISOString().split('T')[0]
        : '';

      this.sessionForm.patchValue({
        name: session.name,
        code: session.code,
        trainingId: (session as any).trainingId || null,
        academicYearId: (session as any).academicYearId || null,
        registrationOpeningDate: formattedOpeningDate,
        registrationDeadline: formattedDeadline,
        startDate: formattedStartDate,
        endDate: formattedEndDate,
        fee: session.fee,
        status: session.status || 'PLANNED'
      });
    } else {
      this.isEditMode.set(false);
      this.editingSessionId.set(null);
      this.sessionForm.reset({ status: 'PLANNED' });
    }
    this.isModalOpen.set(true);
  }

  closeModal() {
    this.isModalOpen.set(false);
    this.sessionForm.reset();
    this.isEditMode.set(false);
    this.editingSessionId.set(null);
  }

  onSubmit() {
    if (this.sessionForm.invalid) {
      this.sessionForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    const currentId = this.editingSessionId();

    if (currentId !== null) {
      const updateRequest = this.buildPromotionRequest();

      this.accreditedService.updateAccreditedPromotion(currentId, updateRequest).subscribe({
        next: (res: any) =>
          this.handleSuccessResponse(
            res,
            'Session modifiée avec succès',
            'Erreur lors de la modification'
          ),
        error: err => this.handleErrorResponse(err, 'Erreur lors de la modification de la session')
      });
    } else {
      const createRequest = this.buildPromotionRequest();

      this.accreditedService.createAccreditedPromotion(createRequest).subscribe({
        next: (res: any) =>
          this.handleSuccessResponse(
            res,
            'Session ajoutée avec succès',
            'Erreur lors de la création'
          ),
        error: err => this.handleErrorResponse(err, 'Erreur lors de la création de la session')
      });
    }
  }

  openDetailsModal(session: AccreditedPromotionResponse) {
    this.selectedSession.set(session);
    this.isDetailsModalOpen.set(true);
  }

  closeDetailsModal() {
    this.isDetailsModalOpen.set(false);
    this.selectedSession.set(null);
  }

  openConfirmStatusModal(newStatus: 'PLANNED' | 'ENROLLMENT_OPEN' | 'IN_PROGRESS' | 'CLOSED') {
    this.pendingStatus.set(newStatus);
    this.isConfirmStatusModalOpen.set(true);
  }

  closeConfirmStatusModal() {
    this.isConfirmStatusModalOpen.set(false);
    this.pendingStatus.set(null);
  }

  confirmStatusChange() {
    const session = this.selectedSession();
    const status = this.pendingStatus();

    if (session?.id && status) {
      this.changeStatus(session.id, status);
    }
  }

  getStatusLabel(status: string | null): string {
    switch (status) {
      case 'ENROLLMENT_OPEN':
        return 'Inscriptions Ouvertes';
      case 'IN_PROGRESS':
        return 'En cours';
      case 'CLOSED':
        return 'Clôturée';
      case 'PLANNED':
        return 'Planifiée';
      default:
        return 'Inconnu';
    }
  }

  changeStatus(id: number, newStatus: 'PLANNED' | 'ENROLLMENT_OPEN' | 'IN_PROGRESS' | 'CLOSED') {
    this.isChangingStatus.set(true);

    this.accreditedService.updatePromotionStatus1(id, newStatus).subscribe({
      next: (res: any) => {
        this.isChangingStatus.set(false);
        if (res.success) {
          this.toastService.success('Statut de la session mis à jour avec succès');

          this.loadSessions();
          this.loadStatistics();
          this.loadActiveSessions();

          if (this.selectedSession()?.id === id) {
            this.selectedSession.set({
              ...this.selectedSession()!,
              status: newStatus
            });
          }

          if (this.isActiveSessionsModalOpen()) {
            this.openActiveSessionsModal();
          }

          this.closeConfirmStatusModal();
        } else {
          this.toastService.error(res.message || 'Erreur lors du changement de statut');
        }
      },
      error: err => {
        this.isChangingStatus.set(false);
        console.error(err);
        this.toastService.error('Erreur lors du changement de statut de la session');
      }
    });
  }

  private buildPromotionRequest(): AccreditedPromotionRequest {
    return {
      name: this.sessionForm.value.name!,
      code: this.sessionForm.value.code!,
      trainingId: this.sessionForm.value.trainingId!,
      academicYearId: this.sessionForm.value.academicYearId!,
      registrationOpeningDate: this.sessionForm.value.registrationOpeningDate!,
      registrationDeadline: this.sessionForm.value.registrationDeadline!,
      startDate: this.sessionForm.value.startDate!,
      endDate: this.sessionForm.value.endDate!,
      fee: this.sessionForm.value.fee!
    };
  }

  private handleSuccessResponse(
    res: any,
    successMessage: string,
    defaultErrorMessage: string
  ): void {
    if (res.success) {
      this.toastService.success(successMessage);
      this.closeModal();
      this.loadSessions();
      this.loadStatistics();
      this.loadActiveSessions();
    } else {
      this.toastService.error(res.message || defaultErrorMessage);
    }
    this.isSubmitting.set(false);
  }

  private handleErrorResponse(err: any, defaultErrorMessage: string): void {
    const errorMessage = err?.error?.message;
    const errorDateMessage = err?.error?.errors?.[0]?.message || err?.error?.message;

    if (errorDateMessage === 'START_DATE_MUST_BE_BEFORE_END_DATE') {
      this.toastService.error('La date de début doit être antérieure à la date de fin.');
    } else if (errorDateMessage === 'REGISTRATION_DEADLINE_MUST_BE_AFTER_OPENING_DATE') {
      this.toastService.error("La date limite d'inscription doit être après la date d'ouverture.");
    } else if (errorMessage === 'PROMOTION_CODE_ALREADY_EXISTS') {
      this.toastService.error('Ce code de promotion existe déjà.');
    } else {
      this.toastService.error(defaultErrorMessage);
    }

    this.isSubmitting.set(false);
  }
}
