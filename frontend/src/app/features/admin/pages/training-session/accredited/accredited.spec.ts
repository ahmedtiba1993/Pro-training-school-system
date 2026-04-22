import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Accredited } from './accredited';

describe('Accredited', () => {
  let component: Accredited;
  let fixture: ComponentFixture<Accredited>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Accredited]
    }).compileComponents();

    fixture = TestBed.createComponent(Accredited);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
