import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { EnrollmentDocumentComponent } from './enrollment-document';
import {
  EnrollmentDocumentControllerService,
  LevelControllerService,
  EnrollmentDocumentResponse,
  LevelResponse
} from '../../../../core/api';
import { ToastService } from '../../../../shared/services/toast.service';

describe('EnrollmentDocumentComponent', () => {
  let component: EnrollmentDocumentComponent;
  let fixture: ComponentFixture<EnrollmentDocumentComponent>;
  let documentServiceMock: any;
  let levelServiceMock: any;
  let toastServiceMock: any;

  const mockLevels: LevelResponse[] = [
    { id: 1, code: 'L1', label: 'Niveau 1' },
    { id: 2, code: 'L2', label: 'Niveau 2' }
  ];

  const mockDocuments: EnrollmentDocumentResponse[] = [
    {
      id: 10,
      code: 'CIN',
      label: 'Carte d\'identité',
      labelAr: 'بطاقة التعريف',
      quantity: 1,
      nature: EnrollmentDocumentResponse.NatureEnum.Original,
      condition: EnrollmentDocumentResponse.ConditionEnum.AllRegistrations,
      mandatory: true,
      isActive: true,
      levels: [mockLevels[0]]
    }
  ];

  beforeEach(async () => {
    documentServiceMock = {
      getAllDocuments: vi.fn().mockReturnValue(of({ data: mockDocuments })),
      createDocument: vi.fn().mockReturnValue(of({ data: 100 })),
      updateDocument: vi.fn().mockReturnValue(of({ data: 10 })),
      updateDocumentStatus: vi.fn().mockReturnValue(of({ success: true, data: 10 })),
      exportArabicForm: vi.fn().mockReturnValue(of(new Blob()))
    };

    levelServiceMock = {
      getAllLevels: vi.fn().mockReturnValue(of({ data: mockLevels })),
      getActiveLevels: vi.fn().mockReturnValue(of({ data: mockLevels }))
    };

    toastServiceMock = {
      success: vi.fn(),
      error: vi.fn(),
      info: vi.fn(),
      warning: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [EnrollmentDocumentComponent],
      providers: [
        { provide: EnrollmentDocumentControllerService, useValue: documentServiceMock },
        { provide: LevelControllerService, useValue: levelServiceMock },
        { provide: ToastService, useValue: toastServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EnrollmentDocumentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('devrait créer le composant et charger les documents et niveaux initiaux', () => {
    expect(component).toBeTruthy();
    expect(component.documents().length).toBe(1);
    expect(component.levels().length).toBe(2);
    expect(component.activeDocuments()).toBe(1);
  });

  describe('Création de document et gestion des réponses backend', () => {
    beforeEach(() => {
      component.openModal();
      component.documentForm.setValue({
        code: 'PASSPORT',
        label: 'Passeport biométrique',
        labelAr: 'جواز السفر',
        quantity: 1,
        nature: 'ORIGINAL',
        condition: 'FOREIGN_STUDENT',
        levelIds: [1],
        mandatory: true
      });
    });

    it('devrait soumettre et notifier le succès en cas de réponse valide', () => {
      documentServiceMock.createDocument.mockReturnValue(
        of({ success: true, message: 'DOCUMENTS_ENROLLMENT_DOCUMENT_CREATED_SUCCESSFULLY', data: 101 })
      );

      component.onSubmit();

      expect(documentServiceMock.createDocument).toHaveBeenCalled();
      expect(toastServiceMock.success).toHaveBeenCalledWith('Document ajouté avec succès');
      expect(component.isModalOpen()).toBe(false);
    });

    it('devrait traduire HTTP 409 avec DOCUMENTS_ENROLLMENT_DOCUMENT_LABEL_ALREADY_EXISTS', () => {
      const errorResponse = new HttpErrorResponse({
        status: 409,
        statusText: 'Conflict',
        error: {
          success: false,
          message: 'DOCUMENTS_ENROLLMENT_DOCUMENT_LABEL_ALREADY_EXISTS'
        }
      });

      documentServiceMock.createDocument.mockReturnValue(throwError(() => errorResponse));

      component.onSubmit();

      expect(toastServiceMock.error).toHaveBeenCalledWith('Un document avec ce libellé existe déjà.');
      expect(component.isSubmitting()).toBe(false);
    });

    it('devrait supporter le code historique DOCUMENT_LABEL_ALREADY_EXISTS', () => {
      const errorResponse = new HttpErrorResponse({
        status: 409,
        statusText: 'Conflict',
        error: {
          success: false,
          message: 'DOCUMENT_LABEL_ALREADY_EXISTS'
        }
      });

      documentServiceMock.createDocument.mockReturnValue(throwError(() => errorResponse));

      component.onSubmit();

      expect(toastServiceMock.error).toHaveBeenCalledWith('Un document avec ce libellé existe déjà.');
    });

    it('devrait traduire HTTP 409 avec DOCUMENTS_ENROLLMENT_DOCUMENT_CODE_ALREADY_EXISTS', () => {
      const errorResponse = new HttpErrorResponse({
        status: 409,
        statusText: 'Conflict',
        error: {
          success: false,
          message: 'DOCUMENTS_ENROLLMENT_DOCUMENT_CODE_ALREADY_EXISTS'
        }
      });

      documentServiceMock.createDocument.mockReturnValue(throwError(() => errorResponse));

      component.onSubmit();

      expect(toastServiceMock.error).toHaveBeenCalledWith('Un document avec ce code existe déjà.');
    });

    it('devrait afficher un message de conflit générique pour un 409 sans code connu', () => {
      const errorResponse = new HttpErrorResponse({
        status: 409,
        statusText: 'Conflict',
        error: {}
      });

      documentServiceMock.createDocument.mockReturnValue(throwError(() => errorResponse));

      component.onSubmit();

      expect(toastServiceMock.error).toHaveBeenCalledWith('Un conflit est survenu : ce document existe déjà.');
    });

    it('devrait utiliser le fallback en cas d erreur serveur inconnue', () => {
      const errorResponse = new HttpErrorResponse({
        status: 500,
        statusText: 'Internal Server Error',
        error: {}
      });

      documentServiceMock.createDocument.mockReturnValue(throwError(() => errorResponse));

      component.onSubmit();

      expect(toastServiceMock.error).toHaveBeenCalledWith("Erreur lors de l'enregistrement");
    });
  });

  describe('Mise à jour de document', () => {
    beforeEach(() => {
      component.openModal(mockDocuments[0]);
    });

    it('devrait traduire HTTP 404 avec DOCUMENTS_ENROLLMENT_DOCUMENT_NOT_FOUND', () => {
      const errorResponse = new HttpErrorResponse({
        status: 404,
        statusText: 'Not Found',
        error: {
          success: false,
          message: 'DOCUMENTS_ENROLLMENT_DOCUMENT_NOT_FOUND'
        }
      });

      documentServiceMock.updateDocument.mockReturnValue(throwError(() => errorResponse));

      component.onSubmit();

      expect(toastServiceMock.error).toHaveBeenCalledWith('Le document demandé est introuvable.');
    });

    it('devrait notifier le succès lors de la mise à jour', () => {
      documentServiceMock.updateDocument.mockReturnValue(
        of({ success: true, message: 'DOCUMENTS_ENROLLMENT_DOCUMENT_UPDATED_SUCCESSFULLY', data: 10 })
      );

      component.onSubmit();

      expect(toastServiceMock.success).toHaveBeenCalledWith('Document mis à jour avec succès');
      expect(component.isModalOpen()).toBe(false);
    });
  });

  describe('Bascule du statut (Toggle Switch)', () => {
    it('devrait désactiver un document actif et notifier le succès', () => {
      const doc = { ...mockDocuments[0], isActive: true };

      component.toggleDocumentStatus(doc);

      expect(documentServiceMock.updateDocumentStatus).toHaveBeenCalledWith(10, false);
      expect(toastServiceMock.success).toHaveBeenCalledWith('Document désactivé avec succès');
      expect(component.documents().find(d => d.id === 10)?.isActive).toBe(false);
      expect(component.activeDocuments()).toBe(0);
    });

    it('devrait activer un document inactif et notifier le succès', () => {
      component.documents.set([{ ...mockDocuments[0], isActive: false }]);
      const doc = component.documents()[0];

      component.toggleDocumentStatus(doc);

      expect(documentServiceMock.updateDocumentStatus).toHaveBeenCalledWith(10, true);
      expect(toastServiceMock.success).toHaveBeenCalledWith('Document activé avec succès');
      expect(component.documents().find(d => d.id === 10)?.isActive).toBe(true);
      expect(component.activeDocuments()).toBe(1);
    });

    it('devrait gérer les erreurs lors du basculement de statut', () => {
      const errorResponse = new HttpErrorResponse({
        status: 500,
        statusText: 'Server Error',
        error: {}
      });
      documentServiceMock.updateDocumentStatus.mockReturnValue(throwError(() => errorResponse));

      component.toggleDocumentStatus(mockDocuments[0]);

      expect(toastServiceMock.error).toHaveBeenCalledWith('Erreur lors du changement de statut');
      expect(component.isToggling(10)).toBe(false);
    });
  });

  describe('Export PDF Arabe', () => {
    it('devrait basculer l\'état du menu déroulant d\'export', () => {
      expect(component.isExportDropdownOpen()).toBe(false);
      component.toggleExportDropdown();
      expect(component.isExportDropdownOpen()).toBe(true);
      component.toggleExportDropdown();
      expect(component.isExportDropdownOpen()).toBe(false);
    });

    it('devrait télécharger le PDF global avec un nom horodaté et afficher un toast de succès', () => {
      const clickSpy = vi.fn();
      let createdLink: any = null;
      const createElementSpy = vi.spyOn(document, 'createElement').mockImplementation((tagName: string) => {
        if (tagName === 'a') {
          createdLink = {
            href: '',
            download: '',
            click: clickSpy
          };
          return createdLink;
        }
        return document.createElement(tagName);
      });
      const appendChildSpy = vi.spyOn(document.body, 'appendChild').mockImplementation(() => null as any);
      const removeChildSpy = vi.spyOn(document.body, 'removeChild').mockImplementation(() => null as any);
      vi.spyOn(window.URL, 'createObjectURL').mockReturnValue('blob:mock-url');
      vi.spyOn(window.URL, 'revokeObjectURL').mockImplementation(() => {});

      component.exportArabicPdf();

      expect(documentServiceMock.exportArabicForm).toHaveBeenCalledWith(undefined, 'body', false, {
        httpHeaderAccept: 'application/pdf'
      });
      expect(createdLink).toBeTruthy();
      expect(createdLink.download).toMatch(/^fiche_inscription_tous_niveaux_\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}\.pdf$/);
      expect(clickSpy).toHaveBeenCalled();
      expect(toastServiceMock.success).toHaveBeenCalledWith('Fichier PDF téléchargé avec succès');
      expect(component.isExporting()).toBe(false);

      createElementSpy.mockRestore();
      appendChildSpy.mockRestore();
      removeChildSpy.mockRestore();
    });

    it('devrait télécharger le PDF filtré pour un niveau spécifique', () => {
      const clickSpy = vi.fn();
      let createdLink: any = null;
      const createElementSpy = vi.spyOn(document, 'createElement').mockImplementation((tagName: string) => {
        if (tagName === 'a') {
          createdLink = {
            href: '',
            download: '',
            click: clickSpy
          };
          return createdLink;
        }
        return document.createElement(tagName);
      });
      const appendChildSpy = vi.spyOn(document.body, 'appendChild').mockImplementation(() => null as any);
      const removeChildSpy = vi.spyOn(document.body, 'removeChild').mockImplementation(() => null as any);
      vi.spyOn(window.URL, 'createObjectURL').mockReturnValue('blob:mock-url');
      vi.spyOn(window.URL, 'revokeObjectURL').mockImplementation(() => {});

      const targetLevel: LevelResponse = { id: 1, code: 'BTP', label: 'Brevet Technicien Pro' };
      component.exportArabicPdf(targetLevel);

      expect(documentServiceMock.exportArabicForm).toHaveBeenCalledWith(1, 'body', false, {
        httpHeaderAccept: 'application/pdf'
      });
      expect(createdLink).toBeTruthy();
      expect(createdLink.download).toMatch(/^fiche_inscription_BTP_\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}\.pdf$/);
      expect(clickSpy).toHaveBeenCalled();
      expect(toastServiceMock.success).toHaveBeenCalledWith('Fichier PDF téléchargé avec succès');
      expect(component.isExporting()).toBe(false);

      createElementSpy.mockRestore();
      appendChildSpy.mockRestore();
      removeChildSpy.mockRestore();
    });

    it('devrait gérer les erreurs de téléchargement sans afficher de toast', () => {
      documentServiceMock.exportArabicForm.mockReturnValue(throwError(() => new Error('Server error')));

      component.exportArabicPdf();

      expect(toastServiceMock.error).not.toHaveBeenCalled();
      expect(component.isExporting()).toBe(false);
    });
  });
});
