import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TeacherSpecialties } from './teacher-specialties';

describe('TeacherSpecialties', () => {
  let component: TeacherSpecialties;
  let fixture: ComponentFixture<TeacherSpecialties>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TeacherSpecialties]
    }).compileComponents();

    fixture = TestBed.createComponent(TeacherSpecialties);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
