import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms'; // <-- IMPORT FORMULAIRES
import { ExamSessionControllerService, ExamSessionDto } from '../../../../../core/api';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-term-sessions',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule],
  templateUrl: './term-sessions.html',
})
export class TermSessions implements OnInit {
  private route = inject(ActivatedRoute);
  private apiService = inject(ExamSessionControllerService);
  private fb = inject(FormBuilder);
  private toast = inject(ToastService);

  termId = signal<number | null>(null);
  sessions = signal<ExamSessionDto[]>([]);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string>('');

  showAddModal = signal<boolean>(false);
  isSaving = signal<boolean>(false);

  sessionForm = this.fb.group({
    sessionType: ['MAIN', [Validators.required]],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]],
  });

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('termId');
    if (idParam) {
      this.termId.set(Number(idParam));
      this.loadSessions();
    }
  }

  loadSessions() {
    const tId = this.termId();
    if (!tId) return;

    this.isLoading.set(true);
    this.errorMessage.set('');

    this.apiService.getAllByTerm(tId).subscribe({
      next: (response: any) => {
        this.sessions.set(response.data || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erreur API', err);
        this.errorMessage.set("Impossible de charger les sessions d'examens.");
        this.isLoading.set(false);
      },
    });
  }

  openAddModal() {
    this.sessionForm.reset({ sessionType: 'MAIN' }); // Reset avec 'MAIN' par défaut
    this.showAddModal.set(true);
  }

  closeAddModal() {
    this.showAddModal.set(false);
  }

  submitSessionForm() {
    if (this.sessionForm.invalid) {
      this.sessionForm.markAllAsTouched();
      return;
    }

    const tId = this.termId();
    if (!tId) return;

    this.isSaving.set(true);

    const payload = {
      sessionType: this.sessionForm.value.sessionType as 'MAIN' | 'RETAKE',
      startDate: this.sessionForm.value.startDate!,
      endDate: this.sessionForm.value.endDate!,
      termId: tId,
    };

    this.apiService.createExamSession(payload).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.closeAddModal();
        this.loadSessions(); // Recharge le tableau
        this.toast.success("Session d'examen ajoutée avec succès !");
      },
      error: (err) => {
        console.error('Erreur ajout', err);
        if (err?.error?.errors) {
          err.error.errors.forEach((e: any) => {
            switch (e.field) {
              case 'sessionType':
                if (e.message === 'SESSION_TYPE_ALREADY_EXISTS_FOR_THIS_TERM') {
                  this.toast.error('Un type de session existe déjà pour cette période');
                }
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
