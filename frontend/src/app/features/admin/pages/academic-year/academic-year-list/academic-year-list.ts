import { Component, inject, OnInit, signal } from '@angular/core';
import {
  AcademicYearControllerService,
  AcademicYearRequest,
  AcademicYearResponse,
  ActiveAcademicYearResponse
} from '../../../../../core/api';
import { PaginationComponent } from '../../../../../shared/components/pagination/pagination';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastService } from '../../../../../shared/services/toast.service';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-academic-year-list',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, RouterLink, PaginationComponent],
  templateUrl: './academic-year-list.html',
  styleUrl: './academic-year-list.css'
})
export class AcademicYearList implements OnInit {
  private apiService = inject(AcademicYearControllerService);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);

  // --- GLOBAL DATA ---
  academicYearsResponse = signal<AcademicYearResponse[]>([]);
  currentActiveYear = signal<ActiveAcademicYearResponse | null>(null);

  // --- LOADING STATES ---
  isLoading = signal<boolean>(true);
  isLoadingActiveYear = signal<boolean>(true);
  isSaving = signal<boolean>(false);

  // --- PAGINATION  ---
  currentPage = signal<number>(0);
  pageSize = signal<number>(3);
  totalElements = signal<number>(0);

  // --- SELECTED ID (used by all action modals) ---
  selectedYearId = signal<number | null>(null);

  // MODAL : ADD & EDIT
  showAddModal = signal<boolean>(false);
  editingYearId = signal<number | null>(null);

  addForm = this.fb.group({
    label: ['', [Validators.required]],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]]
  });

  // MODAL : DETAILS (READ-ONLY)
  showDetailsModal = signal<boolean>(false);
  selectedYearDetails = signal<AcademicYearResponse | null>(null);
  isLoadingDetails = signal<boolean>(false);

  // MODAL : TOGGLE ACTIVE/INACTIVE
  showToggleActiveModal = signal<boolean>(false);
  toggleActionType = signal<'ACTIVATE' | 'DEACTIVATE'>('ACTIVATE');

  // MODALE CHANGE STATUS
  showChangeStatusModal = signal<boolean>(false);
  newStatusValue = signal<string>('');

  ngOnInit(): void {
    this.loadAcademicYears();
    this.loadCurrentActiveYear();
  }

  loadAcademicYears() {
    this.isLoading.set(true);
    this.apiService.getAllAcademicYears(this.currentPage(), this.pageSize()).subscribe({
      next: (response: any) => {
        const pageData = response.data;
        this.academicYearsResponse.set(pageData?.content || []);
        this.totalElements.set(pageData?.totalElements || 0);
        this.isLoading.set(false);
      },
      error: err => {
        console.error('Erreur chargement données', err);
        this.toast.error('Impossible de charger les années scolaires.');
        this.isLoading.set(false);
      }
    });
  }

  loadCurrentActiveYear() {
    this.isLoadingActiveYear.set(true);
    this.apiService.getCurrentActiveSession().subscribe({
      next: (response: any) => {
        if (response.success && response.data) {
          this.currentActiveYear.set(response.data);
        }
        this.isLoadingActiveYear.set(false);
      },
      error: err => {
        console.error('Erreur chargement année active', err);
        this.isLoadingActiveYear.set(false);
      }
    });
  }

  onPageChange(newPage: number) {
    this.currentPage.set(newPage);
    this.loadAcademicYears();
  }

  // ACTIONS: ADD & EDIT
  openAddModal() {
    this.editingYearId.set(null);
    this.addForm.reset();
    this.showAddModal.set(true);
  }

  editAcademicYear(year: AcademicYearResponse) {
    this.editingYearId.set(year.id!);
    this.addForm.patchValue({
      label: year.label,
      startDate: this.formatDateForInput(year.startDate),
      endDate: this.formatDateForInput(year.endDate)
    });
    this.showAddModal.set(true);
  }

  closeAddModal() {
    this.showAddModal.set(false);
    this.editingYearId.set(null);
    this.addForm.reset();
  }

  submitAddForm() {
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

  private handleSuccess(message: string) {
    this.toast.success(message);
    this.isSaving.set(false);
    this.closeAddModal();
    this.loadAcademicYears();
  }

  private handleError(err: any) {
    this.isSaving.set(false);
    const errorBody = err.error;

    if (
      errorBody &&
      errorBody.errorCode === 'ENTITY_ALREADY_EXISTS' &&
      errorBody.message === 'LABEL_ALREADY_EXISTS'
    ) {
      this.toast.error('Ce label existe déjà. Veuillez en choisir un autre.');
      this.addForm.get('label')?.setErrors({ duplicate: true });
      this.addForm.get('label')?.markAsTouched();
    } else {
      this.toast.error('Une erreur inattendue est survenue.');
      console.error('Erreur API:', err);
    }
  }

  private formatDateForInput(dateValue: any): string {
    if (!dateValue) return '';
    if (typeof dateValue === 'string' && dateValue.length === 10) return dateValue;
    const d = new Date(dateValue);
    if (isNaN(d.getTime())) return '';
    return d.toISOString().split('T')[0];
  }

  viewAcademicYearDetails(id: number) {
    this.isLoadingDetails.set(true);
    this.showDetailsModal.set(true);

    this.apiService.getAcademicYearById(id).subscribe({
      next: (response: any) => {
        if (response.success && response.data) {
          this.selectedYearDetails.set(response.data);
        }
        this.isLoadingDetails.set(false);
      },
      error: err => {
        this.toast.error('Impossible de charger les détails.');
        this.isLoadingDetails.set(false);
        this.closeDetailsModal();
      }
    });
  }

  closeDetailsModal() {
    this.showDetailsModal.set(false);
    setTimeout(() => this.selectedYearDetails.set(null), 200);
  }

  translateStatus(status: string | undefined): string {
    if (!status) return 'Inconnu';
    const statusMap: Record<string, string> = {
      PLANNED: 'Planifiée',
      IN_PROGRESS: 'En cours',
      CLOSED: 'Clôturée'
    };
    return statusMap[status] || status;
  }

  openToggleActiveModal(id: number | undefined, currentIsActive: boolean) {
    if (!id) return;
    this.selectedYearId.set(id);
    this.toggleActionType.set(currentIsActive ? 'DEACTIVATE' : 'ACTIVATE');
    this.showToggleActiveModal.set(true);
  }

  closeToggleActiveModal() {
    this.showToggleActiveModal.set(false);
    this.selectedYearId.set(null);
  }

  confirmToggleActive() {
    const id = this.selectedYearId();
    const action = this.toggleActionType();
    if (!id) return;

    this.isLoading.set(true);
    const request$ =
      action === 'ACTIVATE'
        ? this.apiService.activateAcademicYear(id)
        : this.apiService.deactivateAcademicYear(id);

    request$.subscribe({
      next: () => {
        this.toast.success(
          `Année scolaire ${action === 'ACTIVATE' ? 'activée' : 'désactivée'} avec succès.`
        );
        this.loadAcademicYears();
        this.loadCurrentActiveYear();
        this.closeToggleActiveModal();
      },
      error: err => {
        console.error('Erreur activation/désactivation', err);
        this.toast.error("Impossible de modifier l'état d'activation.");
        this.isLoading.set(false);
        this.closeToggleActiveModal();
      }
    });
  }

  openChangeStatusModal(year: AcademicYearResponse) {
    this.selectedYearId.set(year.id!);
    this.newStatusValue.set(year.status!);
    this.showChangeStatusModal.set(true);
  }

  closeChangeStatusModal() {
    this.showChangeStatusModal.set(false);
    this.selectedYearId.set(null);
    this.newStatusValue.set('');
  }

  confirmChangeStatus() {
    const id = this.selectedYearId();
    const status = this.newStatusValue();
    if (!id || !status) return;

    this.isLoading.set(true);

    const statusEnum = status as 'PLANNED' | 'IN_PROGRESS' | 'CLOSED';

    this.apiService.changeAcademicYearStatus(id, statusEnum).subscribe({
      next: () => {
        this.toast.success("Le statut de l'année a été mis à jour avec succès.");
        this.loadAcademicYears();
        this.loadCurrentActiveYear();
        this.closeChangeStatusModal();
      },
      error: err => {
        console.error('Erreur changement statut', err);
        if (err?.error?.message === 'CANNOT_CHANGE_STATUS_OF_CLOSED_YEAR') {
          this.toast.error('Impossible de modifier une année définitivement clôturée.');
        }
        if (err?.error?.message === 'ANOTHER_YEAR_IS_ALREADY_IN_PROGRESS') {
          this.toast.error(
            "Une autre année scolaire est déjà en cours. Veuillez la clôturer avant d'activer celle-ci."
          );
        } else {
          this.toast.error('Impossible de changer le statut.');
        }
        this.isLoading.set(false);
        this.closeChangeStatusModal();
      }
    });
  }

  getStatusClasses(status: string | undefined): string {
    switch (status) {
      case 'PLANNED':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'IN_PROGRESS':
        return 'bg-purple-50 text-purple-700 border-purple-200';
      case 'CLOSED':
        return 'bg-red-50 text-red-700 border-red-200';
      default:
        return 'bg-slate-50 text-slate-600 border-slate-200';
    }
  }
}
