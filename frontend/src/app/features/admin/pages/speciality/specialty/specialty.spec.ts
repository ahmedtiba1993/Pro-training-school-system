import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Specialty } from './specialty';

describe('Specialty', () => {
  let component: Specialty;
  let fixture: ComponentFixture<Specialty>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Specialty]
    }).compileComponents();

    fixture = TestBed.createComponent(Specialty);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
