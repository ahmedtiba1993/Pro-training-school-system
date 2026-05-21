import { Component, OnInit, inject, signal, computed, DestroyRef } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';

import {
  PromotionSubjectControllerService,
  PeriodControllerService,
  SubjectControllerService,
  PromotionStatsResponse,
  PeriodResponse,
  PromotionSubjectResponse,
  SubjectShortResponse,
  PromotionSubjectRequest
} from '../../../../../core/api';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-promotion-subject',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe, ReactiveFormsModule],
  templateUrl: './promotion-subject.html',
  styleUrl: './promotion-subject.css'
})
export class PromotionSubject implements OnInit {
  // --- DEPENDENCY INJECTIONS ---
  private readonly route = inject(ActivatedRoute);
  private readonly promotionSubjectService = inject(PromotionSubjectControllerService);
  private readonly periodService = inject(PeriodControllerService);
  private readonly subjectService = inject(SubjectControllerService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toastService = inject(ToastService);

  // --- DATA STATE SIGNALS ---
  public readonly promotionId = signal<number | null>(null);
  public readonly stats = signal<PromotionStatsResponse | null>(null);
  public readonly periods = signal<PeriodResponse[]>([]);
  public readonly subjectsByPeriod = signal<Record<number, PromotionSubjectResponse[]>>({});
  public readonly flatSubjects = signal<PromotionSubjectResponse[]>([]);
  public readonly availableSubjects = signal<SubjectShortResponse[]>([]);

  // --- INTERFACE & UI STATE SIGNALS ---
  public readonly expandedPeriods = signal<Set<number>>(new Set());
  public readonly loadingSubjectsPeriods = signal<Set<number>>(new Set());

  // Modals
  public readonly isAssignmentModalOpen = signal<boolean>(false);
  public readonly selectedPeriodForAssignment = signal<PeriodResponse | null>(null);
  public readonly isDeleteModalOpen = signal<boolean>(false);
  public readonly idSubjectToDelete = signal<number | null>(null);
  public readonly targetPeriodIdForDelete = signal<number | null>(null);

  public assignmentForm!: FormGroup;

  // Reactive loading states
  public readonly isStatsLoading = signal<boolean>(true);
  public readonly isPeriodsLoading = signal<boolean>(false);
  public readonly isFlatSubjectsLoading = signal<boolean>(false);
  public readonly isSubjectsLoading = signal<boolean>(false);
  public readonly isSubmitting = signal<boolean>(false);
  public readonly isDeleting = signal<boolean>(false);
  public readonly error = signal<string | null>(null);

  // --- COMPUTED SIGNALS ---
  public readonly backRoute = computed<string>(() => {
    const type = this.stats()?.promotionType;
    if (!type) return '/promotions/accredited';
    return `/admin/promotions/${type.toLowerCase()}`;
  });

  public readonly isAccredited = computed<boolean>(() => {
    return this.stats()?.promotionType === 'ACCREDITED';
  });

  public readonly flatTotalCoef = computed(() =>
    this.flatSubjects().reduce((sum, item) => sum + (item.coefficient || 0), 0)
  );

  public readonly flatTotalHours = computed(() =>
    this.flatSubjects().reduce((sum, item) => sum + (item.totalHours || 0), 0)
  );

  public ngOnInit(): void {
    this.initAssignmentForm();
    this.extractRouteParams();
  }

  private initAssignmentForm(): void {
    this.assignmentForm = this.fb.group({
      subjectId: ['', Validators.required],
      coefficient: ['', [Validators.required, Validators.min(1), Validators.max(10)]]
    });

    this.assignmentForm
      .get('subjectId')
      ?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(id => {
        if (!id) return;
        const selectedSubject = this.availableSubjects().find(s => s.id === +id);
        if (selectedSubject && selectedSubject.defaultCoefficient != null) {
          this.assignmentForm.get('coefficient')?.setValue(selectedSubject.defaultCoefficient);
        }
      });
  }

  public openAssignmentModal(period?: PeriodResponse): void {
    this.selectedPeriodForAssignment.set(period || null);
    this.assignmentForm.reset({ subjectId: '', coefficient: '' });
    this.isAssignmentModalOpen.set(true);

    const currentPromotionId = this.promotionId();
    if (currentPromotionId && this.availableSubjects().length === 0) {
      this.loadActiveSubjects(currentPromotionId);
    }
  }

  public closeAssignmentModal(): void {
    this.isAssignmentModalOpen.set(false);
    this.selectedPeriodForAssignment.set(null);
  }

  private loadActiveSubjects(promotionId: number): void {
    this.isSubjectsLoading.set(true);
    this.subjectService
      .getCatalogSubjectsByPromotionId(promotionId)
      .pipe(finalize(() => this.isSubjectsLoading.set(false)))
      .subscribe({
        next: (response: any) => {
          if (response?.success && response?.data) {
            this.availableSubjects.set(response.data as SubjectShortResponse[]);
          }
        },
        error: err => console.error('Error loading subjects', err)
      });
  }

  public onAssignmentSubmit(): void {
    if (this.assignmentForm.invalid) {
      this.assignmentForm.markAllAsTouched();
      return;
    }

    const currentPromoId = this.promotionId();
    if (!currentPromoId) return;

    const payload: PromotionSubjectRequest = {
      promotionId: currentPromoId,
      subjectId: Number(this.assignmentForm.value.subjectId),
      academicPeriodId: this.isAccredited() ? this.selectedPeriodForAssignment()?.id : undefined,
      coefficient: Number(this.assignmentForm.value.coefficient)
    };

    this.isSubmitting.set(true);
    this.error.set(null);

    this.promotionSubjectService
      .create(payload)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (response: any) => {
          if (response && response.success === false) {
            this.handleBackendError(response);
            return;
          }

          this.toastService.success('La matière a été affectée avec succès !');
          this.closeAssignmentModal();

          if (this.isAccredited()) {
            const periodId = this.selectedPeriodForAssignment()?.id;
            if (periodId) {
              this.loadSubjectsForPeriod(periodId);
              if (!this.expandedPeriods().has(periodId)) {
                this.expandedPeriods.update(set => new Set(set).add(periodId));
              }
            }
          } else {
            this.loadFlatSubjects(currentPromoId);
          }

          this.refreshPromotionStats(currentPromoId);
        },
        error: (err: HttpErrorResponse) => this.handleBackendError(err.error)
      });
  }

  // --- DELETION ACTIONS WITH SECURE ERROR HANDLING ---
  public openDeleteModal(promotionSubjectId: number, periodId?: number): void {
    this.idSubjectToDelete.set(promotionSubjectId);
    this.targetPeriodIdForDelete.set(periodId || null);
    this.isDeleteModalOpen.set(true);
  }

  public closeDeleteModal(): void {
    this.isDeleteModalOpen.set(false);
    this.idSubjectToDelete.set(null);
    this.targetPeriodIdForDelete.set(null);
  }

  public onDeleteConfirm(): void {
    const idToDelete = this.idSubjectToDelete();
    const currentPromoId = this.promotionId();

    if (!idToDelete || !currentPromoId) return;

    this.isDeleting.set(true);
    this.promotionSubjectService
      .deletePromotionSubject(idToDelete)
      .pipe(finalize(() => this.isDeleting.set(false)))
      .subscribe({
        next: (response: any) => {
          if (response && response.success === false) {
            this.handleDeleteBackendError(response);
            return;
          }

          this.toastService.success('La matière a été retirée avec succès !');
          this.closeDeleteModal();

          if (this.isAccredited()) {
            const periodId = this.targetPeriodIdForDelete();
            if (periodId) this.loadSubjectsForPeriod(periodId);
          } else {
            this.loadFlatSubjects(currentPromoId);
          }

          this.refreshPromotionStats(currentPromoId);
        },
        error: (err: HttpErrorResponse) => {
          this.handleDeleteBackendError(err.error);
          this.closeDeleteModal();
        }
      });
  }

  /**
   * Targeted error handler for deletion
   */
  private handleDeleteBackendError(errorBody: any): void {
    if (!errorBody) {
      this.toastService.error('Une erreur réseau inconnue est survenue.');
      return;
    }

    // Specific capture of the error on the promotion status
    if (
      errorBody.errorCode === 'ERR_VALIDATION' &&
      errorBody.message === 'PROMOTION_STATUS_FORBIDS_SUBJECT_DELETION'
    ) {
      this.toastService.error(
        'Impossible de supprimer la matière : le statut actuel de la promotion bloque toute modification.'
      );
    } else {
      this.toastService.error(errorBody.message || 'Une erreur est survenue lors du retrait.');
    }
  }

  private handleBackendError(errorBody: any): void {
    if (!errorBody) {
      this.toastService.error('Une erreur réseau inconnue est survenue.');
      return;
    }
    if (
      errorBody.errorCode === 'ENTITY_ALREADY_EXISTS' ||
      errorBody.message === 'SUBJECT_ALREADY_ASSIGNED_TO_THIS_PERIOD'
    ) {
      this.toastService.error('Cette matière est déjà affectée à ce programme.');
    } else {
      this.toastService.error(errorBody.message || "Erreur lors de l'affectation.");
    }
  }

  public togglePeriod(periodId: number): void {
    if (!periodId) return;
    const isExpanded = this.expandedPeriods().has(periodId);
    this.expandedPeriods.update(set => {
      const nextSet = new Set(set);
      if (isExpanded) nextSet.delete(periodId);
      else nextSet.add(periodId);
      return nextSet;
    });

    if (!isExpanded && !this.subjectsByPeriod()[periodId]) {
      this.loadSubjectsForPeriod(periodId);
    }
  }

  private loadSubjectsForPeriod(periodId: number): void {
    const promoId = this.promotionId();
    if (!promoId) return;

    this.loadingSubjectsPeriods.update(set => new Set(set).add(periodId));
    this.promotionSubjectService
      .getSubjectsByPromotionAndPeriod(promoId, periodId)
      .pipe(
        finalize(() =>
          this.loadingSubjectsPeriods.update(set => {
            const nextSet = new Set(set);
            nextSet.delete(periodId);
            return nextSet;
          })
        )
      )
      .subscribe({
        next: (res: any) => {
          if (res?.success) {
            this.subjectsByPeriod.update(cache => ({ ...cache, [periodId]: res.data }));
          }
        }
      });
  }

  private loadFlatSubjects(promotionId: number): void {
    this.isFlatSubjectsLoading.set(true);
    this.promotionSubjectService
      .getSubjectsByPromotionId(promotionId)
      .pipe(finalize(() => this.isFlatSubjectsLoading.set(false)))
      .subscribe({
        next: (res: any) => {
          if (res?.success) this.flatSubjects.set(res.data);
        },
        error: err => console.error('Error loading global subjects', err)
      });
  }

  private refreshPromotionStats(promotionId: number): void {
    this.promotionSubjectService.getPromotionStats(promotionId).subscribe({
      next: (res: any) => {
        if (res?.success) this.stats.set(res.data);
      }
    });
  }

  public isPeriodExpanded(periodId: number): boolean {
    return this.expandedPeriods().has(periodId);
  }
  public isPeriodSubjectsLoading(periodId: number): boolean {
    return this.loadingSubjectsPeriods().has(periodId);
  }

  public getPeriodCoefTotal(periodId: number): number {
    return (this.subjectsByPeriod()[periodId] || []).reduce(
      (sum, item) => sum + (item.coefficient || 0),
      0
    );
  }

  public getPeriodHoursTotal(periodId: number): number {
    return (this.subjectsByPeriod()[periodId] || []).reduce(
      (sum, item) => sum + (item.totalHours || 0),
      0
    );
  }

  private extractRouteParams(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = +idParam;
      this.promotionId.set(id);
      this.loadInitialContext(id);
    }
  }

  private loadInitialContext(promotionId: number): void {
    this.error.set(null);
    this.isStatsLoading.set(true);

    this.promotionSubjectService
      .getPromotionStats(promotionId)
      .pipe(finalize(() => this.isStatsLoading.set(false)))
      .subscribe({
        next: (res: any) => {
          if (res?.success) {
            this.stats.set(res.data);
            if (this.isAccredited()) {
              this.loadPeriodsStructure(promotionId);
            } else {
              this.loadFlatSubjects(promotionId);
            }
          }
        },
        error: () =>
          this.error.set('A network error occurred while loading statistics.')
      });
  }

  private loadPeriodsStructure(promotionId: number): void {
    this.isPeriodsLoading.set(true);
    this.periodService
      .getPeriodsByPromotionId(promotionId)
      .pipe(finalize(() => this.isPeriodsLoading.set(false)))
      .subscribe({
        next: (res: any) => {
          if (res?.success && res.data) {
            const sorted = (res.data as PeriodResponse[]).sort(
              (a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0)
            );
            this.periods.set(sorted);
          }
        }
      });
  }
}
