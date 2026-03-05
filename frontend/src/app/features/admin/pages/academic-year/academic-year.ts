import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { AcademicYearDto } from '../../../../core/api/model/academic-year-dto';
import { AcademicYearControllerService } from '../../../../core/api/api/academic-year-controller.service';
import { PaginationComponent } from '../../../../shared/components/pagination/pagination';

@Component({
  selector: 'app-academic-year',
  standalone: true,
  imports: [DatePipe, PaginationComponent],
  templateUrl: './academic-year.html',
})
export class AcademicYear implements OnInit {
  private apiService = inject(AcademicYearControllerService);

  // Data
  academicYears = signal<AcademicYearDto[]>([]);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string>('');

  // --- PAGINATION ---
  currentPage = signal<number>(0);
  pageSize = signal<number>(10); // Number of items displayed per page
  totalElements = signal<number>(0);

  // Confirmation modal state
  showConfirmModal = signal<boolean>(false);
  selectedYearId = signal<number | null>(null);

  ngOnInit(): void {
    this.loadAcademicYears();
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
      error: (err) => {
        console.error('Error while loading data', err);
        this.errorMessage.set('Unable to load academic years.');
        this.isLoading.set(false);
      },
    });
  }

  // Page change
  onPageChange(newPage: number) {
    this.currentPage.set(newPage);
    this.loadAcademicYears();
  }

  // Actions
  activateAcademicYear() {
    const id = this.selectedYearId();

    if (!id) return;

    this.isLoading.set(true);

    this.apiService.activateAcademicYear(id).subscribe({
      next: () => {
        this.loadAcademicYears();
        this.showConfirmModal.set(false);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error while activating academic year', err);
        this.errorMessage.set('Unable to activate the academic year.');
        this.isLoading.set(false);
      },
    });
  }

  //Action model
  openActivateModal(id?: number) {
    if (!id) return;
    this.selectedYearId.set(id);
    this.showConfirmModal.set(true);
  }

  closeModal() {
    this.showConfirmModal.set(false);
  }
}
