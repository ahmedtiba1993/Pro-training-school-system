import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChangePassword } from './change-password';
import { AuthService } from '../../../core/auth/auth.service';
import { ToastService } from '../../../shared/services/toast.service';
import { of } from 'rxjs';

describe('ChangePassword', () => {
  let component: ChangePassword;
  let fixture: ComponentFixture<ChangePassword>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChangePassword],
      providers: [
        {
          provide: AuthService,
          useValue: {
            changePasswordFirstLogin: () => of({}),
            logout: () => {},
            hasValidToken: () => true
          }
        },
        {
          provide: ToastService,
          useValue: {
            success: () => {},
            error: () => {}
          }
        }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ChangePassword);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
