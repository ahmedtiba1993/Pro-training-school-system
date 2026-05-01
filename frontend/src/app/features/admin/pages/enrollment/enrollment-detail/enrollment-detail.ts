import { ChangeDetectorRef, Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe, NgClass, TitleCasePipe } from '@angular/common';

import { EnrollmentControllerService, EnrollmentResponse } from '../../../../../core/api';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-enrollment-detail',
  standalone: true,
  imports: [RouterLink, NgClass, TitleCasePipe],
  templateUrl: './enrollment-detail.html',
  styleUrl: './enrollment-detail.css'
})
export class EnrollmentDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly enrollmentService = inject(EnrollmentControllerService);
  private readonly toastService = inject(ToastService);
  private readonly cdRef = inject(ChangeDetectorRef);

  // State (Signals)
  enrollment = signal<any | null>(null);
  isLoading = signal<boolean>(true);
  isNotFound = signal<boolean>(false);
  isUpdatingDoc = signal<number | null>(null);

  // NOUVEAU : Signaux pour la gestion du statut
  isStatusModalOpen = signal<boolean>(false);
  isUpdatingStatus = signal<boolean>(false);
  selectedStatusToUpdate = signal<string>('');

  // Computed Properties
  student = computed(() => this.enrollment()?.student);
  promotion = computed(() => this.enrollment()?.promotion);

  parentsList = computed<any[]>(() => this.student()?.parents || []);
  father = computed(() => this.parentsList().find((p: any) => p.link === 'FATHER'));
  mother = computed(() => this.parentsList().find((p: any) => p.link === 'MOTHER'));
  otherGuardian = computed(() => this.parentsList().find((p: any) => p.link === 'OTHER'));

  documents = computed<any[]>(() => this.enrollment()?.enrollmentSubmittedDocuments || []);
  providedDocsCount = computed(() => this.documents().filter((d: any) => d.provided).length);
  totalDocsCount = computed(() => this.documents().length);

  isDossierComplet = computed(
    () => this.totalDocsCount() > 0 && this.providedDocsCount() === this.totalDocsCount()
  );

  // MIS À JOUR avec tes nouveaux statuts (INCOMPLETE, REJECTED)
  readonly statusConfig: Record<string, { label: string; classes: string; icon: string }> = {
    PRE_ENROLLED: {
      label: 'Pré-inscrit',
      classes: 'bg-amber-100 text-amber-700 border-amber-200',
      icon: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z'
    },
    INCOMPLETE: {
      label: 'Incomplet',
      classes: 'bg-orange-100 text-orange-700 border-orange-200',
      icon: 'M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z'
    },
    VALIDATED: {
      label: 'Validé',
      classes: 'bg-emerald-100 text-emerald-700 border-emerald-200',
      icon: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z'
    },
    REJECTED: {
      label: 'Rejeté',
      classes: 'bg-red-100 text-red-700 border-red-200',
      icon:
        'M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636'
    },
    CANCELLED: {
      label: 'Annulé',
      classes: 'bg-rose-100 text-rose-700 border-rose-200',
      icon: 'M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z'
    }
  };

  // Liste des statuts disponibles pour la modale
  availableStatuses = Object.keys(this.statusConfig);

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const idParam = params.get('id');
      if (idParam) {
        this.loadEnrollmentDetails(Number(idParam));
      } else {
        this.isNotFound.set(true);
        this.isLoading.set(false);
      }
    });
  }

  private loadEnrollmentDetails(id: number): void {
    this.isLoading.set(true);
    this.isNotFound.set(false);

    this.enrollmentService.getEnrollmentById(id).subscribe({
      next: (response: any) => {
        this.enrollment.set(response.data || response);
      },
      error: err => {
        console.error(err);
        if (err.error?.message === 'ENROLLMENT_NOT_FOUND' || err.status === 404) {
          this.isNotFound.set(true);
          this.toastService.error("Ce dossier d'inscription n'existe pas.");
        }
        this.isLoading.set(false);
      },
      complete: () => this.isLoading.set(false)
    });
  }

  // --- CHANGEMENT DE STATUT ---
  openStatusModal(): void {
    this.selectedStatusToUpdate.set(this.enrollment().status); // Pré-sélectionne le statut actuel
    this.isStatusModalOpen.set(true);
  }

  closeStatusModal(): void {
    this.isStatusModalOpen.set(false);
  }

  updateStatus(): void {
    const newStatus = this.selectedStatusToUpdate();
    const enrollmentId = this.enrollment().id;

    if (newStatus === this.enrollment().status) {
      this.closeStatusModal();
      return; // Inutile d'appeler l'API si le statut n'a pas changé
    }

    this.isUpdatingStatus.set(true);

    // APPEL DE L'API PATCH
    this.enrollmentService.updateStatus(enrollmentId, newStatus as any).subscribe({
      next: () => {
        // Met à jour localement l'objet pour forcer le rafraîchissement UI
        this.enrollment.update(currentData => {
          if (!currentData) return currentData;
          return { ...currentData, status: newStatus };
        });

        this.toastService.success('Le statut a été mis à jour avec succès.');
        this.closeStatusModal();
      },
      error: err => {
        console.error(err);
        this.toastService.error('Erreur lors de la mise à jour du statut.');
      },
      complete: () => this.isUpdatingStatus.set(false)
    });
  }

  toggleDocumentStatus(doc: any): void {
    this.isUpdatingDoc.set(doc.id);

    this.enrollmentService.toggleDocumentStatus(doc.id).subscribe({
      next: () => {
        const newStatus = !doc.provided;
        this.enrollment.update(currentData => {
          if (!currentData) return currentData;
          return {
            ...currentData,
            enrollmentSubmittedDocuments: currentData.enrollmentSubmittedDocuments.map((d: any) =>
              d.id === doc.id ? { ...d, provided: newStatus } : d
            )
          };
        });
        this.cdRef.detectChanges();
        this.toastService.success(newStatus ? 'Document marqué comme reçu !' : 'Document annulé.');
      },
      error: err => {
        console.error(err);
        this.toastService.error('Erreur lors de la mise à jour du document.');
      },
      complete: () => {
        this.isUpdatingDoc.set(null);
      }
    });
  }

  getStatusConfig(status?: string) {
    if (!status) return { label: 'Inconnu', classes: 'bg-slate-100 text-slate-600', icon: '' };
    return (
      this.statusConfig[status] || {
        label: status,
        classes: 'bg-slate-100 text-slate-600',
        icon: ''
      }
    );
  }

  getInitials(firstName?: string, lastName?: string): string {
    const f = firstName ? firstName.charAt(0).toUpperCase() : '';
    const l = lastName ? lastName.charAt(0).toUpperCase() : '';
    return `${f}${l}` || '??';
  }
}
