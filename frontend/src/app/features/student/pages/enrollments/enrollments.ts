import { Component, inject, OnInit, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { EnrollmentControllerService } from '../../../../core/api/api/enrollment-controller.service';
import { StudentEnrollmentSimpleResponse } from '../../../../core/api/model/student-enrollment-simple-response';

@Component({
  selector: 'app-student-enrollments',
  standalone: true,
  imports: [NgClass, RouterLink],
  templateUrl: './enrollments.html',
  styleUrl: './enrollments.css',
  host: {
    'class': 'flex-1 flex flex-col min-h-0 overflow-hidden'
  }
})
export class StudentEnrollments implements OnInit {
  private enrollmentService = inject(EnrollmentControllerService);

  enrollments = signal<StudentEnrollmentSimpleResponse[]>([]);
  isLoading = signal<boolean>(true);

  ngOnInit(): void {
    this.fetchEnrollments();
  }

  fetchEnrollments(): void {
    this.isLoading.set(true);
    this.enrollmentService.getMyEnrollments().subscribe({
      next: (response) => {
        if (response.data) {
          this.enrollments.set(response.data);
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error fetching student enrollments:', err);
        this.isLoading.set(false);
      }
    });
  }

  // --- UI Helpers ---
  getTypeLabel(type?: string): string {
    switch (type) {
      case 'NEW':
        return 'Nouvelle inscription';
      case 'REPEATER':
        return 'Redoublant';
      case 'REINTEGRATION':
        return 'Réintégration';
      default:
        return type || '-';
    }
  }

  getTypeClasses(type?: string): string {
    switch (type) {
      case 'NEW':
        return 'bg-blue-50 text-blue-700 border border-blue-100';
      case 'REPEATER':
        return 'bg-amber-50 text-amber-700 border border-amber-100';
      case 'REINTEGRATION':
        return 'bg-purple-50 text-purple-700 border border-purple-100';
      default:
        return 'bg-slate-50 text-slate-700 border border-slate-100';
    }
  }

  getStatusConfig(status?: string) {
    switch (status) {
      case 'VALIDATED':
      case 'COMPLETED':
        return { label: 'Validée', classes: 'bg-emerald-100 text-emerald-700 border-emerald-200' };
      case 'PRE_ENROLLED':
        return { label: 'Pré-inscrit', classes: 'bg-blue-100 text-blue-700 border-blue-200' };
      case 'CONDITIONALLY_VALIDATED':
        return { label: 'Validée sous conditions', classes: 'bg-cyan-100 text-cyan-700 border-cyan-200' };
      case 'INCOMPLETE':
        return { label: 'Dossier incomplet', classes: 'bg-orange-100 text-orange-700 border-orange-200' };
      case 'WAITLISTED':
        return { label: 'Liste d\'attente', classes: 'bg-amber-100 text-amber-700 border-amber-200' };
      case 'SUSPENDED':
        return { label: 'Suspendue', classes: 'bg-rose-100 text-rose-700 border-rose-200' };
      case 'REJECTED':
        return { label: 'Rejetée', classes: 'bg-red-100 text-red-700 border-red-200' };
      case 'CANCELLED':
        return { label: 'Annulée', classes: 'bg-slate-100 text-slate-700 border-slate-200' };
      case 'DROPPED_OUT':
        return { label: 'Abandon', classes: 'bg-stone-100 text-stone-700 border-stone-200' };
      default:
        return {
          label: status || 'Inconnu',
          classes: 'bg-slate-50 text-slate-500 border-slate-200'
        };
    }
  }
}
