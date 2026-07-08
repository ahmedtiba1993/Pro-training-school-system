import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Dashboard } from './dashboard';
import { DashboardControllerService } from '../../../../core/api';
import { of } from 'rxjs';

describe('Dashboard', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        {
          provide: DashboardControllerService,
          useValue: {
            getAdminEnrollmentStats: () => of({
              success: true,
              data: {
                totalCount: 10,
                maleCount: 6,
                femaleCount: 4
              }
            })
          }
        }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

