import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ScheduleDetail } from './schedule-detail';

describe('ScheduleDetail', () => {
  let component: ScheduleDetail;
  let fixture: ComponentFixture<ScheduleDetail>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ScheduleDetail]
    }).compileComponents();

    fixture = TestBed.createComponent(ScheduleDetail);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
