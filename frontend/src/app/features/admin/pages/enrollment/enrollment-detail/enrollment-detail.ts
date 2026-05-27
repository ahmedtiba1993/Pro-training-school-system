import {ChangeDetectorRef, Component, computed, inject, OnInit, signal} from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {DatePipe, NgClass, TitleCasePipe} from '@angular/common';

import {EnrollmentControllerService} from '../../../../../core/api';
import {ToastService} from '../../../../../shared/services/toast.service';
import {finalize} from 'rxjs';

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
  isExportingPdf = signal<boolean>(false);

  // Signal to memorize the ID of the source class
  readonly sourceClassId = signal<string | null>(null);

  // Signals for status management
  isStatusModalOpen = signal<boolean>(false);
  isConfirmingStatus = signal<boolean>(false);
  isUpdatingStatus = signal<boolean>(false);
  selectedStatusToUpdate = signal<string | null>(null);

  // Computed Properties
  student = computed(() => this.enrollment()?.student);
  promotion = computed(() => this.enrollment()?.promotion);

  // DYNAMIC RETURN LINK (Calculates the destination based on the source)
  readonly backRouteLink = computed(() => {
    const classId = this.sourceClassId();
    // If we come from a class, we return to it, otherwise return to the general list
    return classId ? ['/admin/class-management', classId, 'students'] : ['/admin/enrollments'];
  });

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

  // --- WORKFLOW RULES (Strict state machine) ---
  private readonly workflowRules: Record<string, string[]> = {
    PRE_ENROLLED: [
      'INCOMPLETE',
      'CONDITIONALLY_VALIDATED',
      'VALIDATED',
      'WAITLISTED',
      'REJECTED',
      'CANCELLED'
    ],
    INCOMPLETE: ['CONDITIONALLY_VALIDATED', 'VALIDATED', 'WAITLISTED', 'CANCELLED'],
    WAITLISTED: ['CONDITIONALLY_VALIDATED', 'VALIDATED', 'CANCELLED'],
    CONDITIONALLY_VALIDATED: ['VALIDATED', 'SUSPENDED', 'DROPPED_OUT'],
    VALIDATED: ['SUSPENDED', 'DROPPED_OUT', 'COMPLETED'],
    SUSPENDED: ['VALIDATED', 'CONDITIONALLY_VALIDATED', 'DROPPED_OUT'],
    REJECTED: [],
    CANCELLED: [],
    DROPPED_OUT: [],
    COMPLETED: []
  };

  // Reactive list of allowed statuses based on the current state
  allowedStatusesToUpdate = computed(() => {
    const currentStatus = this.enrollment()?.status;
    return currentStatus ? this.workflowRules[currentStatus] || [] : [];
  });

  // --- UI CONFIGURATION FOR ALL STATUSES ---
  readonly statusConfig: Record<string, { label: string; classes: string; icon: string }> = {
    PRE_ENROLLED: {
      label: 'Pré-inscrit',
      classes: 'bg-blue-50 text-blue-700 border-blue-200',
      icon: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z'
    },
    INCOMPLETE: {
      label: 'Incomplet',
      classes: 'bg-orange-50 text-orange-700 border-orange-200',
      icon: 'M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z'
    },
    WAITLISTED: {
      label: "Sur liste d'attente",
      classes: 'bg-purple-50 text-purple-700 border-purple-200',
      icon: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z'
    },
    CONDITIONALLY_VALIDATED: {
      label: 'Validé (Sous condition)',
      classes: 'bg-teal-50 text-teal-700 border-teal-200',
      icon:
        'M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z'
    },
    VALIDATED: {
      label: 'Validé',
      classes: 'bg-emerald-50 text-emerald-700 border-emerald-200',
      icon: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z'
    },
    SUSPENDED: {
      label: 'Suspendu',
      classes: 'bg-amber-50 text-amber-700 border-amber-200',
      icon: 'M10 9v6m4-6v6m7-3a9 9 0 11-18 0 9 9 0 0118 0z'
    },
    DROPPED_OUT: {
      label: 'Abandon',
      classes: 'bg-stone-50 text-stone-700 border-stone-200',
      icon: 'M13 7a4 4 0 11-8 0 4 4 0 018 0zM9 14a6 6 0 00-6 6v1h12v-1a6 6 0 00-6-6zM21 12h-6'
    },
    COMPLETED: {
      label: 'Terminé',
      classes: 'bg-indigo-50 text-indigo-700 border-indigo-200',
      icon:
        'M9 12l2 2 4-4M7.835 4.697a3.42 3.42 0 001.946-.806 3.42 3.42 0 014.438 0 3.42 3.42 0 001.946.806 3.42 3.42 0 013.138 3.138 3.42 3.42 0 00.806 1.946 3.42 3.42 0 010 4.438 3.42 3.42 0 00-.806 1.946 3.42 3.42 0 01-3.138 3.138 3.42 3.42 0 00-1.946.806 3.42 3.42 0 01-4.438 0 3.42 3.42 0 00-1.946-.806 3.42 3.42 0 01-3.138-3.138 3.42 3.42 0 00-.806-1.946 3.42 3.42 0 010-4.438 3.42 3.42 0 00.806-1.946 3.42 3.42 0 013.138-3.138z'
    },
    REJECTED: {
      label: 'Rejeté',
      classes: 'bg-rose-50 text-rose-700 border-rose-200',
      icon:
        'M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636'
    },
    CANCELLED: {
      label: 'Annulé',
      classes: 'bg-red-50 text-red-700 border-red-200',
      icon: 'M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z'
    }
  };

  ngOnInit(): void {
    this.route.queryParamMap.subscribe(queryParams => {
      this.sourceClassId.set(queryParams.get('fromClass'));
    });

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

  exportToPdf(): void {
    const currentId = this.enrollment()?.id;
    if (!currentId) return;

    this.isExportingPdf.set(true);

    this.enrollmentService
      .exportPdf(currentId)
      .pipe(finalize(() => this.isExportingPdf.set(false)))
      .subscribe({
        next: (response: any) => {
          try {
            const blob =
              response instanceof Blob
                ? response
                : new Blob([response], {type: 'application/pdf'});
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `fiche_inscription_${currentId}.pdf`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);
            this.toastService.success('Le fichier PDF a été exporté avec succès.');
          } catch (e) {
            console.error('Erreur lors de la création du fichier PDF coté client:', e);
          }
        },
        error: err => {
          console.error('Erreur API lors du téléchargement:', err);
        }
      });
  }

  // --- STATUS CHANGE ---
  openStatusModal(): void {
    this.selectedStatusToUpdate.set(null);
    this.isConfirmingStatus.set(false);
    this.isStatusModalOpen.set(true);
  }

  closeStatusModal(): void {
    this.isStatusModalOpen.set(false);
    this.isConfirmingStatus.set(false);
    this.selectedStatusToUpdate.set(null);
  }

  proceedToStatusConfirmation(): void {
    if (this.selectedStatusToUpdate()) {
      this.isConfirmingStatus.set(true);
    }
  }

  updateStatus(): void {
    const newStatus = this.selectedStatusToUpdate();
    const enrollmentId = this.enrollment().id;

    if (!newStatus) return;

    this.isUpdatingStatus.set(true);

    this.enrollmentService.updateStatus(enrollmentId, newStatus as any).subscribe({
      next: () => {
        this.enrollment.update(currentData => {
          if (!currentData) return currentData;
          return {...currentData, status: newStatus};
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
              d.id === doc.id ? {...d, provided: newStatus} : d
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
    if (!status) return {label: 'Inconnu', classes: 'bg-slate-100 text-slate-600', icon: ''};
    return (
      this.statusConfig[status] || {
        label: status,
        classes: 'bg-slate-100 text-slate-600 border border-slate-200',
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
