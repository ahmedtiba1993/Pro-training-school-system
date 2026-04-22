import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  AcceleratedPromotionControllerService,
  AcceleratedPromotionResponse,
  AcceleratedPromotionRequest,
  PromotionControllerService,
  TrainingControllerService,
  TrainingResponse
} from '../../../../../core/api';
import { PaginationComponent } from '../../../../../shared/components/pagination/pagination';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-accelerated',
  standalone: true,
  imports: [CommonModule, DatePipe, PaginationComponent, ReactiveFormsModule],
  templateUrl: './accelerated.html',
  styleUrl: './accelerated.css'
})
export class Accelerated implements OnInit {
  private acceleratedService = inject(AcceleratedPromotionControllerService);
  private promotionService = inject(PromotionControllerService);
  private trainingService = inject(TrainingControllerService);
  private fb = inject(FormBuilder);
  private toastService = inject(ToastService);

  sessions = signal<AcceleratedPromotionResponse[]>([]);
  activeSessions = signal<AcceleratedPromotionResponse[]>([]);
  trainings = signal<TrainingResponse[]>([]);
  isLoading = signal<boolean>(true);

  // Main Modal Management
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

  // State for the Details modal
  isDetailsModalOpen = signal<boolean>(false);
  selectedSession = signal<AcceleratedPromotionResponse | null>(null);

  // State for the modal listing ALL active sessions
  isActiveSessionsModalOpen = signal<boolean>(false);
  allActiveSessions = signal<AcceleratedPromotionResponse[]>([]);
  isLoadingAllActive = signal<boolean>(false);

  // States for the status confirmation modal
  isConfirmStatusModalOpen = signal<boolean>(false);
  pendingStatus = signal<'PLANNED' | 'ENROLLMENT_OPEN' | 'IN_PROGRESS' | 'CLOSED' | null>(null);
  isChangingStatus = signal<boolean>(false);

  sessionForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    code: ['', [Validators.required]],
    trainingId: [null as number | null, [Validators.required]],
    level: [''],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]],
    numberOfHours: [null as number | null, [Validators.required, Validators.min(1)]],
    fee: [null as number | null, [Validators.required, Validators.min(0)]],
    status: ['PLANNED', [Validators.required]]
  });

  ngOnInit(): void {
    this.loadSessions();
    this.loadStatistics();
    this.loadTrainings();
    this.loadActiveSessions();
  }

  loadActiveSessions(): void {
    // We load only 4 sessions for the featured cards
    this.acceleratedService.getPromotionsByStatus2('IN_PROGRESS', 4).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.activeSessions.set(response.data);
        }
      },
      error: err => console.error('Erreur chargement des sessions actives en vedette', err)
    });
  }

  // Opening the modal listing all active sessions
  openActiveSessionsModal(): void {
    this.isActiveSessionsModalOpen.set(true);
    this.isLoadingAllActive.set(true);

    // We call without limit (or undefined) to get everything
    this.acceleratedService.getPromotionsByStatus2('IN_PROGRESS', undefined as any).subscribe({
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
    this.promotionService.getPromotionStatistics('ACCELERATED').subscribe({
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
    this.acceleratedService
      .getAllAcceleratedPromotions(this.currentPage(), this.pageSize())
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
    this.trainingService.getAllActiveTrainings('ACCELERATED').subscribe({
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

  openModal(session?: AcceleratedPromotionResponse) {
    if (session) {
      this.isEditMode.set(true);
      this.editingSessionId.set(session.id || null);

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
        level: (session as any).level || '',
        startDate: formattedStartDate,
        endDate: formattedEndDate,
        numberOfHours: session.numberOfHours,
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

      this.acceleratedService.updateAcceleratedPromotion(currentId, updateRequest).subscribe({
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

      this.acceleratedService.createAcceleratedPromotion(createRequest).subscribe({
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

  openDetailsModal(session: AcceleratedPromotionResponse) {
    this.selectedSession.set(session);
    this.isDetailsModalOpen.set(true);
  }

  closeDetailsModal() {
    this.isDetailsModalOpen.set(false);
    this.selectedSession.set(null);
  }

  // ==========================================
  // STATUS CHANGE AND CONFIRMATION
  // ==========================================

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

    this.acceleratedService.updatePromotionStatus2(id, newStatus).subscribe({
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

          // Update the large list if the modal is open
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

  // ==========================================
  // PRIVATE METHODS
  // ==========================================

  private buildPromotionRequest(): AcceleratedPromotionRequest {
    return {
      name: this.sessionForm.value.name!,
      code: this.sessionForm.value.code!,
      trainingId: this.sessionForm.value.trainingId!,
      level: this.sessionForm.value.level,
      startDate: this.sessionForm.value.startDate!,
      endDate: this.sessionForm.value.endDate!,
      numberOfHours: this.sessionForm.value.numberOfHours!,
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
    } else if (errorMessage === 'PROMOTION_CODE_ALREADY_EXISTS') {
      this.toastService.error('Ce code de promotion existe déjà.');
    } else {
      this.toastService.error(defaultErrorMessage);
    }

    this.isSubmitting.set(false);
  }
}
