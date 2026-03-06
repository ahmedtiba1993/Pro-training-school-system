import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { TermControllerService } from '../../../../../core/api/api/term-controller.service';
import { TermDto } from '../../../../../core/api/model/term-dto';

@Component({
  selector: 'app-academic-year-terms',
  standalone: true,
  imports: [RouterLink, DatePipe],
  templateUrl: './academic-year-terms.html',
})
export class AcademicYearTerms implements OnInit {
  private route = inject(ActivatedRoute);
  private apiService = inject(TermControllerService);

  academicYearId = signal<number | null>(null);
  terms = signal<TermDto[]>([]);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string>('');

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
}
