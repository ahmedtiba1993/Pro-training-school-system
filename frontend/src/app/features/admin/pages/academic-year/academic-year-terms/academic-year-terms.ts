import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe, NgClass } from '@angular/common';
import { TermControllerService } from '../../../../../core/api/api/term-controller.service';
import { TermDto } from '../../../../../core/api/model/term-dto';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastService } from '../../../../../shared/services/toast.service';
import {
  AcademicYearControllerService,
  AcademicYearDto,
  ExamSessionControllerService,
  ExamSessionDto
} from '../../../../../core/api';

@Component({
  selector: 'app-academic-year-terms',
  standalone: true,
  //imports: [RouterLink, DatePipe, ReactiveFormsModule],
  imports: [ReactiveFormsModule, DatePipe, RouterLink, NgClass],
  templateUrl: './academic-year-terms.html'
})
export class AcademicYearTerms implements OnInit {
  private route = inject(ActivatedRoute);
  private apiService = inject(TermControllerService);
  private academicYerApi = inject(AcademicYearControllerService);
  private sessionApi = inject(ExamSessionControllerService);

  private fb = inject(FormBuilder);
  private toast = inject(ToastService);

  academicYearId = signal<number | null>(null);
  terms = signal<TermDto[]>([]);
  academicYear = signal<AcademicYearDto | null>(null);
  sessions = signal<ExamSessionDto[]>([]);
  selectedTermId = signal<number | null>(null);

  isLoading = signal<boolean>(true);
  errorMessage = signal<string>('');

  // ADD / EDIT MODAL MANAGEMENT
  showAddModal = signal<boolean>(false);
  isSaving = signal<boolean>(false);

  // ADD / EDIT Session Models
  showSessionModal = signal<boolean>(false);

  // Signal to track if we are in edit mode
  editingTermId = signal<number | null>(null);

  // Add/Edit form
  termForm = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]]
  });

  // Session Form
  sessionForm = this.fb.group({
    sessionType: ['MAIN', [Validators.required]],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]]
  });

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.academicYearId.set(Number(idParam));
      this.loadAcademicYear();
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
      error: err => {
        console.error('Erreur API', err);
        this.errorMessage.set('Impossible de charger les trimestres.');
        this.isLoading.set(false);
      }
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
      endDate: term.endDate ? term.endDate.substring(0, 10) : ''
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
      academicYearId: yearId
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
        error: err => this.handleBackendError(err)
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
        error: err => this.handleBackendError(err) // Utilisation de la méthode commune
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

  //Get Acamdic year by id
  loadAcademicYear() {
    const yearId = this.academicYearId();
    if (!yearId) return;

    this.academicYerApi.getAcademicYearById(yearId).subscribe({
      next: (response: any) => {
        this.academicYear.set(response.data);
      },
      error: err => {
        if (err?.error?.errorCode === 'ERR_NOT_FOUND') {
          this.errorMessage.set('Année académique introuvable');
        } else {
          this.errorMessage.set("Erreur lors du chargement de l'année académique");
        }
      }
    });
  }

  // Add Session
  openSessionModal(termId: number) {
    this.selectedTermId.set(termId); // Store the ID
    this.sessionForm.reset({ sessionType: 'MAIN' }); // Reset with 'MAIN' as default
    this.showSessionModal.set(true);
  }

  closeSessionModal() {
    this.showSessionModal.set(false);
    this.selectedTermId.set(null); // Clear the ID
  }

  submitSessionForm() {
    if (this.sessionForm.invalid) {
      this.sessionForm.markAllAsTouched();
      return;
    }

    const termId = this.selectedTermId();
    if (!termId) {
      this.toast.error('Erreur : Aucun trimestre sélectionné.');
      return;
    }

    this.isSaving.set(true);

    // Constructing the payload
    const payload = {
      sessionType: this.sessionForm.value.sessionType as ExamSessionDto['sessionType'],
      startDate: this.sessionForm.value.startDate!,
      endDate: this.sessionForm.value.endDate!,
      termId: termId // The parent term ID
    };

    // API call to create the session
    this.sessionApi.createExamSession(payload).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.closeSessionModal();
        this.loadTerms(); // Reload terms to display the new session
        this.toast.success('Session ajoutée avec succès !');
      },
      error: err => {
        this.isSaving.set(false);
        console.error('Erreur lors de la création de la session', err);
        if (err?.error?.errors) {
          err.error.errors.forEach((e: any) => {
            if (e.field === 'dateRangeValid') {
              this.toast.error('La date de début doit être inférieure à la date de fin');
            } else if (e.field === 'sessionType') {
              this.toast.error('Ce type de session existe déjà pour cette période');
            } else {
              this.toast.error(e.message || 'Erreur de validation');
            }
          });
        } else {
          this.toast.error('Erreur lors de la création de la session');
        }
      }
    });
  }
}
