import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { StudentControllerService } from '../../../../../../core/api/api/student-controller.service';
import { StudentResponse } from '../../../../../../core/api/model/student-response';

@Component({
  selector: 'app-student-detail',
  standalone: true,
  imports: [DatePipe, NgClass, RouterLink],
  templateUrl: './student-detail.html',
  styleUrl: './student-detail.css'
})
export class StudentDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private studentService = inject(StudentControllerService);

  // Signals
  student = signal<StudentResponse | null>(null);
  isLoading = signal<boolean>(true);

  ngOnInit(): void {
    // Get ID from URL
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.fetchStudentDetails(Number(id));
    }
  }

  fetchStudentDetails(id: number): void {
    this.isLoading.set(true);

    this.studentService.getStudentById(id).subscribe({
      next: response => {
        if (response.data) {
          this.student.set(response.data);
        }
        this.isLoading.set(false);
      },
      error: err => {
        console.error("Error retrieving student details", err);
        this.isLoading.set(false);
      }
    });
  }

  // --- UI Helpers ---

  getInitials(firstName?: string, lastName?: string): string {
    const f = firstName ? firstName.charAt(0).toUpperCase() : '';
    const l = lastName ? lastName.charAt(0).toUpperCase() : '';
    return `${f}${l}` || '??';
  }

  getStatusConfig(status: string | undefined) {
    switch (status) {
      case 'ACTIVE':
        return { label: 'Actif', classes: 'bg-emerald-100 text-emerald-700 border-emerald-200' };
      case 'PROSPECT':
        return { label: 'Pré-inscrit', classes: 'bg-blue-100 text-blue-700 border-blue-200' };
      case 'SUSPENDED':
        return { label: 'Suspendu', classes: 'bg-rose-100 text-rose-700 border-rose-200' };
      case 'DROPPED_OUT':
        return { label: 'Abandon', classes: 'bg-orange-100 text-orange-700 border-orange-200' };
      case 'ALUMNI':
        return { label: 'Diplômé', classes: 'bg-purple-100 text-purple-700 border-purple-200' };
      default:
        return {
          label: status || 'Inconnu',
          classes: 'bg-slate-100 text-slate-700 border-slate-200'
        };
    }
  }

  getParentConfig(link: string | undefined) {
    switch (link) {
      case 'FATHER':
        return {
          label: 'Père',
          theme: 'bg-indigo-50/50 border-indigo-100',
          iconTheme: 'bg-white text-indigo-600 border-indigo-200'
        };
      case 'MOTHER':
        return {
          label: 'Mère',
          theme: 'bg-fuchsia-50/30 border-fuchsia-100',
          iconTheme: 'bg-fuchsia-50 text-fuchsia-600 border-fuchsia-100'
        };
      default:
        return {
          label: link || 'Autre',
          theme: 'bg-slate-50 border-slate-200',
          iconTheme: 'bg-white text-slate-600 border-slate-200'
        };
    }
  }

  getMentionLabel(mention: string | undefined): string {
    switch (mention) {
      case 'TRES_BIEN':
        return 'Très Bien';
      case 'BIEN':
        return 'Bien';
      case 'ASSEZ_BIEN':
        return 'Assez Bien';
      case 'PASSABLE':
        return 'Passable';
      default:
        return mention || '-';
    }
  }

  getResidenceLabel(residence: string | undefined): string {
    if (residence === 'URBAN') return 'Urbain';
    if (residence === 'RURAL') return 'Rural';
    if (residence === 'WITH_BOTH_PARENTS') return 'Chez les deux parents';
    return residence || 'Non précisé';
  }

  getParentsSituationLabel(situation: string | undefined): string {
    switch (situation) {
      case 'MARRIED':
        return 'Mariés';
      case 'DIVORCED':
        return 'Divorcés';
      case 'LIVING_TOGETHER':
        return 'Vivent ensemble';
      default:
        return situation || 'Non précisée';
    }
  }
}
