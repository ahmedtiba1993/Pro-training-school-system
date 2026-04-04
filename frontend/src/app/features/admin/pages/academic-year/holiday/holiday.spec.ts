import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Holiday } from './holiday';

describe('Holiday', () => {
  let component: Holiday;
  let fixture: ComponentFixture<Holiday>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Holiday]
    }).compileComponents();

    fixture = TestBed.createComponent(Holiday);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
