import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ParentDashboard } from './dashboard';
import { AuthService } from '../../../../core/auth/auth.service';
import { signal } from '@angular/core';

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
