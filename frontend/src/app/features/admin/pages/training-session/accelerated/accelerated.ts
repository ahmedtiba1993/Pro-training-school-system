import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {CommonModule, DatePipe, CurrencyPipe} from '@angular/common';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {
  AcceleratedPromotionControllerService,
  AcceleratedPromotionResponse,
  TrainingControllerService,
  TrainingResponse
} from '../../../../../core/api';
import {PaginationComponent} from '../../../../../shared/components/pagination/pagination';
import {Router} from '@angular/router';

@Component({
  selector: 'app-accelerated',
  standalone: true,
  imports: [CommonModule, DatePipe, CurrencyPipe, ReactiveFormsModule, PaginationComponent],
  templateUrl: './accelerated.html',
  styleUrl: './accelerated.css'
})
export class Accelerated implements OnInit {
  // --- INJECTIONS ---
  private acceleratedService = inject(AcceleratedPromotionControllerService);
  private trainingService = inject(TrainingControllerService);
  private fb = inject(FormBuilder);
  private router = inject(Router);

  // --- STATE: LIST ---
  sessions = signal<AcceleratedPromotionResponse[]>([]);
  isLoading = signal<boolean>(true);

  // --- STATE: STATISTICS ---
  statusCounts = signal<Record<string, number>>({});
  draftCount = computed(() => this.statusCounts()['DRAFT'] || 0);
  enrollmentCount = computed(() => this.statusCounts()['ENROLLMENT'] || 0);
  inProgressCount = computed(() => this.statusCounts()['IN_PROGRESS'] || 0);
  evaluationCount = computed(() => this.statusCounts()['EVALUATION'] || 0);

  // --- PAGINATION ---
  currentPage = signal<number>(0);
  pageSize = signal<number>(5);
  totalElements = signal<number>(0);

  // --- STATE: DETAILS MODAL & WORKFLOW ---
  isDetailsModalOpen = signal<boolean>(false);
  isLoadingDetails = signal<boolean>(false);
  selectedSession = signal<AcceleratedPromotionResponse | null>(null);

  isConfirmModalOpen = signal<boolean>(false);
  pendingStatusChange = signal<string | null>(null);
  isChangingStatus = signal<boolean>(false);

