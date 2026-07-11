import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { AttendanceRecordControllerService } from '../../../../core/api/api/attendance-record-controller.service';
import { StudentAttendanceRecordResponse } from '../../../../core/api/model/student-attendance-record-response';

@Component({
  selector: 'app-student-attendance',
  standalone: true,
  imports: [DatePipe, NgClass],
  templateUrl: './attendance.html',
  styleUrl: './attendance.css',
  host: {
    'class': 'flex-1 flex flex-col min-h-0 overflow-hidden'
  }
})
export class StudentAttendanceComponent implements OnInit {
  private attendanceService = inject(AttendanceRecordControllerService);

  // State signals
  isLoading = signal<boolean>(true);

  // Paginated records
  records = signal<StudentAttendanceRecordResponse[]>([]);
  pageNumber = signal<number>(0);
  pageSize = signal<number>(10);
  totalPages = signal<number>(0);
  totalElements = signal<number>(0);
  isLast = signal<boolean>(true);

  ngOnInit(): void {
    this.loadPage();
  }

  formatTime(timeStr: string | undefined): string {
    if (!timeStr) return '';
    const parts = timeStr.split(':');
    if (parts.length >= 2) {
      return `${parts[0]}h${parts[1]}`;
    }
    return timeStr;
  }

  loadPage(): void {
    this.isLoading.set(true);
    this.attendanceService.getAttendanceRecordsByStudent(this.pageNumber(), this.pageSize()).subscribe({
      next: (response) => {
        if (response.data) {
          this.records.set(response.data.content || []);
          this.totalPages.set(response.data.totalPages || 0);
          this.totalElements.set(response.data.totalElements || 0);
          this.isLast.set(response.data.isLast ?? true);
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error loading paginated attendance records:', err);
        this.isLoading.set(false);
      }
    });
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.pageNumber.set(page);
      this.loadPage();
    }
  }

  nextPage(): void {
    if (!this.isLast()) {
      this.pageNumber.update(p => p + 1);
      this.loadPage();
    }
  }

  prevPage(): void {
    if (this.pageNumber() > 0) {
      this.pageNumber.update(p => p - 1);
      this.loadPage();
    }
  }
}
