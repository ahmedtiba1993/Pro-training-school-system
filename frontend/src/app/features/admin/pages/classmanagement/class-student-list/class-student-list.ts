import {Component, OnInit, inject, signal, computed} from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {FormsModule} from '@angular/forms';
import {
  ClassAssignmentControllerService,
  ClassGroupControllerService,
  ClassGroupResponse,
  ClassStudentResponse,
  EnrollmentControllerService,
  UnassignedEnrollmentResponse,
  ClassAssignmentRequest,
  ClassGroupDetailResponse,
  EnrollmentDocumentControllerService,
  EnrollmentDocumentSimpleResponse
} from '../../../../../core/api';
import {ToastService} from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-class-student-list',
  standalone: true,
  imports: [RouterLink, FormsModule],
  templateUrl: './class-student-list.html',
  styleUrl: './class-student-list.css'
})
export class ClassStudentList implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly classAssignmentService = inject(ClassAssignmentControllerService);
  private readonly classGroupService = inject(ClassGroupControllerService);
  private readonly enrollmentService = inject(EnrollmentControllerService);
  private readonly enrollmentDocumentService = inject(EnrollmentDocumentControllerService);
  private readonly toastService = inject(ToastService);

  // =========================================================================
  // SIGNALS : MAIN STATE
  // =========================================================================
  readonly classGroupId = signal<number | null>(null);
  readonly classDetails = signal<ClassGroupResponse | null>(null);
  readonly classGroupDetail = signal<ClassGroupDetailResponse | null>(null);
  readonly students = signal<ClassStudentResponse[]>([]);
  readonly searchTerm = signal<string>('');
  readonly selectedStatusFilter = signal<string>('');

  readonly isLoading = signal<boolean>(true);
  readonly errorMessage = signal<string | null>(null);

  // =========================================================================
  // SIGNALS : GLOBAL ASSIGNMENT MODAL
  // =========================================================================
  readonly isAssignModalOpen = signal<boolean>(false);
  readonly modalSearchQuery = signal<string>('');
  readonly isModalLoading = signal<boolean>(false);
  readonly unassignedStudents = signal<UnassignedEnrollmentResponse[]>([]);

  readonly isAssignConfirmModalOpen = signal<boolean>(false);
  readonly selectedEnrollmentToAssign = signal<UnassignedEnrollmentResponse | null>(null);
  readonly isAssigning = signal<boolean>(false);

  // =========================================================================
  // SIGNALS : PDF EXPORT
  // =========================================================================
  readonly isExportingPdf = signal<boolean>(false);
  readonly isExportDocsModalOpen = signal<boolean>(false);
  readonly isDocsLoading = signal<boolean>(false);
  readonly isExportingDocsPdf = signal<boolean>(false);

  readonly availableDocuments = signal<EnrollmentDocumentSimpleResponse[]>([]);
  readonly selectedDocumentIds = signal<number[]>([]);

  // =========================================================================
  // COMPUTED SIGNALS
  // =========================================================================
  readonly filteredStudents = computed(() => {
    let result = this.students();
    const search = this.searchTerm()
      .toLowerCase()
      .trim();
    const statusFilter = this.selectedStatusFilter();

    if (search) {
      result = result.filter(
        s =>
          s.firstName?.toLowerCase().includes(search) ||
          s.lastName?.toLowerCase().includes(search) ||
          s.studentCode?.toLowerCase().includes(search)
      );
    }
    if (statusFilter) {
      result = result.filter(s => s.enrollmentStatus === statusFilter);
    }
    return result;
  });

  readonly filteredModalStudents = computed(() => {
    const query = this.modalSearchQuery()
      .toLowerCase()
      .trim();
    const list = this.unassignedStudents();
    if (!query) return list;

    return list.filter(
      s =>
        s.firstName?.toLowerCase().includes(query) ||
        s.lastName?.toLowerCase().includes(query) ||
        s.studentCode?.toLowerCase().includes(query) ||
        s.enrollmentNumber?.toLowerCase().includes(query)
    );
  });

  readonly totalStudentsCount = computed(() => this.students().length);
  readonly activeStudentsCount = computed(
    () =>
      this.students().filter(
        s => s.enrollmentStatus === 'VALIDATED' || s.enrollmentStatus === 'COMPLETED'
      ).length
  );
  readonly suspendedStudentsCount = computed(
    () => this.students().filter(s => s.enrollmentStatus === 'SUSPENDED').length
  );
  readonly fillPercentage = computed(
    () => (this.totalStudentsCount() / (this.classDetails()?.capacity ?? 30)) * 100
  );

  readonly isAllDocumentsSelected = computed(() => {
    const docs = this.availableDocuments();
    return docs.length > 0 && docs.length === this.selectedDocumentIds().length;
  });

  // =========================================================================
  // INIT & LOADING
  // =========================================================================
  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.classGroupId.set(Number(idParam));
      this.loadClassDetailsAndStudents(Number(idParam));
    } else {
      this.errorMessage.set("Identifiant de classe introuvable dans l'URL.");
      this.isLoading.set(false);
    }
  }

  loadClassDetailsAndStudents(classGroupId: number): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.classGroupService.getClassGroupDetails(classGroupId).subscribe({
      next: response => {
        if (response?.success && response.data) this.classGroupDetail.set(response.data);
      },
      error: err => console.error('API Error getClassGroupDetails:', err)
    });

    this.classGroupService.getAllClassGroups().subscribe({
      next: response => {
        if (response?.success && response.data) {
          const currentClass = response.data.find(c => c.id === classGroupId);
          if (currentClass) this.classDetails.set(currentClass);
        }
      }
    });

    this.classAssignmentService.getStudentsByClass(classGroupId).subscribe({
      next: response => {
        if (response?.success && response.data) {
          this.students.set(response.data);
        } else {
          this.errorMessage.set(response.message || 'Impossible de charger les élèves.');
        }
        this.isLoading.set(false);
      },
      error: err => {
        console.error('API Error:', err);
        this.errorMessage.set('Une erreur réseau est survenue.');
        this.isLoading.set(false);
      }
    });
  }

  // =========================================================================
  // PDF MANAGEMENT
  // =========================================================================
  exportToPdf(): void {
    const currentId = this.classGroupId();
    if (!currentId) return;

    this.isExportingPdf.set(true);
    this.classGroupService.exportPdf1(currentId).subscribe({
      next: (response: any) => {
        this.processBlobDownload(response, `liste_eleves_classe_${currentId}.pdf`);
        this.isExportingPdf.set(false);
      },
      error: err => {
        console.error('PDF API Error:', err);
        this.isExportingPdf.set(false);
      }
    });
  }

  onOpenExportDocsModal(): void {
    const currentClassId = this.classGroupId();
    if (!currentClassId) {
      this.toastService.error('Identifiant de la classe introuvable.');
      return;
    }

    this.isExportDocsModalOpen.set(true);
    this.isDocsLoading.set(true);
    this.selectedDocumentIds.set([]);

    this.enrollmentDocumentService.getDocumentsByClassGroup(currentClassId).subscribe({
      next: response => {
        if (response?.success && response.data) {
          this.availableDocuments.set(response.data);

          // Pre-check all documents by default
          const allIds = response.data
            .map(doc => doc.id)
            .filter((id): id is number => id !== undefined);

          this.selectedDocumentIds.set(allIds);
        }
        this.isDocsLoading.set(false);
      },
      error: err => {
        console.error('API documents error:', err);
        this.toastService.error('Erreur lors de la récupération des documents.');
        this.isDocsLoading.set(false);
      }
    });
  }

  toggleDocument(docId: number | undefined): void {
    if (!docId) return;
    this.selectedDocumentIds.update(ids =>
      ids.includes(docId) ? ids.filter(id => id !== docId) : [...ids, docId]
    );
  }

  toggleAllDocuments(event: Event): void {
    const isChecked = (event.target as HTMLInputElement).checked;
    if (isChecked) {
      const allIds = this.availableDocuments()
        .map(d => d.id)
        .filter((id): id is number => id !== undefined);
      this.selectedDocumentIds.set(allIds);
    } else {
      this.selectedDocumentIds.set([]);
    }
  }

  exportPdfWithDocuments(): void {
    const currentId = this.classGroupId();
    const selectedIds = this.selectedDocumentIds();

    if (!currentId) return;
    if (selectedIds.length === 0) {
      this.toastService.error("Veuillez sélectionner au moins un document pour l'export.");
      return;
    }

    this.isExportingDocsPdf.set(true);

    this.classGroupService.exportPdfWithDocuments(currentId, selectedIds).subscribe({
      next: (response: any) => {
        this.processBlobDownload(response, `liste_eleves_documents_${currentId}.pdf`);
        this.isExportingDocsPdf.set(false);
        this.isExportDocsModalOpen.set(false);
      },
      error: err => {
        console.error('PDF with documents API error:', err);
        this.isExportingDocsPdf.set(false);
      }
    });
  }

  private processBlobDownload(response: any, filename: string): void {
    try {
      const blob =
        response instanceof Blob ? response : new Blob([response], {type: 'application/pdf'});
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      this.toastService.success('Export PDF généré avec succès.');
    } catch (e) {
      console.error('Blob processing error:', e);
      this.toastService.error('Erreur lors de la génération locale du fichier.');
    }
  }

  // =========================================================================
  // STUDENT ASSIGNMENT MANAGEMENT
  // =========================================================================
  onOpenAssignModal(): void {
    const promotionId = this.classDetails()?.promotionId;
    if (!promotionId) {
      this.toastService.error('Impossible de récupérer la promotion rattachée.');
      return;
    }
    this.isAssignModalOpen.set(true);
    this.isModalLoading.set(true);
    this.modalSearchQuery.set('');

    this.enrollmentService.getUnassignedValidatedEnrollments({promotionId}).subscribe({
      next: response => {
        if (response?.success && response.data) {
          this.unassignedStudents.set(response.data);
        }
        this.isModalLoading.set(false);
      },
      error: err => {
        console.error('Error:', err);
        this.toastService.error('Erreur lors de la récupération des étudiants.');
        this.isModalLoading.set(false);
      }
    });
  }

  onInitiateStudentAssignment(enrollment: UnassignedEnrollmentResponse): void {
    this.selectedEnrollmentToAssign.set(enrollment);
    this.isAssignConfirmModalOpen.set(true);
  }

  onExecuteStudentAssignment(): void {
    const student = this.selectedEnrollmentToAssign();
    const classId = this.classGroupId();
    if (!student || !classId || this.isAssigning()) return;

    this.isAssigning.set(true);
    const assignmentPayload: ClassAssignmentRequest = {
      classGroupId: classId,
      enrollmentId: student.enrollmentId!
    };

    this.classAssignmentService.assignStudentToClass(assignmentPayload).subscribe({
      next: response => {
        if (response?.success) {
          this.toastService.success(
            `${student.firstName} ${student.lastName} a été affecté à la classe.`
          );
          this.unassignedStudents.update(list =>
            list.filter(s => s.enrollmentId !== student.enrollmentId)
          );
          this.loadClassDetailsAndStudents(classId);
          this.onCancelAssignment();
        } else {
          this.toastService.error(response.message || "Erreur lors de l'affectation.");
          this.isAssigning.set(false);
        }
      },
      error: err => {
        console.error('HTTP Post assignment error:', err);
        this.toastService.error("Impossible d'affecter l'élève. Le serveur a rejeté la demande.");
        this.isAssigning.set(false);
      }
    });
  }

  onCancelAssignment(): void {
    this.isAssignConfirmModalOpen.set(false);
    this.selectedEnrollmentToAssign.set(null);
    this.isAssigning.set(false);
  }

  // =========================================================================
  // UTILS (Badges, Initials)
  // =========================================================================
  getStatusBadgeClass(status: string | undefined): string {
    if (!status) return 'bg-slate-50 text-slate-600 border-slate-200';
    switch (status.toUpperCase()) {
      case 'VALIDATED':
        return 'bg-emerald-50 text-emerald-700 border border-emerald-200';
      case 'CONDITIONALLY_VALIDATED':
        return 'bg-amber-50 text-amber-700 border border-amber-200 animate-pulse';
      case 'SUSPENDED':
        return 'bg-rose-50 text-rose-700 border border-rose-200 font-bold';
      case 'DROPPED_OUT':
        return 'bg-slate-100 text-slate-500 border border-slate-300 line-through opacity-60';
      case 'COMPLETED':
        return 'bg-blue-50 text-blue-700 border border-blue-200';
      default:
        return 'bg-slate-50 text-slate-600 border border-slate-200';
    }
  }

  getTemplateInitials(firstName?: string, lastName?: string): string {
    const f = firstName ? firstName.charAt(0).toUpperCase() : '';
    const l = lastName ? lastName.charAt(0).toUpperCase() : '';
    return `${f}${l}` || '??';
  }

  getTemplateStatusLabel(status: string | undefined): string {
    if (!status) return 'Inconnu';
    const mapping: Record<string, string> = {
      VALIDATED: 'En règle',
      CONDITIONALLY_VALIDATED: 'Dossier Incomplet',
      SUSPENDED: 'Suspendu',
      DROPPED_OUT: 'Abandon',
      COMPLETED: 'Terminé'
    };
    return mapping[status.toUpperCase()] || status;
  }
}
