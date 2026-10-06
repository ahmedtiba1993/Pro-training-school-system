import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { AcademicCalendarComponent } from './academic-calendar';
import {
  AcademicYearControllerService,
  ApiResponseAcademicCalendarResponse,
  AcademicCalendarResponse,
} from '../../../core/api';

describe('AcademicCalendarComponent', () => {
  let component: AcademicCalendarComponent;
  let fixture: ComponentFixture<AcademicCalendarComponent>;
  let mockAcademicYearService: Partial<AcademicYearControllerService>;

  const mockCalendarData: AcademicCalendarResponse = {
    id: 1,
    label: 'Année Universitaire 2025-2026',
    startDate: '2025-09-01',
    endDate: '2026-06-30',
    periods: [
      {
        id: 10,
        label: 'Semestre 1',
        startDate: '2025-09-01',
        endDate: '2026-01-31',
        orderIndex: 1,
        sessions: [
          {
            id: 101,
            label: 'Examens Fin S1',
            sessionType: 'MAIN',
            startDate: '2026-01-15',
            endDate: '2026-01-25',
          },
        ],
      },
    ],
    holidays: [
      {
        id: 201,
        label: 'Vacances d\'hiver',
        type: 'SCHOOL_BREAK',
        startDate: '2025-12-20',
        endDate: '2026-01-04',
        numberOfDays: 16,
      },
    ],
  };

  const mockApiResponse: ApiResponseAcademicCalendarResponse = {
    success: true,
    message: 'ACADEMIC_YEAR_CALENDAR_RETRIEVED',
    data: mockCalendarData,
  };

  beforeEach(async () => {
    mockAcademicYearService = {
      getDefaultAcademicCalendar: vi.fn().mockReturnValue(of(mockApiResponse)),
    };

    await TestBed.configureTestingModule({
      imports: [AcademicCalendarComponent],
      providers: [
        {
          provide: AcademicYearControllerService,
          useValue: mockAcademicYearService,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AcademicCalendarComponent);
    component = fixture.componentInstance;
  });

  it('devrait initialiser le composant et charger le calendrier', () => {
    fixture.detectChanges();

    expect(component.calendar()).toEqual(mockCalendarData);
    expect(component.isLoading()).toBe(false);
    expect(component.error()).toBeNull();
    expect(component.periodsCount()).toBe(1);
    expect(component.holidaysCount()).toBe(1);
    expect(component.sessionsCount()).toBe(1);
  });

  it('devrait mettre à jour l\'onglet actif avec setTab', () => {
    fixture.detectChanges();

    expect(component.activeTab()).toBe('ALL');
    component.setTab('PERIODS');
    expect(component.activeTab()).toBe('PERIODS');
    component.setTab('HOLIDAYS');
    expect(component.activeTab()).toBe('HOLIDAYS');
  });

  it('devrait gérer les erreurs de chargement API', () => {
    mockAcademicYearService.getDefaultAcademicCalendar = vi
      .fn()
      .mockReturnValue(throwError(() => new Error('API Error')));

    component.loadCalendar();

    expect(component.calendar()).toBeNull();
    expect(component.isLoading()).toBe(false);
    expect(component.error()).toBe('Impossible de charger le calendrier de l\'année scolaire.');
  });
});
