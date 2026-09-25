import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, finalize, of } from 'rxjs';
import {
  PromotionSubjectControllerService,
  AssessmentControllerService,
  PromotionStatsResponse,
  PromotionSubjectResponse,
  AssessmentResponse
} from '../../../../../core/api';
import { ToastService } from '../../../../../shared/services/toast.service';

export interface TypeFilterOption {
  value: string;
  label: string;
}

export interface StatusFilterOption {
  value: string;
  label: string;
}

@Component({
  selector: 'app-promotion-assessments',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './promotion-assessments.html',
  styleUrl: './promotion-assessments.css'
})
export class PromotionAssessments implements OnInit {
  // --- DEPENDENCY INJECTIONS ---
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);
  private readonly promotionSubjectService = inject(PromotionSubjectControllerService);
  private readonly assessmentService = inject(AssessmentControllerService);

  // --- STATE SIGNALS ---
  public readonly promotionId = signal<number | null>(null);
  public readonly stats = signal<PromotionStatsResponse | null>(null);
  public readonly subjects = signal<PromotionSubjectResponse[]>([]);
  public readonly assessments = signal<AssessmentResponse[]>([]);
  public readonly isLoading = signal<boolean>(true);

  // --- FILTERS ---
  public readonly selectedSubjectId = signal<number | 'ALL'>('ALL');
  public readonly selectedType = signal<string>('ALL');
  public readonly selectedStatus = signal<string>('ALL');
  public readonly searchQuery = signal<string>('');

  // --- FILTER OPTIONS ---
  public readonly typeOptions: TypeFilterOption[] = [
    { value: 'ALL', label: 'Tous les types' },
    { value: AssessmentResponse.AssessmentTypeEnum.Ds, label: 'DS (Contrôle écrit)' },
    { value: AssessmentResponse.AssessmentTypeEnum.Tp, label: 'TP (Pratique / Labo)' },
    { value: AssessmentResponse.AssessmentTypeEnum.FinalExam, label: 'Examen Final' },
    { value: AssessmentResponse.AssessmentTypeEnum.Retake, label: 'Rattrapage' }
  ];

  public readonly statusOptions: StatusFilterOption[] = [
    { value: 'ALL', label: 'Tous les statuts' },
    { value: AssessmentResponse.StatusEnum.Draft, label: 'Brouillon' },
    { value: AssessmentResponse.StatusEnum.Planned, label: 'Planifiée' },
    { value: AssessmentResponse.StatusEnum.GradingInProgress, label: 'Saisie en cours' },
    { value: AssessmentResponse.StatusEnum.SubmittedToAdmin, label: 'Soumise' },
    { value: AssessmentResponse.StatusEnum.Published, label: 'Publiée' },
    { value: AssessmentResponse.StatusEnum.Locked, label: 'Verrouillée' },
    { value: AssessmentResponse.StatusEnum.Cancelled, label: 'Annulée' }
  ];

  // --- COMPUTED SIGNALS ---
  public readonly filteredAssessments = computed(() => {
    let list = this.assessments();
    const query = this.searchQuery().toLowerCase().trim();
    const subjectId = this.selectedSubjectId();
    const type = this.selectedType();
    const status = this.selectedStatus();

    // Filter by search query
    if (query) {
      list = list.filter(a =>
        (a.title && a.title.toLowerCase().includes(query)) ||
        (a.subjectName && a.subjectName.toLowerCase().includes(query))
      );
    }

    // Filter by subject
    if (subjectId !== 'ALL') {
      list = list.filter(a => a.promotionSubjectId === subjectId);
    }

    // Filter by assessment type
    if (type !== 'ALL') {
      list = list.filter(a => a.assessmentType === type);
    }

    // Filter by status
    if (status !== 'ALL') {
      list = list.filter(a => a.status === status);
    }

    return list;
  });

  public readonly totalAssessmentsCount = computed(() => this.assessments().length);

  public readonly gradingInProgressCount = computed(() =>
    this.assessments().filter(a => a.status === AssessmentResponse.StatusEnum.GradingInProgress).length
  );

  public readonly publishedCount = computed(() =>
    this.assessments().filter(a => a.status === AssessmentResponse.StatusEnum.Published).length
  );

  public readonly plannedCount = computed(() =>
    this.assessments().filter(a => a.status === AssessmentResponse.StatusEnum.Planned).length
  );

  public ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = +idParam;
      this.promotionId.set(id);
      this.loadPromotionData(id);
    } else {
      this.toastService.error('Identifiant de promotion manquant.');
      this.router.navigate(['/admin/grades']);
    }
  }

  /**
   * Loads promotion information, subjects, and associated assessments.
   */
  public loadPromotionData(promotionId: number): void {
    this.isLoading.set(true);

    // Fetch promotion stats and context
    this.promotionSubjectService.getPromotionStats(promotionId).subscribe({
      next: (res) => {
        if (res?.success && res.data) {
          this.stats.set(res.data);
        }
      },
      error: (err) => console.error('Erreur chargement statistiques promotion', err)
    });

    // Fetch promotion subjects
    this.promotionSubjectService.getSubjectsByPromotionId(promotionId).subscribe({
      next: (res) => {
        if (res?.success && res.data) {
          const subjectList = res.data;
          this.subjects.set(subjectList);

          if (subjectList.length === 0) {
            this.assessments.set([]);
            this.isLoading.set(false);
            return;
          }

          // For each subject, fetch grading assessments
          const validSubjects = subjectList.filter(s => s.id !== undefined && s.id !== null);

          if (validSubjects.length === 0) {
            this.assessments.set([]);
            this.isLoading.set(false);
            return;
          }

          const assessmentRequests = validSubjects.map(sub =>
            this.assessmentService.getGradingAssessmentsByPromotionSubject(sub.id!)
          );

          forkJoin(assessmentRequests)
            .pipe(finalize(() => this.isLoading.set(false)))
            .subscribe({
              next: (responses) => {
                const combinedList: AssessmentResponse[] = [];
                responses.forEach((response, index) => {
                  if (response?.success && response.data) {
                    const currentSubject = validSubjects[index];
                    const itemsWithSubject = response.data.map(item => ({
                      ...item,
                      subjectName: item.subjectName || currentSubject.subjectName,
                      promotionSubjectId: item.promotionSubjectId || currentSubject.id
                    }));
                    combinedList.push(...itemsWithSubject);
                  }
                });
                this.assessments.set(combinedList);
              },
              error: (err) => {
                console.error('Erreur lors du chargement des épreuves', err);
                this.toastService.error('Une erreur est survenue lors du chargement des épreuves.');
              }
            });
        } else {
          this.isLoading.set(false);
          this.toastService.error('Impossible de charger les matières de la promotion.');
        }
      },
      error: (err) => {
        this.isLoading.set(false);
        console.error('Erreur chargement matières promotion', err);
        this.toastService.error('Erreur de communication avec le serveur.');
      }
    });
  }

  // --- BADGE LABELS AND STYLES ---
  public getTypeBadgeText(type?: string): string {
    switch (type) {
      case 'DS':
      case AssessmentResponse.AssessmentTypeEnum.Ds:
        return 'DS';
      case 'TP':
      case AssessmentResponse.AssessmentTypeEnum.Tp:
        return 'TP';
      case 'FINAL_EXAM':
      case AssessmentResponse.AssessmentTypeEnum.FinalExam:
        return 'Examen Final';
      case 'RETAKE':
      case AssessmentResponse.AssessmentTypeEnum.Retake:
        return 'Rattrapage';
      default:
        return type || 'Épreuve';
    }
  }

  public getTypeBadgeClasses(type?: string): string {
    switch (type) {
      case 'DS':
      case AssessmentResponse.AssessmentTypeEnum.Ds:
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'TP':
      case AssessmentResponse.AssessmentTypeEnum.Tp:
        return 'bg-teal-50 text-teal-700 border-teal-200';
      case 'FINAL_EXAM':
      case AssessmentResponse.AssessmentTypeEnum.FinalExam:
        return 'bg-purple-50 text-purple-700 border-purple-200';
      case 'RETAKE':
      case AssessmentResponse.AssessmentTypeEnum.Retake:
        return 'bg-amber-50 text-amber-700 border-amber-200';
      default:
        return 'bg-slate-50 text-slate-700 border-slate-200';
    }
  }

  public getStatusLabel(status?: string): string {
    switch (status) {
      case AssessmentResponse.StatusEnum.Draft:
        return 'Brouillon';
      case AssessmentResponse.StatusEnum.Planned:
        return 'Planifiée';
      case AssessmentResponse.StatusEnum.GradingInProgress:
        return 'Saisie en cours';
      case AssessmentResponse.StatusEnum.SubmittedToAdmin:
        return 'Soumise à l\'admin';
      case AssessmentResponse.StatusEnum.Published:
        return 'Publiée';
      case AssessmentResponse.StatusEnum.Locked:
        return 'Verrouillée';
      case AssessmentResponse.StatusEnum.Cancelled:
        return 'Annulée';
      default:
        return status || 'Inconnu';
    }
  }

  public getStatusBadgeClasses(status?: string): string {
    switch (status) {
      case AssessmentResponse.StatusEnum.Draft:
        return 'bg-slate-100 text-slate-700 border-slate-200';
      case AssessmentResponse.StatusEnum.Planned:
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case AssessmentResponse.StatusEnum.GradingInProgress:
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case AssessmentResponse.StatusEnum.SubmittedToAdmin:
        return 'bg-indigo-50 text-indigo-700 border-indigo-200';
      case AssessmentResponse.StatusEnum.Published:
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case AssessmentResponse.StatusEnum.Locked:
        return 'bg-purple-50 text-purple-700 border-purple-200';
      case AssessmentResponse.StatusEnum.Cancelled:
        return 'bg-rose-50 text-rose-700 border-rose-200';
      default:
        return 'bg-slate-50 text-slate-700 border-slate-200';
    }
  }

  public getStatusDotColor(status?: string): string {
    switch (status) {
      case AssessmentResponse.StatusEnum.Draft:
        return 'bg-slate-400';
      case AssessmentResponse.StatusEnum.Planned:
        return 'bg-blue-500';
      case AssessmentResponse.StatusEnum.GradingInProgress:
        return 'bg-amber-500 animate-pulse';
      case AssessmentResponse.StatusEnum.SubmittedToAdmin:
        return 'bg-indigo-500';
      case AssessmentResponse.StatusEnum.Published:
        return 'bg-emerald-500';
      case AssessmentResponse.StatusEnum.Locked:
        return 'bg-purple-500';
      case AssessmentResponse.StatusEnum.Cancelled:
        return 'bg-rose-500';
      default:
        return 'bg-slate-400';
    }
  }

  /**
   * Handles click on an assessment to navigate to grade entry or consultation.
   */
  public selectAssessment(assessment: AssessmentResponse): void {
    if (!assessment?.id) return;
    const promoId = this.promotionId();
    if (promoId) {
      this.router.navigate(['/admin/grades', promoId, 'assessments', assessment.id], {
        state: { assessment, promotionName: this.stats()?.promotionName }
      });
    } else {
      this.router.navigate(['/admin/grades/assessments', assessment.id], {
        state: { assessment }
      });
    }
  }
}
