import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { ParentDashboard } from './dashboard';
import { AuthService } from '../../../../core/auth/auth.service';
import { AcademicYearControllerService } from '../../../../core/api';

describe('ParentDashboard', () => {
  let component: ParentDashboard;
  let fixture: ComponentFixture<ParentDashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ParentDashboard],
      providers: [
        {
          provide: AuthService,
          useValue: {
            currentUser: signal({
              firstName: 'Test',
              lastName: 'Parent',
              role: 'ROLE_PARENT'
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

    fixture = TestBed.createComponent(ParentDashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
