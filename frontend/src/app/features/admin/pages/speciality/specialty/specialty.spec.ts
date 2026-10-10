import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { Specialty } from './specialty';
import { SpecialtyControllerService, SpecialtyResponse } from '../../../../../core/api';
import { ToastService } from '../../../../../shared/services/toast.service';

describe('Specialty Component', () => {
  let component: Specialty;
  let fixture: ComponentFixture<Specialty>;
  let mockSpecialtyService: Partial<SpecialtyControllerService>;
  let mockToastService: Partial<ToastService>;

  const mockSpecialties: SpecialtyResponse[] = [
    { id: 1, label: 'Informatique', code: 'INFO', isActive: true },
    { id: 2, label: 'Gestion', code: 'GEST', isActive: false }
  ];

  beforeEach(async () => {
    mockSpecialtyService = {
      getAllSpecialties: vi.fn().mockReturnValue(of({ success: true, data: mockSpecialties })),
      updateSpecialtyStatus: vi
        .fn()
        .mockReturnValue(of({ success: true, message: 'SPECIALTY_STATUS_UPDATED', data: 1 })),
      createSpecialty: vi
        .fn()
        .mockReturnValue(of({ success: true, message: 'SPECIALTY_CREATED', data: 3 })),
      updateSpecialty: vi
        .fn()
        .mockReturnValue(of({ success: true, message: 'SPECIALTY_UPDATED', data: 1 }))
    };

    mockToastService = {
      success: vi.fn(),
      error: vi.fn(),
      info: vi.fn(),
      warning: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [Specialty],
      providers: [
        { provide: SpecialtyControllerService, useValue: mockSpecialtyService },
        { provide: ToastService, useValue: mockToastService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Specialty);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load specialties on init', () => {
    expect(component).toBeTruthy();
    expect(mockSpecialtyService.getAllSpecialties).toHaveBeenCalled();
    expect(component.specialties()).toEqual(mockSpecialties);
    expect(component.isLoading()).toBe(false);
  });

  it('should open and close the status toggle confirmation modal', () => {
    const specialty = mockSpecialties[0];
    component.openToggleConfirmModal(specialty);

    expect(component.isConfirmModalOpen()).toBe(true);
    expect(component.specialtyToToggle()).toEqual(specialty);

    component.closeToggleConfirmModal();

    expect(component.isConfirmModalOpen()).toBe(false);
    expect(component.specialtyToToggle()).toBeNull();
  });

  it('should toggle specialty status to inactive and display success toast', () => {
    const specialty = mockSpecialties[0]; // isActive: true
    component.openToggleConfirmModal(specialty);

    component.confirmToggleSpecialtyStatus();

    expect(mockSpecialtyService.updateSpecialtyStatus).toHaveBeenCalledWith(1, false);
    expect(mockToastService.success).toHaveBeenCalledWith('Spécialité désactivée avec succès');
    expect(component.isConfirmModalOpen()).toBe(false);

    const updated = component.specialties().find(s => s.id === 1);
    expect(updated?.isActive).toBe(false);
    expect(component.isTogglingStatus()).toBe(false);
  });

  it('should toggle specialty status to active and display success toast', () => {
    const specialty = mockSpecialties[1]; // isActive: false
    component.openToggleConfirmModal(specialty);

    component.confirmToggleSpecialtyStatus();

    expect(mockSpecialtyService.updateSpecialtyStatus).toHaveBeenCalledWith(2, true);
    expect(mockToastService.success).toHaveBeenCalledWith('Spécialité activée avec succès');
    expect(component.isConfirmModalOpen()).toBe(false);

    const updated = component.specialties().find(s => s.id === 2);
    expect(updated?.isActive).toBe(true);
  });

  it('should handle error when toggle status fails', () => {
    mockSpecialtyService.updateSpecialtyStatus = vi.fn().mockReturnValue(
      throwError(() => ({ error: { message: 'SPECIALTY_NOT_FOUND' } }))
    );

    const specialty = mockSpecialties[0];
    component.openToggleConfirmModal(specialty);

    component.confirmToggleSpecialtyStatus();

    expect(mockToastService.error).toHaveBeenCalledWith('Spécialité introuvable.');
    expect(component.isTogglingStatus()).toBe(false);
  });
});
