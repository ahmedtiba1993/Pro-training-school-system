import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastService } from '../../../../../shared/services/toast.service';
import {
  TrainingControllerService,
  TrainingResponse,
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

  trainings = signal<TrainingResponse[]>([]);
  levels = signal<any[]>([]);
  specialties = signal<any[]>([]);

  isLoading = signal<boolean>(true);
  isModalOpen = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);

  trainingToToggle = signal<TrainingResponse | null>(null);
  editingTrainingId = signal<number | null>(null);

  currentPage = signal<number>(0);
  pageSize = signal<number>(5);
  totalElements = signal<number>(0);

  // Signal to store statistics
  trainingStats = signal({
    ACCREDITED: 0,
    CONTINUOUS: 0,
    ACCELERATED: 0
  });

  trainingForm: FormGroup = this.fb.group({
    levelId: ['', [Validators.required]],
    specialtyId: ['', [Validators.required]],
    trainingType: ['ACCREDITED', [Validators.required]],
    durationInMonths: [0, [Validators.required, Validators.min(1)]],
    active: [false]
  });

  ngOnInit(): void {
    this.loadTrainings();
    this.loadStats();
    this.loadLevels();
    this.loadSpecialties();
  }

  // --- STATISTICS LOADING ---
  loadStats(): void {
    this.trainingService.getActiveTrainingStats().subscribe({
      next: (res: any) => {
        if (res.success && res.data) {
          // On initialise à 0
          const newStats = { ACCREDITED: 0, CONTINUOUS: 0, ACCELERATED: 0 };

          res.data.forEach((item: any) => {
            if (item.trainingType === 'ACCREDITED') newStats.ACCREDITED = item.count;
            if (item.trainingType === 'CONTINUOUS') newStats.CONTINUOUS = item.count;
            if (item.trainingType === 'ACCELERATED') newStats.ACCELERATED = item.count;
          });

          this.trainingStats.set(newStats);
        }
      },
      error: err => {
        console.error('Erreur lors du chargement des statistiques', err);
      }
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
      error: err => {
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

  // ACTIVATION CONFIRMATION MANAGEMENT
  confirmToggleActivation(training: TrainingResponse): void {
    this.trainingToToggle.set(training);
  }

  cancelToggle(): void {
    this.trainingToToggle.set(null);
  }

  executeToggleActivation(): void {
    const training = this.trainingToToggle();
    if (!training) return;

    this.isSubmitting.set(true);

    this.trainingService.changeTrainingActivation(training.id!).subscribe({
      next: (res: any) => {
        if (res.success) {
          this.toastService.success(
            training.active ? 'Formation désactivée avec succès' : 'Formation activée avec succès'
          );
          this.loadTrainings();
          this.loadStats();
        } else {
          this.toastService.error(res.message || 'Erreur lors du changement de statut');
        }
        this.isSubmitting.set(false);
        this.cancelToggle();
      },
      error: err => {
        console.error('Erreur API:', err);
        this.toastService.error('Impossible de changer le statut de cette formation');
        this.isSubmitting.set(false);
        this.cancelToggle();
      }
    });
  }

  // ADD AND EDIT MODAL MANAGEMENT
  openAddModal(): void {
    this.editingTrainingId.set(null);
    this.trainingForm.reset({
      levelId: '',
      specialtyId: '',
      trainingType: 'ACCREDITED',
      durationInMonths: 24,
      active: true
    });
    this.isModalOpen.set(true);
  }

  openEditModal(training: TrainingResponse): void {
    this.editingTrainingId.set(training.id!);
    this.trainingForm.patchValue({
      levelId: training.levelId,
      specialtyId: training.specialtyId,
      trainingType: training.trainingType,
      durationInMonths: training.durationInMonths,
      active: training.active
    });
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
    this.editingTrainingId.set(null);
  }

  // FORM SUBMISSION (CREATE & UPDATE)
  onSubmit(): void {
    if (this.trainingForm.invalid) {
      this.trainingForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    const formValue = this.trainingForm.value;
    const currentId = this.editingTrainingId();

    const request = {
      trainingType: formValue.trainingType,
      durationInMonths: Number(formValue.durationInMonths),
      isActive: formValue.active,
      levelId: Number(formValue.levelId),
      specialtyId: Number(formValue.specialtyId)
    };

    if (currentId !== null) {
      this.trainingService.updateTraining(currentId, request).subscribe({
        next: (res: any) => {
          if (res.success) {
            this.toastService.success('Formation modifiée avec succès');
            this.closeModal();
            this.loadTrainings();
            this.loadStats();
          } else {
            this.toastService.error(res.message || 'Erreur lors de la modification');
          }
          this.isSubmitting.set(false);
        },
        error: err => {
          console.error('Erreur API:', err);
          this.toastService.error('Erreur : Cette combinaison existe déjà.');
          this.isSubmitting.set(false);
        }
      });
    } else {
      this.trainingService.createTraining(request).subscribe({
        next: (res: any) => {
          if (res.success) {
            this.toastService.success('Formation créée avec succès');
            this.closeModal();
            this.loadTrainings();
            this.loadStats();
          } else {
            this.toastService.error(res.message || 'Erreur lors de la création');
          }
          this.isSubmitting.set(false);
        },
        error: err => {
          console.error('Erreur API:', err);
          this.toastService.error('Cette formation existe déjà ou erreur serveur.');
          this.isSubmitting.set(false);
        }
      });
    }
  }

  onPageChange(newPage: number) {
    this.currentPage.set(newPage);
    this.loadTrainings();
  }
}
