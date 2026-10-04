import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AcademicYearCardComponent } from './academic-year-card';
import { AcademicYearControllerService, ApiResponsePublicAcademicYearResponse } from '../../../core/api';

describe('AcademicYearCardComponent', () => {
  let component: AcademicYearCardComponent;
  let fixture: ComponentFixture<AcademicYearCardComponent>;
  let mockService: { getDefaultAcademicYear: any };

  const mockYearResponse: ApiResponsePublicAcademicYearResponse = {
    success: true,
    data: {
      id: 1,
      label: '2025-2026',
      startDate: '2025-09-01',
      endDate: '2026-06-30',
      status: 'IN_PROGRESS',
      isDefault: true,
      isLocked: false
    }
  };

  beforeEach(async () => {
    mockService = {
      getDefaultAcademicYear: () => of(mockYearResponse)
    };

    await TestBed.configureTestingModule({
      imports: [AcademicYearCardComponent],
      providers: [
        { provide: AcademicYearControllerService, useValue: mockService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AcademicYearCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should load and display the default academic year', () => {
    expect(component.academicYear()).toEqual(mockYearResponse.data);
    expect(component.isLoading()).toBe(false);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('2025-2026');
    expect(compiled.textContent).toContain('Année en cours');
  });

  it('should handle ACADEMIC_YEAR_NOT_FOUND error in French', () => {
    const notFoundError = {
      status: 404,
      error: {
        errorCode: 'ERR_NOT_FOUND',
        message: 'ACADEMIC_YEAR_NOT_FOUND',
        success: false
      }
    };
    mockService.getDefaultAcademicYear = () => throwError(() => notFoundError);
    component.loadDefaultAcademicYear();
    fixture.detectChanges();

    expect(component.isLoading()).toBe(false);
    expect(component.isNotFound()).toBe(true);
    expect(component.academicYear()).toBeNull();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Aucune année scolaire active n\'a été trouvée');
  });

  it('should handle generic API error', () => {
    mockService.getDefaultAcademicYear = () => throwError(() => new Error('Server error'));
    component.loadDefaultAcademicYear();
    fixture.detectChanges();

    expect(component.isLoading()).toBe(false);
    expect(component.isNotFound()).toBe(false);
    expect(component.error()).toBe('Impossible de charger les informations de l\'année scolaire en cours.');
  });

  it('should map status labels correctly', () => {
    expect(component.getStatusLabel('ENROLLMENT')).toBe('Inscriptions en cours');
    expect(component.getStatusLabel('IN_PROGRESS')).toBe('Année en cours');
    expect(component.getStatusLabel('PLANNED')).toBe('Planifiée');
    expect(component.getStatusLabel('CLOSING')).toBe('Clôture en cours');
    expect(component.getStatusLabel('COMPLETED')).toBe('Année terminée');
    expect(component.getStatusLabel('CLOSED')).toBe('Clôturée');
  });
});
