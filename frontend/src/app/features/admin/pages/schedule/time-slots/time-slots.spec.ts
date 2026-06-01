import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TimeSlots } from './time-slots';

describe('TimeSlots', () => {
  let component: TimeSlots;
  let fixture: ComponentFixture<TimeSlots>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TimeSlots]
    }).compileComponents();

    fixture = TestBed.createComponent(TimeSlots);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
