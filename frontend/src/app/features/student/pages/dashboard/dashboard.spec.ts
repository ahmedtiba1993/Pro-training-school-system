import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { StudentDashboard } from './dashboard';
import { AuthService } from '../../../../core/auth/auth.service';
import { AcademicYearControllerService } from '../../../../core/api';

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
        },
        {
          provide: AcademicYearControllerService,
          useValue: {
            getDefaultAcademicYear: vi.fn().mockReturnValue(of({
              success: true,
              data: {
                id: 1,
                label: '2025-2026',
                startDate: '2025-09-01',
                endDate: '2026-06-30',
                status: 'IN_PROGRESS',
                isDefault: true,
                isLocked: false
              }
            }))
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
