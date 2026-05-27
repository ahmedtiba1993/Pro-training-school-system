import {Component, OnInit, inject, signal, computed, HostListener} from '@angular/core';
import {DecimalPipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {
  ApiResponseClassManagementStatsResponse,
  ClassGroupControllerService,
  ClassGroupResponse,
  LevelControllerService,
  LevelResponse,
  TrainingControllerService,
  TrainingResponse,
  PromotionControllerService,
  PromotionLookupResponse,
  ClassGroupRequest
} from '../../../../../core/api';
import {ToastService} from '../../../../../shared/services/toast.service';
import {ActivatedRoute, RouterLink} from '@angular/router';

export type ClassGroupStatus = 'ACTIVE' | 'DRAFT' | 'INACTIVE' | 'ARCHIVED';

interface StatusFilter {
  value: ClassGroupStatus;
  label: string;
}

@Component({
  selector: 'app-classmanagement-list',
  standalone: true,
  imports: [DecimalPipe, FormsModule, RouterLink],
  templateUrl: './classmanagement-list.html',
  styleUrl: './classmanagement-list.css'
})
export class ClassmanagementList implements OnInit {
  private readonly classGroupService = inject(ClassGroupControllerService);
  private readonly levelService = inject(LevelControllerService);
  private readonly trainingService = inject(TrainingControllerService);
  private readonly promotionService = inject(PromotionControllerService);
  private readonly toastService = inject(ToastService);
  private readonly route = inject(ActivatedRoute);

  readonly statusFilters: StatusFilter[] = [
    {value: 'ACTIVE', label: 'Actives'},
    {value: 'DRAFT', label: 'Brouillons'},
    {value: 'INACTIVE', label: 'Inactives'},
    {value: 'ARCHIVED', label: 'Archivées'}
  ];

  // =========================================================================
  // STATE SIGNALS: MAIN LIST & DASHBOARD FILTERS
  // =========================================================================
  readonly isModalOpen = signal<boolean>(false);
  readonly openDropdownId = signal<number | null>(null);
  readonly selectedStatus = signal<ClassGroupStatus>('ACTIVE');
  readonly selectedLevelId = signal<string>('');
  readonly selectedTrainingId = signal<string>('');
  readonly selectedTypeFilter = signal<string>(''); // signal for curriculum type filter

  readonly classGroups = signal<ClassGroupResponse[]>([]);
  readonly levels = signal<LevelResponse[]>([]);
  readonly trainings = signal<TrainingResponse[]>([]);

  readonly isLoading = signal<boolean>(true);
  readonly errorMessage = signal<string | null>(null);

  // =========================================================================
  // STATE SIGNALS: STATUS CONFIRMATION POPUP
  // =========================================================================
  readonly isConfirmModalOpen = signal<boolean>(false);
  readonly statusChangePayload = signal<{ id: number; targetStatus: ClassGroupStatus } | null>(
    null
  );

  // Add Modal Form States
  readonly modalSelectedLevelId = signal<string>('');
  readonly modalSelectedTrainingId = signal<string>('');
  readonly modalSelectedPromotionId = signal<string>('');
  readonly classNameInput = signal<string>('');
  readonly classCapacityInput = signal<number>(30);
  readonly isSaving = signal<boolean>(false);

  readonly modalTrainings = signal<TrainingResponse[]>([]);
  readonly modalPromotions = signal<PromotionLookupResponse[]>([]);

  private readonly statsResponse = signal<ApiResponseClassManagementStatsResponse | null>(null);

  // =========================================================================
  // COMPUTED STATES
  // =========================================================================
  readonly activeClassesCount = computed(() => this.statsResponse()?.data?.activeClassesCount ?? 0);
  readonly assignedStudentsCount = computed(
    () => this.statsResponse()?.data?.assignedStudentsInActiveClassesCount ?? 0
  );

  // Reactive combined filtering (Instantly filters the list without querying the API on each click)
  readonly filteredClassGroups = computed(() => {
    const list = this.classGroups();
    const typeFilter = this.selectedTypeFilter();

    if (!typeFilter) return list;
    return list.filter(item => item.promotionType === typeFilter);
  });

  readonly isFormValid = computed(() => {
    return (
      this.classNameInput().trim().length >= 2 &&
      this.classCapacityInput() > 0 &&
      this.modalSelectedPromotionId() !== ''
    );
  });

  readonly confirmModalMessage = computed(() => {
    const payload = this.statusChangePayload();
    if (!payload) return '';

    const targetLabel = this.getStatusLabel(payload.targetStatus);
    if (payload.targetStatus === 'ARCHIVED') {
      return `Êtes-vous sûr de vouloir archiver cette classe ? Cette action est irréversible et aucun retour en arrière ne sera possible au sein du système.`;
    }
    return `Êtes-vous sûr de vouloir basculer le statut de cette classe vers "${targetLabel}" ?`;
  });

  @HostListener('document:click')
  closeDropdowns(): void {
    this.openDropdownId.set(null);
  }

  ngOnInit(): void {
    this.loadClassManagementStats();
    this.loadActiveLevels();
    this.loadClassGroups();
  }

  loadClassManagementStats(): void {
    this.classGroupService.getClassManagementStats().subscribe({
      next: response => {
        if (response && response.success) {
          this.statsResponse.set(response);
        }
      },
      error: err => console.error('KPIs Error:', err)
    });
  }

  loadActiveLevels(): void {
    this.levelService.getActiveLevels().subscribe({
      next: response => {
        if (response && response.success && response.data) {
          this.levels.set(response.data);
        }
      },
      error: err => console.error('Error retrieving levels:', err)
    });
  }

  loadTrainingsByLevel(levelId: number): void {
    this.trainingService.getOngoingTrainingsByLevel(levelId).subscribe({
      next: response => {
        if (response && response.success && response.data) {
          this.trainings.set(response.data);
        }
      },
      error: err => console.error('Dashboard Specialties Error:', err)
    });
  }

  loadModalTrainingsByLevel(levelId: number): void {
    this.trainingService.getOngoingTrainingsByLevel(levelId).subscribe({
      next: response => {
        if (response && response.success && response.data) {
          this.modalTrainings.set(response.data);
        }
      },
      error: err => console.error('Modal Specialties Error:', err)
    });
  }

  loadModalPromotionsByTraining(trainingId: number): void {
    this.promotionService.getOpenPromotionsLookup(trainingId).subscribe({
      next: response => {
        if (response && response.success && response.data) {
          this.modalPromotions.set(response.data);
        }
      },
      error: err => console.error('Modal Promotions Error:', err)
    });
  }

  loadClassGroups(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const levelIdParam = this.selectedLevelId() ? Number(this.selectedLevelId()) : undefined;
    const trainingIdParam = this.selectedTrainingId()
      ? Number(this.selectedTrainingId())
      : undefined;

    this.classGroupService
      .getAllClassGroups(levelIdParam, trainingIdParam, this.selectedStatus() as any)
      .subscribe({
        next: response => {
          if (response && response.success && response.data) {
            this.classGroups.set(response.data);
          } else {
            this.errorMessage.set(
              response.message || 'Impossible de traiter la liste des classes.'
            );
          }
          this.isLoading.set(false);
        },
        error: err => {
          console.error('getAllClassGroups Error:', err);
          this.errorMessage.set(
            'Une erreur réseau est survenue lors de la récupération des classes.'
          );
          this.isLoading.set(false);
        }
      });
  }

  onCreateClassGroup(): void {
    if (!this.isFormValid() || this.isSaving()) return;

    this.isSaving.set(true);
    this.errorMessage.set(null);

    const newClassPayload: ClassGroupRequest = {
      name: this.classNameInput().trim(),
      capacity: this.classCapacityInput(),
      promotionId: Number(this.modalSelectedPromotionId())
    };

    this.classGroupService.createClassGroup(newClassPayload).subscribe({
      next: response => {
        if (response && response.success) {
          this.closeAndResetModal();
          this.loadClassGroups();
          this.loadClassManagementStats();
          this.toastService.success('La classe a été configurée et ajoutée avec succès !');
        } else {
          this.parseAndDisplayBackendError(response);
        }
        this.isSaving.set(false);
      },
      error: err => {
        this.parseAndDisplayBackendError(err?.error);
        this.isSaving.set(false);
      }
    });
  }

  onInitiateStatusChange(id: number | undefined, targetStatus: ClassGroupStatus): void {
    if (!id) return;
    this.statusChangePayload.set({id, targetStatus});
    this.isConfirmModalOpen.set(true);
  }

  onExecuteStatusUpdate(): void {
    const payload = this.statusChangePayload();
    if (!payload) return;

    this.classGroupService
      .updateClassGroupStatus(payload.id, payload.targetStatus as any)
      .subscribe({
        next: response => {
          if (response && response.success) {
            this.toastService.success('Le statut du groupe a été modifié avec succès.');
            this.loadClassGroups();
            this.loadClassManagementStats();
          } else {
            this.toastService.error(response.message || 'Erreur lors du changement de statut.');
          }
          this.onCancelStatusChange();
        },
        error: err => {
          console.error('HTTP Patch status error:', err);
          this.toastService.error('Impossible de modifier le statut de cette classe.');
          this.onCancelStatusChange();
        }
      });
  }

  onCancelStatusChange(): void {
    this.isConfirmModalOpen.set(false);
    this.statusChangePayload.set(null);
  }

  getAllAllowedTransitions(currentStatus: string | undefined): ClassGroupStatus[] {
    if (!currentStatus) return [];
    switch (currentStatus.toUpperCase()) {
      case 'DRAFT':
        return ['ACTIVE', 'INACTIVE', 'ARCHIVED'];
      case 'ACTIVE':
        return ['INACTIVE', 'ARCHIVED'];
      case 'INACTIVE':
        return ['ACTIVE', 'ARCHIVED'];
      case 'ARCHIVED':
      default:
        return [];
    }
  }

  getStatusLabel(status: string | undefined): string {
    if (!status) return 'Inconnu';
    const mapping: Record<string, string> = {
      ACTIVE: 'Active',
      DRAFT: 'Brouillon',
      INACTIVE: 'Inactive',
      ARCHIVED: 'Archivée'
    };
    return mapping[status.toUpperCase()] || status;
  }

  // =========================================================================
  // PROMOTION THEME AND STYLE HANDLERS
  // =========================================================================
  getCardThemeClass(type: string | undefined, isFull: boolean): string {
    const base =
      'rounded-2xl border shadow-sm hover:shadow-md transition-all flex flex-col group overflow-hidden relative ';
    if (isFull) return base + 'bg-white border-rose-200';
    if (!type) return base + 'bg-white border-slate-200';

    switch (type.toUpperCase()) {
      case 'ACCREDITED':
        return (
          base +
          'bg-gradient-to-br from-indigo-50/30 via-white to-white border-indigo-100 hover:border-indigo-300'
        );
      case 'ACCELERATED':
        return (
          base +
          'bg-gradient-to-br from-amber-50/20 via-white to-white border-amber-100 hover:border-amber-300'
        );
      case 'CONTINUOUS':
        return (
          base +
          'bg-gradient-to-br from-purple-50/20 via-white to-white border-purple-100 hover:border-purple-300'
        );
      default:
        return base + 'bg-white border-slate-200';
    }
  }

  getPromotionBadgeClass(type: string | undefined): string {
    if (!type) return 'bg-slate-100 text-slate-600 border-slate-200';
    switch (type.toUpperCase()) {
      case 'ACCREDITED':
        return 'bg-indigo-50 text-indigo-700 border border-indigo-100';
      case 'ACCELERATED':
        return 'bg-amber-50 text-amber-700 border border-amber-200';
      case 'CONTINUOUS':
        return 'bg-purple-50 text-purple-700 border border-purple-200';
      default:
        return 'bg-slate-100 text-slate-600 border-slate-200';
    }
  }

  getPromotionLabel(type: string | undefined): string {
    if (!type) return '';
    switch (type.toUpperCase()) {
      case 'ACCREDITED':
        return 'Accrédité';
      case 'ACCELERATED':
        return 'Accéléré';
      case 'CONTINUOUS':
        return 'Continu';
      default:
        return type;
    }
  }

  getStatusBadgeClass(status: string | undefined): string {
    if (!status) return 'bg-slate-100 text-slate-600';
    switch (status.toUpperCase()) {
      case 'ACTIVE':
        return 'bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100/70';
      case 'DRAFT':
        return 'bg-slate-100 text-slate-700 border border-slate-200 hover:bg-slate-200';
      case 'INACTIVE':
        return 'bg-amber-50 text-amber-700 border border-amber-200 hover:bg-amber-100';
      case 'ARCHIVED':
        return 'bg-rose-50 text-rose-700 border border-rose-200 opacity-75 cursor-not-allowed';
      default:
        return 'bg-slate-50 text-slate-600 border border-slate-200';
    }
  }

  private parseAndDisplayBackendError(errorPayload: any): void {
    console.warn('Backend business error interception:', errorPayload);

    const errorCode = errorPayload?.errorCode;
    const message = errorPayload?.message;

    if (
      errorCode === 'ENTITY_ALREADY_EXISTS' ||
      message === 'CLASS_ALREADY_EXISTS_IN_THIS_PROMOTION'
    ) {
      this.toastService.error(
        'Ce nom de classe existe déjà pour cette promotion. Veuillez choisir un autre nom.'
      );
    } else {
      this.toastService.error(message || 'Une erreur est survenue lors du traitement.');
    }
  }

  onStatusChange(status: ClassGroupStatus): void {
    this.selectedStatus.set(status);
    this.loadClassGroups();
  }

  onTemplateCloseModal(): void {
    this.closeAndResetModal();
  }

  private closeAndResetModal(): void {
    this.isModalOpen.set(false);
    this.classNameInput.set('');
    this.classCapacityInput.set(30);
    this.modalSelectedLevelId.set('');
    this.modalSelectedTrainingId.set('');
    this.modalSelectedPromotionId.set('');
    this.modalTrainings.set([]);
    this.modalPromotions.set([]);
  }

  onLevelChange(levelId: string): void {
    this.selectedLevelId.set(levelId);
    this.selectedTrainingId.set('');
    this.trainings.set([]);

    if (levelId) {
      this.loadTrainingsByLevel(Number(levelId));
    }
    this.loadClassGroups();
  }

  onTrainingChange(trainingId: string): void {
    this.selectedTrainingId.set(trainingId);
    this.loadClassGroups();
  }

  onModalLevelChange(levelId: string): void {
    this.modalSelectedLevelId.set(levelId);
    this.modalSelectedTrainingId.set('');
    this.modalSelectedPromotionId.set('');
    this.modalTrainings.set([]);
    this.modalPromotions.set([]);

    if (levelId) {
      this.loadModalTrainingsByLevel(Number(levelId));
    }
  }

  onModalTrainingChange(trainingId: string): void {
    this.modalSelectedTrainingId.set(trainingId);
    this.modalSelectedPromotionId.set('');
    this.modalPromotions.set([]);

    if (trainingId) {
      this.loadModalPromotionsByTraining(Number(trainingId));
    }
  }
}
