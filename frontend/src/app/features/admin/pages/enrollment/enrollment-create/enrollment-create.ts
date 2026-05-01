import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgClass } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';

import {
  LevelControllerService,
  TrainingControllerService,
  PromotionControllerService,
  EnrollmentDocumentControllerService,
  EnrollmentControllerService,
  EnrollmentDocumentResponse
} from '../../../../../core/api';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-enrollment-create',
  standalone: true,
  imports: [ReactiveFormsModule, NgClass],
  templateUrl: './enrollment-create.html'
})
export class EnrollmentCreate implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);

  // Services API
  private readonly levelService = inject(LevelControllerService);
  private readonly trainingService = inject(TrainingControllerService);
  private readonly promotionService = inject(PromotionControllerService);
  private readonly documentService = inject(EnrollmentDocumentControllerService);
  private readonly enrollmentService = inject(EnrollmentControllerService);

  // --- Signals (State) ---
  levels = signal<any[]>([]);
  trainings = signal<any[]>([]);
  promotions = signal<any[]>([]);
  documents = signal<EnrollmentDocumentResponse[]>([]);

  isLoadingLevels = signal<boolean>(false);
  isLoadingTrainings = signal<boolean>(false);
  isLoadingPromotions = signal<boolean>(false);
  isLoadingDocuments = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);

  // Signaux UI
  isTrainingDropdownOpen = signal<boolean>(false);
  selectedTrainingId = signal<number | string | null>(null);
  providedDocsCount = signal<number>(0);

  // Toggles de sections
  hasSiblings = signal<boolean>(true);
  isParentsSectionEnabled = signal<boolean>(true);
  isFatherEnabled = signal<boolean>(true);
  isMotherEnabled = signal<boolean>(true);

  // --- Formulaire Complet ---
  enrollmentForm: FormGroup = this.fb.group({
    type: ['NEW'],
    observation: [''],

    // Affectation
    levelId: ['', Validators.required],
    trainingId: [{ value: '', disabled: true }, Validators.required],
    promotionId: [{ value: '', disabled: true }, Validators.required],

    // Student & Family Information
    studentInfo: this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      firstNameAr: [''],
      lastNameAr: [''],
      email: ['', [Validators.email]],
      phone: ['', [Validators.pattern('^[0-9]{8}$')]],
      cin: [''],
      birthDate: ['', Validators.required],
      birthPlace: [''],
      governorate: [''],
      delegation: [''],
      residenceAddress: [''],
      correspondenceAddress: [''],
      residence: ['WITH_BOTH_PARENTS', Validators.required],
      parentsSituation: ['LIVING_TOGETHER'],
      hasChronicDisease: [false],
      diseaseDescription: [''],

      studentSiblings: this.fb.array([]),

      legalGuardianRole: ['FATHER'],

      father: this.fb.group({
        firstName: ['', Validators.required],
        lastName: ['', Validators.required],
        firstNameAr: [''],
        lastNameAr: [''],
        cin: [''],
        phone: [''],
        email: [''],
        profession: ['']
      }),

      mother: this.fb.group({
        firstName: ['', Validators.required],
        lastName: ['', Validators.required],
        firstNameAr: [''],
        lastNameAr: [''],
        cin: [''],
        phone: [''],
        email: [''],
        profession: ['']
      }),

      otherGuardian: this.fb.group({
        relation: [''],
        firstName: ['', Validators.required],
        lastName: ['', Validators.required],
        firstNameAr: [''],
        lastNameAr: [''],
        cin: [''],
        phone: [''],
        email: [''],
        profession: ['']
      })
    }),

    // Tableau des documents
    enrollmentSubmittedDocuments: this.fb.array([])
  });

  get documentsArray(): FormArray {
    return this.enrollmentForm.get('enrollmentSubmittedDocuments') as FormArray;
  }
  get studentSiblingsArray(): FormArray {
    return this.enrollmentForm.get('studentInfo.studentSiblings') as FormArray;
  }

  // --- Computed ---
  selectedTraining = computed(() => {
    const id = this.selectedTrainingId();
    if (!id) return null;
    return (
      this.trainings().find(t => {
        const currentId = t.id || t.trainingId;
        return currentId?.toString() === id.toString();
      }) || null
    );
  });

  readonly trainingTypeConfig: Record<string, { label: string; classes: string }> = {
    CONTINUOUS: {
      label: 'Formation Continue',
      classes:
        'inline-flex items-center rounded-md bg-blue-400/10 px-2 py-1 text-xs font-medium text-blue-400 inset-ring inset-ring-blue-400/30'
    },
    ACCELERATED: {
      label: 'Accélérée',
      classes:
        'inline-flex items-center rounded-md bg-yellow-400/10 px-2 py-1 text-xs font-medium text-yellow-500 inset-ring inset-ring-yellow-400/20'
    },
    ACCREDITED: {
      label: 'Homologuée',
      classes:
        'inline-flex items-center rounded-md bg-green-400/10 px-2 py-1 text-xs font-medium text-green-400 inset-ring inset-ring-green-500/20'
    }
  };

  constructor() {
    // Disable 'otherGuardian' by default to avoid blocking the form
    this.enrollmentForm.get('studentInfo.otherGuardian')?.disable();

    this.enrollmentForm
      .get('levelId')
      ?.valueChanges.pipe(takeUntilDestroyed())
      .subscribe(levelId => {
        if (levelId) {
          this.loadTrainings(levelId);
          this.loadDocuments(levelId);
        } else {
          this.resetTrainings();
          this.resetDocuments();
        }
      });

    this.enrollmentForm
      .get('trainingId')
      ?.valueChanges.pipe(takeUntilDestroyed())
      .subscribe(trainingId => {
        if (trainingId) this.loadPromotions(trainingId);
        else this.resetPromotions();
      });

    // Enable/Disable the disease field
    this.enrollmentForm
      .get('studentInfo.hasChronicDisease')
      ?.valueChanges.pipe(takeUntilDestroyed())
      .subscribe(hasDisease => {
        const descControl = this.enrollmentForm.get('studentInfo.diseaseDescription');
        if (hasDisease) descControl?.setValidators([Validators.required]);
        else {
          descControl?.clearValidators();
          descControl?.setValue('');
        }
        descControl?.updateValueAndValidity();
      });

    // Enable/Disable Other Guardian
    this.enrollmentForm
      .get('studentInfo.legalGuardianRole')
      ?.valueChanges.pipe(takeUntilDestroyed())
      .subscribe(role => {
        const otherGroup = this.enrollmentForm.get('studentInfo.otherGuardian');
        if (role === 'OTHER' && this.isParentsSectionEnabled()) otherGroup?.enable();
        else otherGroup?.disable();
      });

    this.enrollmentForm
      .get('enrollmentSubmittedDocuments')
      ?.valueChanges.pipe(takeUntilDestroyed())
      .subscribe(docs => {
        this.providedDocsCount.set(docs.filter((d: any) => d.provided).length);
      });
  }

  ngOnInit(): void {
    this.loadLevels();
    this.addSibling();
  }

  // --- UI Toggles WITH VALIDATORS MANAGEMENT ---
  toggleSiblings(event: Event): void {
    const isChecked = (event.target as HTMLInputElement).checked;
    this.hasSiblings.set(isChecked);
    if (!isChecked) this.studentSiblingsArray.clear();
    else if (this.studentSiblingsArray.length === 0) this.addSibling();
  }

  toggleParentsSection(event: Event): void {
    const isChecked = (event.target as HTMLInputElement).checked;
    this.isParentsSectionEnabled.set(isChecked);
    const father = this.enrollmentForm.get('studentInfo.father');
    const mother = this.enrollmentForm.get('studentInfo.mother');
    const other = this.enrollmentForm.get('studentInfo.otherGuardian');

    if (!isChecked) {
      father?.disable();
      mother?.disable();
      other?.disable();
    } else {
      if (this.isFatherEnabled()) father?.enable();
      if (this.isMotherEnabled()) mother?.enable();
      if (this.enrollmentForm.get('studentInfo.legalGuardianRole')?.value === 'OTHER')
        other?.enable();
    }
  }

  toggleFather(event: Event): void {
    const isChecked = (event.target as HTMLInputElement).checked;
    this.isFatherEnabled.set(isChecked);
    if (isChecked && this.isParentsSectionEnabled())
      this.enrollmentForm.get('studentInfo.father')?.enable();
    else this.enrollmentForm.get('studentInfo.father')?.disable();
  }

  toggleMother(event: Event): void {
    const isChecked = (event.target as HTMLInputElement).checked;
    this.isMotherEnabled.set(isChecked);
    if (isChecked && this.isParentsSectionEnabled())
      this.enrollmentForm.get('studentInfo.mother')?.enable();
    else this.enrollmentForm.get('studentInfo.mother')?.disable();
  }

  addSibling(): void {
    this.studentSiblingsArray.push(
      this.fb.group({
        fullName: ['', Validators.required],
        age: ['', [Validators.required, Validators.min(0)]],
        schoolOrWorkplace: ['']
      })
    );
  }
  removeSibling(index: number): void {
    this.studentSiblingsArray.removeAt(index);
  }

  // --- API Loading Calls ---
  private loadLevels(): void {
    this.isLoadingLevels.set(true);
    this.levelService.getAllLevels().subscribe({
      next: (res: any) => this.levels.set(res.data || res),
      error: () => this.toastService.error('Erreur niveaux'),
      complete: () => this.isLoadingLevels.set(false)
    });
  }

  private loadTrainings(levelId: number): void {
    this.resetTrainings();
    this.isLoadingTrainings.set(true);
    this.enrollmentForm.get('trainingId')?.enable();
    this.trainingService.getActiveTrainingsByLevel(levelId).subscribe({
      next: (res: any) => this.trainings.set(res.data || res),
      error: () => this.toastService.error('Erreur spécialités'),
      complete: () => this.isLoadingTrainings.set(false)
    });
  }

  private loadPromotions(trainingId: number): void {
    this.resetPromotions();
    this.isLoadingPromotions.set(true);
    this.enrollmentForm.get('promotionId')?.enable();
    this.promotionService.getOpenPromotionsLookup(trainingId).subscribe({
      next: (res: any) => this.promotions.set(res.data || res),
      error: () => this.toastService.error('Erreur promotions'),
      complete: () => this.isLoadingPromotions.set(false)
    });
  }

  private loadDocuments(levelId: number): void {
    this.resetDocuments();
    this.isLoadingDocuments.set(true);
    this.documentService.getDocumentsByLevel(levelId).subscribe({
      next: (res: any) => {
        const docs = res.data || res;
        this.documents.set(docs);
        docs.forEach((doc: any) => {
          this.documentsArray.push(
            this.fb.group({ enrollmentDocumentId: [doc.id], provided: [false] })
          );
        });
      },
      error: () => this.toastService.error('Erreur documents'),
      complete: () => this.isLoadingDocuments.set(false)
    });
  }

  private resetTrainings(): void {
    this.trainings.set([]);
    this.selectedTrainingId.set(null);
    this.enrollmentForm.get('trainingId')?.setValue('');
    this.enrollmentForm.get('trainingId')?.disable();
    this.resetPromotions();
  }
  private resetPromotions(): void {
    this.promotions.set([]);
    this.enrollmentForm.get('promotionId')?.setValue('');
    this.enrollmentForm.get('promotionId')?.disable();
  }
  private resetDocuments(): void {
    this.documents.set([]);
    this.documentsArray.clear();
    this.providedDocsCount.set(0);
  }

  getTrainingTypeConfig(type?: string) {
    if (!type) return null;
    return (
      this.trainingTypeConfig[type] || {
        label: type,
        classes:
          'inline-flex items-center rounded-md bg-gray-400/10 px-2 py-1 text-xs font-medium text-gray-400 inset-ring inset-ring-gray-400/20'
      }
    );
  }

  toggleTrainingDropdown(): void {
    if (!this.enrollmentForm.get('trainingId')?.disabled)
      this.isTrainingDropdownOpen.update(v => !v);
  }

  selectTraining(trainingId: number | string | undefined): void {
    if (trainingId) {
      this.selectedTrainingId.set(trainingId);
      this.enrollmentForm.get('trainingId')?.setValue(trainingId);
      this.isTrainingDropdownOpen.set(false);
    }
  }

  // --- Cleaning Helper ---
  private cleanPayload(obj: any): any {
    if (obj === null || obj === undefined || obj === '') return null;
    if (Array.isArray(obj)) return obj.map(item => this.cleanPayload(item)).filter(i => i !== null);
    if (typeof obj === 'object') {
      const cleanedObj: any = {};
      let hasValidData = false;
      for (const [key, value] of Object.entries(obj)) {
        const cleanedValue = this.cleanPayload(value);
        cleanedObj[key] = cleanedValue;
        if (cleanedValue !== null) hasValidData = true;
      }
      return hasValidData ? cleanedObj : null;
    }
    return obj;
  }

  // --- SUBMISSION ---
  onSubmit(): void {
    if (this.enrollmentForm.invalid) {
      // Force red display for all empty fields!
      this.enrollmentForm.markAllAsTouched();
      this.toastService.error('Veuillez remplir les champs obligatoires.');
      return;
    }

    this.isSubmitting.set(true);

    const rawPayload = this.enrollmentForm.getRawValue();
    delete rawPayload.levelId;
    delete rawPayload.trainingId;

    const parentsList = [];
    const studentInfo = rawPayload.studentInfo;
    const guardianRole = studentInfo.legalGuardianRole;

    if (this.isParentsSectionEnabled()) {
      if (this.isFatherEnabled() && studentInfo.father) {
        const cleanedFather = this.cleanPayload(studentInfo.father);
        if (cleanedFather)
          parentsList.push({
            link: 'FATHER',
            isLegalGuardian: guardianRole === 'FATHER',
            parent: cleanedFather
          });
      }
      if (this.isMotherEnabled() && studentInfo.mother) {
        const cleanedMother = this.cleanPayload(studentInfo.mother);
        if (cleanedMother)
          parentsList.push({
            link: 'MOTHER',
            isLegalGuardian: guardianRole === 'MOTHER',
            parent: cleanedMother
          });
      }
      if (guardianRole === 'OTHER' && studentInfo.otherGuardian) {
        const cleanedOther = this.cleanPayload(studentInfo.otherGuardian);
        if (cleanedOther) {
          delete cleanedOther.relation;
          parentsList.push({ link: 'OTHER', isLegalGuardian: true, parent: cleanedOther });
        }
      }
    }

    rawPayload.studentInfo.parents = parentsList;
    delete rawPayload.studentInfo.father;
    delete rawPayload.studentInfo.mother;
    delete rawPayload.studentInfo.otherGuardian;
    delete rawPayload.studentInfo.legalGuardianRole;

    const finalPayload = this.cleanPayload(rawPayload);
    console.log('Payload Final Envoyé :', finalPayload);

    this.enrollmentService.createEnrollment(finalPayload).subscribe({
      next: () => {
        this.toastService.success('Inscription créée avec succès !');
        this.router.navigate(['/admin/enrollments']);
      },
      error: err => {
        console.error(err);
        this.toastService.error('Une erreur est survenue.');
        this.isSubmitting.set(false);
      },
      complete: () => this.isSubmitting.set(false)
    });
  }
}
