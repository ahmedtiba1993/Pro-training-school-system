import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AcademicYearList } from './academic-year-list';

describe('AcademicYearList', () => {
  let component: AcademicYearList;
  let fixture: ComponentFixture<AcademicYearList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AcademicYearList]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AcademicYearList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
