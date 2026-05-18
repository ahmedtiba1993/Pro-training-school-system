import { Component, inject, OnInit, signal, computed } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
  FormControl
} from '@angular/forms';
import { DatePipe } from '@angular/common';

import { TeacherControllerService } from '../../../../../../core/api/api/teacher-controller.service';
import { RefTeacherSpecialtyControllerService } from '../../../../../../core/api/api/ref-teacher-specialty-controller.service';
import { TeacherResponse } from '../../../../../../core/api/model/teacher-response';
import { RefTeacherSpecialtySimpleResponse } from '../../../../../../core/api/model/ref-teacher-specialty-simple-response';
import { TeacherRequest } from '../../../../../../core/api/model/teacher-request';
import { PaginationComponent } from '../../../../../../shared/components/pagination/pagination';

@Component({
  selector: 'app-teacher-list',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, PaginationComponent],
  templateUrl: './teacher-list.html',
  styleUrls: ['./teacher-list.css']
})
export class TeacherList implements OnInit {
  private teacherService = inject(TeacherControllerService);
  private specialtyService = inject(RefTeacherSpecialtyControllerService);
  private fb = inject(FormBuilder);

  // === GLOBAL STATE ===
  teachers = signal<TeacherResponse[]>([]);
  specialties = signal<RefTeacherSpecialtySimpleResponse[]>([]);
  activeTeachersCount = signal<number>(0);

  // === PAGINATION & LOADING ===
  isLoading = signal<boolean>(false);
  isActiveCountLoading = signal<boolean>(true);
  currentPage = signal<number>(0);
  pageSize = signal<number>(15);
  totalElements = signal<number>(0);

