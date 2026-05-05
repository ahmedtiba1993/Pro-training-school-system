import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe, NgClass } from '@angular/common';
import {
  AcademicYearControllerService,
  AcademicYearResponse,
  AcademicYearRequest,
  ActiveAcademicYearResponse
} from '../../../../../core/api';
import { ToastService } from '../../../../../shared/services/toast.service';
import { PaginationComponent } from '../../../../../shared/components/pagination/pagination';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-academic-year-list',
  standalone: true,
  imports: [DatePipe, NgClass, ReactiveFormsModule, PaginationComponent, RouterLink],
  templateUrl: './academic-year-list.html',
  styleUrl: './academic-year-list.css'
})
export class AcademicYearList implements OnInit {
  private apiService = inject(AcademicYearControllerService);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);

  // --- LIST STATE ---
  academicYears = signal<AcademicYearResponse[]>([]);
  isLoading = signal<boolean>(true);
  error = signal<string | null>(null);

  // --- CURRENT YEAR STATE (HEADER) ---
  currentActiveYear = signal<ActiveAcademicYearResponse | null>(null);
  isLoadingCurrentYear = signal<boolean>(true);

  // --- PAGINATION ---
  currentPage = signal<number>(0);
  pageSize = signal<number>(5);
  totalElements = signal<number>(0);

  // --- ADD/EDIT MODAL & FORM ---
  showAddModal = signal<boolean>(false);
  isSaving = signal<boolean>(false);
  editingYearId = signal<number | null>(null);

  addForm = this.fb.group({
    label: ['', [Validators.required]],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]]
  });

  // --- DETAILS MODAL ---
  showDetailsModal = signal<boolean>(false);
  isLoadingDetails = signal<boolean>(false);
  selectedYearDetails = signal<AcademicYearResponse | null>(null);

  // --- DEFAULT CONFIRMATION MODAL ---
  showDefaultModal = signal<boolean>(false);
  selectedYearForDefault = signal<AcademicYearResponse | null>(null);
  isSettingDefault = signal<boolean>(false);

  // --- LOCK / UNLOCK CONFIRMATION MODAL ---
  showToggleLockModal = signal<boolean>(false);
  selectedYearForToggleLock = signal<AcademicYearResponse | null>(null);
  isTogglingLock = signal<boolean>(false);

  // --- STATUS CHANGE MODAL ---
  showStatusConfirmModal = signal<boolean>(false);
  pendingNextStatus = signal<string | null>(null);
  isChangingStatus = signal<boolean>(false);

  // Strict workflow order
  readonly WORKFLOW_STEPS = [
    { value: 'PLANNED', label: 'Planifiée' },
    { value: 'ENROLLMENT', label: 'Inscription' },
    { value: 'IN_PROGRESS', label: 'En cours' },
    { value: 'CLOSING', label: 'Clôture' },
    { value: 'COMPLETED', label: 'Terminée' }
  ];

  ngOnInit(): void {
    this.loadAcademicYears();
    this.loadCurrentActiveYear();
  }

  // ==========================================
  // DATA LOADING & PAGINATION
  // ==========================================

  private loadCurrentActiveYear(): void {
    this.isLoadingCurrentYear.set(true);

    this.apiService.getCurrentAcademicYear().subscribe({
      next: (response: any) => {
        if (response.success && response.data) {
          this.currentActiveYear.set(response.data);
        } else {
          this.currentActiveYear.set(null);
        }
        this.isLoadingCurrentYear.set(false);
      },
      error: () => {
        this.currentActiveYear.set(null);
        this.isLoadingCurrentYear.set(false);
      }
    });
  }

  private loadAcademicYears(): void {
    this.isLoading.set(true);
    this.apiService.getAllAcademicYears(this.currentPage(), this.pageSize()).subscribe({
      next: (response: any) => {
        this.academicYears.set(response?.data?.content || []);
        this.totalElements.set(response?.data?.totalElements || 0);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger les années scolaires.');
        this.isLoading.set(false);
      }
    });
  }

  onPageChange(newPage: number) {
    this.currentPage.set(newPage);
    this.loadAcademicYears();
  }

  // ==========================================
  // ADD/EDIT MODAL ACTIONS
  // ==========================================
  openAddModal(): void {
    this.editingYearId.set(null);
    this.addForm.reset();
    this.showAddModal.set(true);
  }

  editAcademicYear(year: AcademicYearResponse): void {
    this.editingYearId.set(year.id!);
    this.addForm.patchValue({
      label: year.label,
      startDate: this.formatDateForInput(year.startDate),
      endDate: this.formatDateForInput(year.endDate)
    });
    this.showAddModal.set(true);
  }

  closeAddModal(): void {
    this.showAddModal.set(false);
    this.editingYearId.set(null);
    this.addForm.reset();
  }

  submitAddForm(): void {
    if (this.addForm.invalid) {
      this.addForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);

    const payload: AcademicYearRequest = {
      label: this.addForm.value.label!,
      startDate: this.addForm.value.startDate!,
      endDate: this.addForm.value.endDate!
    };

    const currentId = this.editingYearId();

    if (currentId !== null) {
      this.apiService.updateAcademicYear(currentId, payload).subscribe({
        next: () => this.handleSuccess('Année modifiée avec succès'),
        error: err => this.handleError(err)
      });
    } else {
      this.apiService.createAcademicYear(payload).subscribe({
        next: () => this.handleSuccess('Année ajoutée avec succès'),
        error: err => this.handleError(err)
      });
    }
  }

  private handleSuccess(message: string): void {
    this.toast.success(message);
    this.isSaving.set(false);
    this.closeAddModal();
    this.loadAcademicYears();
  }

  private handleError(err: any): void {
    this.isSaving.set(false);
    const reponseErreur = err.error;

    if (reponseErreur?.errors) {
      for (const erreur of reponseErreur.errors) {
        if (erreur.message === 'INVALID_LABEL_FORMAT') {
          this.toast.error('Le format du label est invalide (ex: 2024-2025).');
        }
        if (erreur.message === 'INVALID_DURATION_MUST_BE_8_TO_11_MONTHS') {
          this.toast.error('La durée doit être comprise entre 8 et 11 mois.');
        }
        if (erreur.message === 'INVALID_YEAR_GAP') {
          this.toast.error('Les années du label doivent se suivre logiquement (ex: 2024-2025).');
        }
        if (erreur.message === 'END_DATE_MUST_BE_AFTER_START_DATE') {
          this.toast.error('La date de fin doit être postérieure à la date de début.');
        }
      }
    } else if (
      reponseErreur?.message === 'ACADEMIC_YEAR_LABEL_ALREADY_EXISTS' ||
      reponseErreur?.message === 'LABEL_ALREADY_EXISTS'
    ) {
      this.toast.error('Ce label existe déjà. Veuillez en choisir un autre.');
    } else if (reponseErreur?.message === 'ACADEMIC_YEAR_DATES_OVERLAP') {
      this.toast.error('Les dates se chevauchent avec une autre année scolaire existante.');
    } else {
      this.toast.error('Une erreur inattendue est survenue.');
      console.error('Erreur API:', err);
    }
  }

  // ==========================================
  // SET AS DEFAULT ACTIONS
  // ==========================================
  openDefaultModal(year: AcademicYearResponse): void {
    this.selectedYearForDefault.set(year);
    this.showDefaultModal.set(true);
  }

  closeDefaultModal(): void {
    this.showDefaultModal.set(false);
    this.selectedYearForDefault.set(null);
  }

  confirmSetDefault(): void {
    const year = this.selectedYearForDefault();
    if (!year || !year.id) return;

    this.isSettingDefault.set(true);

    this.apiService.setDefaultAcademicYear(year.id).subscribe({
      next: () => {
        this.toast.success(`L'année ${year.label} est maintenant l'année par défaut.`);
        this.isSettingDefault.set(false);
        this.closeDefaultModal();
        this.loadAcademicYears();
      },
      error: err => {
        this.isSettingDefault.set(false);
        this.closeDefaultModal();

        const errorMessage = err.error?.message;
        if (errorMessage === 'CANNOT_SET_DEFAULT_FOR_CLOSED_OR_PLANNED_YEAR') {
          this.toast.error(
            'Impossible. Seule une année "En cours" ou "En inscription" peut être définie par défaut.'
          );
        } else {
          this.toast.error("Erreur lors du changement de l'année par défaut.");
        }
        console.error('Erreur API SetDefault:', err);
      }
    });
  }

  // ==========================================
  // LOCK / UNLOCK ACTIONS
  // ==========================================
  openToggleLockModal(year: AcademicYearResponse): void {
    this.selectedYearForToggleLock.set(year);
    this.showToggleLockModal.set(true);
  }

  closeToggleLockModal(): void {
    this.showToggleLockModal.set(false);
    this.selectedYearForToggleLock.set(null);
  }

  confirmToggleLock(): void {
    const year = this.selectedYearForToggleLock();
    if (!year || !year.id) return;

    this.isTogglingLock.set(true);

    this.apiService.toggleLock(year.id).subscribe({
      next: () => {
        const actionMessage = year.isLocked ? 'déverrouillée' : 'verrouillée';
        this.toast.success(`L'année ${year.label} a été ${actionMessage} avec succès.`);
        this.isTogglingLock.set(false);
        this.closeToggleLockModal();
        this.loadAcademicYears();
      },
      error: err => {
        this.isTogglingLock.set(false);
        this.closeToggleLockModal();

        const errorMessage = err.error?.message;
        if (errorMessage === 'CANNOT_MODIFY_LOCK_ON_COMPLETED_YEAR') {
          this.toast.error("Impossible de modifier le verrouillage d'une année qui est clôturée.");
        } else {
          this.toast.error('Erreur lors de la modification du verrouillage.');
        }
        console.error('Erreur API ToggleLock:', err);
      }
    });
  }

  // ==========================================
  // DETAILS MODAL ACTIONS
  // ==========================================
  viewAcademicYearDetails(id: number): void {
    this.isLoadingDetails.set(true);
    this.showDetailsModal.set(true);

    this.apiService.getAcademicYearById(id).subscribe({
      next: (response: any) => {
        if (response.success && response.data) {
          this.selectedYearDetails.set(response.data);
        }
        this.isLoadingDetails.set(false);
      },
      error: () => {
        this.toast.error('Impossible de charger les détails.');
        this.isLoadingDetails.set(false);
        this.closeDetailsModal();
      }
    });
  }

  closeDetailsModal(): void {
    this.showDetailsModal.set(false);
    setTimeout(() => this.selectedYearDetails.set(null), 200);
  }

  // ==========================================
  // WORKFLOW ACTIONS (STATUS CHANGE)
  // ==========================================
  getCurrentStepIndex(status: string | undefined): number {
    if (!status) return 0;
    return this.WORKFLOW_STEPS.findIndex(step => step.value === status);
  }

  getNextStatus(currentStatus: string | undefined): string | null {
    const currentIndex = this.getCurrentStepIndex(currentStatus);
    if (currentIndex === -1 || currentIndex === this.WORKFLOW_STEPS.length - 1) {
      return null;
    }
    return this.WORKFLOW_STEPS[currentIndex + 1].value;
  }

  openStatusConfirmModal(nextStatus: string): void {
    this.pendingNextStatus.set(nextStatus);
    this.showStatusConfirmModal.set(true);
  }

  closeStatusConfirmModal(): void {
    this.showStatusConfirmModal.set(false);
    this.pendingNextStatus.set(null);
  }

  confirmChangeStatus(): void {
    const yearDetails = this.selectedYearDetails();
    const nextStatus = this.pendingNextStatus();

    if (!yearDetails || !yearDetails.id || !nextStatus) return;

    this.isChangingStatus.set(true);

    this.apiService.changeAcademicYearStatus(yearDetails.id, nextStatus as any).subscribe({
      next: () => {
        this.toast.success("Le statut de l'année scolaire a été mis à jour.");
        this.isChangingStatus.set(false);
        this.closeStatusConfirmModal();
        this.closeDetailsModal();
        this.loadAcademicYears();
        this.loadCurrentActiveYear();
      },
      error: err => {
        this.isChangingStatus.set(false);
        this.closeStatusConfirmModal();

        const errorMsg = err.error?.message;
        if (errorMsg === 'ANOTHER_YEAR_ALREADY_IN_PROGRESS') {
          this.toast.error("Une autre année est déjà en cours. Clôturez-la d'abord.");
        } else if (errorMsg === 'CANNOT_START_YEAR_BEFORE_START_DATE') {
          this.toast.error('Impossible de démarrer cette année avant sa date de début officielle.');
        } else {
          this.toast.error('Impossible de changer le statut de cette année.');
        }
        console.error('Erreur Changement Statut:', err);
      }
    });
  }

  // ==========================================
  // HELPERS
  // ==========================================
  private formatDateForInput(dateValue: any): string {
    if (!dateValue) return '';
    if (typeof dateValue === 'string' && dateValue.length === 10) return dateValue;
    const d = new Date(dateValue);
    if (isNaN(d.getTime())) return '';
    return d.toISOString().split('T')[0];
  }

  getStatusLabel(status: string | undefined): string {
    switch (status) {
      case 'ENROLLMENT':
        return 'En inscription';
      case 'IN_PROGRESS':
        return 'En cours';
      case 'PLANNED':
        return 'Planifiée';
      case 'CLOSING':
        return 'Clôture';
      case 'COMPLETED':
        return 'Terminée';
      case 'CLOSED':
        return 'Clôturée';
      default:
        return status || 'Inconnu';
    }
  }

  getStatusClass(status: string | undefined): string {
    switch (status) {
      case 'ENROLLMENT':
        return 'bg-yellow-100 text-yellow-700';
      case 'IN_PROGRESS':
        return 'bg-emerald-100 text-emerald-700';
      case 'PLANNED':
        return 'bg-blue-100 text-blue-700';
      case 'CLOSING':
      case 'COMPLETED':
      case 'CLOSED':
        return 'bg-slate-100 text-slate-600';
      default:
        return 'bg-gray-100 text-gray-600';
    }
  }
}
