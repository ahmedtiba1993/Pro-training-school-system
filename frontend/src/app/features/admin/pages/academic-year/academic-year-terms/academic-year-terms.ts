import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { TermControllerService } from '../../../../../core/api/api/term-controller.service';
import { TermDto } from '../../../../../core/api/model/term-dto';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-academic-year-terms',
  standalone: true,
  imports: [RouterLink, DatePipe, ReactiveFormsModule],
  templateUrl: './academic-year-terms.html',
})
export class AcademicYearTerms implements OnInit {
  private route = inject(ActivatedRoute);
  private apiService = inject(TermControllerService);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);

  academicYearId = signal<number | null>(null);
  terms = signal<TermDto[]>([]);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string>('');

  // ADD / EDIT MODAL MANAGEMENT
  showAddModal = signal<boolean>(false);
  isSaving = signal<boolean>(false);

  // Signal to track if we are in edit mode
  editingTermId = signal<number | null>(null);

  // Add/Edit form
  termForm = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]],
  });

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.academicYearId.set(Number(idParam));
      this.loadTerms();
    }
  }

  loadTerms() {
    const yearId = this.academicYearId();
    if (!yearId) return;

    this.isLoading.set(true);
    this.errorMessage.set('');
    this.apiService.getAllTermsByYear(yearId).subscribe({
      next: (response: any) => {
        this.terms.set(response.data || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erreur API', err);
        this.errorMessage.set('Impossible de charger les trimestres.');
        this.isLoading.set(false);
      },
    });
  }

  // --- MODAL ACTIONS ---

  openAddModal() {
    this.editingTermId.set(null); // Creation mode
    this.termForm.reset();
    this.showAddModal.set(true);
  }

  // Open the modal with pre-filled data
  editTerm(term: TermDto) {
    this.editingTermId.set(term.id!); // Edit mode
    this.termForm.patchValue({
      name: term.name,
      startDate: term.startDate ? term.startDate.substring(0, 10) : '',
      endDate: term.endDate ? term.endDate.substring(0, 10) : '',
    });
    this.showAddModal.set(true);
  }

  closeAddModal() {
    this.showAddModal.set(false);
  }

  submitTermForm() {
    if (this.termForm.invalid) {
      this.termForm.markAllAsTouched();
      return;
    }

    const yearId = this.academicYearId();
    if (!yearId) return;

    this.isSaving.set(true);

    const payload = {
      name: this.termForm.value.name!,
      startDate: this.termForm.value.startDate!,
      endDate: this.termForm.value.endDate!,
      academicYearId: yearId,
    };

    const termId = this.editingTermId();

    if (termId) {
      // Edit mode
      this.apiService.updateTerm(termId, payload).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.closeAddModal();
          this.loadTerms();
          this.toast.success('Trimestre modifié avec succès !');
        },
        error: (err) => this.handleBackendError(err),
      });
    } else {
      // Creation mode
      this.apiService.createTerm(payload).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.closeAddModal();
          this.loadTerms();
          this.toast.success('Trimestre ajouté avec succès !');
        },
        error: (err) => this.handleBackendError(err), // Utilisation de la méthode commune
      });
    }
  }

  private handleBackendError(err: any) {
    console.error('Erreur Backend', err);
    if (err?.error?.errors) {
      err.error.errors.forEach((e: any) => {
        switch (e.field) {
          case 'name':
            if (e.message === 'TERM_NAME_ALREADY_EXISTS_IN_THIS_YEAR') {
              this.toast.error('Le nom de la période existe déjà pour cette année');
            }
            break;
          case 'dateRangeValid':
            this.toast.error('La date de début doit être inférieure à la date de fin');
            break;
        }
      });
    } else {
      this.errorMessage.set(err?.message || 'Erreur inconnue survenue.');
    }
    this.isSaving.set(false);
  }
}
