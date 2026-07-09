import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StudentDashboard } from './dashboard';
import { AuthService } from '../../../../core/auth/auth.service';
import { signal } from '@angular/core';

describe('StudentDashboard', () => {
  let component: StudentDashboard;
  let fixture: ComponentFixture<StudentDashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StudentDashboard],
      providers: [
        {
          provide: AuthService,
          useValue: {
            currentUser: signal({
              firstName: 'Test',
              lastName: 'Student',
              role: 'ROLE_STUDENT'
            })
          }
        }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StudentDashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
