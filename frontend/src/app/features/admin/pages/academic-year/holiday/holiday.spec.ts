import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { of } from 'rxjs';
import { Holiday } from './holiday';
import {
  AcademicYearControllerService,
  HolidayControllerService,
  HolidayResponse
} from '../../../../../core/api';
import { ToastService } from '../../../../../shared/services/toast.service';

describe('Holiday Component', () => {
  let component: Holiday;
  let fixture: ComponentFixture<Holiday>;
  let mockAcademicYearService: Partial<AcademicYearControllerService>;
  let mockHolidayService: Partial<HolidayControllerService>;
  let mockToastService: Partial<ToastService>;

  const mockOpenYears = [
    { id: 1, label: '2025-2026', isDefault: true },
    { id: 2, label: '2026-2027', isDefault: false }
  ];

  const mockHolidays: HolidayResponse[] = [
    {
      id: 10,
      label: "Vacances d'hiver",
      labelAr: 'عطلة الشتاء',
      startDate: '2026-12-20',
      endDate: '2027-01-03',
      type: 'SCHOOL_BREAK',
      numberOfDays: 14,
      academicYearId: 1
    },
    {
      id: 11,
      label: 'Fête de la Révolution',
      labelAr: 'عيد الثورة',
      startDate: '2027-01-14',
      endDate: '2027-01-14',
      type: 'PUBLIC_HOLIDAY',
      numberOfDays: 1,
      academicYearId: 1
    }
  ];

  beforeEach(async () => {
    mockAcademicYearService = {
      getOpenAcademicYears: vi.fn().mockReturnValue(of({ success: true, data: mockOpenYears }))
    };

    mockHolidayService = {
      getHolidaysByAcademicYearId: vi.fn().mockReturnValue(of({ success: true, data: mockHolidays })),
      createHoliday: vi.fn().mockReturnValue(of({ success: true, data: 100 })),
      updateHoliday: vi.fn().mockReturnValue(of({ success: true, data: mockHolidays[0] }))
    };

    mockToastService = {
      success: vi.fn(),
      error: vi.fn(),
      info: vi.fn(),
      warning: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [Holiday],
      providers: [
        { provide: AcademicYearControllerService, useValue: mockAcademicYearService },
        { provide: HolidayControllerService, useValue: mockHolidayService },
        { provide: ToastService, useValue: mockToastService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Holiday);
    component = fixture.componentInstance;
  });

  it('devrait initialiser le composant et charger les années et les vacances', () => {
    fixture.detectChanges();

    expect(component.academicYears().length).toBe(2);
    expect(component.selectedYearId()).toBe(1);
    expect(component.holidays().length).toBe(2);
    expect(component.isLoading()).toBe(false);
    expect(component.totalHolidayDays()).toBe(15);
  });

  it('devrait réinitialiser le formulaire incluant labelAr lors de openModal', () => {
    component.openModal();

    expect(component.showHolidayModal()).toBe(true);
    expect(component.editingHolidayId()).toBeNull();
    expect(component.holidayForm.get('labelAr')?.value).toBe('');
    expect(component.holidayForm.get('type')?.value).toBe('SCHOOL_BREAK');
  });

  it('devrait pré-remplir le champ labelAr lors de editHoliday', () => {
    const holidayToEdit: HolidayResponse = {
      id: 10,
      label: "Vacances d'hiver",
      labelAr: 'عطلة الشتاء',
      startDate: '2026-12-20',
      endDate: '2027-01-03',
      type: 'SCHOOL_BREAK',
      academicYearId: 1
    };

    component.editHoliday(holidayToEdit);

    expect(component.showHolidayModal()).toBe(true);
    expect(component.editingHolidayId()).toBe(10);
    expect(component.holidayForm.get('label')?.value).toBe("Vacances d'hiver");
    expect(component.holidayForm.get('labelAr')?.value).toBe('عطلة الشتاء');
  });

  it('devrait envoyer le payload contenant labelAr lors de la création d une vacance', () => {
    fixture.detectChanges();
    component.openModal();

    component.holidayForm.setValue({
      label: "Vacances de Printemps",
      labelAr: "عطلة الربيع",
      type: "SCHOOL_BREAK",
      startDate: "2027-03-15",
      endDate: "2027-03-28"
    });

    component.submitHoliday();

    expect(mockHolidayService.createHoliday).toHaveBeenCalledWith(
      expect.objectContaining({
        label: "Vacances de Printemps",
        labelAr: "عطلة الربيع",
        type: "SCHOOL_BREAK",
        startDate: "2027-03-15",
        endDate: "2027-03-28",
        academicYearId: 1
      })
    );
    expect(mockToastService.success).toHaveBeenCalledWith('Vacances ajoutées avec succès');
    expect(component.showHolidayModal()).toBe(false);
  });

  it('devrait envoyer labelAr undefined si le champ arabe est laissé vide', () => {
    fixture.detectChanges();
    component.openModal();

    component.holidayForm.setValue({
      label: "Congé exceptionnel",
      labelAr: "   ",
      type: "EXCEPTIONAL",
      startDate: "2027-04-10",
      endDate: ""
    });

    component.submitHoliday();

    expect(mockHolidayService.createHoliday).toHaveBeenCalledWith(
      expect.objectContaining({
        label: "Congé exceptionnel",
        labelAr: undefined,
        type: "EXCEPTIONAL",
        startDate: "2027-04-10",
        endDate: "",
        academicYearId: 1
      })
    );
  });

  it('devrait envoyer le payload avec labelAr lors de la mise à jour d une vacance', () => {
    fixture.detectChanges();
    component.editHoliday(mockHolidays[0]);

    component.holidayForm.patchValue({
      labelAr: "عطلة الشتاء المحدثة"
    });

    component.submitHoliday();

    expect(mockHolidayService.updateHoliday).toHaveBeenCalledWith(
      10,
      expect.objectContaining({
        label: "Vacances d'hiver",
        labelAr: "عطلة الشتاء المحدثة",
        academicYearId: 1
      })
    );
    expect(mockToastService.success).toHaveBeenCalledWith('Vacances modifiées avec succès');
  });
});
