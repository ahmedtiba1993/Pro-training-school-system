import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { TeacherControllerService } from '../../../core/api/api/teacher-controller.service';
import { TeacherResponse } from '../../../core/api/model/teacher-response';

@Component({
  selector: 'app-teacher-profile',
  standalone: true,
  imports: [DatePipe, NgClass],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
  host: {
    'class': 'flex-1 flex flex-col min-h-0 overflow-hidden'
  }
})
export class TeacherProfileComponent implements OnInit {
  private teacherService = inject(TeacherControllerService);

  // Signals
  teacher = signal<TeacherResponse | null>(null);
  isLoading = signal<boolean>(true);

  ngOnInit(): void {
    this.fetchProfile();
  }

  fetchProfile(): void {
    this.isLoading.set(true);
    this.teacherService.getMyTeacherProfile().subscribe({
      next: (response) => {
        if (response.data) {
          this.teacher.set(response.data);
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error fetching teacher profile:', err);
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

  getStatusConfig(status: TeacherResponse.StatusEnum | undefined) {
    switch (status) {
      case 'ACTIVE':
        return { label: 'Actif', classes: 'bg-emerald-100 text-emerald-700 border-emerald-200' };
      case 'ONBOARDING':
        return { label: 'Intégration', classes: 'bg-blue-100 text-blue-700 border-blue-200' };
      case 'ON_LEAVE':
        return { label: 'En Congé', classes: 'bg-amber-100 text-amber-700 border-amber-200' };
      case 'SUSPENDED':
        return { label: 'Suspendu', classes: 'bg-rose-100 text-rose-700 border-rose-200' };
      case 'DEPARTED':
        return { label: 'Départ', classes: 'bg-slate-100 text-slate-700 border-slate-200' };
      default:
        return {
          label: status || 'Inconnu',
          classes: 'bg-slate-100 text-slate-700 border-slate-200'
        };
    }
  }

  getContractTypeLabel(type: TeacherResponse.ContractTypeEnum | undefined): string {
    switch (type) {
      case 'CDI':
        return 'CDI (Contrat Durée Indéterminée)';
      case 'CDD':
        return 'CDD (Contrat Durée Déterminée)';
      case 'FREELANCE':
        return 'Freelance / Indépendant';
      case 'GUEST':
        return 'Vacataire / Invité';
      default:
        return type || 'Non précisé';
    }
  }

  getContractTypeConfig(type: TeacherResponse.ContractTypeEnum | undefined) {
    switch (type) {
      case 'CDI':
        return { theme: 'bg-indigo-50/50 border-indigo-100 text-indigo-700' };
      case 'CDD':
        return { theme: 'bg-blue-50/50 border-blue-100 text-blue-700' };
      case 'FREELANCE':
        return { theme: 'bg-purple-50/50 border-purple-100 text-purple-700' };
      case 'GUEST':
        return { theme: 'bg-amber-50/50 border-amber-100 text-amber-700' };
      default:
        return { theme: 'bg-slate-50 border-slate-100 text-slate-700' };
    }
  }

  getDegreeLabel(degree: TeacherResponse.DegreeEnum | undefined): string {
    switch (degree) {
      case 'BACHELOR':
        return 'Licence / Bac+3';
      case 'FOUR_YEAR_BACHELOR':
        return 'Maîtrise / Bac+4';
      case 'MASTER':
        return 'Master / Bac+5';
      case 'ENGINEERING_DEGREE':
        return "Diplôme d'Ingénieur";
      case 'DOCTORATE':
        return 'Doctorat';
      default:
        return degree || 'Non précisé';
    }
  }

  getGenderLabel(gender: TeacherResponse.GenderEnum | undefined): string {
    if (gender === 'MALE') return 'Homme';
    if (gender === 'FEMALE') return 'Femme';
    return gender || 'Non précisé';
  }
}
