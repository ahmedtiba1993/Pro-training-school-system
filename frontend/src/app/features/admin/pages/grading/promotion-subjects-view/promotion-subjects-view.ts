import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { ToastService } from '../../../../../shared/services/toast.service';
import {
  PromotionSubjectControllerService,
  PeriodControllerService,
  AssessmentControllerService,
  PromotionStatsResponse,
  PeriodResponse,
  PromotionSubjectResponse,
  AssessmentRequest,
  AssessmentResponse
} from '../../../../../core/api';

export interface AssessmentTypeOption {
  value: AssessmentRequest.AssessmentTypeEnum;
  label: string;
  badge: string;
  description: string;
  category: string;
}

export interface AssessmentStatusOption {
  value: AssessmentResponse.StatusEnum;
  label: string;
  code: string;
  dotColor: string;
  badgeClasses: string;
}

export interface AssessmentStatusConfirmModal {
  assessment: AssessmentResponse;
  action: 'plan' | 'open_grading' | 'submit' | 'reject' | 'publish' | 'cancel' | 'lock' | 'delete';
  fromStatus: string;
  toStatus: string;
  title: string;
  message: string;
  confirmLabel: string;
  confirmButtonClass: string;
}

export interface AssessmentCreateConfirmModal {
  request: AssessmentRequest;
  subjectName: string;
  typeFrenchName: string;
  description?: string;
}

export interface AssessmentEditConfirmModal {
  assessmentId: number;
  request: AssessmentRequest;
  subjectName: string;
  typeFrenchName: string;
  oldTitle: string;
  newTitle: string;
  oldWeight: number;
  newWeight: number;
  oldMarks: number;
  newMarks: number;
  oldType?: string;
  newType: string;
}

@Component({
  selector: 'app-promotion-subjects-view',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe, ReactiveFormsModule, FormsModule],
  templateUrl: './promotion-subjects-view.html',
  styleUrl: './promotion-subjects-view.css'
})
export class PromotionSubjectsView implements OnInit {
  // --- DEPENDENCY INJECTIONS ---
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly toastService = inject(ToastService);
  private readonly promotionSubjectService = inject(PromotionSubjectControllerService);
  private readonly periodService = inject(PeriodControllerService);
  private readonly assessmentService = inject(AssessmentControllerService);

  // --- ASSESSMENT TYPES LIST (ASSESSMENT_TYPE) ---
  public readonly assessmentTypes: AssessmentTypeOption[] = [
    {
      value: AssessmentRequest.AssessmentTypeEnum.Ds,
      label: 'Devoir Surveillé (Contrôle écrit en classe)',
      badge: 'DS (Contrôle écrit)',
      description: 'Devoir Surveillé (Contrôle écrit en classe)',
      category: 'Contrôle Continu (Géré par le professeur)'
    },
    {
      value: AssessmentRequest.AssessmentTypeEnum.Tp,
      label: 'Travaux Pratiques (Note de labo ou d\'atelier)',
      badge: 'TP (Labo/Atelier)',
      description: 'Travaux Pratiques (Note de labo ou d\'atelier)',
      category: 'Contrôle Continu (Géré par le professeur)'
    },
    {
      value: AssessmentRequest.AssessmentTypeEnum.FinalExam,
      label: 'Examen Principal de fin de semestre',
      badge: 'Examen Final',
      description: 'Examen Principal de fin de semestre',
      category: 'Examens'
    },
    {
      value: AssessmentRequest.AssessmentTypeEnum.Retake,
      label: 'Examen de Rattrapage',
      badge: 'Rattrapage',
      description: 'Examen de Rattrapage',
      category: 'Examens'
    }
  ];

