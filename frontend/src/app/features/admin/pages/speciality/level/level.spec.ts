import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Level } from './level';

describe('Level', () => {
  let component: Level;
  let fixture: ComponentFixture<Level>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Level]
    }).compileComponents();

    fixture = TestBed.createComponent(Level);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
