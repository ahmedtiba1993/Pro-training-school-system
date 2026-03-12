import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RegistrationDocument } from './registration-document';

describe('RegistrationDocument', () => {
  let component: RegistrationDocument;
  let fixture: ComponentFixture<RegistrationDocument>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegistrationDocument]
    }).compileComponents();

    fixture = TestBed.createComponent(RegistrationDocument);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
