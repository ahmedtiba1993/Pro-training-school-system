import {Component, OnInit, computed, inject, signal, DestroyRef} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ReactiveFormsModule, FormBuilder, FormGroup, Validators} from '@angular/forms';
import {Router} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {finalize, forkJoin} from 'rxjs';

import {
  AccreditedPromotionControllerService,
  AcademicYearControllerService,
  LevelControllerService,
  TrainingControllerService
} from '../../../../../core/api';
import {PaginationComponent} from '../../../../../shared/components/pagination/pagination';
import {ToastService} from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-accredited',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PaginationComponent],
  templateUrl: './accredited.html'
})
export class Accredited implements OnInit {
  private readonly promotionService = inject(AccreditedPromotionControllerService);
  private readonly academicYearService = inject(AcademicYearControllerService);
  private readonly levelService = inject(LevelControllerService);
  private readonly trainingService = inject(TrainingControllerService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  public readonly promotions = signal<any[]>([]);
  public readonly isLoading = signal<boolean>(false);
  public readonly error = signal<string | null>(null);

  public readonly currentPage = signal<number>(0);
  public readonly pageSize = signal<number>(5);
  public readonly totalElements = signal<number>(0);

  public readonly hasData = computed(() => this.promotions().length > 0);

  // STATISTICS
  public readonly isStatsLoading = signal<boolean>(false);
  public readonly promotionStats = signal<{
    [key: string]: { count: number; enrollments: number };
  }>({
    DRAFT: {count: 0, enrollments: 0},
    ENROLLMENT: {count: 0, enrollments: 0},
    IN_PROGRESS: {count: 0, enrollments: 0},
    EVALUATION: {count: 0, enrollments: 0}
  });

  // CURRENT PROMOTIONS (ONGOING)
  public readonly ongoingPromotions = signal<any[]>([]);
  public readonly isOngoingLoading = signal<boolean>(false);

  // ALL ACTIVE PROMOTIONS (Modal)
  public readonly isOngoingModalOpen = signal<boolean>(false);
  public readonly isAllOngoingLoading = signal<boolean>(false);
  public readonly allOngoingPromotions = signal<any[]>([]);

  // MODALS STATES
  public readonly isModalOpen = signal<boolean>(false);
  public readonly isModalLoading = signal<boolean>(false);
  public readonly openAcademicYears = signal<any[]>([]);
  public readonly activeLevels = signal<any[]>([]);
  public readonly activeTrainings = signal<any[]>([]);
  public readonly isTrainingsLoading = signal<boolean>(false);
  public createForm!: FormGroup;

  public readonly isDetailsModalOpen = signal<boolean>(false);
  public readonly isDetailsLoading = signal<boolean>(false);
  public readonly selectedPromotion = signal<any | null>(null);

  public readonly isEditModalOpen = signal<boolean>(false);
  public readonly isEditLoading = signal<boolean>(false);
  public readonly isEditFetching = signal<boolean>(false);
  public readonly currentEditId = signal<number | null>(null);
  public editForm!: FormGroup;

  public readonly isConfirmModalOpen = signal<boolean>(false);
  public readonly isStatusChanging = signal<boolean>(false);
  public readonly confirmData = signal<{ id: number; status: string; label: string } | null>(null);

  ngOnInit(): void {
    this.initForm();
    this.setupFormListeners();
    this.refreshData();
  }

  // ==========================================
  // NAVIGATION ACTIONS
  // ==========================================
  
  public navigateToSubjects(promotionId: number): void {
    if (!promotionId) return;
    this.router.navigate(['/admin/promotions/accredited', promotionId, 'subjects']);
  }

  // ==========================================
  // BUSINESS ENGINE METHODS
  // ==========================================

  public refreshData(): void {
    this.loadPromotions();
    this.loadStatistics();
    this.loadOngoingPromotions();
  }

  public initForm(): void {
    this.createForm = this.fb.group({
      name: ['', Validators.required],
      levelId: ['', Validators.required],
      trainingId: [{value: '', disabled: true}, Validators.required],
      academicYearId: ['', Validators.required],
      capacity: ['', [Validators.required, Validators.min(1)]],
      registrationFee: ['', [Validators.required, Validators.min(0)]],
      tuitionFee: ['', [Validators.required, Validators.min(0)]],
      registrationOpeningDate: ['', Validators.required],
      registrationDeadline: ['', Validators.required]
    });

    this.editForm = this.fb.group({
      name: ['', Validators.required],
      capacity: ['', [Validators.required, Validators.min(1)]],
      fee: ['', [Validators.required, Validators.min(0)]],
      registrationOpeningDate: ['', Validators.required],
      registrationDeadline: ['', Validators.required]
    });
  }

  public setupFormListeners(): void {
    this.createForm
      .get('levelId')
      ?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(id => {
        if (id) this.loadTrainingsByLevel(id);
        else {
          this.activeTrainings.set([]);
          this.createForm.get('trainingId')?.disable({emitEvent: false});
          this.createForm.get('trainingId')?.setValue('');
        }
      });
  }

  public loadOngoingPromotions(): void {
    this.isOngoingLoading.set(true);
    this.promotionService
      .getOngoingPromotions(4)
      .pipe(finalize(() => this.isOngoingLoading.set(false)))
      .subscribe({
        next: (response: any) => {
          if (response.success && response.data) {
            this.ongoingPromotions.set(response.data);
          }
        },
        error: (err: any) => console.error('Error loading Ongoing', err)
      });
  }

  public loadStatistics(): void {
    this.isStatsLoading.set(true);
    this.promotionService
      .getAccreditedPromotionStatistics()
      .pipe(finalize(() => this.isStatsLoading.set(false)))
      .subscribe({
        next: (response: any) => {
          if (response.success && response.data) {
            const newStats = {
              DRAFT: {count: 0, enrollments: 0},
              ENROLLMENT: {count: 0, enrollments: 0},
              IN_PROGRESS: {count: 0, enrollments: 0},
              EVALUATION: {count: 0, enrollments: 0}
            };
            response.data.forEach((stat: any) => {
              const statusKey = stat.status.toUpperCase();
              if (newStats[statusKey as keyof typeof newStats]) {
                newStats[statusKey as keyof typeof newStats].count = stat.promotionCount;
                newStats[statusKey as keyof typeof newStats].enrollments = stat.totalEnrollments;
              }
            });
            this.promotionStats.set(newStats);
          }
        },
        error: (err: any) => console.error('Error stats', err)
      });
  }

  public loadPromotions(): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.promotionService
      .getAllAccreditedPromotions(this.currentPage(), this.pageSize())
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response: any) => {
          if (response.success && response.data) {
            this.promotions.set(response.data.content || []);
            this.totalElements.set(response.data.totalElements || 0);
          }
        },
        error: () => this.error.set('Impossible de charger les données.')
      });
  }

  public onPageChange(newPage: number) {
    this.currentPage.set(newPage);
    this.loadPromotions();
  }

  public openOngoingModal(): void {
    this.isOngoingModalOpen.set(true);
    this.loadAllOngoingPromotions();
  }

  public closeOngoingModal(): void {
    this.isOngoingModalOpen.set(false);
    this.allOngoingPromotions.set([]);
  }

  public loadAllOngoingPromotions(): void {
    this.isAllOngoingLoading.set(true);
    this.promotionService
      .getOngoingPromotions(1000)
      .pipe(finalize(() => this.isAllOngoingLoading.set(false)))
      .subscribe({
        next: (response: any) => {
          if (response.success && response.data) {
            this.allOngoingPromotions.set(response.data);
          }
        },
        error: (err: any) => console.error('Error loading all active sessions', err)
      });
  }

  public openModal(): void {
    this.isModalOpen.set(true);
    if (this.openAcademicYears().length === 0 || this.activeLevels().length === 0)
      this.loadModalInitialData();
  }

  public closeModal(): void {
    this.isModalOpen.set(false);
    this.createForm.reset({
      name: '',
      levelId: '',
      academicYearId: '',
      capacity: '',
      registrationFee: '',
      tuitionFee: '',
      registrationOpeningDate: '',
      registrationDeadline: ''
    });
    this.activeTrainings.set([]);
    this.createForm.get('trainingId')?.setValue('');
    this.createForm.get('trainingId')?.disable({emitEvent: false});
  }

  public loadModalInitialData(): void {
    this.isModalLoading.set(true);
    forkJoin({
      academicYears: this.academicYearService.getOpenAcademicYears(),
      levels: this.levelService.getActiveLevels()
    })
      .pipe(finalize(() => this.isModalLoading.set(false)))
      .subscribe({
        next: (res: any) => {
          if (res.academicYears?.success) this.openAcademicYears.set(res.academicYears.data);
          if (res.levels?.success) this.activeLevels.set(res.levels.data);
        }
      });
  }

  public loadTrainingsByLevel(levelId: number): void {
    this.isTrainingsLoading.set(true);
    this.createForm.get('trainingId')?.disable({emitEvent: false});
    this.trainingService
      .getActiveTrainingsByLevel(levelId, 'ACCREDITED' as any)
      .pipe(
        finalize(() => {
          this.isTrainingsLoading.set(false);
          this.createForm.get('trainingId')?.enable({emitEvent: false});
        })
      )
      .subscribe({
        next: (res: any) => {
          if (res.success) this.activeTrainings.set(res.data);
        }
      });
  }

  public onSubmit(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      this.toast.error('Veuillez remplir tous les champs obligatoires.');
      return;
    }
    this.isModalLoading.set(true);
    this.promotionService
      .createAccreditedPromotion(this.createForm.getRawValue())
      .pipe(finalize(() => this.isModalLoading.set(false)))
      .subscribe({
        next: () => {
          this.toast.success('Session créée avec succès !');
          this.closeModal();
          this.refreshData();
        },
        error: (err: any) => {
          let msg = 'Erreur lors de la création.';
          if (err.error?.message === 'ACCREDITED_PROMOTION_ALREADY_EXISTS_FOR_THIS_YEAR')
            msg = 'Une session existe déjà pour cette formation et année.';
          this.toast.error(msg);
        }
      });
  }

  public openEditModal(id: number): void {
    this.currentEditId.set(id);
    this.isEditModalOpen.set(true);
    this.isEditFetching.set(true);
    this.promotionService
      .getAccreditedPromotionById(id)
      .pipe(finalize(() => this.isEditFetching.set(false)))
      .subscribe({
        next: (response: any) => {
          if (response.success && response.data) {
            const data = response.data;
            this.editForm.patchValue({
              name: data.name,
              capacity: data.capacity || 1,
              fee: data.fee || data.tuitionFee || 0,
              registrationOpeningDate: data.registrationOpeningDate,
              registrationDeadline: data.registrationDeadline
            });
          }
        },
        error: () => {
          this.toast.error('Erreur lors du chargement des données.');
          this.closeEditModal();
        }
      });
  }

  public closeEditModal(): void {
    this.isEditModalOpen.set(false);
    this.editForm.reset({
      name: '',
      capacity: '',
      fee: '',
      registrationOpeningDate: '',
      registrationDeadline: ''
    });
    this.currentEditId.set(null);
  }

  public onEditSubmit(): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }
    const id = this.currentEditId();
    if (!id) return;
    this.isEditLoading.set(true);
    this.promotionService
      .updateAccreditedPromotion(id, this.editForm.getRawValue())
      .pipe(finalize(() => this.isEditLoading.set(false)))
      .subscribe({
        next: () => {
          this.toast.success('Session mise à jour !');
          this.closeEditModal();
          this.refreshData();
        },
        error: (err: any) => this.toast.error(err.error?.message || 'Erreur.')
      });
  }

  public openDetailsModal(id: number): void {
    this.isDetailsModalOpen.set(true);
    this.isDetailsLoading.set(true);
    this.selectedPromotion.set(null);
    this.promotionService
      .getAccreditedPromotionById(id)
      .pipe(finalize(() => this.isDetailsLoading.set(false)))
      .subscribe({
        next: (response: any) => {
          if (response.success) this.selectedPromotion.set(response.data);
        },
        error: () => this.toast.error('Impossible de charger les détails.')
      });
  }

  public closeDetailsModal(): void {
    this.isDetailsModalOpen.set(false);
    this.selectedPromotion.set(null);
  }

  public requestStatusChange(id: number, newStatus: string): void {
    this.confirmData.set({id, status: newStatus, label: this.getStatusLabel(newStatus)});
    this.isConfirmModalOpen.set(true);
  }

  public closeConfirmModal(): void {
    this.isConfirmModalOpen.set(false);
    this.confirmData.set(null);
  }

  public confirmStatusChange(): void {
    const data = this.confirmData();
    if (!data) return;

    this.isStatusChanging.set(true);
    this.promotionService
      .changePromotionStatus(data.id, data.status as any)
      .pipe(
        finalize(() => {
          this.isStatusChanging.set(false);
          this.closeConfirmModal();
        })
      )
      .subscribe({
        next: () => {
          this.toast.success(`Le statut a été passé en "${data.label}" avec succès !`);
          this.openDetailsModal(data.id);
          this.refreshData();
        },
        error: (err: any) => {
          this.toast.error(err.error?.message || 'Erreur lors du changement de statut.');
        }
      });
  }

  public getCardTheme(index: number) {
    const themes = [
      {
        border: 'border-indigo-500',
        bg: 'bg-indigo-500',
        lightBg: 'bg-indigo-50',
        text: 'text-indigo-600',
        hoverBg: 'hover:bg-indigo-100'
      },
      {
        border: 'border-amber-500',
        bg: 'bg-amber-500',
        lightBg: 'bg-amber-50',
        text: 'text-amber-600',
        hoverBg: 'hover:bg-amber-100'
      },
      {
        border: 'border-emerald-500',
        bg: 'bg-emerald-500',
        lightBg: 'bg-emerald-50',
        text: 'text-emerald-600',
        hoverBg: 'hover:bg-emerald-100'
      },
      {
        border: 'border-rose-500',
        bg: 'bg-rose-500',
        lightBg: 'bg-rose-50',
        text: 'text-rose-600',
        hoverBg: 'hover:bg-rose-100'
      }
    ];
    return themes[index % 4];
  }

  public calculateProgress(start: string, end: string): number {
    if (!start || !end) return 0;
    const startDate = new Date(start).getTime();
    const endDate = new Date(end).getTime();
    const now = new Date().getTime();

    if (now <= startDate) return 0;
    if (now >= endDate) return 100;

    const total = endDate - startDate;
    const current = now - startDate;
    return Math.round((current / total) * 100);
  }

  public getRemainingDays(end: string): number | string {
    if (!end) return '?';
    const endDate = new Date(end).getTime();
    const now = new Date().getTime();
    const diff = endDate - now;
    if (diff < 0) return 0;
    return Math.ceil(diff / (1000 * 3600 * 24));
  }

  public getCapacityPercentage(enrollment: number, capacity: number): number {
    if (!capacity || capacity <= 0) return 0;
    const percent = (enrollment / capacity) * 100;
    return percent > 100 ? 100 : percent;
  }

  public getStatusStyle(status: string | null | undefined): string {
    if (!status) return 'bg-gray-100 text-gray-500 border-gray-200';
    switch (status.toUpperCase()) {
      case 'DRAFT':
        return 'bg-slate-100 text-slate-600 border-slate-200';
      case 'OPEN':
      case 'ENROLLMENT':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'IN_PROGRESS':
        return 'bg-indigo-50 text-indigo-700 border-indigo-100';
      case 'EVALUATION':
        return 'bg-purple-50 text-purple-700 border-purple-200';
      case 'CLOSED':
        return 'bg-slate-100 text-slate-700 border-slate-200';
      case 'CANCELLED':
        return 'bg-rose-50 text-rose-700 border-rose-200';
      default:
        return 'bg-gray-100 text-gray-700 border-gray-200';
    }
  }

  public getStatusLabel(status: string | null | undefined): string {
    if (!status) return 'Non défini';
    switch (status.toUpperCase()) {
      case 'DRAFT':
        return 'Brouillon';
      case 'OPEN':
      case 'ENROLLMENT':
        return 'Inscriptions Ouvertes';
      case 'IN_PROGRESS':
        return 'En Cours';
      case 'EVALUATION':
        return 'En Évaluation';
      case 'CLOSED':
        return 'Clôturée';
      case 'CANCELLED':
        return 'Annulée';
      default:
        return status;
    }
  }
}