  // --- ASSESSMENT STATUSES LIST (ASSESSMENT_STATUS) ---
  public readonly assessmentStatuses: AssessmentStatusOption[] = [
    {
      value: AssessmentResponse.StatusEnum.Draft,
      label: 'Brouillon',
      code: 'DRAFT',
      dotColor: 'bg-slate-400',
      badgeClasses: 'bg-slate-100 text-slate-700 border-slate-300'
    },
    {
      value: AssessmentResponse.StatusEnum.Planned,
      label: 'Planifiée',
      code: 'PLANNED',
      dotColor: 'bg-blue-500',
      badgeClasses: 'bg-blue-50 text-blue-700 border-blue-200'
    },
    {
      value: AssessmentResponse.StatusEnum.GradingInProgress,
      label: 'En cours de notation',
      code: 'GRADING_IN_PROGRESS',
      dotColor: 'bg-amber-500',
      badgeClasses: 'bg-amber-50 text-amber-700 border-amber-200'
    },
    {
      value: AssessmentResponse.StatusEnum.SubmittedToAdmin,
      label: 'Soumise à l\'administration',
      code: 'SUBMITTED_TO_ADMIN',
      dotColor: 'bg-indigo-500',
      badgeClasses: 'bg-indigo-50 text-indigo-700 border-indigo-200'
    },
    {
      value: AssessmentResponse.StatusEnum.Published,
      label: 'Publiée',
      code: 'PUBLISHED',
      dotColor: 'bg-emerald-500',
      badgeClasses: 'bg-emerald-50 text-emerald-700 border-emerald-200'
    },
    {
      value: AssessmentResponse.StatusEnum.Cancelled,
      label: 'Annulée',
      code: 'CANCELLED',
      dotColor: 'bg-rose-500',
      badgeClasses: 'bg-rose-50 text-rose-700 border-rose-200'
    },
    {
      value: AssessmentResponse.StatusEnum.Locked,
      label: 'Verrouillée',
      code: 'LOCKED',
      dotColor: 'bg-purple-500',
      badgeClasses: 'bg-purple-50 text-purple-700 border-purple-200'
    }
  ];

  // --- DATA STATE SIGNALS ---
  public readonly promotionId = signal<number | null>(null);
  public readonly stats = signal<PromotionStatsResponse | null>(null);
  public readonly periods = signal<PeriodResponse[]>([]);
  public readonly subjectsByPeriod = signal<Record<number, PromotionSubjectResponse[]>>({});
  public readonly flatSubjects = signal<PromotionSubjectResponse[]>([]);

  // --- ASSESSMENTS PER SUBJECT (LOADED FROM API) ---
  public readonly assessmentsBySubject = signal<Record<number, AssessmentResponse[]>>({});
  public readonly isAssessmentsListLoading = signal<boolean>(false);
  public readonly listModalError = signal<string | null>(null);

  // --- UI STATE SIGNALS ---
  public readonly expandedPeriods = signal<Set<number>>(new Set());
  public readonly loadingSubjectsPeriods = signal<Set<number>>(new Set());
  public readonly isStatsLoading = signal<boolean>(true);
  public readonly isPeriodsLoading = signal<boolean>(false);
  public readonly isFlatSubjectsLoading = signal<boolean>(false);
  public readonly error = signal<string | null>(null);

  // --- ADD ASSESSMENT MODAL (API) ---
  public readonly isModalOpen = signal<boolean>(false);
  public readonly selectedSubject = signal<PromotionSubjectResponse | null>(null);
  public readonly isSubmittingAssessment = signal<boolean>(false);
  public readonly modalErrorMessage = signal<string | null>(null);
  public readonly confirmCreateModal = signal<AssessmentCreateConfirmModal | null>(null);

  // --- ASSESSMENTS LIST MODAL ---
  public readonly isListModalOpen = signal<boolean>(false);
  public readonly selectedSubjectForList = signal<PromotionSubjectResponse | null>(null);

  // --- STATUS WORKFLOW MANAGEMENT (STATE MACHINE) ---
  public readonly updatingStatusAssessmentId = signal<number | null>(null);
  public readonly confirmStatusModal = signal<AssessmentStatusConfirmModal | null>(null);

