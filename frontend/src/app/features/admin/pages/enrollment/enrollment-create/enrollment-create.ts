import { Component, computed, inject, OnInit, signal } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { DatePipe, NgClass } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { forkJoin, Observable } from 'rxjs';

import {
  LevelControllerService,
  TrainingControllerService,
  PromotionControllerService,
  EnrollmentDocumentControllerService,
  EnrollmentControllerService,
  EnrollmentDocumentResponse,
  StudentControllerService,
  StudentResponse,
  ParentControllerService,
  ParentResponse
} from '../../../../../core/api';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-enrollment-create',
  standalone: true,
  imports: [ReactiveFormsModule, NgClass, DatePipe],
  templateUrl: './enrollment-create.html',
  styleUrls: ['./enrollment-create.scss']
})
export class EnrollmentCreate implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);

  private readonly levelService = inject(LevelControllerService);
  private readonly trainingService = inject(TrainingControllerService);
  private readonly promotionService = inject(PromotionControllerService);
  private readonly documentService = inject(EnrollmentDocumentControllerService);
  private readonly enrollmentService = inject(EnrollmentControllerService);
  private readonly studentService = inject(StudentControllerService);
  private readonly parentService = inject(ParentControllerService);

  // --- DYNAMIC WIZARD ---
  enrollmentMode = signal<'NEW' | 'EXISTING'>('NEW');
  currentStepIndex = signal<number>(0);

  wizardSteps = computed(() => {
    if (this.enrollmentMode() === 'EXISTING') {
      return [
        { key: 'SEARCH', title: 'Recherche', desc: "Vérification de l'apprenti " },
        { key: 'AFFECTATION', title: 'Affectation', desc: 'Cursus & classe' },
        { key: 'DOCUMENTS', title: 'Récapitulatif', desc: 'Validation & Envoi' }
      ];
    }
    return [
      { key: 'SEARCH', title: 'Recherche', desc: "Vérification de l'apprenti" },
      { key: 'PROFILE', title: 'Profil Apprenti', desc: 'Identité & contact' },
      { key: 'AFFECTATION', title: 'Affectation', desc: 'Cursus & classe' },
      { key: 'FAMILY', title: 'Famille', desc: 'Parents & fratrie' },
      { key: 'DOCUMENTS', title: 'Récapitulatif', desc: 'Validation finale' }
    ];
  });

  activeStep = computed(() => this.wizardSteps()[this.currentStepIndex()]);
  progressPercentage = computed(
    () => (this.currentStepIndex() / (this.wizardSteps().length - 1)) * 100
  );

  // --- STUDENT SEARCH ---
  searchCtrl = new FormControl('');
  searchResults = signal<StudentResponse[]>([]);
  isSearching = signal<boolean>(false);
  hasSearched = signal<boolean>(false);
  selectedStudent = signal<StudentResponse | null>(null);

  // --- PARENT SEARCH ---
  fatherMode = signal<'SEARCH' | 'NEW' | 'SELECTED'>('SEARCH');
  searchFatherCtrl = new FormControl('');
  fatherSearchResults = signal<ParentResponse[]>([]);
  isSearchingFather = signal<boolean>(false);

  motherMode = signal<'SEARCH' | 'NEW' | 'SELECTED'>('SEARCH');
  searchMotherCtrl = new FormControl('');
  motherSearchResults = signal<ParentResponse[]>([]);
  isSearchingMother = signal<boolean>(false);

  otherMode = signal<'SEARCH' | 'NEW' | 'SELECTED'>('SEARCH');
  searchOtherCtrl = new FormControl('');
  otherSearchResults = signal<ParentResponse[]>([]);
  isSearchingOther = signal<boolean>(false);

  // --- API STATE ---
  levels = signal<any[]>([]);
  specialities = signal<any[]>([]);
  promotions = signal<any[]>([]);
  documents = signal<EnrollmentDocumentResponse[]>([]);

  isLoadingLevels = signal<boolean>(false);
  isLoadingTrainings = signal<boolean>(false);
  isLoadingPromotions = signal<boolean>(false);
  isLoadingDocuments = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);

  isValidatingProfile = signal<boolean>(false);
  isValidatingFamily = signal<boolean>(false);
  isValidatingAffectation = signal<boolean>(false);
  showConfirmationModal = signal<boolean>(false);

  // --- UI STATE ---
  providedDocsCount = signal<number>(0);
  hasSiblings = signal<boolean>(true);
  isParentsSectionEnabled = signal<boolean>(true);
  isFatherEnabled = signal<boolean>(true);
  isMotherEnabled = signal<boolean>(true);

  // --- FORM ---
  enrollmentForm: FormGroup = this.fb.group({
    type: ['NEW'],
    existingStudentId: [null],
    observation: [''],

    levelId: ['', Validators.required],
    trainingId: [{ value: '', disabled: true }, Validators.required],
    promotionId: [{ value: '', disabled: true }, Validators.required],

    studentInfo: this.fb.group({
      id: [null],
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      firstNameAr: [''],
      lastNameAr: [''],
      email: ['', [Validators.email]],
      phone: ['', [Validators.pattern('^[0-9]{8}$')]],
      cin: [''],
      gender: ['', Validators.required],
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
        id: [null],
        firstName: ['', Validators.required],
        lastName: ['', Validators.required],
        firstNameAr: [''],
        lastNameAr: [''],
        phone: [''],
        email: ['', Validators.email],
        profession: ['']
      }),
      mother: this.fb.group({
        id: [null],
        firstName: ['', Validators.required],
        lastName: ['', Validators.required],
        firstNameAr: [''],
        lastNameAr: [''],
        phone: [''],
        email: ['', Validators.email],
        profession: ['']
      }),
      otherGuardian: this.fb.group({
        id: [null],
        relation: ['', Validators.required],
        firstName: ['', Validators.required],
        lastName: ['', Validators.required],
        firstNameAr: [''],
        lastNameAr: [''],
        phone: [''],
        email: ['', Validators.email],
        profession: ['']
      })
    }),

    enrollmentSubmittedDocuments: this.fb.array([])
  });

  get documentsArray(): FormArray {
    return this.enrollmentForm.get('enrollmentSubmittedDocuments') as FormArray;
  }

  get studentSiblingsArray(): FormArray {
    return this.enrollmentForm.get('studentInfo.studentSiblings') as FormArray;
  }

  get fatherForm(): FormGroup {
    return this.enrollmentForm.get('studentInfo.father') as FormGroup;
  }

  get motherForm(): FormGroup {
    return this.enrollmentForm.get('studentInfo.mother') as FormGroup;
  }

  get otherForm(): FormGroup {
    return this.enrollmentForm.get('studentInfo.otherGuardian') as FormGroup;
  }

  constructor() {
    this.otherForm.disable();

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

    this.enrollmentForm
      .get('studentInfo.legalGuardianRole')
      ?.valueChanges.pipe(takeUntilDestroyed())
      .subscribe(role => {
        if (role === 'OTHER' && this.isParentsSectionEnabled()) this.otherForm.enable();
        else this.otherForm.disable();
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

  // --- STUDENT AND PARENT SEARCHES REMAIN UNCHANGED ---
  onSearch(): void {
    const keyword = this.searchCtrl.value?.trim();
    if (!keyword) return;
    this.isSearching.set(true);
    this.hasSearched.set(true);
    this.studentService.searchStudents(keyword).subscribe({
      next: (res: any) => this.searchResults.set(res.data || res),
      error: () => {
        this.toastService.error('Erreur de recherche.');
        this.isSearching.set(false);
      },
      complete: () => this.isSearching.set(false)
    });
  }

  selectExistingStudent(student: StudentResponse): void {
    this.selectedStudent.set(student);
    this.enrollmentMode.set('EXISTING');
    this.enrollmentForm.patchValue({ type: 'NEW', existingStudentId: student.id });
    this.currentStepIndex.set(1);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  createNewStudent(): void {
    this.selectedStudent.set(null);
    this.enrollmentMode.set('NEW');
    this.enrollmentForm.patchValue({ type: 'NEW', existingStudentId: null });
    this.currentStepIndex.set(1);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  onSearchFather(): void {
    const keyword = this.searchFatherCtrl.value?.trim();
    if (!keyword) return;
    this.isSearchingFather.set(true);
    this.parentService.searchParents(keyword).subscribe({
      next: (res: any) => this.fatherSearchResults.set(res.data || res),
      error: () => {
        this.toastService.error('Erreur recherche.');
        this.isSearchingFather.set(false);
      },
      complete: () => this.isSearchingFather.set(false)
    });
  }

  selectFather(parent: ParentResponse): void {
    this.fatherForm.patchValue(parent);
    this.fatherMode.set('SELECTED');
  }

  createNewFather(): void {
    this.fatherForm.reset();
    this.fatherMode.set('NEW');
  }

  resetFatherSearch(): void {
    this.fatherForm.reset();
    this.searchFatherCtrl.setValue('');
    this.fatherSearchResults.set([]);
    this.fatherMode.set('SEARCH');
  }

  onSearchMother(): void {
    const keyword = this.searchMotherCtrl.value?.trim();
    if (!keyword) return;
    this.isSearchingMother.set(true);
    this.parentService.searchParents(keyword).subscribe({
      next: (res: any) => this.motherSearchResults.set(res.data || res),
      error: () => {
        this.toastService.error('Erreur recherche.');
        this.isSearchingMother.set(false);
      },
      complete: () => this.isSearchingMother.set(false)
    });
  }

  selectMother(parent: ParentResponse): void {
    this.motherForm.patchValue(parent);
    this.motherMode.set('SELECTED');
  }

  createNewMother(): void {
    this.motherForm.reset();
    this.motherMode.set('NEW');
  }

  resetMotherSearch(): void {
    this.motherForm.reset();
    this.searchMotherCtrl.setValue('');
    this.motherSearchResults.set([]);
    this.motherMode.set('SEARCH');
  }

  onSearchOther(): void {
    const keyword = this.searchOtherCtrl.value?.trim();
    if (!keyword) return;
    this.isSearchingOther.set(true);
    this.parentService.searchParents(keyword).subscribe({
      next: (res: any) => this.otherSearchResults.set(res.data || res),
      error: () => {
        this.toastService.error('Erreur recherche.');
        this.isSearchingOther.set(false);
      },
      complete: () => this.isSearchingOther.set(false)
    });
  }

  selectOther(parent: ParentResponse): void {
    this.otherForm.patchValue(parent);
    this.otherMode.set('SELECTED');
  }

  createNewOther(): void {
    this.otherForm.reset();
    this.otherMode.set('NEW');
  }

  resetOtherSearch(): void {
    this.otherForm.reset();
    this.searchOtherCtrl.setValue('');
    this.otherSearchResults.set([]);
    this.otherMode.set('SEARCH');
  }

  // --- WIZARD NAVIGATION & VALIDATION ---
  nextStep(): void {
    if (!this.isCurrentStepValid()) {
      this.markCurrentStepAsTouched();
      this.toastService.error('Veuillez remplir correctement les champs requis.');
      return;
    }

    if (this.activeStep().key === 'PROFILE') {
      this.checkProfileExistenceAndProceed();
    } else if (this.activeStep().key === 'FAMILY' && this.enrollmentMode() === 'NEW') {
      if (this.checkPhoneDuplicates()) return;
      this.checkFamilyExistenceAndProceed();
    } else if (this.activeStep().key === 'AFFECTATION' && this.enrollmentMode() === 'EXISTING') {
      this.checkAffectationExistenceAndProceed();
    } else {
      this.proceedToNextStep();
    }
  }

  private proceedToNextStep(): void {
    if (this.currentStepIndex() < this.wizardSteps().length - 1) {
      this.currentStepIndex.update(i => i + 1);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  previousStep(): void {
    if (this.currentStepIndex() > 0) {
      this.currentStepIndex.update(i => i - 1);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  setStep(index: number): void {
    if (index < this.currentStepIndex()) this.currentStepIndex.set(index);
    else if (index === this.currentStepIndex() + 1 && this.isCurrentStepValid()) this.nextStep();
  }

  private checkPhoneDuplicates(): boolean {
    const studentPhone = this.enrollmentForm.get('studentInfo.phone')?.value?.trim();
    const phones: { owner: string; phone: string }[] = [];

    if (studentPhone) phones.push({ owner: "l'Apprenti", phone: studentPhone });

    if (this.isParentsSectionEnabled()) {
      if (this.isFatherEnabled()) {
        const fPhone = this.fatherForm.get('phone')?.value?.trim();
        if (fPhone) phones.push({ owner: 'le Père', phone: fPhone });
      }
      if (this.isMotherEnabled()) {
        const mPhone = this.motherForm.get('phone')?.value?.trim();
        if (mPhone) phones.push({ owner: 'la Mère', phone: mPhone });
      }
      if (this.enrollmentForm.get('studentInfo.legalGuardianRole')?.value === 'OTHER') {
        const oPhone = this.otherForm.get('phone')?.value?.trim();
        if (oPhone) phones.push({ owner: 'Autre Tuteur', phone: oPhone });
      }
    }

    const seen = new Map<string, string>();
    for (const p of phones) {
      if (seen.has(p.phone)) {
        const conflictingOwner = seen.get(p.phone);
        this.toastService.error(
          `Erreur : Le numéro ${p.phone} est attribué à ${conflictingOwner} ET à ${p.owner}. Les numéros doivent être différents.`
        );
        return true;
      }
      seen.set(p.phone, p.owner);
    }
    return false;
  }

  private checkProfileExistenceAndProceed(): void {
    const studentInfo = this.enrollmentForm.get('studentInfo')?.value;
    const cin = studentInfo.cin?.trim() || undefined;
    const email = studentInfo.email?.trim() || undefined;
    const phone = studentInfo.phone?.trim() || undefined;

    if (!cin && !email && !phone) {
      this.proceedToNextStep();
      return;
    }

    this.isValidatingProfile.set(true);
    this.studentService.checkExistence(cin, email, phone).subscribe({
      next: (res: any) => {
        const data = res.data;
        const hasConflict =
          res.message === 'CONFLICTS_FOUND' ||
          data.cinExists ||
          data.emailExists ||
          data.phoneExists;
        if (hasConflict) {
          let errors = [];
          if (data.cinExists) errors.push('le CIN');
          if (data.emailExists) errors.push("l'Email");
          if (data.phoneExists) errors.push('le Téléphone');
          this.toastService.error(
            errors.length > 0
              ? `Création bloquée : ${errors.join(', ')} est déjà utilisé.`
              : `Ce profil existe déjà.`
          );
          this.isValidatingProfile.set(false);
        } else {
          this.isValidatingProfile.set(false);
          this.proceedToNextStep();
        }
      },
      error: () => {
        this.toastService.error('Impossible de vérifier les doublons. Réessayez.');
        this.isValidatingProfile.set(false);
      }
    });
  }

  private checkFamilyExistenceAndProceed(): void {
    const checks: Observable<any>[] = [];
    const checkLabels: string[] = [];

    if (this.isParentsSectionEnabled()) {
      if (this.isFatherEnabled() && this.fatherMode() === 'NEW') {
        const f = this.fatherForm.value;
        if (f.email || f.phone) {
          checks.push(
            this.parentService.checkParentExistence(f.email || undefined, f.phone || undefined)
          );
          checkLabels.push('Père');
        }
      }
      if (this.isMotherEnabled() && this.motherMode() === 'NEW') {
        const m = this.motherForm.value;
        if (m.email || m.phone) {
          checks.push(
            this.parentService.checkParentExistence(m.email || undefined, m.phone || undefined)
          );
          checkLabels.push('Mère');
        }
      }
      if (
        this.enrollmentForm.get('studentInfo.legalGuardianRole')?.value === 'OTHER' &&
        this.otherMode() === 'NEW'
      ) {
        const o = this.otherForm.value;
        if (o.email || o.phone) {
          checks.push(
            this.parentService.checkParentExistence(o.email || undefined, o.phone || undefined)
          );
          checkLabels.push('Autre Tuteur');
        }
      }
    }

    if (checks.length === 0) {
      this.proceedToNextStep();
      return;
    }

    this.isValidatingFamily.set(true);

    forkJoin(checks).subscribe({
      next: (results: any[]) => {
        let hasConflict = false;
        let errorMessages: string[] = [];

        results.forEach((res, index) => {
          const data = res.data;
          if (res.message === 'CONFLICTS_FOUND' || data.emailExists || data.phoneExists) {
            hasConflict = true;
            let fields = [];
            if (data.emailExists) fields.push("l'Email");
            if (data.phoneExists) fields.push('le Téléphone');
            errorMessages.push(`${checkLabels[index]} (${fields.join(' et ')})`);
          }
        });

        if (hasConflict) {
          this.toastService.error(
            `Conflit détecté : ${errorMessages.join(
              ', '
            )} existent déjà. Veuillez vérifier vos saisies.`
          );
          this.isValidatingFamily.set(false);
        } else {
          this.isValidatingFamily.set(false);
          this.proceedToNextStep();
        }
      },
      error: () => {
        this.toastService.error('Erreur lors de la vérification des parents.');
        this.isValidatingFamily.set(false);
      }
    });
  }

  private checkAffectationExistenceAndProceed(): void {
    const studentId = this.enrollmentForm.get('existingStudentId')?.value;
    const promotionId = this.enrollmentForm.get('promotionId')?.value;

    if (!studentId || !promotionId) {
      this.proceedToNextStep();
      return;
    }

    this.isValidatingAffectation.set(true);

    this.enrollmentService.checkStudentEnrollmentExistence(studentId, promotionId).subscribe({
      next: (res: any) => {
        const isAlreadyEnrolled = res.data;
        if (isAlreadyEnrolled) {
          this.toastService.error('Cet apprenti est déjà inscrit dans cette promotion.');
          this.isValidatingAffectation.set(false);
        } else {
          this.isValidatingAffectation.set(false);
          this.proceedToNextStep();
        }
      },
      error: () => {
        this.toastService.error("Erreur lors de la vérification de l'inscription.");
        this.isValidatingAffectation.set(false);
      }
    });
  }

  private isCurrentStepValid(): boolean {
    const key = this.activeStep().key;
    if (key === 'SEARCH') return this.enrollmentMode() === 'NEW' || this.selectedStudent() !== null;
    if (key === 'PROFILE') {
      const g = this.enrollmentForm.get('studentInfo') as FormGroup;
      const baseFieldsValid = ['firstName', 'lastName', 'gender', 'birthDate', 'residence'].every(
        f => g.get(f)?.valid
      );
      const diseaseValid = !g.get('hasChronicDisease')?.value || g.get('diseaseDescription')?.valid;
      return baseFieldsValid && (diseaseValid ?? true);
    }
    if (key === 'AFFECTATION')
      return ['levelId', 'trainingId', 'promotionId'].every(f => this.enrollmentForm.get(f)?.valid);
    if (key === 'FAMILY') {
      const g = this.enrollmentForm.get('studentInfo') as FormGroup;
      const sibValid = !this.hasSiblings() || g.get('studentSiblings')?.valid;

      const fValid =
        !this.isFatherEnabled() || this.fatherMode() !== 'NEW' || this.fatherForm.valid;
      const mValid =
        !this.isMotherEnabled() || this.motherMode() !== 'NEW' || this.motherForm.valid;

      const role = g.get('legalGuardianRole')?.value;
      const oValid = role !== 'OTHER' || (this.otherMode() !== 'SEARCH' && this.otherForm.valid);

      const isFatherResolved = !this.isFatherEnabled() || this.fatherMode() !== 'SEARCH';
      const isMotherResolved = !this.isMotherEnabled() || this.motherMode() !== 'SEARCH';

      return (
        (sibValid ?? true) && fValid && mValid && oValid && isFatherResolved && isMotherResolved
      );
    }
    return true;
  }

  private markCurrentStepAsTouched(): void {
    const key = this.activeStep().key;
    if (key === 'PROFILE') {
      const g = this.enrollmentForm.get('studentInfo') as FormGroup;
      [
        'firstName',
        'lastName',
        'birthDate',
        'gender',
        'residence',
        'diseaseDescription'
      ].forEach(f => g.get(f)?.markAsTouched());
    } else if (key === 'AFFECTATION') {
      ['levelId', 'trainingId', 'promotionId'].forEach(f =>
        this.enrollmentForm.get(f)?.markAsTouched()
      );
    } else if (key === 'FAMILY') {
      const g = this.enrollmentForm.get('studentInfo') as FormGroup;
      if (this.hasSiblings()) g.get('studentSiblings')?.markAllAsTouched();
      if (this.isFatherEnabled() && this.fatherMode() === 'NEW') this.fatherForm.markAllAsTouched();
      if (this.isMotherEnabled() && this.motherMode() === 'NEW') this.motherForm.markAllAsTouched();
      if (g.get('legalGuardianRole')?.value === 'OTHER') this.otherForm.markAllAsTouched();
    }
  }

  isFormReadyToSubmit(): boolean {
    const isAffectationValid =
      this.enrollmentForm.get('levelId')?.valid &&
      this.enrollmentForm.get('trainingId')?.valid &&
      this.enrollmentForm.get('promotionId')?.valid;

    if (this.enrollmentMode() === 'EXISTING') return !!isAffectationValid;
    return this.enrollmentForm.valid;
  }

  // --- HELPERS FOR SUMMARY ---
  getLevelLabel(id: string | number): string {
    const level = this.levels().find(l => l.id == id);
    return level ? level.code || level.label : 'N/A';
  }

  getTrainingLabel(id: string | number): string {
    const spec = this.specialities().find(t => (t.id || t.trainingId) == id);
    return spec ? spec.specialtyLabel || spec.label || spec.name : 'N/A';
  }

  getPromoLabel(id: string | number): string {
    const promo = this.promotions().find(p => p.id == id);
    return promo ? promo.name : 'N/A';
  }

  getGuardianLabel(role: string): string {
    if (role === 'FATHER') return 'Le Père';
    if (role === 'MOTHER') return 'La Mère';
    return 'Autre';
  }

  // --- TOGGLES AND ADDITIONS ---
  toggleSiblings(event: Event): void {
    const isChecked = (event.target as HTMLInputElement).checked;
    this.hasSiblings.set(isChecked);
    if (!isChecked) this.studentSiblingsArray.clear();
    else if (this.studentSiblingsArray.length === 0) this.addSibling();
  }

  toggleParentsSection(event: Event): void {
    const isChecked = (event.target as HTMLInputElement).checked;
    this.isParentsSectionEnabled.set(isChecked);
    if (!isChecked) {
      this.fatherForm.disable();
      this.motherForm.disable();
      this.otherForm.disable();
    } else {
      if (this.isFatherEnabled()) this.fatherForm.enable();
      if (this.isMotherEnabled()) this.motherForm.enable();
      if (this.enrollmentForm.get('studentInfo.legalGuardianRole')?.value === 'OTHER')
        this.otherForm.enable();
    }
  }

  toggleFather(event: Event): void {
    const isChecked = (event.target as HTMLInputElement).checked;
    this.isFatherEnabled.set(isChecked);
    if (isChecked && this.isParentsSectionEnabled()) this.fatherForm.enable();
    else this.fatherForm.disable();
  }

  toggleMother(event: Event): void {
    const isChecked = (event.target as HTMLInputElement).checked;
    this.isMotherEnabled.set(isChecked);
    if (isChecked && this.isParentsSectionEnabled()) this.motherForm.enable();
    else this.motherForm.disable();
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

  // --- API CALLS ---
  private loadLevels(): void {
    this.levelService.getAllLevels().subscribe(res => this.levels.set((res as any).data || res));
  }

  private loadTrainings(levelId: number): void {
    this.specialities.set([]);
    this.enrollmentForm.get('trainingId')?.setValue('');
    this.enrollmentForm.get('trainingId')?.disable();
    this.promotions.set([]);
    this.enrollmentForm.get('promotionId')?.setValue('');
    this.enrollmentForm.get('promotionId')?.disable();
    this.enrollmentForm.get('trainingId')?.enable();
    this.trainingService
      .getActiveTrainingsByLevel(levelId)
      .subscribe(res => this.specialities.set((res as any).data || res));
  }

  private loadPromotions(trainingId: number): void {
    this.promotions.set([]);
    this.enrollmentForm.get('promotionId')?.setValue('');
    this.enrollmentForm.get('promotionId')?.disable();
    this.enrollmentForm.get('promotionId')?.enable();
    this.promotionService
      .getOpenPromotionsLookup(trainingId)
      .subscribe(res => this.promotions.set((res as any).data || res));
  }

  private loadDocuments(levelId: number): void {
    this.documents.set([]);
    this.documentsArray.clear();
    this.providedDocsCount.set(0);
    this.documentService.getDocumentsByLevel(levelId).subscribe((res: any) => {
      const docs = res.data || res;
      this.documents.set(docs);
      docs.forEach((doc: any) =>
        this.documentsArray.push(
          this.fb.group({ enrollmentDocumentId: [doc.id], provided: [false] })
        )
      );
    });
  }

  private resetTrainings(): void {
    this.specialities.set([]);
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
    if (!this.isFormReadyToSubmit()) {
      this.toastService.error('Veuillez remplir tous les champs obligatoires.');
      this.showConfirmationModal.set(false);
      return;
    }

    this.isSubmitting.set(true);
    const rawPayload = this.enrollmentForm.getRawValue();
    let finalPayload: any;

    if (this.enrollmentMode() === 'EXISTING') {
      finalPayload = this.cleanPayload({
        type: rawPayload.type,
        existingStudentId: rawPayload.existingStudentId,
        promotionId: rawPayload.promotionId,
        observation: rawPayload.observation,
        enrollmentSubmittedDocuments: rawPayload.enrollmentSubmittedDocuments
      });
    } else {
      const parentsList: any[] = [];
      const studentInfo = rawPayload.studentInfo;
      const guardianRole = studentInfo.legalGuardianRole;

      if (this.isParentsSectionEnabled()) {
        // Utility function respecting the XOR of StudentParentInfoRequest (Java)
        const processParent = (
          formValue: any,
          mode: string,
          roleLink: string,
          isLegal: boolean
        ) => {
          // 1. If an existing parent is selected
          if (mode === 'SELECTED' && formValue.id) {
            return {
              link: roleLink,
              isLegalGuardian: isLegal,
              existingParentId: formValue.id // WE SEND JUST THE ID
              // NO "parent" key, which validates the XOR
            };
          }

          // 2. If creating a new parent
          if (mode === 'NEW') {
            const cleaned = this.cleanPayload(formValue);
            if (cleaned) {
              delete cleaned.id; // Security: we remove any ID for a new parent
              return {
                link: roleLink,
                isLegalGuardian: isLegal,
                parent: cleaned // WE SEND THE FULL OBJECT
                // NO "existingParentId" key, which validates the XOR
              };
            }
          }
          return null;
        };

        // --- FATHER MANAGEMENT ---
        if (this.isFatherEnabled() && studentInfo.father) {
          const fatherPayload = processParent(
            studentInfo.father,
            this.fatherMode(),
            'FATHER',
            guardianRole === 'FATHER'
          );
          if (fatherPayload) parentsList.push(fatherPayload);
        }

        // --- MOTHER MANAGEMENT ---
        if (this.isMotherEnabled() && studentInfo.mother) {
          const motherPayload = processParent(
            studentInfo.mother,
            this.motherMode(),
            'MOTHER',
            guardianRole === 'MOTHER'
          );
          if (motherPayload) parentsList.push(motherPayload);
        }

        // --- OTHER GUARDIAN MANAGEMENT ---
        if (guardianRole === 'OTHER' && studentInfo.otherGuardian) {
          const linkRelation = studentInfo.otherGuardian.relation || 'OTHER';
          const otherPayload = processParent(
            studentInfo.otherGuardian,
            this.otherMode(),
            linkRelation,
            true
          );
          if (otherPayload) {
            if (otherPayload.parent) {
              delete otherPayload.parent.relation; // Cleanup
            }
            parentsList.push(otherPayload);
          }
        }
      }

      // Replacing parent forms with the formatted list
      studentInfo.parents = parentsList;
      delete studentInfo.father;
      delete studentInfo.mother;
      delete studentInfo.otherGuardian;
      delete studentInfo.legalGuardianRole;

      finalPayload = this.cleanPayload({
        type: rawPayload.type,
        promotionId: rawPayload.promotionId,
        observation: rawPayload.observation,
        studentInfo: studentInfo,
        enrollmentSubmittedDocuments: rawPayload.enrollmentSubmittedDocuments
      });
    }

    this.enrollmentService.createEnrollment(finalPayload).subscribe({
      next: () => {
        this.toastService.success('Inscription validée avec succès !');
        this.showConfirmationModal.set(false);
        this.router.navigate(['/admin/enrollments']);
      },
      error: () => {
        this.toastService.error('Une erreur est survenue.');
        this.isSubmitting.set(false);
        this.showConfirmationModal.set(false);
      },
      complete: () => {
        this.isSubmitting.set(false);
      }
    });
  }
}
