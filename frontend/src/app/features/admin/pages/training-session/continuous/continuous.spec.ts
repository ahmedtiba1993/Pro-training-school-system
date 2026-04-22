import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Continuous } from './continuous';

describe('Continuous', () => {
  let component: Continuous;
  let fixture: ComponentFixture<Continuous>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Continuous]
    }).compileComponents();

    fixture = TestBed.createComponent(Continuous);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
