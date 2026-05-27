import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ClassStudentList } from './class-student-list';

describe('ClassStudentList', () => {
  let component: ClassStudentList;
  let fixture: ComponentFixture<ClassStudentList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ClassStudentList]
    }).compileComponents();

    fixture = TestBed.createComponent(ClassStudentList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
