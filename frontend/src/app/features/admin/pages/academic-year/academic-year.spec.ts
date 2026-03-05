import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AcademicYear } from './academic-year';

describe('AcademicYear', () => {
  let component: AcademicYear;
  let fixture: ComponentFixture<AcademicYear>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AcademicYear]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AcademicYear);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
