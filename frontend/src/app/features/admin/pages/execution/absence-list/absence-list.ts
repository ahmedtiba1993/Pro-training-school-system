import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CourseSessionControllerService } from '../../../../../core/api/api/course-session-controller.service';
import { CourseSessionResponse } from '../../../../../core/api/model/course-session-response';
import { ApiResponseCourseSessionResponse } from '../../../../../core/api/model/api-response-course-session-response';
import { ApiResponseListStudentAttendanceResponse } from '../../../../../core/api/model/api-response-list-student-attendance-response';
import { AttendanceRecordControllerService } from '../../../../../core/api/api/attendance-record-controller.service';
import { ToastService } from '../../../../../shared/services/toast.service';

export interface StudentAttendance {
  enrollmentId: number;
  firstName: string;
  lastName: string;
  studentCode: string;
  status: 'PRESENT' | 'ABSENT';
}

@Component({
  selector: 'app-absence-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './absence-list.html',
  styleUrl: './absence-list.css'
})
export class AbsenceList implements OnInit {
  private route = inject(ActivatedRoute);
  private courseSessionService = inject(CourseSessionControllerService);
  private attendanceService = inject(AttendanceRecordControllerService);
  private toastService = inject(ToastService);

  isLoading = signal<boolean>(false);
  courseSessionId = signal<number | null>(null);
  courseSession = signal<CourseSessionResponse | null>(null);
  studentAttendanceList = signal<StudentAttendance[]>([]);

  searchQuery = signal<string>('');
  currentFilter = signal<'ALL' | 'PRESENT' | 'ABSENT'>('ALL');
  sessionStatus = signal<'DRAFT' | 'COMPLETED'>('DRAFT');
  successBannerMessage = signal<{ title: string; body: string } | null>(null);
  showSuccessBanner = signal<boolean>(false);

  // Confirm Modal Signals
  showConfirmModal = signal<boolean>(false);
  confirmModalTitle = signal<string>('');
  confirmModalMessage = signal<string>('');
  confirmModalAction = signal<'DRAFT' | 'VALIDATE' | null>(null);

