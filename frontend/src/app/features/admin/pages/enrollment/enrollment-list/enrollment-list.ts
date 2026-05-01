import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ToastService } from '../../../../../shared/services/toast.service';
import { EnrollmentControllerService, EnrollmentListResponse } from '../../../../../core/api';
import { PaginationComponent } from '../../../../../shared/components/pagination/pagination';

@Component({
  selector: 'app-enrollment-list',
  standalone: true,
  imports: [DatePipe, RouterLink, PaginationComponent],
  templateUrl: './enrollment-list.html',
  styleUrl: './enrollment-list.css'
})
export class EnrollmentList implements OnInit {
  // Injections
  private readonly enrollmentService = inject(EnrollmentControllerService);
  private readonly toastService = inject(ToastService);

  // State (Signals)
  enrollments = signal<EnrollmentListResponse[]>([]);
  isLoading = signal<boolean>(true);

  // --- PAGINATION  ---
  currentPage = signal<number>(0);
  pageSize = signal<number>(10);
  totalElements = signal<number>(0);

  // Configuration dynamique des statuts alignée sur ton Enum Java
  readonly statusConfig: Record<string, { label: string; classes: string }> = {
    PRE_ENROLLED: {
      label: 'Pré-inscrit',
      classes: 'bg-amber-50 text-amber-700 border-amber-100'
    },
    INCOMPLETE: {
      label: 'Incomplet',
      classes: 'bg-orange-50 text-orange-700 border-orange-100'
    },
    VALIDATED: {
      label: 'Validé',
      classes: 'bg-emerald-50 text-emerald-700 border-emerald-100'
    },
    REJECTED: {
      label: 'Rejeté',
      classes: 'bg-red-50 text-red-700 border-red-100'
    },
    CANCELLED: {
      label: 'Annulé',
      classes: 'bg-rose-50 text-rose-700 border-rose-100'
    }
  };

  // NOUVEAU: Configuration des Types d'inscription (Avec les beaux badges inset-ring)
  readonly typeConfig: Record<string, { label: string; classes: string }> = {
    NEW: {
      label: 'Nouvelle Inscription',
      classes:
        'inline-flex items-center rounded-md bg-blue-400/10 px-2 py-1 text-[10px] font-bold uppercase tracking-wider text-blue-700 inset-ring inset-ring-blue-400/30'
    },
    RE_ENROLLMENT: {
      label: 'Réinscription',
      classes:
        'inline-flex items-center rounded-md bg-indigo-400/10 px-2 py-1 text-[10px] font-bold uppercase tracking-wider text-indigo-700 inset-ring inset-ring-indigo-400/30'
    }
  };

  ngOnInit(): void {
    this.loadEnrollments();
  }

  loadEnrollments(): void {
    this.isLoading.set(true);

    this.enrollmentService.getAllEnrollmentPaged(this.currentPage(), this.pageSize()).subscribe({
      next: (response: any) => {
        this.enrollments.set(response.content || []);
        this.totalElements.set(response?.totalElements || 0);
      },
      error: err => {
        console.error(err);
        this.toastService.error('Erreur lors du chargement des inscriptions');
        this.isLoading.set(false);
      },
      complete: () => this.isLoading.set(false)
    });
  }

  getInitials(firstName?: string, lastName?: string): string {
    const first = firstName ? firstName.charAt(0).toUpperCase() : '';
    const last = lastName ? lastName.charAt(0).toUpperCase() : '';
    return `${first}${last}` || '??';
  }

  getStatusConfig(status?: string) {
    if (!status) {
      return { label: 'Inconnu', classes: 'bg-slate-50 text-slate-500 border-slate-200' };
    }
    return (
      this.statusConfig[status] || {
        label: status,
        classes: 'bg-slate-50 text-slate-700 border-slate-200'
      }
    );
  }

  // NOUVEAU: Helper pour les types d'inscriptions
  getTypeConfig(type?: string) {
    if (!type) {
      return {
        label: 'Type Inconnu',
        classes:
          'inline-flex items-center rounded-md bg-gray-400/10 px-2 py-1 text-[10px] font-bold uppercase tracking-wider text-gray-600 inset-ring inset-ring-gray-400/20'
      };
    }
    return (
      this.typeConfig[type] || {
        label: type,
        classes:
          'inline-flex items-center rounded-md bg-slate-400/10 px-2 py-1 text-[10px] font-bold uppercase tracking-wider text-slate-700 inset-ring inset-ring-slate-400/20'
      }
    );
  }

  onPageChange(newPage: number) {
    this.currentPage.set(newPage);
    this.loadEnrollments();
  }
}
