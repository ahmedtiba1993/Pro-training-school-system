import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PaginationComponent } from '../../../../../shared/components/pagination/pagination';
import { ToastService } from '../../../../../shared/services/toast.service';

@Component({
  selector: 'app-enrollment-list',
  standalone: true,
  imports: [],
  templateUrl: './enrollment-list.html',
  styleUrl: './enrollment-list.css'
})
export class EnrollmentList {}
