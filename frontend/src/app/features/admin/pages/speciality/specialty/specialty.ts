import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastService } from '../../../../../shared/services/toast.service';
// Adaptez le chemin de votre service d'API selon votre projet
import { SpecialtyControllerService } from '../../../../../core/api';

export interface SpecialtyDto {
  id: number;
  code: string;
  label: string;
}

@Component({
  selector: 'app-specialty',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './specialty.html',
  styleUrl: './specialty.css'
})
export class Specialty implements OnInit {
  private specialtyService = inject(SpecialtyControllerService);
  private toastService = inject(ToastService);
  private fb = inject(FormBuilder);

  specialties = signal<SpecialtyDto[]>([]);
  isLoading = signal<boolean>(true);
  isModalOpen = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);
  editingSpecialtyId = signal<number | null>(null);

  specialtyForm: FormGroup = this.fb.group({
    code: ['', [Validators.required]],
    label: ['', [Validators.required]]
  });

  ngOnInit(): void {
    this.loadSpecialties();
  }

  loadSpecialties(): void {
    this.isLoading.set(true);
    this.specialtyService.getAllSpecialties().subscribe({
      next: (response: any) => {
        if (response.success) {
          this.specialties.set(response.data);
        } else {
          this.toastService.error(response.message || 'Erreur lors du chargement.');
        }
        this.isLoading.set(false);
      },
      error: err => {
        this.toastService.error('Erreur de connexion au serveur');
        this.isLoading.set(false);
      }
    });
  }

  // OPEN MODAL: ADD
  openAddModal(): void {
    this.editingSpecialtyId.set(null);
    this.specialtyForm.reset();
    this.specialtyForm.markAsUntouched();
    this.isModalOpen.set(true);
  }

  // OPEN MODAL: EDIT
  openEditModal(specialty: SpecialtyDto): void {
    this.editingSpecialtyId.set(specialty.id);
    this.specialtyForm.patchValue({
      code: specialty.code,
      label: specialty.label
    });
    this.specialtyForm.markAsUntouched();
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
    this.editingSpecialtyId.set(null);
  }

  onSubmit(): void {
    if (this.specialtyForm.invalid) {
      this.specialtyForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    const request = this.specialtyForm.value;
    const currentId = this.editingSpecialtyId();

    if (currentId !== null) {
      // UPDATE
      this.specialtyService.updateSpecialty(currentId, request).subscribe({
        next: (res: any) => {
          if (res.success) {
            this.toastService.success('Spécialité modifiée avec succès');
            this.closeModal();
            this.loadSpecialties();
          } else {
            this.toastService.error(res.message || 'Erreur lors de la modification');
          }
          this.isSubmitting.set(false);
        },
        error: err => {
          this.toastService.error('Erreur lors de la modification');
          this.isSubmitting.set(false);
        }
      });
    } else {
      // CREATE
      this.specialtyService.createSpecialty(request).subscribe({
        next: (res: any) => {
          if (res.success) {
            this.toastService.success('Spécialité ajoutée avec succès');
            this.closeModal();
            this.loadSpecialties();
          } else {
            this.toastService.error(res.message || 'Erreur lors de la création');
          }
          this.isSubmitting.set(false);
        },
        error: err => {
          this.toastService.error('Erreur lors de la création');
          this.isSubmitting.set(false);
        }
      });
    }
  }
}
