import { Component } from '@angular/core';
import { AccountSecurityComponent } from '../../../../shared/components/account-security/account-security';

@Component({
  selector: 'app-parent-security',
  standalone: true,
  imports: [AccountSecurityComponent],
  template: `<app-account-security class="flex-1 flex flex-col min-h-0 overflow-hidden" />`,
  host: {
    class: 'flex-1 flex flex-col min-h-0 overflow-hidden'
  }
})
export class ParentSecurity {}
