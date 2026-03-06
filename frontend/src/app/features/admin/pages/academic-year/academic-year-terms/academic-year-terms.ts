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

  // --- ADD MODAL MANAGEMENT ---
  showAddModal = signal<boolean>(false);
  isSaving = signal<boolean>(false);

  // Add form
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
    this.termForm.reset();
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

    // Preparing the object to send to the backend
    const payload = {
      name: this.termForm.value.name!,
      startDate: this.termForm.value.startDate!,
      endDate: this.termForm.value.endDate!,
      academicYearId: yearId,
    };

    this.apiService.createTerm(payload).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.closeAddModal();
        this.loadTerms();
        this.toast.success('Trimestre ajouté avec succès !');
      },
      error: (err) => {
        console.error('Erreur ajout', err);
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
          this.errorMessage.set(err?.message || 'Unknown error');
        }
        this.isSaving.set(false);
      },
    });
  }
}
