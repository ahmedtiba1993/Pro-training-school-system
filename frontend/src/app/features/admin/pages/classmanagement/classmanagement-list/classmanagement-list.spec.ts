import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ClassmanagementList } from './classmanagement-list';

describe('ClassmanagementList', () => {
  let component: ClassmanagementList;
  let fixture: ComponentFixture<ClassmanagementList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ClassmanagementList]
    }).compileComponents();

    fixture = TestBed.createComponent(ClassmanagementList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
