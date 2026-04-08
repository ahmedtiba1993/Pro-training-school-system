import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';

import {
  AcademicYearControllerService,
  LevelControllerService,
  SpecialtyControllerService,
  SpecialtyResponse
} from '../../../../core/api';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastService } from '../../../../shared/services/toast.service';
import { PaginationComponent } from '../../../../shared/components/pagination/pagination';

@Component({
  selector: 'app-training-session',
  standalone: true,
  imports: [],
  templateUrl: './training-session.html',
  styleUrl: './training-session.css'
})
export class TrainingSession {}
