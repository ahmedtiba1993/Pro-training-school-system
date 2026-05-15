import {Component, inject, OnInit, signal} from '@angular/core';
import {DatePipe, NgClass} from '@angular/common';
import {RouterLink} from '@angular/router';

import {StudentControllerService} from '../../../../../../core/api/api/student-controller.service';
import {StudentListResponse} from '../../../../../../core/api/model/student-list-response';
import {PaginationComponent} from '../../../../../../shared/components/pagination/pagination';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [DatePipe, NgClass, PaginationComponent, RouterLink],
  templateUrl: './student-list.html',
  styleUrl: './student-list.css'
})
export class StudentList implements OnInit {
  private studentService = inject(StudentControllerService);

  // --- STATE (SIGNALS) ---
  students = signal<StudentListResponse[]>([]);
  isLoading = signal<boolean>(false);
  activeStudentsCount = signal<number>(0);

  // --- FILTERS ---
  searchKeyword = signal<string>('');
  selectedStatus = signal<string>('');

  // --- PAGINATION ---
  currentPage = signal<number>(0);
  pageSize = signal<number>(6);
  totalElements = signal<number>(0);

  ngOnInit(): void {
    this.fetchStudents();
    this.fetchActiveStudentsCount();
  }

  // --- FILTER INPUT HANDLING ---

  updateKeyword(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchKeyword.set(input.value);
  }

  updateStatus(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.selectedStatus.set(select.value);
  }

  applyFilters(): void {
    this.currentPage.set(0);
    this.fetchStudents();
  }

  resetFilters(): void {
    this.searchKeyword.set('');
    this.selectedStatus.set('');
    this.currentPage.set(0);
    this.fetchStudents();
  }

  fetchStudents(): void {
    this.isLoading.set(true);

    const keywordParam =
      this.searchKeyword().trim() !== '' ? this.searchKeyword().trim() : undefined;
    const statusParam = this.selectedStatus() !== '' ? this.selectedStatus() : undefined;

    this.studentService
      .getStudentsPaginated(this.currentPage(), this.pageSize(), keywordParam, statusParam as any)
      .subscribe({
        next: response => {
          const pageData = response.data;
          if (pageData) {
            this.students.set(pageData.content || []);
            this.totalElements.set(response?.data?.totalElements || 0);
          }
          this.isLoading.set(false);
        },
        error: err => {
          console.error('Error fetching students', err);
          this.isLoading.set(false);
        }
      });
  }

  fetchActiveStudentsCount(): void {
    this.studentService.countActiveStudents().subscribe({
      next: response => {
        if (response.data !== undefined) {
          this.activeStudentsCount.set(response.data);
        }
      },
      error: err => {
        console.error('Error fetching statistics', err);
      }
    });
  }

  // --- UI Utilities ---

  getInitials(firstName?: string, lastName?: string): string {
    const f = firstName ? firstName.charAt(0).toUpperCase() : '';
    const l = lastName ? lastName.charAt(0).toUpperCase() : '';
    return `${f}${l}` || '??';
  }

  getStatusConfig(status: string | undefined) {
    switch (status) {
      case 'ACTIVE':
        return {
          label: 'Actif',
          classes: 'bg-emerald-50 text-emerald-700 border-emerald-200',
          icon: 'check'
        };
      case 'PROSPECT':
        return {
          label: 'Pré-inscrit',
          classes: 'bg-blue-50 text-blue-700 border-blue-200',
          icon: 'clock'
        };
      case 'SUSPENDED':
        return {
          label: 'Suspendu',
          classes: 'bg-rose-50 text-rose-700 border-rose-200',
          icon: 'alert'
        };
      case 'DROPPED_OUT':
        return {
          label: 'Abandon',
          classes: 'bg-orange-50 text-orange-700 border-orange-200',
          icon: 'x'
        };
      case 'ALUMNI':
        return {
          label: 'Diplômé',
          classes: 'bg-purple-50 text-purple-700 border-purple-200',
          icon: 'award'
        };
      default:
        return {
          label: status || 'Inconnu',
          classes: 'bg-slate-50 text-slate-700 border-slate-200',
          icon: 'help'
        };
    }
  }

  onPageChange(newPage: number) {
    this.currentPage.set(newPage);
    this.fetchStudents();
  }
}
