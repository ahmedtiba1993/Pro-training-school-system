import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PaginationComponent } from '../../../../../shared/components/pagination/pagination';
import { ToastService } from '../../../../../shared/services/toast.service';
import { EnrollmentControllerService, EnrollmentResponse } from '../../../../../core/api';

@Component({
  selector: 'app-enrollment-list',
  standalone: true,
  imports: [DatePipe, RouterLink, PaginationComponent, NgClass],
  templateUrl: './enrollment-list.html',
  styleUrl: './enrollment-list.css'
})
export class EnrollmentList implements OnInit {
  private apiService = inject(EnrollmentControllerService);
  private toast = inject(ToastService);

  // --- DATA SIGNALS ---
  enrollments = signal<EnrollmentResponse[]>([]);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string>('');

  // --- PAGINATION SIGNALS ---
  currentPage = signal<number>(0);
  pageSize = signal<number>(10); // 10 lignes par page par défaut
  totalElements = signal<number>(0);

  ngOnInit(): void {
    this.loadEnrollments();
  }

  loadEnrollments() {
    this.isLoading.set(true);
    this.errorMessage.set('');

    // Assurez-vous que le nom de la méthode correspond à votre service API généré
    this.apiService.getAllEnrollments(this.currentPage(), this.pageSize()).subscribe({
      next: (response: any) => {
        const pageData = response.data;

        this.enrollments.set(pageData?.content || []);
        this.totalElements.set(pageData?.totalElements || 0);

        this.isLoading.set(false);
      },
      error: err => {
        console.error('Erreur lors du chargement des inscriptions', err);
        this.errorMessage.set('Impossible de charger la liste des inscriptions.');
        this.toast.error('Erreur de connexion au serveur');
        this.isLoading.set(false);
      }
    });
  }

  // --- PAGINATION ACTION ---
  onPageChange(newPage: number) {
    this.currentPage.set(newPage);
    this.loadEnrollments();
  }

  // --- NOUVEAU : Calculer les initiales (ex: "Amine Mansouri" -> "AM") ---
  getInitials(fullName: string): string {
    if (!fullName) return '??';
    const parts = fullName.split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return fullName.substring(0, 2).toUpperCase();
  }

  // --- NOUVEAU : Configuration visuelle des statuts ---
  getStatusConfig(status: string) {
    switch (status) {
      case 'APPROVED':
        return {
          label: 'Validé',
          class: 'bg-emerald-50 text-emerald-700 border-emerald-100',
          icon: 'check-circle'
        };
      case 'PENDING':
        return {
          label: 'En attente',
          class: 'bg-blue-50 text-blue-700 border-blue-100',
          icon: 'clock'
        };
      case 'INCOMPLETE': // Si vous avez ce statut
        return {
          label: 'Incomplet',
          class: 'bg-amber-50 text-amber-700 border-amber-200',
          icon: 'alert-circle'
        };
      case 'REJECTED':
        return {
          label: 'Rejeté',
          class: 'bg-rose-50 text-rose-700 border-rose-200',
          icon: 'x-circle'
        };
      default:
        return {
          label: status,
          class: 'bg-slate-50 text-slate-700 border-slate-200',
          icon: 'info'
        };
    }
  }

  // Couleurs de fond de l'avatar aléatoires basées sur la première lettre
  getAvatarColor(name: string): string {
    const colors = [
      'bg-indigo-100 text-indigo-700',
      'bg-blue-100 text-blue-700',
      'bg-emerald-100 text-emerald-700',
      'bg-amber-100 text-amber-700',
      'bg-rose-100 text-rose-700'
    ];
    const index = name ? name.charCodeAt(0) % colors.length : 0;
    return colors[index];
  }
}
