import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FinancialContractList } from './financial-contract-list';

describe('FinancialContractList', () => {
  let component: FinancialContractList;
  let fixture: ComponentFixture<FinancialContractList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FinancialContractList]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FinancialContractList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
