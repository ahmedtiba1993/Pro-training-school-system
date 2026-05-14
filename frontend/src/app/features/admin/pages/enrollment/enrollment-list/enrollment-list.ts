import {Component, inject, OnInit, signal, computed} from '@angular/core';
import {DatePipe, KeyValuePipe} from '@angular/common';
import {RouterLink} from '@angular/router';
import {FormsModule} from '@angular/forms';

import {ToastService} from '../../../../../shared/services/toast.service';
import {PaginationComponent} from '../../../../../shared/components/pagination/pagination';

import {
  EnrollmentControllerService,
  EnrollmentListResponse,
  EnrollmentSearchRequest,
  LevelControllerService,
  LevelResponse,
  TrainingControllerService,
  TrainingResponse,
  PromotionControllerService,
  PromotionLookupResponse
} from '../../../../../core/api';

@Component({
  selector: 'app-enrollment-list',
  standalone: true,
  imports: [DatePipe, RouterLink, PaginationComponent, FormsModule],
  templateUrl: './enrollment-list.html',
  styleUrl: './enrollment-list.css'
})
export class EnrollmentList implements OnInit {
  private readonly enrollmentService = inject(EnrollmentControllerService);
  private readonly levelService = inject(LevelControllerService);
  private readonly trainingService = inject(TrainingControllerService);
  private readonly promotionService = inject(PromotionControllerService); // <-- Injection
  private readonly toastService = inject(ToastService);

  // --- STATE (Table Data) ---
  enrollments = signal<EnrollmentListResponse[]>([]);
  isLoading = signal<boolean>(true);

  // --- STATE (Pagination) ---
  currentPage = signal<number>(0);
  pageSize = signal<number>(5);
  totalElements = signal<number>(0);

  // --- STATE (Filters) ---
  keyword = signal<string>('');
  levelId = signal<number | null>(null);
  specialtyId = signal<number | null>(null);
  promotionId = signal<number | null>(null);
  status = signal<EnrollmentSearchRequest.StatusEnum | ''>('');

  // --- DYNAMIC DATA (API) ---
  levels = signal<LevelResponse[]>([]);
  trainings = signal<TrainingResponse[]>([]);
  promotions = signal<PromotionLookupResponse[]>([]);

  // Computed to extract a unique list of Specialties from Trainings
  specialities = computed(() => {
    const allTrainings = this.trainings();
    const uniqueSpecs = new Map<number, string>();

    allTrainings.forEach(training => {
      if (training.specialtyId && training.specialtyLabel) {
        uniqueSpecs.set(training.specialtyId, training.specialtyLabel);
      }
    });

    return Array.from(uniqueSpecs.entries()).map(([id, label]) => ({id, label}));
  });

  // --- STATUS CONFIGURATION ---
  readonly statusConfig: Record<string, { label: string; classes: string }> = {
    PRE_ENROLLED: {label: 'Pré-inscrit', classes: 'bg-amber-50 text-amber-700 border-amber-100'},
    INCOMPLETE: {label: 'Incomplet', classes: 'bg-orange-50 text-orange-700 border-orange-100'},
    WAITLISTED: {
      label: "Liste d'attente",
      classes: 'bg-purple-50 text-purple-700 border-purple-100'
    },
    CONDITIONALLY_VALIDATED: {
      label: 'Validé (Conditions)',
      classes: 'bg-teal-50 text-teal-700 border-teal-100'
    },
    VALIDATED: {label: 'Validé', classes: 'bg-emerald-50 text-emerald-700 border-emerald-100'},
    SUSPENDED: {label: 'Suspendu', classes: 'bg-slate-100 text-slate-700 border-slate-200'},
    DROPPED_OUT: {label: 'Abandon', classes: 'bg-rose-100 text-rose-700 border-rose-200'},
    REJECTED: {label: 'Rejeté', classes: 'bg-red-50 text-red-700 border-red-100'},
    CANCELLED: {label: 'Annulé', classes: 'bg-zinc-100 text-zinc-600 border-zinc-200'},
    COMPLETED: {label: 'Terminé', classes: 'bg-blue-50 text-blue-700 border-blue-100'}
  };