  // === MODAL STATE ===
  isModalOpen = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);
  errorMessage = signal<string | null>(null);

  // Signal pour différencier Création (null) et Édition (ID du prof)
  editingTeacherId = signal<number | null>(null);

  isDetailsModalOpen = signal<boolean>(false);
  isDetailsLoading = signal<boolean>(false);
  selectedTeacher = signal<TeacherResponse | null>(null);

  isStatusModalOpen = signal<boolean>(false);
  targetTeacherId = signal<number | null>(null);

  // === FORMS ===
  filterForm: FormGroup;
  createForm: FormGroup;
  statusControl: FormControl;

  constructor() {
    this.filterForm = this.fb.group({
      keyword: [''],
      specialtyId: [''],
      contractType: [''],
      status: ['']
    });

    this.createForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', Validators.required],
      cin: ['', Validators.required],
      gender: ['MALE', Validators.required],
      contractType: ['CDI', Validators.required],
      status: ['ONBOARDING', Validators.required],
      hireDate: ['', Validators.required],
      degree: ['ENGINEERING_DEGREE', Validators.required],
      specialtyIds: [[], Validators.required]
    });

    this.statusControl = this.fb.control('', Validators.required);
  }

  ngOnInit(): void {
    this.loadSpecialties();
    this.loadActiveCount();
    this.loadTeachers();
  }

  // === SEARCH ACTIONS ===
  onSearch(): void {
    this.currentPage.set(0);
    this.loadTeachers();
  }

  // === API DATA LOADING ===
  loadSpecialties(): void {
    this.specialtyService.getAllTeacherSpecialtiesSimple().subscribe({
      next: (res: any) => this.specialties.set(res.data || [])
    });
  }

  loadActiveCount(): void {
    this.isActiveCountLoading.set(true);
    this.teacherService.getActiveTeachersCount().subscribe({
      next: (res: any) => {
        this.activeTeachersCount.set(res.data || 0);
        this.isActiveCountLoading.set(false);
      },
      error: () => this.isActiveCountLoading.set(false)
    });
  }

  loadTeachers(): void {
    this.isLoading.set(true);
    const filters = this.filterForm.value;

    const filtreRequest = {
      keyword: filters.keyword || undefined,
      specialtyId: filters.specialtyId || undefined,
      contractType: filters.contractType || undefined,
      status: filters.status || undefined
    };

    this.teacherService
      .getAllTeachers(filtreRequest, this.currentPage(), this.pageSize())
      .subscribe({
        next: (res: any) => {
          const pageData = res.data;
          this.teachers.set(pageData.content || []);
          this.totalElements.set(res?.data?.totalElements || 0);
          this.isLoading.set(false);
        },
        error: () => this.isLoading.set(false)
      });
  }

  onPageChange(newPage: number) {
    this.currentPage.set(newPage);
    this.loadTeachers();
  }

  // === MODAL MANAGEMENT (CREATE & EDIT) ===
  openModal(): void {
    this.errorMessage.set(null);
    this.editingTeacherId.set(null); // Mode Création
    this.createForm.reset({
      gender: 'MALE',
      contractType: 'CDI',
      status: 'ONBOARDING',
      degree: 'ENGINEERING_DEGREE',
      specialtyIds: []
    });
    this.isModalOpen.set(true);
  }

  openEditModal(teacher: TeacherResponse): void {
    this.errorMessage.set(null);
    this.editingTeacherId.set(teacher.id!); // Mode Édition

    // Formatage de la date pour l'input type="date" (YYYY-MM-DD)
    let formattedDate = '';
    if (teacher.hireDate) {
      formattedDate = new Date(teacher.hireDate).toISOString().split('T')[0];
    }

    // Gestion sécurisée de Set vs Array pour extraire les IDs
    let specIds: number[] = [];
    if (teacher.specialties) {
      if (Array.isArray(teacher.specialties)) {
        specIds = teacher.specialties.map(s => s.id!);
      } else {
        specIds = Array.from(teacher.specialties).map(s => s.id!);
      }
    }

    this.createForm.patchValue({
      firstName: teacher.firstName,
      lastName: teacher.lastName,
      email: teacher.email,
      phone: teacher.phone,
      cin: teacher.cin,
      gender: teacher.gender,
      contractType: teacher.contractType,
      status: teacher.status,
      hireDate: formattedDate,
      degree: teacher.degree,
      specialtyIds: specIds
    });

    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.errorMessage.set(null);
    this.isModalOpen.set(false);
    this.editingTeacherId.set(null);
  }

  private handleBackendError(err: any): void {
    this.isSubmitting.set(false);
    const backendMessage = err.error?.message;

    switch (backendMessage) {
      case 'EMAIL_ALREADY_EXISTS':
        this.errorMessage.set('Cette adresse e-mail est déjà utilisée.');
        break;
      case 'CIN_ALREADY_EXISTS':
        this.errorMessage.set('Ce numéro de CIN / Passeport existe déjà dans le système.');
        break;
      case 'PHONE_NUMBER_ALREADY_EXISTS':
        this.errorMessage.set('Ce numéro de téléphone est déjà associé à un autre formateur.');
        break;
      default:
        this.errorMessage.set(
          'Une erreur inattendue est survenue. Veuillez vérifier les informations saisies.'
        );
        break;
    }
  }

  // Gère l'ajout et la modification
  submitTeacher(): void {
    if (this.createForm.invalid) return;

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const request: TeacherRequest = this.createForm.value;
    const currentId = this.editingTeacherId();

    if (currentId) {
      // Mode Édition
      this.teacherService.updateTeacher(currentId, request).subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.closeModal();
          this.loadTeachers();
        },
        error: err => this.handleBackendError(err)
      });
    } else {
      // Mode Création
      this.teacherService.createTeacher(request).subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.closeModal();
          this.loadTeachers();
          this.loadActiveCount();
        },
        error: err => this.handleBackendError(err)
      });
    }
  }

  isSpecialtySelected(id: number): boolean {
    const currentSpecialties = (this.createForm.get('specialtyIds')?.value as number[]) || [];
    return currentSpecialties.includes(id);
  }

  toggleSpecialty(id: number, event: Event): void {
    const isChecked = (event.target as HTMLInputElement).checked;
    const currentSpecialties = (this.createForm.get('specialtyIds')?.value as number[]) || [];
    if (isChecked) {
      this.createForm.patchValue({ specialtyIds: [...currentSpecialties, id] });
    } else {
      this.createForm.patchValue({
        specialtyIds: currentSpecialties.filter(specId => specId !== id)
      });
    }
  }

  // === DETAILS MODAL MANAGEMENT ===
  openDetailsModal(id: number): void {
    this.isDetailsModalOpen.set(true);
    this.isDetailsLoading.set(true);

    this.teacherService.getTeacherById(id).subscribe({
      next: (res: any) => {
        this.selectedTeacher.set(res.data);
        this.isDetailsLoading.set(false);
      },
      error: () => this.isDetailsLoading.set(false)
    });
  }

  closeDetailsModal(): void {
    this.isDetailsModalOpen.set(false);
    this.selectedTeacher.set(null);
  }

  // === STATUS MODAL MANAGEMENT ===
  openStatusModal(teacher: TeacherResponse): void {
    this.targetTeacherId.set(teacher.id!);
    this.statusControl.setValue(teacher.status);
    this.isStatusModalOpen.set(true);
  }

  closeStatusModal(): void {
    this.isStatusModalOpen.set(false);
    this.targetTeacherId.set(null);
  }

  submitStatus(): void {
    if (this.statusControl.invalid || !this.targetTeacherId()) return;

    this.isSubmitting.set(true);

    this.teacherService
      .updateTeacherStatus(this.targetTeacherId()!, this.statusControl.value)
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.closeStatusModal();
          this.loadTeachers();
          this.loadActiveCount();
        },
        error: () => this.isSubmitting.set(false)
      });
  }

  // === TRANSLATIONS AND DISPLAY HELPERS ===
  getStatusConfig(status: string) {
    const configs: Record<string, { label: string; classes: string; icon: string }> = {
      ACTIVE: {
        label: 'Actif',
        classes: 'bg-emerald-50 text-emerald-700 border-emerald-100',
        icon: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z'
      },
      ONBOARDING: {
        label: 'En intégration',
        classes: 'bg-blue-50 text-blue-700 border-blue-100',
        icon: 'M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z'
      },
      ON_LEAVE: {
        label: 'En congé',
        classes: 'bg-amber-50 text-amber-700 border-amber-100',
        icon: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z'
      },
      SUSPENDED: {
        label: 'Suspendu',
        classes: 'bg-rose-50 text-rose-700 border-rose-100',
        icon:
          'M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636'
      },
      DEPARTED: {
        label: 'Parti',
        classes: 'bg-slate-100 text-slate-700 border-slate-200',
        icon:
          'M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1'
      }
    };
    return configs[status] || { label: status, classes: 'bg-slate-100 text-slate-700', icon: '' };
  }

  getDegreeLabel(degree: string | undefined | null): string {
    if (!degree) return 'Non renseigné';
    const degrees: Record<string, string> = {
      BACHELOR: 'Licence (Bac+3)',
      FOUR_YEAR_BACHELOR: 'Maîtrise (Bac+4)',
      MASTER: 'Master (Bac+5)',
      ENGINEERING_DEGREE: "Diplôme d'Ingénieur",
      DOCTORATE: 'Doctorat'
    };
    return degrees[degree] || degree;
  }

  getContractLabel(contract: string | undefined | null): string {
    if (!contract) return 'Non renseigné';
    const contracts: Record<string, string> = {
      CDI: 'CDI',
      CDD: 'CDD',
      FREELANCE: 'Freelance / Vacataire',
      GUEST: 'Invité'
    };
    return contracts[contract] || contract;
  }
}
