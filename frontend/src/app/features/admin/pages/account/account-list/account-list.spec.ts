import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { AccountList } from './account-list';
import { UserControllerService } from '../../../../../core/api/api/user-controller.service';

describe('AccountList', () => {
  let component: AccountList;
  let fixture: ComponentFixture<AccountList>;

  const mockUserApiService = {
    getAllUsersPaged: () =>
      of({
        data: {
          content: [],
          totalElements: 0,
          pageNumber: 0,
          pageSize: 10,
          totalPages: 0,
          isLast: true
        }
      }),
    suspendUser: () => of({}),
    activateUser: () => of({}),
    archiveUser: () => of({}),
    resetPassword: () => of({})
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountList],
      providers: [{ provide: UserControllerService, useValue: mockUserApiService }]
    }).compileComponents();

    fixture = TestBed.createComponent(AccountList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