  // Empty form group
  public readonly assessmentForm: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(150)]],
    type: ['', [Validators.required]],
    weight: [null, [Validators.required, Validators.min(0), Validators.max(100)]],
    maxScore: [null, [Validators.required, Validators.min(0.5)]],
    description: ['']
  });

  // --- EDIT ASSESSMENT MODAL (API PUT) ---
  public readonly editingAssessment = signal<AssessmentResponse | null>(null);
  public readonly isSubmittingEdit = signal<boolean>(false);
  public readonly editErrorMessage = signal<string | null>(null);
  public readonly confirmEditModal = signal<AssessmentEditConfirmModal | null>(null);

  public readonly editAssessmentForm: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(150)]],
    type: ['', [Validators.required]],
    weight: [null, [Validators.required, Validators.min(0), Validators.max(100)]],
    maxScore: [null, [Validators.required, Validators.min(0.5)]],
    description: ['']
  });

  // --- COMPUTED SIGNALS ---
  public readonly isAccredited = computed<boolean>(() => {
    return this.stats()?.promotionType === 'ACCREDITED';
  });

  public readonly flatTotalCoef = computed(() =>
    this.flatSubjects().reduce((sum, item) => sum + (item.coefficient || 0), 0)
  );

  public readonly currentSubjectAssessments = computed<AssessmentResponse[]>(() => {
    const sub = this.selectedSubjectForList();
    if (!sub?.id) return [];
    return this.assessmentsBySubject()[sub.id] || [];
  });

  public readonly currentSubjectTotalWeight = computed<number>(() => {
    return this.currentSubjectAssessments().reduce(
      (sum, item) => sum + (item.weightPercentage || 0),
      0
    );
  });

  public readonly backRoute = '/admin/grading';

  public ngOnInit(): void {
    this.extractRouteParams();
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
          this.error.set('Une erreur réseau est survenue lors du chargement des statistiques de la promotion.')
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

  private loadFlatSubjects(promotionId: number): void {
    this.isFlatSubjectsLoading.set(true);
    this.promotionSubjectService
      .getSubjectsByPromotionId(promotionId)
      .pipe(finalize(() => this.isFlatSubjectsLoading.set(false)))
      .subscribe({
        next: (res: any) => {
          if (res?.success) {
            this.flatSubjects.set(res.data);
          }
        },
        error: err => console.error('Error loading global subjects', err)
      });
  }

  public togglePeriod(periodId: number): void {
    if (!periodId) return;
    const isExpanded = this.expandedPeriods().has(periodId);
    this.expandedPeriods.update(set => {
      const nextSet = new Set(set);
      if (isExpanded) {
        nextSet.delete(periodId);
      } else {
        nextSet.add(periodId);
      }
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

  // --- ASSESSMENTS LIST MODAL (API) ---
  public openAssessmentsListModal(subject: PromotionSubjectResponse): void {
    this.selectedSubjectForList.set(subject);
    this.listModalError.set(null);
    this.isListModalOpen.set(true);
    if (subject.id) {
      this.loadAssessmentsForSubject(subject.id);
    }
  }

  public loadAssessmentsForSubject(promotionSubjectId: number): void {
    this.isAssessmentsListLoading.set(true);
    this.listModalError.set(null);

    this.assessmentService
      .getAssessmentsByPromotionSubject(promotionSubjectId)
      .pipe(finalize(() => this.isAssessmentsListLoading.set(false)))
      .subscribe({
        next: (res: any) => {
          if (res?.success && res.data) {
            this.assessmentsBySubject.update(cache => ({
              ...cache,
              [promotionSubjectId]: res.data
            }));
          }
        },
        error: (err: any) => {
          const msg = this.translateApiError(err);
          this.listModalError.set(msg);
        }
      });
  }

  public closeAssessmentsListModal(): void {
    this.isListModalOpen.set(false);
    this.selectedSubjectForList.set(null);
    this.listModalError.set(null);
  }

  public openAddAssessmentFromList(): void {
    const sub = this.selectedSubjectForList();
    this.closeAssessmentsListModal();
    if (sub) {
      this.openAddAssessmentModal(sub);
    }
  }

  // --- ASSESSMENT FORM MANAGEMENT AND API CALLS ---
  public openAddAssessmentModal(subject: PromotionSubjectResponse): void {
    this.selectedSubject.set(subject);
    this.modalErrorMessage.set(null);
    this.assessmentForm.get('weight')?.enable();
    this.assessmentForm.reset({
      title: '',
      type: '',
      weight: null,
      maxScore: null,
      description: ''
    });
    this.isModalOpen.set(true);
  }

  public closeAddAssessmentModal(): void {
    if (this.isSubmittingAssessment()) return;
    this.confirmCreateModal.set(null);
    this.isModalOpen.set(false);
    this.selectedSubject.set(null);
    this.modalErrorMessage.set(null);
  }

  public onTypeChange(type: string): void {
    const weightControl = this.assessmentForm.get('weight');
    if (type === AssessmentRequest.AssessmentTypeEnum.Retake || type === 'RETAKE') {
      weightControl?.setValue(0);
      weightControl?.disable();
    } else {
      if (weightControl?.disabled) {
        weightControl?.enable();
        weightControl?.setValue(null);
      }
    }
  }

  public getSelectedTypeFrenchName(): string {
    const typeVal = this.assessmentForm.get('type')?.value;
    if (!typeVal) return '';
    const found = this.assessmentTypes.find(t => t.value === typeVal);
    return found ? found.badge : typeVal;
  }

  public getAssessmentTypeLabel(type?: string): string {
    if (!type) return '';
    const found = this.assessmentTypes.find(t => t.value === type);
    return found ? found.label : type;
  }

  public getAssessmentTypeBadgeText(type?: string): string {
    if (!type) return '';
    const found = this.assessmentTypes.find(t => t.value === type);
    return found ? found.badge : type;
  }

  public getAssessmentTypeBadgeClasses(type?: string): string {
    switch (type) {
      case 'DS':
      case AssessmentRequest.AssessmentTypeEnum.Ds:
        return 'bg-indigo-50 text-indigo-700 border-indigo-200';
      case 'TP':
      case AssessmentRequest.AssessmentTypeEnum.Tp:
        return 'bg-teal-50 text-teal-700 border-teal-200';
      case 'FINAL_EXAM':
      case AssessmentRequest.AssessmentTypeEnum.FinalExam:
        return 'bg-rose-50 text-rose-700 border-rose-200';
      case 'RETAKE':
      case AssessmentRequest.AssessmentTypeEnum.Retake:
        return 'bg-amber-50 text-amber-700 border-amber-200';
      default:
        return 'bg-slate-50 text-slate-700 border-slate-200';
    }
  }

  private normalizeStatus(status?: string): string {
    if (!status) return '';
    if (status === 'GRADING') return AssessmentResponse.StatusEnum.GradingInProgress;
    if (status === 'SUBMITTED') return AssessmentResponse.StatusEnum.SubmittedToAdmin;
    return status;
  }

  public getAssessmentStatusLabel(status?: string): string {
    if (!status) return 'Inconnu';
    const normalized = this.normalizeStatus(status);
    const found = this.assessmentStatuses.find(s => s.value === normalized || s.code === normalized);
    return found ? found.label : status;
  }

  public getAssessmentStatusBadgeClasses(status?: string): string {
    if (!status) return 'bg-slate-50 text-slate-700 border-slate-200';
    const normalized = this.normalizeStatus(status);
    const found = this.assessmentStatuses.find(s => s.value === normalized || s.code === normalized);
    return found ? found.badgeClasses : 'bg-slate-50 text-slate-700 border-slate-200';
  }

  public getAssessmentStatusDotColor(status?: string): string {
    if (!status) return 'bg-slate-400';
    const normalized = this.normalizeStatus(status);
    const found = this.assessmentStatuses.find(s => s.value === normalized || s.code === normalized);
    return found ? found.dotColor : 'bg-slate-400';
  }

  private translateApiError(rawError: any): string {
    const errorKey =
      rawError?.error?.message ||
      rawError?.error?.code ||
      rawError?.error?.error ||
      rawError?.message ||
      '';

    if (typeof errorKey === 'string') {
      if (errorKey.includes('WEIGHT_PERCENTAGE_SUM_EXCEEDS_100')) {
        return 'Le cumul des pondérations des épreuves ne peut pas dépasser 100% pour cette matière.';
      }
      if (errorKey.includes('PROMOTION_STATUS_FORBIDS_ASSESSMENT_CREATION')) {
        return 'Le statut actuel de la promotion ne permet pas la création d\'évaluations.';
      }
      if (errorKey.includes('PROMOTION_STATUS_FORBIDS_ASSESSMENT_UPDATE')) {
        return 'Le statut actuel de la promotion ne permet pas la modification d\'évaluations.';
      }
      if (errorKey.includes('ASSESSMENT_ALREADY_EXISTS_FOR_TYPE')) {
        return 'Une épreuve de ce type existe déjà pour cette matière.';
      }
      if (errorKey.includes('ASSESSMENT_TITLE_ALREADY_EXISTS')) {
        return 'Une épreuve avec ce titre existe déjà pour cette matière.';
      }
      if (errorKey.includes('ASSESSMENT_NOT_FOUND')) {
        return 'L\'épreuve demandée est introuvable.';
      }
      if (errorKey.includes('PROMOTION_SUBJECT_NOT_FOUND')) {
        return 'La matière sélectionnée est introuvable pour cette promotion.';
      }
      if (errorKey.includes('ASSESSMENT_ALREADY_LOCKED')) {
        return 'Cette épreuve est verrouillée et ne peut plus être modifiée.';
      }
      if (errorKey.includes('TOTAL_MARKS_MUST_BE_POSITIVE')) {
        return 'Le barème total doit être supérieur à 0.';
      }
      if (errorKey.includes('CANNOT_TRANSITION') || errorKey.includes('INVALID_STATUS') || errorKey.includes('STATUS_TRANSITION_NOT_ALLOWED')) {
        return 'Cette transition de statut n\'est pas autorisée pour cette épreuve dans son état actuel.';
      }
      if (errorKey.includes('ASSESSMENT_NOT_IN_SUBMITTED_STATUS')) {
        return 'L\'épreuve doit être soumise pour pouvoir être publiée ou renvoyée.';
      }
      if (errorKey.includes('ASSESSMENT_NOT_IN_PLANNED_STATUS')) {
        return 'L\'épreuve doit être planifiée pour pouvoir ouvrir la notation.';
      }
      if (errorKey.includes('ASSESSMENT_CAN_ONLY_BE_UPDATED_IN_PLANNED_STATUS')) {
        return 'Seule une épreuve au statut Brouillon ou Planifiée peut être modifiée.';
      }
      if (errorKey.includes('ASSESSMENT_CAN_ONLY_BE_DELETED_IN_DRAFT_STATUS')) {
        return 'Seule une épreuve au statut Brouillon (DRAFT) peut être définitivement supprimée.';
      }
    }

    return rawError?.error?.message || rawError?.message || 'Une erreur est survenue lors de l\'opération sur l\'épreuve.';
  }

  // =========================================================================
  // ASSESSMENTS WORKFLOW & STATE MACHINE (ASSESSMENT_STATUS)
  // =========================================================================

  /**
   * Prompts confirmation for permanent deletion (DRAFT only).
   */
  public promptDeleteAssessment(assessment: AssessmentResponse): void {
    this.confirmStatusModal.set({
      assessment,
      action: 'delete',
      fromStatus: assessment.status || 'DRAFT',
      toStatus: 'DELETED',
      title: 'Supprimer définitivement l\'épreuve',
      message: `Attention : Êtes-vous sûr de vouloir supprimer définitivement l'épreuve "${assessment.title}" ? Cette action est irréversible et libérera son barème et sa pondération.`,
      confirmLabel: 'Supprimer définitivement',
      confirmButtonClass: 'bg-rose-600 hover:bg-rose-700 text-white'
    });
  }

  /**
   * Prompts confirmation to transition from DRAFT to PLANNED.
   */
  public promptMarkAsPlanned(assessment: AssessmentResponse): void {
    this.confirmStatusModal.set({
      assessment,
      action: 'plan',
      fromStatus: assessment.status || 'DRAFT',
      toStatus: 'PLANNED',
      title: 'Planifier l\'épreuve',
      message: `Voulez-vous marquer l'épreuve "${assessment.title}" comme Planifiée (PLANNED) ? La configuration de l'épreuve sera validée avant l'ouverture de la notation.`,
      confirmLabel: 'Confirmer la planification',
      confirmButtonClass: 'bg-blue-600 hover:bg-blue-700 text-white'
    });
  }

  /**
   * Prompts confirmation to transition from PLANNED to GRADING_IN_PROGRESS.
   */
  public promptOpenForGrading(assessment: AssessmentResponse): void {
    this.confirmStatusModal.set({
      assessment,
      action: 'open_grading',
      fromStatus: assessment.status || 'PLANNED',
      toStatus: 'GRADING_IN_PROGRESS',
      title: 'Ouvrir la saisie des notes',
      message: `Autoriser dès maintenant la saisie et la modification des notes pour l'épreuve "${assessment.title}" (GRADING_IN_PROGRESS) ?`,
      confirmLabel: 'Ouvrir la notation',
      confirmButtonClass: 'bg-amber-500 hover:bg-amber-600 text-white'
    });
  }

  /**
   * Prompts confirmation to transition from GRADING_IN_PROGRESS to SUBMITTED_TO_ADMIN.
   */
  public promptSubmitToAdmin(assessment: AssessmentResponse): void {
    this.confirmStatusModal.set({
      assessment,
      action: 'submit',
      fromStatus: assessment.status || 'GRADING_IN_PROGRESS',
      toStatus: 'SUBMITTED_TO_ADMIN',
      title: 'Soumettre les notes à l\'administration',
      message: `Confirmez-vous la finalisation de la saisie des notes pour l'épreuve "${assessment.title}" ? Les notes seront transmises à l'administration pour validation (SUBMITTED_TO_ADMIN).`,
      confirmLabel: 'Soumettre à l\'administration',
      confirmButtonClass: 'bg-indigo-600 hover:bg-indigo-700 text-white'
    });
  }

  /**
   * Prompts confirmation to transition from SUBMITTED_TO_ADMIN to GRADING_IN_PROGRESS.
   */
  public promptRejectAndReturn(assessment: AssessmentResponse): void {
    this.confirmStatusModal.set({
      assessment,
      action: 'reject',
      fromStatus: assessment.status || 'SUBMITTED_TO_ADMIN',
      toStatus: 'GRADING_IN_PROGRESS',
      title: 'Renvoyer à l\'enseignant pour correction',
      message: `Refuser la saisie et renvoyer l'épreuve "${assessment.title}" à l'enseignant pour correction (retour au statut GRADING_IN_PROGRESS) ?`,
      confirmLabel: 'Renvoyer pour correction',
      confirmButtonClass: 'bg-amber-600 hover:bg-amber-700 text-white'
    });
  }

  /**
   * Prompts confirmation to transition from SUBMITTED_TO_ADMIN to PUBLISHED.
   */
  public promptPublishAssessment(assessment: AssessmentResponse): void {
    this.confirmStatusModal.set({
      assessment,
      action: 'publish',
      fromStatus: assessment.status || 'SUBMITTED_TO_ADMIN',
      toStatus: 'PUBLISHED',
      title: 'Publier officiellement les notes',
      message: `Confirmez-vous la publication officielle des notes de l'épreuve "${assessment.title}" (PUBLISHED) ? Les notes deviendront visibles par les étudiants.`,
      confirmLabel: 'Publier les notes',
      confirmButtonClass: 'bg-emerald-600 hover:bg-emerald-700 text-white'
    });
  }

  /**
   * Prompts confirmation to cancel the assessment (transition to CANCELLED).
   */
  public promptCancelAssessment(assessment: AssessmentResponse): void {
    this.confirmStatusModal.set({
      assessment,
      action: 'cancel',
      fromStatus: assessment.status || 'PLANNED',
      toStatus: 'CANCELLED',
      title: 'Annuler l\'épreuve',
      message: `Attention : Êtes-vous sûr de vouloir annuler l'épreuve "${assessment.title}" (force majeure) ? Son statut deviendra CANCELLED et sa pondération sera libérée.`,
      confirmLabel: 'Confirmer l\'annulation',
      confirmButtonClass: 'bg-rose-600 hover:bg-rose-700 text-white'
    });
  }

  /**
   * Prompts confirmation to transition from PUBLISHED to LOCKED.
   */
  public promptLockAssessment(assessment: AssessmentResponse): void {
    this.confirmStatusModal.set({
      assessment,
      action: 'lock',
      fromStatus: assessment.status || 'PUBLISHED',
      toStatus: 'LOCKED',
      title: 'Verrouiller définitivement l\'épreuve',
      message: `Attention : Le verrouillage de l'épreuve "${assessment.title}" est irréversible (clôture annuelle). Aucune modification ultérieure ne sera permise.`,
      confirmLabel: 'Verrouiller définitivement',
      confirmButtonClass: 'bg-purple-600 hover:bg-purple-700 text-white'
    });
  }

  /**
   * Closes the workflow confirmation modal.
   */
  public closeConfirmStatusModal(): void {
    if (this.updatingStatusAssessmentId() !== null) return;
    this.confirmStatusModal.set(null);
  }

  /**
   * Executes the confirmed workflow action.
   */
  public executeConfirmedStatusAction(): void {
    const modalData = this.confirmStatusModal();
    if (!modalData?.assessment?.id) return;

    const id = modalData.assessment.id;
    const title = modalData.assessment.title || 'Épreuve';

    let obs$;
    let successMsg = '';

    switch (modalData.action) {
      case 'plan':
        obs$ = this.assessmentService.markAsPlanned(id);
        successMsg = `L'épreuve "${title}" est maintenant planifiée (PLANNED).`;
        break;
      case 'delete':
        obs$ = this.assessmentService.deleteAssessment(id);
        successMsg = `L'épreuve "${title}" a été supprimée définitivement.`;
        break;
      case 'open_grading':
        obs$ = this.assessmentService.openForGrading(id);
        successMsg = `La saisie des notes est ouverte pour l'épreuve "${title}" (GRADING_IN_PROGRESS).`;
        break;
      case 'submit':
        obs$ = this.assessmentService.submitToAdmin(id);
        successMsg = `Les notes de l'épreuve "${title}" ont été soumises pour validation (SUBMITTED_TO_ADMIN).`;
        break;
      case 'reject':
        obs$ = this.assessmentService.rejectAndReturnToTeacher(id);
        successMsg = `L'épreuve "${title}" a été renvoyée à l'enseignant pour correction.`;
        break;
      case 'publish':
        obs$ = this.assessmentService.publishAssessment(id);
        successMsg = `L'épreuve "${title}" a été publiée avec succès. Les notes sont désormais officielles.`;
        break;
      case 'cancel':
        obs$ = this.assessmentService.cancelAssessment(id);
        successMsg = `L'épreuve "${title}" a été annulée.`;
        break;
      case 'lock':
        obs$ = this.assessmentService.lockAssessment(id);
        successMsg = `L'épreuve "${title}" a été verrouillée définitivement.`;
        break;
    }

    if (obs$) {
      this.executeStatusTransition(id, obs$, successMsg, () => {
        this.confirmStatusModal.set(null);
      });
    }
  }

  /**
   * Executes a status transition and synchronizes the display.
   */
  private executeStatusTransition(
    assessmentId: number,
    obs$: any,
    successMessage: string,
    onSuccessCallback?: () => void
  ): void {
    this.updatingStatusAssessmentId.set(assessmentId);

    obs$
      .pipe(finalize(() => this.updatingStatusAssessmentId.set(null)))
      .subscribe({
        next: () => {
          this.toastService.success(successMessage);
          if (onSuccessCallback) {
            onSuccessCallback();
          }

          // Refresh assessments for the current subject
          const sub = this.selectedSubjectForList();
          if (sub?.id) {
            this.loadAssessmentsForSubject(sub.id);
          }

          // Refresh promotion statistics
          const promoId = this.promotionId();
          if (promoId) {
            this.promotionSubjectService.getPromotionStats(promoId).subscribe({
              next: (res: any) => {
                if (res?.success && res.data) {
                  this.stats.set(res.data);
                }
              }
            });
          }
        },
        error: (err: any) => {
          const errMsg = this.translateApiError(err);
          this.toastService.error(errMsg);
        }
      });
  }

  public onSubmitAssessment(): void {
    if (this.assessmentForm.invalid) {
      this.assessmentForm.markAllAsTouched();
      return;
    }

    const currentSubject = this.selectedSubject();
    if (!currentSubject?.id) {
      this.modalErrorMessage.set('Identifiant de la matière de promotion introuvable.');
      return;
    }

    this.modalErrorMessage.set(null);

    const formVal = this.assessmentForm.getRawValue();
    const isRetake = formVal.type === AssessmentRequest.AssessmentTypeEnum.Retake || formVal.type === 'RETAKE';
    const request: AssessmentRequest = {
      promotionSubjectId: currentSubject.id,
      title: formVal.title.trim(),
      assessmentType: formVal.type as AssessmentRequest.AssessmentTypeEnum,
      totalMarks: Number(formVal.maxScore),
      weightPercentage: isRetake ? 0 : Math.round(Number(formVal.weight)),
      description: formVal.description ? formVal.description.trim() : undefined
    };

    // Open creation confirmation modal
    this.confirmCreateModal.set({
      request,
      subjectName: currentSubject.subjectName || 'la matière',
      typeFrenchName: this.getSelectedTypeFrenchName(),
      description: request.description
    });
  }

  public closeConfirmCreateModal(): void {
    if (this.isSubmittingAssessment()) return;
    this.confirmCreateModal.set(null);
  }

  public confirmAndCreateAssessment(): void {
    const modalData = this.confirmCreateModal();
    if (!modalData) return;

    const currentSubject = this.selectedSubject();
    const request = modalData.request;
    const subjectName = modalData.subjectName;
    const typeFrenchName = modalData.typeFrenchName;

    this.modalErrorMessage.set(null);
    this.isSubmittingAssessment.set(true);

    this.assessmentService
      .createAssessment(request)
      .pipe(finalize(() => this.isSubmittingAssessment.set(false)))
      .subscribe({
        next: () => {
          // Immediately reload assessments for this subject from API
          if (currentSubject?.id) {
            this.loadAssessmentsForSubject(currentSubject.id);
          }

          // Refresh promotion statistics
          const promoId = this.promotionId();
          if (promoId) {
            this.promotionSubjectService.getPromotionStats(promoId).subscribe({
              next: (res: any) => {
                if (res?.success && res.data) {
                  this.stats.set(res.data);
                }
              }
            });
          }

          // Success toast notification via ToastService
          this.toastService.success(
            `L'épreuve "${request.title}" (${typeFrenchName} - ${request.weightPercentage}%, Sur ${request.totalMarks}) a été ajoutée avec succès pour ${subjectName}.`
          );

          this.confirmCreateModal.set(null);
          this.isModalOpen.set(false);
          this.selectedSubject.set(null);
        },
        error: (err: any) => {
          const apiMsg = this.translateApiError(err);
          this.modalErrorMessage.set(apiMsg);
          this.toastService.error(apiMsg);
          this.confirmCreateModal.set(null);
        }
      });
  }

  // =========================================================================
  // EDIT ASSESSMENT MODAL & OPERATIONS (API PUT /api/v1/assessments/{id})
  // =========================================================================

  public openEditAssessmentModal(assessment: AssessmentResponse): void {
    if (!assessment?.id) return;
    this.editingAssessment.set(assessment);
    this.editErrorMessage.set(null);

    const isRetake =
      assessment.assessmentType === AssessmentResponse.AssessmentTypeEnum.Retake;

    this.editAssessmentForm.reset({
      title: assessment.title || '',
      type: assessment.assessmentType || '',
      weight: isRetake ? 0 : (assessment.weightPercentage ?? null),
      maxScore: assessment.totalMarks ?? null,
      description: ''
    });

    const weightControl = this.editAssessmentForm.get('weight');
    if (isRetake) {
      weightControl?.disable();
    } else {
      weightControl?.enable();
    }
  }

  public closeEditAssessmentModal(): void {
    if (this.isSubmittingEdit()) return;
    this.confirmEditModal.set(null);
    this.editingAssessment.set(null);
    this.editErrorMessage.set(null);
  }

  public onEditTypeChange(type: string): void {
    const weightControl = this.editAssessmentForm.get('weight');
    if (type === AssessmentRequest.AssessmentTypeEnum.Retake) {
      weightControl?.setValue(0);
      weightControl?.disable();
    } else {
      if (weightControl?.disabled) {
        weightControl?.enable();
        const initial = this.editingAssessment();
        if (
          initial &&
          initial.assessmentType !== AssessmentResponse.AssessmentTypeEnum.Retake
        ) {
          weightControl.setValue(initial.weightPercentage ?? null);
        } else {
          weightControl.setValue(null);
        }
      }
    }
  }

  public getSelectedEditTypeFrenchName(): string {
    const typeVal = this.editAssessmentForm.get('type')?.value;
    if (!typeVal) return '';
    const found = this.assessmentTypes.find(t => t.value === typeVal);
    return found ? found.badge : typeVal;
  }

  public onSubmitEditAssessment(): void {
    if (this.editAssessmentForm.invalid) {
      this.editAssessmentForm.markAllAsTouched();
      return;
    }

    const assessment = this.editingAssessment();
    if (!assessment?.id) {
      this.editErrorMessage.set("Identifiant de l'épreuve introuvable.");
      return;
    }

    const currentSubject = this.selectedSubjectForList();
    const subjectId = assessment.promotionSubjectId || currentSubject?.id;
    if (!subjectId) {
      this.editErrorMessage.set("Identifiant de la matière introuvable.");
      return;
    }

    this.editErrorMessage.set(null);

    const formVal = this.editAssessmentForm.getRawValue();
    const isRetake =
      formVal.type === AssessmentRequest.AssessmentTypeEnum.Retake;
    const request: AssessmentRequest = {
      promotionSubjectId: subjectId,
      title: formVal.title.trim(),
      assessmentType: formVal.type as AssessmentRequest.AssessmentTypeEnum,
      totalMarks: Number(formVal.maxScore),
      weightPercentage: isRetake ? 0 : Math.round(Number(formVal.weight)),
      description: formVal.description ? formVal.description.trim() : undefined
    };

    this.confirmEditModal.set({
      assessmentId: assessment.id,
      request,
      subjectName: currentSubject?.subjectName || assessment.subjectName || 'la matière',
      typeFrenchName: this.getSelectedEditTypeFrenchName(),
      oldTitle: assessment.title || '',
      newTitle: request.title,
      oldWeight: assessment.weightPercentage ?? 0,
      newWeight: request.weightPercentage,
      oldMarks: assessment.totalMarks ?? 0,
      newMarks: request.totalMarks,
      oldType: assessment.assessmentType,
      newType: request.assessmentType
    });
  }

  public closeConfirmEditModal(): void {
    if (this.isSubmittingEdit()) return;
    this.confirmEditModal.set(null);
  }

  public confirmAndUpdateAssessment(): void {
    const modalData = this.confirmEditModal();
    if (!modalData?.assessmentId) return;

    const id = modalData.assessmentId;
    const request = modalData.request;
    const title = request.title;
    const subjectId = request.promotionSubjectId;

    this.editErrorMessage.set(null);
    this.isSubmittingEdit.set(true);

    this.assessmentService
      .updateAssessment(id, request)
      .pipe(finalize(() => this.isSubmittingEdit.set(false)))
      .subscribe({
        next: () => {
          this.toastService.success(`L'épreuve "${title}" a été modifiée avec succès.`);
          this.confirmEditModal.set(null);
          this.editingAssessment.set(null);

          // Refresh assessments for this subject
          if (subjectId) {
            this.loadAssessmentsForSubject(subjectId);
          }

          // Refresh promotion statistics
          const promoId = this.promotionId();
          if (promoId) {
            this.promotionSubjectService.getPromotionStats(promoId).subscribe({
              next: (res: any) => {
                if (res?.success && res.data) {
                  this.stats.set(res.data);
                }
              }
            });
          }
        },
        error: (err: any) => {
          const apiMsg = this.translateApiError(err);
          this.editErrorMessage.set(apiMsg);
          this.toastService.error(apiMsg);
          this.confirmEditModal.set(null);
        }
      });
  }

  public canEditAssessment(assessment: AssessmentResponse): boolean {
    return (
      assessment.status === AssessmentResponse.StatusEnum.Draft ||
      assessment.status === AssessmentResponse.StatusEnum.Planned
    );
  }
}
