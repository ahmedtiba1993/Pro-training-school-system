import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  ContinuousPromotionControllerService,
  ContinuousPromotionResponse,
  ContinuousPromotionRequest,
  PromotionControllerService,
  TrainingControllerService,
  TrainingResponse
} from '../../../../../core/api';
import { PaginationComponent } from '../../../../../shared/components/pagination/pagination';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-continuous',
  standalone: true,
  imports: [CommonModule, DatePipe, PaginationComponent, ReactiveFormsModule],
  templateUrl: './continuous.html',
  styleUrl: './continuous.css'
})
export class Continuous implements OnInit {
  private continuousService = inject(ContinuousPromotionControllerService);
  private promotionService = inject(PromotionControllerService);
  private trainingService = inject(TrainingControllerService);
  private fb = inject(FormBuilder);
  private toastService = inject(ToastService);

  sessions = signal<ContinuousPromotionResponse[]>([]);
  activeSessions = signal<ContinuousPromotionResponse[]>([]); // Les 4 cartes en vedette
  trainings = signal<TrainingResponse[]>([]);
  isLoading = signal<boolean>(true);

  // Gestion de la Modale Principale
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

  // State pour la modale de Détails
  isDetailsModalOpen = signal<boolean>(false);
  selectedSession = signal<ContinuousPromotionResponse | null>(null);

  // State pour la modale listant TOUTES les sessions actives
  isActiveSessionsModalOpen = signal<boolean>(false);
  allActiveSessions = signal<ContinuousPromotionResponse[]>([]);
  isLoadingAllActive = signal<boolean>(false);

  // States pour la modale de confirmation de statut
  isConfirmStatusModalOpen = signal<boolean>(false);
  pendingStatus = signal<'PLANNED' | 'ENROLLMENT_OPEN' | 'IN_PROGRESS' | 'CLOSED' | null>(null);
  isChangingStatus = signal<boolean>(false);

  // Modification ici : durationInMonths au lieu de numberOfHours
  sessionForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    code: ['', [Validators.required]],
    trainingId: [null as number | null, [Validators.required]],
    level: [''],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]],
    duration: [null as number | null, [Validators.required, Validators.min(1)]],
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
    // On charge uniquement 4 sessions pour les cartes en vedette
    this.continuousService.getPromotionsByStatus('IN_PROGRESS', 4).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.activeSessions.set(response.data);
        }
      },
      error: err => console.error('Erreur chargement des sessions actives en vedette', err)
    });
  }

  // Ouverture de la modale listant toutes les sessions actives
  openActiveSessionsModal(): void {
    this.isActiveSessionsModalOpen.set(true);
    this.isLoadingAllActive.set(true);

    // On appelle sans limite (ou undefined) pour tout récupérer
    this.continuousService.getPromotionsByStatus('IN_PROGRESS', undefined as any).subscribe({
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
    this.promotionService.getPromotionStatistics('CONTINUOUS').subscribe({
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
    this.continuousService
      .getAllContinuousPromotions(this.currentPage(), this.pageSize())
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
    this.trainingService.getAllActiveTrainings('CONTINUOUS').subscribe({
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

  openModal(session?: ContinuousPromotionResponse) {
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
        duration: (session as any).duration, // Modification ici
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

      this.continuousService.updateContinuousPromotion(currentId, updateRequest).subscribe({
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

      this.continuousService.createContinuousPromotion(createRequest).subscribe({
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

  openDetailsModal(session: ContinuousPromotionResponse) {
    this.selectedSession.set(session);
    this.isDetailsModalOpen.set(true);
  }

  closeDetailsModal() {
    this.isDetailsModalOpen.set(false);
    this.selectedSession.set(null);
  }

  // ==========================================
  // CHANGEMENT DE STATUT ET CONFIRMATION
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

    this.continuousService.updatePromotionStatus(id, newStatus).subscribe({
      next: (res: any) => {
        this.isChangingStatus.set(false);
        if (res.success) {
          this.toastService.success('Statut de la session mis à jour avec succès');

          this.loadSessions();
          this.loadStatistics();
          this.loadActiveSessions(); // Rafraîchir les cartes

          if (this.selectedSession()?.id === id) {
            this.selectedSession.set({
              ...this.selectedSession()!,
              status: newStatus
            });
          }

          // Mettre à jour la grande liste si la modale est ouverte
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

  calculateProgress(startDate: string | undefined, endDate: string | undefined): number {
    if (!startDate || !endDate) return 0;

    const start = new Date(startDate).getTime();
    const end = new Date(endDate).getTime();
    const now = new Date().getTime();

    if (now <= start) return 0;
    if (now >= end) return 100;

    const totalDuration = end - start;
    const elapsed = now - start;
    return Math.round((elapsed / totalDuration) * 100);
  }

  getRemainingDays(endDate: string | undefined): number {
    if (!endDate) return 0;
    const end = new Date(endDate).getTime();
    const now = new Date().getTime();
    if (now >= end) return 0;
    return Math.ceil((end - now) / (1000 * 60 * 60 * 24));
  }

  // ==========================================
  // MÉTHODES PRIVÉES
  // ==========================================

  private buildPromotionRequest(): ContinuousPromotionRequest {
    return {
      name: this.sessionForm.value.name!,
      code: this.sessionForm.value.code!,
      trainingId: this.sessionForm.value.trainingId!,
      startDate: this.sessionForm.value.startDate!,
      endDate: this.sessionForm.value.endDate!,
      duration: this.sessionForm.value.duration!, // Modification ici
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
