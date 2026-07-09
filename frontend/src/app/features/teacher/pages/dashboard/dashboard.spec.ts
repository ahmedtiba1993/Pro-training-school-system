import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TeacherDashboard } from './dashboard';
import { AuthService } from '../../../../core/auth/auth.service';
import { signal } from '@angular/core';

describe('TeacherDashboard', () => {
  let component: TeacherDashboard;
  let fixture: ComponentFixture<TeacherDashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TeacherDashboard],
      providers: [
        {
          provide: AuthService,
          useValue: {
            currentUser: signal({
              firstName: 'Test',
              lastName: 'Teacher',
              role: 'ROLE_TEACHER'
            })
          }
        }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TeacherDashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
