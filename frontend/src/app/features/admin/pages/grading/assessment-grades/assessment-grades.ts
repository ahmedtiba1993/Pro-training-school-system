import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { GradeRecordControllerService } from '../../../../../core/api/api/grade-record-controller.service';
import { GradeRecordResponse } from '../../../../../core/api/model/grade-record-response';
import { AssessmentResponse } from '../../../../../core/api/model/assessment-response';
import { ToastService } from '../../../../../shared/services/toast.service';

export interface LocalGradeItem {
  id?: number;
  enrollmentId?: number;
  matricule?: string;
  nom?: string;
  prenom?: string;
  presence: 'PRESENT' | 'ABSENT_JUSTIFIED' | 'ABSENT_UNJUSTIFIED';
  note?: number | null;
  commentaire?: string;
}

export interface GradeRecordRequestPayload {
  assessmentId: number;
  enrollmentId: number;
  score: number | null;
  attendanceStatus: 'PRESENT' | 'ABSENT_JUSTIFIED' | 'ABSENT_UNJUSTIFIED';
  teacherComment: string | null;
  isOverridden: boolean;
  overrideReason: string | null;
}

@Component({
  selector: 'app-assessment-grades',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './assessment-grades.html',
  styleUrl: './assessment-grades.css'
})
export class AssessmentGrades implements OnInit {
  // --- DEPENDENCY INJECTIONS ---
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);
  private readonly gradeRecordService = inject(GradeRecordControllerService);
  private readonly toastService = inject(ToastService);

  // --- CONTEXT SIGNALS ---
  public readonly assessmentId = signal<number | null>(null);
  public readonly promotionId = signal<number | null>(null);
  public readonly assessment = signal<AssessmentResponse | null>(null);
  public readonly promotionName = signal<string>('');

  // --- DATA & STATE SIGNALS ---
  public readonly records = signal<GradeRecordResponse[]>([]);
  public readonly editableRecords = signal<LocalGradeItem[]>([]);
  public readonly isEditMode = signal<boolean>(false);
  public readonly isLoading = signal<boolean>(true);
  public readonly isSaving = signal<boolean>(false);
  public readonly searchQuery = signal<string>('');

  // --- COMPUTED SIGNALS ---
  public readonly maxScore = computed<number>(() => {
    return this.assessment()?.totalMarks || 20;
  });

  public readonly filteredRecords = computed(() => {
    let list = this.records();
    const query = this.searchQuery().toLowerCase().trim();

    if (query) {
      list = list.filter(r =>
        (r.matricule && r.matricule.toLowerCase().includes(query)) ||
        (r.nom && r.nom.toLowerCase().includes(query)) ||
        (r.prenom && r.prenom.toLowerCase().includes(query))
      );
    }

    return list;
  });

  public readonly filteredEditableRecords = computed(() => {
    let list = this.editableRecords();
    const query = this.searchQuery().toLowerCase().trim();

    if (query) {
      list = list.filter(r =>
        (r.matricule && r.matricule.toLowerCase().includes(query)) ||
        (r.nom && r.nom.toLowerCase().includes(query)) ||
        (r.prenom && r.prenom.toLowerCase().includes(query))
      );
    }

    return list;
  });

  public readonly totalCount = computed(() => this.records().length);

  public readonly presentCount = computed(() => {
    const list = this.isEditMode() ? this.editableRecords() : this.records();
    return list.filter(r => r.presence === 'PRESENT').length;
  });

  public readonly absentCount = computed(() => {
    const list = this.isEditMode() ? this.editableRecords() : this.records();
    return list.filter(r => r.presence && r.presence !== 'PRESENT').length;
  });

  public ngOnInit(): void {
    const aidParam = this.route.snapshot.paramMap.get('assessmentId');
    const pidParam = this.route.snapshot.paramMap.get('id');

    if (aidParam) {
      this.assessmentId.set(+aidParam);
    }
    if (pidParam) {
      this.promotionId.set(+pidParam);
    }

    const nav = this.router.getCurrentNavigation();
    const stateData = nav?.extras.state || history.state;
    if (stateData?.assessment) {
      this.assessment.set(stateData.assessment);
    }
    if (stateData?.promotionName) {
      this.promotionName.set(stateData.promotionName);
    }

    const currentAssessmentId = this.assessmentId();
    if (currentAssessmentId) {
      this.loadGrades(currentAssessmentId);
    } else {
      this.toastService.error('Identifiant d\'épreuve manquant.');
      this.goBack();
    }
  }

  /**
   * Loads grades list via the getAllGradesByAssessment API.
   */
  public loadGrades(assessmentId: number): void {
    this.isLoading.set(true);
    this.gradeRecordService.getAllGradesByAssessment(assessmentId)
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response: any) => {
          if (response?.success && response.data) {
            this.records.set(response.data);
          } else {
            this.toastService.error('Impossible de charger les notes pour cette épreuve.');
          }
        },
        error: (err: any) => {
          console.error('Erreur chargement des notes', err);
          this.toastService.error('Erreur lors de la récupération des notes.');
        }
      });
  }

  /**
   * Enables edit mode (opens the inline table form).
   */
  public startEdit(): void {
    const currentList = this.records();
    const editableList: LocalGradeItem[] = currentList.map(r => ({
      id: r.id,
      enrollmentId: r.enrollmentId,
      matricule: r.matricule,
      nom: r.nom,
      prenom: r.prenom,
      presence: (r.presence as 'PRESENT' | 'ABSENT_JUSTIFIED' | 'ABSENT_UNJUSTIFIED') || 'PRESENT',
      note: r.note !== undefined && r.note !== null ? r.note : null,
      commentaire: r.commentaire || ''
    }));

    this.editableRecords.set(editableList);
    this.isEditMode.set(true);
  }

  /**
   * Cancels edit mode without saving.
   */
  public cancelEdit(): void {
    this.isEditMode.set(false);
  }

  /**
   * Updates attendance status in the edit form.
   */
  public onPresenceChange(item: LocalGradeItem, newStatus: string): void {
    item.presence = newStatus as 'PRESENT' | 'ABSENT_JUSTIFIED' | 'ABSENT_UNJUSTIFIED';
    if (newStatus !== 'PRESENT') {
      item.note = null;
    }
  }

  /**
   * Updates the entered grade with score limit validation.
   */
  public onNoteChange(item: LocalGradeItem, rawVal: string): void {
    if (rawVal === '' || rawVal === null || rawVal === undefined) {
      item.note = null;
      return;
    }

    const val = parseFloat(rawVal);
    if (isNaN(val)) {
      item.note = null;
      return;
    }

    const max = this.maxScore();
    if (val < 0) {
      item.note = 0;
      this.toastService.warning('La note ne peut pas être négative.');
    } else if (val > max) {
      item.note = max;
      this.toastService.warning(`La note ne peut pas dépasser le barème de ${max} points.`);
    } else {
      item.note = val;
    }
  }

  /**
   * Updates the teacher's comment.
   */
  public onCommentChange(item: LocalGradeItem, text: string): void {
    item.commentaire = text;
  }

  /**
   * Saves changes via API: POST /api/v1/grades/assessment/{assessmentId}
   */
  public saveGrades(): void {
    const aid = this.assessmentId();
    if (!aid) return;

    this.isSaving.set(true);

    const payload: GradeRecordRequestPayload[] = this.editableRecords().map(item => ({
      assessmentId: aid,
      enrollmentId: item.enrollmentId || 0,
      score: item.presence === 'PRESENT' ? (item.note ?? null) : null,
      attendanceStatus: item.presence,
      teacherComment: item.commentaire ? item.commentaire.trim() : null,
      isOverridden: false,
      overrideReason: null
    }));

    const basePath = (this.gradeRecordService as any)?.configuration?.basePath || '';
    const url = `${basePath}/api/v1/grades/assessment/${aid}`;

    this.http.post<any>(url, payload)
      .pipe(finalize(() => this.isSaving.set(false)))
      .subscribe({
        next: () => {
          this.toastService.success('Notes et présences enregistrées avec succès.');
          this.isEditMode.set(false);
          this.loadGrades(aid);
        },
        error: (err: any) => {
          console.error('Erreur enregistrement des notes', err);
          const msg = err?.error?.message || 'Erreur lors de l\'enregistrement des notes sur le serveur.';
          this.toastService.error(msg);
        }
      });
  }

  public getPresenceLabel(presence?: string): string {
    switch (presence) {
      case 'PRESENT':
        return 'Présent';
      case 'ABSENT_JUSTIFIED':
        return 'Absent justifié';
      case 'ABSENT_UNJUSTIFIED':
        return 'Absent non justifié';
      default:
        return presence || '—';
    }
  }

  public getPresenceBadgeClass(presence?: string): string {
    switch (presence) {
      case 'PRESENT':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'ABSENT_JUSTIFIED':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'ABSENT_UNJUSTIFIED':
        return 'bg-rose-50 text-rose-700 border-rose-200';
      default:
        return 'bg-slate-50 text-slate-600 border-slate-200';
    }
  }

  public getPresenceDotClass(presence?: string): string {
    switch (presence) {
      case 'PRESENT':
        return 'bg-emerald-500';
      case 'ABSENT_JUSTIFIED':
        return 'bg-amber-500';
      case 'ABSENT_UNJUSTIFIED':
        return 'bg-rose-500';
      default:
        return 'bg-slate-400';
    }
  }

  public goBack(): void {
    const pid = this.promotionId();
    if (pid) {
      this.router.navigate(['/admin/grades', pid, 'assessments']);
    } else {
      this.router.navigate(['/admin/grades']);
    }
  }
}