  readonly statusList = [
    {value: 'PRE_ENROLLED', label: 'Pré-inscrit'},
    {value: 'INCOMPLETE', label: 'Incomplet'},
    {value: 'WAITLISTED', label: "Liste d'attente"},
    {value: 'CONDITIONALLY_VALIDATED', label: 'Validé (Conditions)'},
    {value: 'VALIDATED', label: 'Validé'},
    {value: 'SUSPENDED', label: 'Suspendu'},
    {value: 'DROPPED_OUT', label: 'Abandon'},
    {value: 'REJECTED', label: 'Rejeté'},
    {value: 'CANCELLED', label: 'Annulé'},
    {value: 'COMPLETED', label: 'Terminé'}
  ];

  ngOnInit(): void {
    this.loadLevels();
    this.loadTrainings();
    this.loadPromotions();
    this.loadEnrollments();
  }

  // --- API CALLS ---

  loadLevels(): void {
    this.levelService.getActiveLevels().subscribe({
      next: (response: any) => this.levels.set(response.data || []),
      error: err => {
        console.error(err);
        this.toastService.error('Erreur lors du chargement des niveaux');
      }
    });
  }

  loadTrainings(): void {
    this.trainingService.getAllActiveTrainings().subscribe({
      next: (response: any) => this.trainings.set(response.data || []),
      error: err => {
        console.error(err);
        this.toastService.error('Erreur lors du chargement des spécialités');
      }
    });
  }

  loadPromotions(): void {
    this.promotionService.getActivePromotionsLookup().subscribe({
      next: (response: any) => {
        this.promotions.set(response.data || []);
      },
      error: err => {
        console.error(err);
        this.toastService.error('Erreur lors du chargement des promotions');
      }
    });
  }

  loadEnrollments(): void {
    this.isLoading.set(true);

    const filterRequest: EnrollmentSearchRequest = {
      keyword: this.keyword() || undefined,
      levelId: this.levelId() ?? undefined,
      specialtyId: this.specialtyId() ?? undefined,
      promotionId: this.promotionId() ?? undefined,
      status: (this.status() as EnrollmentSearchRequest.StatusEnum) || undefined,
      page: this.currentPage(),
      size: this.pageSize()
    };

    this.enrollmentService.getAllEnrollments(filterRequest).subscribe({
      next: (response: any) => {
        this.enrollments.set(response.content || []);
        this.totalElements.set(response?.totalElements || 0);
      },
      error: err => {
        console.error(err);
        this.toastService.error('Erreur lors du chargement des inscriptions');
        this.isLoading.set(false);
      },
      complete: () => this.isLoading.set(false)
    });
  }

  // --- FILTER ACTIONS ---

  onSearch(): void {
    this.currentPage.set(0);
    this.loadEnrollments();
  }

  clearFilters(): void {
    this.keyword.set('');
    this.levelId.set(null);
    this.specialtyId.set(null);
    this.promotionId.set(null);
    this.status.set('');

    this.currentPage.set(0);
    this.loadEnrollments();
  }

  onPageChange(newPage: number) {
    this.currentPage.set(newPage);
    this.loadEnrollments();
  }

  // --- UI HELPERS ---

  getInitials(firstName?: string, lastName?: string): string {
    const first = firstName ? firstName.charAt(0).toUpperCase() : '';
    const last = lastName ? lastName.charAt(0).toUpperCase() : '';
    return `${first}${last}` || '??';
  }

  getStatusConfig(status?: string) {
    if (!status)
      return {label: 'Inconnu', classes: 'bg-slate-50 text-slate-500 border-slate-200'};
    return (
      this.statusConfig[status] || {
        label: status,
        classes: 'bg-slate-50 text-slate-700 border-slate-200'
      }
    );
  }
}
