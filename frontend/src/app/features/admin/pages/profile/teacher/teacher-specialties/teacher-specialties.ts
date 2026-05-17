import {Component, inject, OnInit, signal, computed} from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';

import {
  RefTeacherSpecialtyControllerService
} from '../../../../../../core/api/api/ref-teacher-specialty-controller.service';
import {RefTeacherSpecialtyResponse} from '../../../../../../core/api/model/ref-teacher-specialty-response';
import {RefTeacherSpecialtyRequest} from '../../../../../../core/api/model/ref-teacher-specialty-request';
import {UpdateSpecialtyRequest} from '../../../../../../core/api/model/update-specialty-request';
import {TeacherSimpleResponse} from '../../../../../../core/api/model/teacher-simple-response';

@Component({
  selector: 'app-teacher-specialties',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './teacher-specialties.html',
  styleUrls: ['./teacher-specialties.css']
})
export class TeacherSpecialties implements OnInit {
  private apiService = inject(RefTeacherSpecialtyControllerService);
  private fb = inject(FormBuilder);

  // === GLOBAL STATE (Signals) ===
  specialties = signal<RefTeacherSpecialtyResponse[]>([]);
  isLoading = signal<boolean>(false);
  totalSpecialties = computed(() => this.specialties().length);

  // === ADD/EDIT MODAL ===
  isModalOpen = signal<boolean>(false);
  modalMode = signal<'CREATE' | 'EDIT'>('CREATE');
  selectedSpecialtyId = signal<number | null>(null);
  errorMessage = signal<string | null>(null);

  // === ACTIVE TEACHERS MODAL ===
  isActiveTeachersModalOpen = signal<boolean>(false);
  activeTeachers = signal<TeacherSimpleResponse[]>([]);
  isActiveTeachersLoading = signal<boolean>(false);
  selectedSpecialtyName = signal<string>('');

  // === FORMS ===
  searchControl = this.fb.control('');
  specialtyForm: FormGroup;

  constructor() {
    this.specialtyForm = this.fb.group({
      code: ['', [Validators.required]],
      label: ['', [Validators.required]],
      description: ['']
    });
  }

  ngOnInit(): void {
    this.loadSpecialties();
  }

  // === SEARCH AND LIST MANAGEMENT ===

  onSearch(): void {
    const searchTerm = this.searchControl.value?.trim() || '';
    this.loadSpecialties(searchTerm);
  }

  loadSpecialties(search: string = ''): void {
    this.isLoading.set(true);
    this.apiService.getAllTeacherSpecialtiesWithCount(search).subscribe({
      next: (response: any) => {
        this.specialties.set(response.data || []);
        this.isLoading.set(false);
      },
      error: err => {
        console.error('Error loading specialties', err);
        this.isLoading.set(false);
      }
    });
  }

  // === ADD/EDIT MODAL MANAGEMENT ===

  openCreateModal(): void {
    this.modalMode.set('CREATE');
    this.selectedSpecialtyId.set(null);
    this.errorMessage.set(null);
    this.specialtyForm.reset();
    this.specialtyForm.get('code')?.enable();
    this.isModalOpen.set(true);
  }

  openEditModal(specialty: RefTeacherSpecialtyResponse): void {
    this.modalMode.set('EDIT');
    this.selectedSpecialtyId.set(specialty.id!);
    this.errorMessage.set(null);

    this.specialtyForm.patchValue({
      code: specialty.code,
      label: specialty.label,
      description: specialty.description
    });

    this.specialtyForm.get('code')?.disable();
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
    this.errorMessage.set(null);
    this.specialtyForm.reset();
  }

  private handleBackendError(err: any): void {
    this.isLoading.set(false);
    const backendMessage = err.error?.message;

    switch (backendMessage) {
      case 'SPECIALTY_CODE_ALREADY_EXISTS':
        this.errorMessage.set('Ce code de spécialité existe déjà. Veuillez en choisir un autre.');
        break;
      case 'SPECIALTY_LABEL_ALREADY_EXISTS':
        this.errorMessage.set('Ce libellé de spécialité existe déjà.');
        break;
      default:
        this.errorMessage.set('Une erreur inattendue est survenue. Vérifiez vos données.');
        break;
    }
  }

  submitForm(): void {
    if (this.specialtyForm.invalid) return;

    this.isLoading.set(true);
    this.errorMessage.set(null);

    const formValue = this.specialtyForm.getRawValue();

    if (this.modalMode() === 'CREATE') {
      const request: RefTeacherSpecialtyRequest = {
        code: formValue.code,
        label: formValue.label,
        description: formValue.description
      };

      this.apiService.createTeacherSpecialty(request).subscribe({
        next: () => {
          this.onSearch();
          this.closeModal();
        },
        error: err => this.handleBackendError(err)
      });
    } else {
      const request: UpdateSpecialtyRequest = {
        label: formValue.label,
        description: formValue.description
      };

      this.apiService.updateRefTeacherSpeciality(this.selectedSpecialtyId()!, request).subscribe({
        next: () => {
          this.onSearch();
          this.closeModal();
        },
        error: err => this.handleBackendError(err)
      });
    }
  }

  // === ACTIVE TEACHERS LIST MODAL MANAGEMENT ===

  openActiveTeachersModal(spec: RefTeacherSpecialtyResponse): void {
    this.selectedSpecialtyName.set(spec.label || '');
    this.isActiveTeachersModalOpen.set(true);
    this.isActiveTeachersLoading.set(true);
    this.activeTeachers.set([]);

    this.apiService.getActiveTeachersBySpecialty(spec.id!).subscribe({
      next: (response: any) => {
        this.activeTeachers.set(response.data || []);
        this.isActiveTeachersLoading.set(false);
      },
      error: () => {
        console.error('Error retrieving teachers');
        this.isActiveTeachersLoading.set(false);
      }
    });
  }

  closeActiveTeachersModal(): void {
    this.isActiveTeachersModalOpen.set(false);
    this.activeTeachers.set([]);
  }
}
