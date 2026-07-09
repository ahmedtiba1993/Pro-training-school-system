import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NgClass } from '@angular/common';
import { EnrollmentControllerService } from '../../../../../core/api/api/enrollment-controller.service';
import { EnrollmentResponse } from '../../../../../core/api/model/enrollment-response';

@Component({
  selector: 'app-student-enrollment-detail',
  standalone: true,
  imports: [RouterLink, NgClass],
  templateUrl: './detail.html',
  styleUrl: './detail.css',
  host: {
    'class': 'flex-1 flex flex-col min-h-0 overflow-hidden'
  }
})
export class StudentEnrollmentDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private enrollmentService = inject(EnrollmentControllerService);

  enrollment = signal<EnrollmentResponse | null>(null);
  isLoading = signal<boolean>(true);
  isNotFound = signal<boolean>(false);

  // Computed properties
  documents = computed(() => this.enrollment()?.enrollmentSubmittedDocuments || []);
  providedDocsCount = computed(() => this.documents().filter((d: any) => d.provided).length);
  totalDocsCount = computed(() => this.documents().length);
  isDossierComplet = computed(
    () => this.totalDocsCount() > 0 && this.providedDocsCount() === this.totalDocsCount()
  );

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.fetchEnrollmentDetails(Number(id));
    } else {
      this.isNotFound.set(true);
      this.isLoading.set(false);
    }
  }

  fetchEnrollmentDetails(id: number): void {
    this.isLoading.set(true);
    this.isNotFound.set(false);
    this.enrollmentService.getMyEnrollmentDetails(id).subscribe({
      next: (response) => {
        if (response.data) {
          this.enrollment.set(response.data);
        } else {
          this.isNotFound.set(true);
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error fetching enrollment details:', err);
        this.isNotFound.set(true);
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
        return { 
          label: 'Validée', 
          classes: 'bg-emerald-100 text-emerald-700 border-emerald-200',
          desc: 'Votre dossier a été validé. Votre inscription est pleinement active pour cette promotion.'
        };
      case 'PRE_ENROLLED':
        return { 
          label: 'Pré-inscrit', 
          classes: 'bg-blue-100 text-blue-700 border-blue-200',
          desc: 'Votre pré-inscription a été soumise avec succès. L\'administration étudie actuellement votre dossier.'
        };
      case 'CONDITIONALLY_VALIDATED':
        return { 
          label: 'Validée sous conditions', 
          classes: 'bg-cyan-100 text-cyan-700 border-cyan-200',
          desc: 'Votre inscription est approuvée sous réserve de la présentation de documents complémentaires ou du règlement des frais.'
        };
      case 'INCOMPLETE':
        return { 
          label: 'Dossier incomplet', 
          classes: 'bg-orange-100 text-orange-700 border-orange-200',
          desc: 'Certains documents obligatoires manquent à votre dossier. Veuillez les fournir dans les plus brefs délais.'
        };
      case 'WAITLISTED':
        return { 
          label: 'Liste d\'attente', 
          classes: 'bg-amber-100 text-amber-700 border-amber-200',
          desc: 'Les places pour cette promotion sont actuellement complètes. Votre dossier est placé sur liste d\'attente.'
        };
      case 'SUSPENDED':
        return { 
          label: 'Suspendue', 
          classes: 'bg-rose-100 text-rose-700 border-rose-200',
          desc: 'Votre inscription est temporairement suspendue. Veuillez vous rapprocher de l\'administration.'
        };
      case 'REJECTED':
        return { 
          label: 'Rejetée', 
          classes: 'bg-red-100 text-red-700 border-red-200',
          desc: 'Votre demande d\'inscription a été refusée.'
        };
      case 'CANCELLED':
        return { 
          label: 'Annulée', 
          classes: 'bg-slate-100 text-slate-700 border-slate-200',
          desc: 'L\'inscription a été annulée.'
        };
      case 'DROPPED_OUT':
        return { 
          label: 'Abandon', 
          classes: 'bg-stone-100 text-stone-700 border-stone-200',
          desc: 'Le statut indique un abandon ou un désistement de cette formation.'
        };
      default:
        return {
          label: status || 'Inconnu',
          classes: 'bg-slate-50 text-slate-500 border-slate-200',
          desc: 'Statut de l\'inscription non spécifié.'
        };
    }
  }
}
