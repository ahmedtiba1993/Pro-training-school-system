import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AcademicYearsPeriod } from './academic-years-period';

describe('AcademicYearsPeriod', () => {
  let component: AcademicYearsPeriod;
  let fixture: ComponentFixture<AcademicYearsPeriod>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AcademicYearsPeriod]
    }).compileComponents();

    fixture = TestBed.createComponent(AcademicYearsPeriod);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
