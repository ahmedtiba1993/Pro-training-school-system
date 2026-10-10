import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { AcademicYearsPeriod } from './academic-years-period';

describe('AcademicYearsPeriod', () => {
  let component: AcademicYearsPeriod;
  let fixture: ComponentFixture<AcademicYearsPeriod>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AcademicYearsPeriod],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AcademicYearsPeriod);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
