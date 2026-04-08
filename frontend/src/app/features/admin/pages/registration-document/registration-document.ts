import { Component, computed, inject, signal } from '@angular/core';
import { LevelControllerService } from '../../../../core/api';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastService } from '../../../../shared/services/toast.service';

@Component({
  selector: 'app-registration-document',
  imports: [ReactiveFormsModule],
  templateUrl: './registration-document.html',
  styleUrl: './registration-document.css'
})
export class RegistrationDocument {}