  // Computed signals
  filteredStudents = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const filter = this.currentFilter();
    return this.studentAttendanceList().filter(s => {
      const nameMatch =
        s.firstName.toLowerCase().includes(query) ||
        s.lastName.toLowerCase().includes(query) ||
        s.studentCode.toLowerCase().includes(query);
      const filterMatch = filter === 'ALL' || s.status === filter;
      return nameMatch && filterMatch;
    });
  });

  stats = computed(() => {
    const list = this.studentAttendanceList();
    const total = list.length;
    const presents = list.filter(s => s.status === 'PRESENT').length;
    const absents = list.filter(s => s.status === 'ABSENT').length;
    const rate = total > 0 ? Math.round((presents / total) * 100) : 0;
    return {
      total,
      presents,
      absents,
      rate
    };
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      if (!isNaN(id)) {
        this.courseSessionId.set(id);
        this.loadCourseSessionAndStudents(id);
      }
    }
  }

  loadCourseSessionAndStudents(sessionId: number): void {
    this.isLoading.set(true);
    this.courseSessionService.getCourseSessionById(sessionId).subscribe({
      next: (res: ApiResponseCourseSessionResponse) => {
        if (res.data) {
          this.courseSession.set(res.data);
          if (res.data.attendanceSubmitted || res.data.status === 'COMPLETED') {
            this.sessionStatus.set('COMPLETED');
          } else {
            this.sessionStatus.set('DRAFT');
          }
        }
      },
      error: (err: any) => {
        console.error('Error loading course session:', err);
      }
    });

    this.attendanceService.getStudentsAttendanceForSession(sessionId).subscribe({
      next: (res: ApiResponseListStudentAttendanceResponse) => {
        if (res.data) {
          const mapped: StudentAttendance[] = res.data.map((s) => ({
            enrollmentId: s.enrollmentId ?? 0,
            firstName: s.firstName ?? '',
            lastName: s.lastName ?? '',
            studentCode: s.studentCode ?? '',
            status: s.status === 'ABSENT' ? 'ABSENT' : 'PRESENT'
          }));
          this.studentAttendanceList.set(mapped);
        }
        this.isLoading.set(false);
      },
      error: (err: any) => {
        console.error('Error loading student attendance:', err);
        this.isLoading.set(false);
      }
    });
  }

  updateStudentStatus(enrollmentId: number, status: 'PRESENT' | 'ABSENT'): void {
    if (this.sessionStatus() === 'COMPLETED') return;
    this.studentAttendanceList.update((list) =>
      list.map((s) => {
        if (s.enrollmentId === enrollmentId) {
          return {
            ...s,
            status
          };
        }
        return s;
      })
    );
  }

  setQuickFilter(filter: 'ALL' | 'PRESENT' | 'ABSENT'): void {
    this.currentFilter.set(filter);
  }

  triggerSaveDraft(): void {
    if (this.sessionStatus() === 'COMPLETED') return;
    this.confirmModalTitle.set('Enregistrer le brouillon ?');
    this.confirmModalMessage.set("Les modifications temporaires de la feuille d'appel seront enregistrées comme brouillon. Vous pourrez toujours les modifier plus tard.");
    this.confirmModalAction.set('DRAFT');
    this.showConfirmModal.set(true);
  }

  triggerValidate(): void {
    if (this.sessionStatus() === 'COMPLETED') return;
    this.confirmModalTitle.set('Validation définitive ?');
    this.confirmModalMessage.set("L'émargement sera enregistré de manière définitive dans le cahier de texte. Cette action est irréversible et verrouillera la saisie.");
    this.confirmModalAction.set('VALIDATE');
    this.showConfirmModal.set(true);
  }

  confirmAction(): void {
    const action = this.confirmModalAction();
    this.showConfirmModal.set(false);
    if (action === 'DRAFT') {
      this.saveAsDraft();
    } else if (action === 'VALIDATE') {
      this.validateAttendance();
    }
  }

  closeConfirmModal(): void {
    this.showConfirmModal.set(false);
    this.confirmModalAction.set(null);
  }

  saveAsDraft(): void {
    const courseSessionId = this.courseSessionId();
    if (!courseSessionId) return;

    this.isLoading.set(true);
    const items = this.studentAttendanceList().map(s => ({
      enrollmentId: s.enrollmentId,
      status: s.status as 'PRESENT' | 'ABSENT'
    }));

    const request = {
      courseSessionId,
      isDraft: true,
      items
    };

    this.attendanceService.submitAttendance(request).subscribe({
      next: (res) => {
        this.toastService.success('Brouillon enregistré avec succès !');
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error saving draft:', err);
        this.isLoading.set(false);
      }
    });
  }

  validateAttendance(): void {
    const courseSessionId = this.courseSessionId();
    if (!courseSessionId) return;

    this.isLoading.set(true);
    const items = this.studentAttendanceList().map(s => ({
      enrollmentId: s.enrollmentId,
      status: s.status as 'PRESENT' | 'ABSENT'
    }));

    const request = {
      courseSessionId,
      isDraft: false,
      items
    };

    this.attendanceService.submitAttendance(request).subscribe({
      next: (res) => {
        this.sessionStatus.set('COMPLETED');
        this.successBannerMessage.set({
          title: "Feuille d'appel validée avec succès !",
          body: "L'émargement a été enregistré de manière définitive dans le cahier de texte de l'établissement."
        });
        this.showSuccessBanner.set(true);
        this.isLoading.set(false);
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      error: (err) => {
        console.error('Error validating attendance:', err);
        this.isLoading.set(false);
      }
    });
  }

  closeSuccessBanner(): void {
    this.showSuccessBanner.set(false);
  }
}
