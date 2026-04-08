import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { LevelControllerService } from '../../../../../core/api';

@Component({
  selector: 'app-enrollment-create',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './enrollment-create.html'
})
export class EnrollmentCreate {}
