import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Accelerated } from './accelerated';

describe('Accelerated', () => {
  let component: Accelerated;
  let fixture: ComponentFixture<Accelerated>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Accelerated]
    }).compileComponents();

    fixture = TestBed.createComponent(Accelerated);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
