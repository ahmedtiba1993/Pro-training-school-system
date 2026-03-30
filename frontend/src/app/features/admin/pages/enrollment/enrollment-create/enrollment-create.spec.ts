import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EnrollmentCreate } from './enrollment-create';

describe('EnrollmentCreate', () => {
  let component: EnrollmentCreate;
  let fixture: ComponentFixture<EnrollmentCreate>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EnrollmentCreate]
    }).compileComponents();

    fixture = TestBed.createComponent(EnrollmentCreate);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
