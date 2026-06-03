import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CourseSession } from './course-session';

describe('CourseSession', () => {
  let component: CourseSession;
  let fixture: ComponentFixture<CourseSession>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CourseSession]
    }).compileComponents();

    fixture = TestBed.createComponent(CourseSession);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
