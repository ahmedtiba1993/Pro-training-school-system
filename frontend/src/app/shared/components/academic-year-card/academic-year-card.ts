import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { AcademicYearControllerService, PublicAcademicYearResponse } from '../../../core/api';

@Component({
  selector: 'app-academic-year-card',
  standalone: true,
  imports: [DatePipe, NgClass],
  templateUrl: './academic-year-card.html',
})
export class AcademicYearCardComponent implements OnInit {
  private readonly academicYearService = inject(AcademicYearControllerService);

  readonly academicYear = signal<PublicAcademicYearResponse | null>(null);
  readonly isLoading = signal<boolean>(true);
  readonly isNotFound = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.loadDefaultAcademicYear();
  }

  loadDefaultAcademicYear(): void {
    this.isLoading.set(true);
    this.isNotFound.set(false);
    this.error.set(null);

    this.academicYearService.getDefaultAcademicYear().subscribe({
      next: (response) => {
        if (response && response.success && response.data) {
          this.academicYear.set(response.data);
          this.isNotFound.set(false);
        } else if (
          response &&
          !response.success &&
          (response.message === 'ACADEMIC_YEAR_NOT_FOUND' || response.errorCode === 'ERR_NOT_FOUND')
        ) {
          this.academicYear.set(null);
          this.isNotFound.set(true);
        } else if (response && response.data) {
          this.academicYear.set(response.data);
          this.isNotFound.set(false);
        } else {
          this.academicYear.set(null);
          this.isNotFound.set(true);
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        const errorBody = err?.error;
        const isNotFoundError =
          err?.status === 404 ||
          errorBody?.message === 'ACADEMIC_YEAR_NOT_FOUND' ||
          errorBody?.errorCode === 'ERR_NOT_FOUND';

        if (isNotFoundError) {
          this.isNotFound.set(true);
          this.academicYear.set(null);
          this.error.set(null);
        } else {
          console.error('Erreur lors du chargement de l\'année scolaire par défaut:', err);
          this.isNotFound.set(false);
          this.error.set('Impossible de charger les informations de l\'année scolaire en cours.');
        }
        this.isLoading.set(false);
      }
    });
  }

  getStatusLabel(status: string | undefined): string {
    switch (status) {
      case 'ENROLLMENT':
        return 'Inscriptions en cours';
      case 'IN_PROGRESS':
        return 'Année en cours';
      case 'PLANNED':
        return 'Planifiée';
      case 'CLOSING':
        return 'Clôture en cours';
      case 'COMPLETED':
        return 'Année terminée';
      case 'CLOSED':
        return 'Clôturée';
      default:
        return status || 'Statut non spécifié';
    }
  }

  getStatusBadgeClass(status: string | undefined): string {
    switch (status) {
      case 'IN_PROGRESS':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'ENROLLMENT':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'PLANNED':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'CLOSING':
        return 'bg-orange-50 text-orange-700 border-orange-200';
      case 'COMPLETED':
      case 'CLOSED':
        return 'bg-slate-100 text-slate-700 border-slate-200';
      default:
        return 'bg-slate-50 text-slate-600 border-slate-200';
    }
  }
}
