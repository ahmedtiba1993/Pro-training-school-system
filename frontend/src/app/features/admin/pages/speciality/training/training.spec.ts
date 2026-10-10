import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { of } from 'rxjs';
import { Training } from './training';
import {
  TrainingControllerService,
  LevelControllerService,
  SpecialtyControllerService,
  SpecialtyResponse
} from '../../../../../core/api';
import { ToastService } from '../../../../../shared/services/toast.service';

describe('Training Component', () => {
  let component: Training;
  let fixture: ComponentFixture<Training>;
  let mockTrainingService: Partial<TrainingControllerService>;
  let mockLevelService: Partial<LevelControllerService>;
  let mockSpecialtyService: Partial<SpecialtyControllerService>;
  let mockToastService: Partial<ToastService>;

  const mockActiveSpecialties: SpecialtyResponse[] = [
    { id: 1, label: 'Développement Web', code: 'DEV', isActive: true },
    { id: 2, label: 'Réseaux & Sécurité', code: 'RES', isActive: true }
  ];

  beforeEach(async () => {
    mockTrainingService = {
      getAllPaged: vi.fn().mockReturnValue(
        of({
          success: true,
          data: { content: [], totalElements: 0, totalPages: 0 }
        })
      ),
      getActiveTrainingStats: vi.fn().mockReturnValue(
        of({
          success: true,
          data: []
        })
      )
    };

    mockLevelService = {
      getAllLevels: vi.fn().mockReturnValue(
        of({
          success: true,
          data: []
        })
      )
    };

    mockSpecialtyService = {
      getActiveSpecialties: vi.fn().mockReturnValue(
        of({
          success: true,
          message: 'SPECIALTY_ACTIVE_LIST_RETRIEVED',
          data: mockActiveSpecialties
        })
      )
    };

    mockToastService = {
      success: vi.fn(),
      error: vi.fn(),
      info: vi.fn(),
      warning: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [Training],
      providers: [
        { provide: TrainingControllerService, useValue: mockTrainingService },
        { provide: LevelControllerService, useValue: mockLevelService },
        { provide: SpecialtyControllerService, useValue: mockSpecialtyService },
        { provide: ToastService, useValue: mockToastService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Training);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load only active specialties on init', () => {
    expect(component).toBeTruthy();
    expect(mockSpecialtyService.getActiveSpecialties).toHaveBeenCalled();
    expect(component.specialties()).toEqual(mockActiveSpecialties);
    expect(component.specialties().every(s => s.isActive)).toBe(true);
  });
});