  // --- STATE: FORM MODAL (CREATION / MODIFICATION) ---
  isFormModalOpen = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);
  editingSessionId = signal<number | null>(null);
  currentEditSession = signal<AcceleratedPromotionResponse | null>(null);
  activeTrainings = signal<TrainingResponse[]>([]);

  // State for the modal listing ALL active sessions
  isActiveSessionsModalOpen = signal<boolean>(false);
  allActiveSessions = signal<AcceleratedPromotionResponse[]>([]);
  isLoadingAllActive = signal<boolean>(false);
  activeSessions = signal<AcceleratedPromotionResponse[]>([]);

  // Reactive Form
  sessionForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    trainingId: ['', [Validators.required]],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]],
    registrationOpeningDate: ['', [Validators.required]],
    registrationDeadline: ['', [Validators.required]],
    registrationFee: [0, [Validators.required, Validators.min(0)]],
    tuitionFee: [0, [Validators.required, Validators.min(0)]],
    capacity: [0, [Validators.required, Validators.min(1)]]
  });

  ngOnInit(): void {
    this.loadSessions();
    this.loadStatusCounts();
    this.loadActiveSessions();
  }

  // ==========================================
  // READ API (GET)
  // ==========================================

  loadSessions(): void {
    this.isLoading.set(true);
    this.acceleratedService
      .getAllAcceleratedPromotions(this.currentPage(), this.pageSize())
      .subscribe({
        next: (response: any) => {
          if (response.success && response.data) {
            this.sessions.set(response.data.content);
            this.totalElements.set(response?.data?.totalElements || 0);
          }
          this.isLoading.set(false);
        },
        error: err => {
          console.error('Error loading sessions', err);
          this.isLoading.set(false);
          this.sessions.set([]);
        }
      });
  }

  loadStatusCounts(): void {
    this.acceleratedService.getStatusCounts().subscribe({
      next: (response: any) => {
        if (response.success && response.data) {
          this.statusCounts.set(response.data);
        }
      }
    });
  }

  // FIX: Safely load and impose a Frontend limit of 4
  loadActiveSessions(): void {
    // Pass undefined to prevent the backend from taking a "4" as page 4
    this.acceleratedService.getPromotionsByStatus1('IN_PROGRESS', undefined as any).subscribe({
      next: (response: any) => {
        if (response.success && response.data) {
          // Handles both direct arrays and Spring paginated returns (Page<>)
          const items = response.data.content || response.data;
          const dataArray = Array.isArray(items) ? items : [];

          // Strictly limit default display to 4 sessions
          this.activeSessions.set(dataArray.slice(0, 4));
        }
      },
      error: err => console.error('Error loading featured active sessions', err)
    });
  }

  // FIX: Display all sessions without limitation
  openActiveSessionsModal(): void {
    this.isActiveSessionsModalOpen.set(true);
    this.isLoadingAllActive.set(true);

    this.acceleratedService.getPromotionsByStatus1('IN_PROGRESS', undefined as any).subscribe({
      next: (response: any) => {
        if (response.success && response.data) {
          const items = response.data.content || response.data;
          const dataArray = Array.isArray(items) ? items : [];

          // Load all active sessions into the variable dedicated to the modal
          this.allActiveSessions.set(dataArray);
        }
        this.isLoadingAllActive.set(false);
      },
      error: err => {
        console.error('Error loading all active sessions', err);
        this.isLoadingAllActive.set(false);
      }
    });
  }

  closeActiveSessionsModal(): void {
    this.isActiveSessionsModalOpen.set(false);
  }

  loadActiveTrainings(): void {
    this.trainingService.getAllActiveTrainings('ACCELERATED' as any).subscribe({
      next: (response: any) => {
        if (response.success && response.data) {
          this.activeTrainings.set(response.data);
        }
      },
      error: err => console.error('Error loading trainings', err)
    });
  }

  onPageChange(newPage: number): void {
    this.currentPage.set(newPage);
    this.loadSessions();
  }

  // ==========================================
  // MODAL: FORM (POST / PUT)
  // ==========================================

  openCreateModal(): void {
    this.editingSessionId.set(null);
    this.currentEditSession.set(null);

    this.sessionForm.enable();
    this.sessionForm.reset({
      name: '',
      trainingId: '',
      startDate: '',
      endDate: '',
      registrationOpeningDate: '',
      registrationDeadline: '',
      registrationFee: 0,
      tuitionFee: 0,
      capacity: 0
    });

    this.loadActiveTrainings();
    this.isFormModalOpen.set(true);
  }

  openEditModal(session: AcceleratedPromotionResponse): void {
    if (session.status === 'COMPLETED' || session.status === 'CANCELLED') return;

    this.editingSessionId.set(session.id!);
    this.currentEditSession.set(session);
    this.sessionForm.enable();

    this.sessionForm.patchValue({
      name: session.name,
      trainingId: session.trainingId,
      startDate: this.formatDateForInput(session.startDate),
      endDate: this.formatDateForInput(session.endDate),
      registrationOpeningDate: this.formatDateForInput(session.registrationOpeningDate),
      registrationDeadline: this.formatDateForInput(session.registrationDeadline),
      registrationFee: session.registrationFee,
      tuitionFee: session.tuitionFee,
      capacity: session.capacity
    });

    if (session.enrollmentCount! > 0) {
      this.sessionForm.get('registrationFee')?.disable();
      this.sessionForm.get('tuitionFee')?.disable();
    }
    if (session.status !== 'DRAFT' && session.status !== 'ENROLLMENT') {
      this.sessionForm.get('startDate')?.disable();
    }

    this.loadActiveTrainings();
    this.isDetailsModalOpen.set(false);
    this.isFormModalOpen.set(true);
  }

  closeFormModal(): void {
    this.isFormModalOpen.set(false);
  }

  submitForm(): void {
    if (this.sessionForm.invalid) {
      this.sessionForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    const payload = this.sessionForm.getRawValue();

    if (this.editingSessionId()) {
      this.acceleratedService
        .updateAcceleratedPromotion(this.editingSessionId()!, payload)
        .subscribe({
          next: (response: any) => {
            if (response.success) {
              this.loadSessions();
              this.closeFormModal();
            }
            this.isSubmitting.set(false);
          },
          error: err => {
            console.error('Error during modification', err);
            this.isSubmitting.set(false);
          }
        });
    } else {
      this.acceleratedService.createAcceleratedPromotion(payload).subscribe({
        next: (response: any) => {
          if (response.success) {
            this.loadSessions();
            this.loadStatusCounts();
            this.closeFormModal();
          }
          this.isSubmitting.set(false);
        },
        error: err => {
          console.error('Error during creation', err);
          this.isSubmitting.set(false);
        }
      });
    }
  }

  // ==========================================
  // MODAL: DETAILS & WORKFLOW (PATCH)
  // ==========================================

  openDetailsModal(session: AcceleratedPromotionResponse): void {
    this.isDetailsModalOpen.set(true);
    this.isLoadingDetails.set(true);
    this.selectedSession.set(null);

    this.acceleratedService.getAcceleratedPromotionById(session.id!).subscribe({
      next: (response: any) => {
        if (response.success && response.data) {
          this.selectedSession.set(response.data);
        }
        this.isLoadingDetails.set(false);
      },
      error: () => this.isLoadingDetails.set(false)
    });
  }

  closeDetailsModal(): void {
    this.isDetailsModalOpen.set(false);
    setTimeout(() => this.selectedSession.set(null), 300);
  }

  getAvailableNextStatuses(currentStatus: string): string[] {
    switch (currentStatus) {
      case 'DRAFT':
        return ['ENROLLMENT', 'CANCELLED'];
      case 'ENROLLMENT':
        return ['IN_PROGRESS', 'CANCELLED'];
      case 'IN_PROGRESS':
        return ['EVALUATION', 'CANCELLED'];
      case 'EVALUATION':
        return ['COMPLETED'];
      default:
        return [];
    }
  }

  getActionLabel(targetStatus: string): string {
    switch (targetStatus) {
      case 'ENROLLMENT':
        return 'Ouvrir les inscriptions';
      case 'IN_PROGRESS':
        return 'Démarrer la session';
      case 'EVALUATION':
        return 'Passer en évaluation';
      case 'COMPLETED':
        return 'Clôturer la session';
      case 'CANCELLED':
        return 'Annuler la session';
      default:
        return targetStatus;
    }
  }

  getActionStyle(targetStatus: string): string {
    if (targetStatus === 'CANCELLED')
      return 'bg-rose-50 text-rose-600 hover:bg-rose-100 hover:text-rose-700';
    if (targetStatus === 'COMPLETED') return 'bg-emerald-600 text-white hover:bg-emerald-700';
    return 'bg-indigo-600 text-white hover:bg-indigo-700 shadow-sm';
  }

  promptStatusChange(targetStatus: string): void {
    this.pendingStatusChange.set(targetStatus);
    this.isConfirmModalOpen.set(true);
  }

  closeConfirmModal(): void {
    this.isConfirmModalOpen.set(false);
    setTimeout(() => this.pendingStatusChange.set(null), 200);
  }

  confirmStatusChange(): void {
    const targetStatus = this.pendingStatusChange();
    const session = this.selectedSession();

    if (!session || !session.id || !targetStatus) return;
    this.isChangingStatus.set(true);

    this.acceleratedService.changeAcceleratedStatus(session.id, targetStatus as any).subscribe({
      next: () => {
        this.selectedSession.set({
          ...session,
          status: targetStatus as AcceleratedPromotionResponse['status']
        });
        this.loadSessions();
        this.loadStatusCounts();
        this.isChangingStatus.set(false);
        this.closeConfirmModal();
      },
      error: err => {
        console.error('Error changing status', err);
        this.isChangingStatus.set(false);
      }
    });
  }

  public navigateToSubjects(promotionId: number): void {
    if (!promotionId) return;
    this.router.navigate(['/admin/promotions/accelerated', promotionId, 'subjects']);
  }

  // ==========================================
  // UTILS & HELPERS
  // ==========================================

  private formatDateForInput(dateVal: any): string {
    if (!dateVal) return '';
    if (typeof dateVal === 'string') return dateVal.split('T')[0];
    if (dateVal instanceof Date) {
      const y = dateVal.getFullYear();
      const m = String(dateVal.getMonth() + 1).padStart(2, '0');
      const d = String(dateVal.getDate()).padStart(2, '0');
      return `${y}-${m}-${d}`;
    }
    return '';
  }

  getStatusConfig(status: string) {
    switch (status) {
      case 'DRAFT':
        return {label: 'Brouillon', classes: 'bg-slate-100 text-slate-700 border-slate-200'};
      case 'ENROLLMENT':
        return {label: 'Inscriptions', classes: 'bg-sky-50 text-sky-700 border-sky-200'};
      case 'IN_PROGRESS':
        return {label: 'En cours', classes: 'bg-indigo-50 text-indigo-700 border-indigo-200'};
      case 'EVALUATION':
        return {label: 'Évaluation', classes: 'bg-amber-50 text-amber-700 border-amber-200'};
      case 'COMPLETED':
        return {label: 'Terminée', classes: 'bg-emerald-50 text-emerald-700 border-emerald-200'};
      case 'CANCELLED':
        return {label: 'Annulée', classes: 'bg-rose-50 text-rose-700 border-rose-200'};
      default:
        return {label: status, classes: 'bg-gray-50 text-gray-700 border-gray-200'};
    }
  }

  getDurationTranslation(unit: string): string {
    switch (unit) {
      case 'MONTHS':
        return 'Mois';
      case 'WEEKS':
        return 'Semaines';
      case 'DAYS':
        return 'Jours';
      case 'HOURS':
        return 'Heures';
      default:
        return unit;
    }
  }
}
