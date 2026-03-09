import { Component, inject, OnInit, signal } from '@angular/core';
import {
  AcademicYearControllerService,
  AcademicYearDto,
  ActiveAcademicYearDTO
} from '../../../../../core/api';
import { PaginationComponent } from '../../../../../shared/components/pagination/pagination';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastService } from '../../../../../shared/services/toast.service';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-academic-year-list',
  imports: [DatePipe, ReactiveFormsModule, RouterLink, PaginationComponent],
  templateUrl: './academic-year-list.html',
  styleUrl: './academic-year-list.css'
})
export class AcademicYearList implements OnInit {
  private apiService = inject(AcademicYearControllerService);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);

  // Data
  academicYears = signal<AcademicYearDto[]>([]);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string>('');
  editingYearId = signal<number | null>(null);
  currentActiveYear = signal<any | null>(null);
  isLoadingActiveYear = signal<boolean>(true);

  // --- PAGINATION ---
  currentPage = signal<number>(0);
  pageSize = signal<number>(5); // Number of items displayed per page
  totalElements = signal<number>(0);

  // Confirmation modal state
  showConfirmModal = signal<boolean>(false);
  selectedYearId = signal<number | null>(null);

  // --- MODALE D'AJOUT ---
  showAddModal = signal<boolean>(false);
  isSaving = signal<boolean>(false); // Spinner pour le bouton enregistrer

  // Définition du formulaire
  addForm = this.fb.group({
    label: ['', [Validators.required]],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]]
  });

  ngOnInit(): void {
    this.loadAcademicYears();
    this.loadCurrentActiveYear();
  }

  loadAcademicYears() {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.apiService.getAllAcademicYears(this.currentPage(), this.pageSize()).subscribe({
      next: (response: any) => {
        const pageData = response.data;

        this.academicYears.set(pageData?.content || []);

        // knows how many pages exist
        this.totalElements.set(pageData?.totalElements || 0);

        this.isLoading.set(false);
      },
      error: err => {
        console.error('Error while loading data', err);
        this.errorMessage.set('Unable to load academic years.');
        this.isLoading.set(false);
      }
    });
  }

  // Page change
  onPageChange(newPage: number) {
    this.currentPage.set(newPage);
    this.loadAcademicYears();
  }

  // Actions activater academic year
  openActivateModal(id?: number) {
    if (!id) return;
    this.selectedYearId.set(id);
    this.showConfirmModal.set(true);
  }

  closeModal() {
    this.showConfirmModal.set(false);
    this.editingYearId.set(null);
  }
  activateAcademicYear() {
    const id = this.selectedYearId();

    if (!id) return;

    this.isLoading.set(true);

    this.apiService.activateAcademicYear(id).subscribe({
      next: () => {
        this.loadAcademicYears();
        this.loadCurrentActiveYear();
        this.showConfirmModal.set(false);
        this.isLoading.set(false);
      },
      error: err => {
        console.error('Error while activating academic year', err);
        this.errorMessage.set('Unable to activate the academic year.');
        this.isLoading.set(false);
      }
    });
  }

  // --- ADD ACTIONS new academic year ---
  openAddModal() {
    // Reset the form every time the modal is opened
    this.addForm.reset();
    this.showAddModal.set(true);
  }

  closeAddModal() {
    this.showAddModal.set(false);
  }

  submitAddForm() {
    if (!this.isFormValid()) return;

    this.isSaving.set(true);
    const payload = this.buildPayload();
    const id = this.editingYearId();

    if (id) {
      // Update
      this.apiService.updateAcademicYear(id, payload).subscribe({
        next: () => {
          this.toast.success('Année académique modifiée avec succès');
          this.handleSuccess();
        },
        error: err => this.handleError(err)
      });
    } else {
      // Add
      this.apiService.createAcademicYear(payload).subscribe({
        next: () => {
          this.toast.success('Année académique ajoutée avec succès');
          this.handleSuccess();
        },
        error: err => this.handleError(err)
      });
    }
  }

  private isFormValid(): boolean {
    if (this.addForm.invalid) {
      this.addForm.markAllAsTouched();
      return false;
    }
    return true;
  }

  private buildPayload() {
    return {
      label: this.addForm.value.label!,
      startDate: this.addForm.value.startDate!,
      endDate: this.addForm.value.endDate!
    };
  }

  private handleSuccess() {
    this.isSaving.set(false);
    this.closeAddModal();
    this.loadAcademicYears();
  }

  private handleError(err: any) {
    console.error('Error creating academic year', err);

    this.resetFormErrors();

    if (err?.error?.errors) {
      err.error.errors.forEach((e: any) => {
        this.handleBackendError(e);
      });
    } else {
      this.errorMessage.set(err?.message || 'Unknown error');
    }

    this.isSaving.set(false);
  }

  private resetFormErrors() {
    Object.keys(this.addForm.controls).forEach(key => {
      this.addForm.get(key)?.setErrors(null);
    });
  }

  private handleBackendError(e: any) {
    switch (e.field) {
      case 'label':
        if (e.message === 'LABEL_ALREADY_EXISTS') {
          this.toast.error('Cette année académique existe déjà');
        }
        break;

      case 'dateRangeValid':
        this.toast.error('La date de début doit être antérieure à la date de fin');
        break;
    }
  }

  // METHOD FOR UPDATING
  editAcademicYear(year: AcademicYearDto) {
    // Switch to Edit mode
    this.editingYearId.set(year.id!);

    // Pre-fill the form with the academic year's data
    // .substring(0, 10) keeps only "YYYY-MM-DD" if your backend returns time
    this.addForm.patchValue({
      label: year.label,
      startDate: year.startDate ? year.startDate.substring(0, 10) : '',
      endDate: year.endDate ? year.endDate.substring(0, 10) : ''
    });

    // Open the same modal
    this.showAddModal.set(true);
  }

  // Get the active year
  loadCurrentActiveYear() {
    this.isLoadingActiveYear.set(true);

    this.apiService.getCurrentSession().subscribe({
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
}
