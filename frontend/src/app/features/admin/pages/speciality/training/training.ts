import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  FormControl,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ToastService } from '../../../../../shared/services/toast.service';
import {
  TrainingControllerService,
  LevelControllerService,
  SpecialtyControllerService
} from '../../../../../core/api';
import { PaginationComponent } from '../../../../../shared/components/pagination/pagination';

@Component({
  selector: 'app-training',
  standalone: true,
  imports: [ReactiveFormsModule, PaginationComponent],
  templateUrl: './training.html',
  styleUrl: './training.css'
})
export class Training implements OnInit {
  private trainingService = inject(TrainingControllerService);
  private levelService = inject(LevelControllerService);
  private specialtyService = inject(SpecialtyControllerService);
  private toastService = inject(ToastService);
  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);

  trainings = signal<any[]>([]);
  levels = signal<any[]>([]);
  specialties = signal<any[]>([]);

  isLoading = signal<boolean>(true);
  isModalOpen = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);

  editingTrainingId = signal<number | null>(null);
  viewingTraining = signal<any | null>(null);
  isRestrictedEdit = signal<boolean>(false);

  // --- STATUS MANAGEMENT ---
  isStatusModalOpen = signal<boolean>(false);
  trainingForStatus = signal<any | null>(null);
  statusControl = this.fb.control('', Validators.required);

  availableStatuses = signal([
    { value: 'DRAFT', label: 'Brouillon' },
    { value: 'ACTIVE', label: 'Actif' },
    { value: 'SUSPENDED', label: 'Suspendu' },
    { value: 'ARCHIVED', label: 'Archivé' }
  ]);

  currentPage = signal<number>(0);
  pageSize = signal<number>(5);
  totalElements = signal<number>(0);

  trainingStats = signal({
    ACCREDITED: 0,
    CONTINUOUS: 0,
    ACCELERATED: 0
  });

  readonly UNIT_MAP: Record<string, { value: string; label: string }[]> = {
    ACCELERATED: [
      { value: 'HOURS', label: 'Heures' },
      { value: 'DAYS', label: 'Jours' }
    ],
    ACCREDITED: [
      { value: 'MONTHS', label: 'Mois' },
      { value: 'YEARS', label: 'Années' }
    ],
    CONTINUOUS: [
      { value: 'MONTHS', label: 'Mois' },
      { value: 'WEEKS', label: 'Semaines' }
    ]
  };

  availableDurationUnits = signal(this.UNIT_MAP['ACCREDITED']);

  trainingForm: FormGroup = this.fb.group({
    description: [''],
    levelId: ['', [Validators.required]],
    specialtyId: ['', [Validators.required]],
    trainingType: ['ACCREDITED', [Validators.required]],
    durationValue: [1, [Validators.required, Validators.min(1)]],
    durationUnit: ['MONTHS', [Validators.required]]
  });

  ngOnInit(): void {
    this.loadTrainings();
    this.loadStats();
    this.loadLevels();
    this.loadSpecialties();

    this.trainingForm
      .get('trainingType')
      ?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(type => {
        if (type) {
          const newUnits = this.UNIT_MAP[type] || [];
          this.availableDurationUnits.set(newUnits);
          if (newUnits.length > 0) {
            this.trainingForm.patchValue({ durationUnit: newUnits[0].value }, { emitEvent: false });
          }
        }
      });
  }

  loadStats(): void {
    this.trainingService.getActiveTrainingStats().subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          const newStats = { ACCREDITED: 0, CONTINUOUS: 0, ACCELERATED: 0 };
          res.data.forEach((item: any) => {
            if (item.trainingType === 'ACCREDITED') newStats.ACCREDITED = item.count;
            if (item.trainingType === 'CONTINUOUS') newStats.CONTINUOUS = item.count;
            if (item.trainingType === 'ACCELERATED') newStats.ACCELERATED = item.count;
          });
          this.trainingStats.set(newStats);
        }
      },
      error: err => console.error('Erreur stats', err)
    });
  }

  loadTrainings(): void {
    this.isLoading.set(true);
    this.trainingService.getAllPaged(this.currentPage(), this.pageSize()).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.trainings.set(response.data.content);
          this.totalElements.set(response.data.totalElements);
        } else {
          this.toastService.error(response.message || 'Erreur chargement formations');
        }
        this.isLoading.set(false);
      },
      error: () => {
        this.toastService.error('Erreur de connexion au serveur');
        this.isLoading.set(false);
      }
    });
  }

  loadLevels(): void {
    this.levelService.getAllLevels().subscribe({
      next: (res: any) => {
        if (res.success) this.levels.set(res.data);
      }
    });
  }

  loadSpecialties(): void {
    this.specialtyService.getAllSpecialties().subscribe({
      next: (res: any) => {
        if (res.success) this.specialties.set(res.data);
      }
    });
  }

  // --- ADD MODAL ---
  openAddModal(): void {
    this.editingTrainingId.set(null);
    this.isRestrictedEdit.set(false);
    this.trainingForm.enable();
    this.availableDurationUnits.set(this.UNIT_MAP['ACCREDITED']);
    this.trainingForm.reset({
      description: '',
      levelId: '',
      specialtyId: '',
      trainingType: 'ACCREDITED',
      durationValue: 1,
      durationUnit: 'MONTHS'
    });
    this.isModalOpen.set(true);
  }

  // --- EDIT MODAL ---
  openEditModal(training: any): void {
    if (training.status === 'ARCHIVED') {
      this.toastService.error('Action interdite : cette formation est gelée (Archivée).');
      return;
    }

    this.editingTrainingId.set(training.id);
    const isRestricted = training.status === 'ACTIVE' || training.status === 'SUSPENDED';
    this.isRestrictedEdit.set(isRestricted);

    this.availableDurationUnits.set(this.UNIT_MAP[training.trainingType] || []);
    this.trainingForm.enable();

    this.trainingForm.patchValue(
      {
        description: training.description || '',
        levelId: training.levelId,
        specialtyId: training.specialtyId,
        trainingType: training.trainingType,
        durationValue: training.durationValue,
        durationUnit: training.durationUnit
      },
      { emitEvent: false }
    );

    if (isRestricted) {
      this.trainingForm.controls['levelId'].disable();
      this.trainingForm.controls['specialtyId'].disable();
      this.trainingForm.controls['trainingType'].disable();
      this.trainingForm.controls['durationValue'].disable();
      this.trainingForm.controls['durationUnit'].disable();
    }

    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
    this.editingTrainingId.set(null);
    this.isRestrictedEdit.set(false);
  }

  // --- DETAILS MODAL ---
  openViewModal(training: any): void {
    this.viewingTraining.set(training);
  }

  closeViewModal(): void {
    this.viewingTraining.set(null);
  }

  // --- STATUS CHANGE MODAL ---
  openStatusModal(training: any): void {
    this.trainingForStatus.set(training);
    this.statusControl.setValue(training.status);
    this.isStatusModalOpen.set(true);
  }

  closeStatusModal(): void {
    this.isStatusModalOpen.set(false);
    this.trainingForStatus.set(null);
  }

  submitStatusChange(): void {
    if (this.statusControl.invalid) return;

    const currentTraining = this.trainingForStatus();
    const newStatus = this.statusControl.value as 'DRAFT' | 'ACTIVE' | 'SUSPENDED' | 'ARCHIVED';

    if (currentTraining.status === newStatus) {
      this.toastService.success('Le statut sélectionné est déjà le statut actuel.');
      this.closeStatusModal();
      return;
    }

    this.isSubmitting.set(true);

    this.trainingService.updateTrainingStatus(currentTraining.id, newStatus).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.toastService.success('Le statut a été mis à jour avec succès.');
          this.closeStatusModal();
          this.loadTrainings(); // Reload table
          this.loadStats(); // Reload stats
        } else {
          this.toastService.error(res.message || 'Erreur lors de la mise à jour du statut.');
        }
        this.isSubmitting.set(false);
      },
      error: err => {
        console.error('Erreur API Statut:', err);
        this.toastService.error('Erreur serveur lors de la mise à jour du statut.');
        this.isSubmitting.set(false);
      }
    });
  }

  // --- CREATE / UPDATE SUBMISSION ---
  onSubmit(): void {
    if (this.trainingForm.invalid) {
      this.trainingForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    const formValue = this.trainingForm.getRawValue();
    const currentId = this.editingTrainingId();

    const request = {
      description: formValue.description,
      trainingType: formValue.trainingType,
      durationValue: Number(formValue.durationValue),
      durationUnit: formValue.durationUnit,
      levelId: Number(formValue.levelId),
      specialtyId: Number(formValue.specialtyId)
    };

    const action$ = currentId
      ? this.trainingService.updateTraining(currentId, request)
      : this.trainingService.createTraining(request);

    action$.subscribe({
      next: (res: any) => {
        if (res.success) {
          this.toastService.success(`Formation ${currentId ? 'modifiée' : 'créée'} avec succès`);
          this.closeModal();
          this.loadTrainings();
          this.loadStats();
        } else {
          this.toastService.error(res.message || 'Erreur lors de la sauvegarde');
        }
        this.isSubmitting.set(false);
      },
      error: err => {
        console.error('Erreur API:', err);
        this.isSubmitting.set(false);
        const errorBody = err.error;
        if (
          errorBody?.errorCode === 'ENTITY_ALREADY_EXISTS' ||
          errorBody?.message === 'TRAINING_COMBINATION_ALREADY_EXISTS'
        ) {
          this.toastService.error('Cette combinaison (Spécialité + Niveau + Type) existe déjà.');
        } else {
          this.toastService.error('Erreur serveur lors de la sauvegarde de la formation.');
        }
      }
    });
  }

  onPageChange(newPage: number) {
    this.currentPage.set(newPage);
    this.loadTrainings();
  }

  formatDuration(value: number, unit: string): string {
    const unitsLabels: Record<string, string> = {
      YEARS: value > 1 ? 'Années' : 'Année',
      MONTHS: 'Mois',
      WEEKS: value > 1 ? 'Semaines' : 'Semaine',
      DAYS: value > 1 ? 'Jours' : 'Jour',
      HOURS: value > 1 ? 'Heures' : 'Heure'
    };
    return `${value} ${unitsLabels[unit] || unit}`;
  }
}
