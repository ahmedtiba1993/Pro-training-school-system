import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PromotionSubject } from './promotion-subject';

describe('PromotionSubject', () => {
  let component: PromotionSubject;
  let fixture: ComponentFixture<PromotionSubject>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PromotionSubject]
    }).compileComponents();

    fixture = TestBed.createComponent(PromotionSubject);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
