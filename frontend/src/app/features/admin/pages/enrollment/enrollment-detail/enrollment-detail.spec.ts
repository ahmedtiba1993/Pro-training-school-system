import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EnrollmentDetail } from './enrollment-detail';

describe('EnrollmentDetail', () => {
  let component: EnrollmentDetail;
  let fixture: ComponentFixture<EnrollmentDetail>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EnrollmentDetail]
    }).compileComponents();

    fixture = TestBed.createComponent(EnrollmentDetail);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
