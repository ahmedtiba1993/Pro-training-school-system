import { Component, computed, inject, signal } from '@angular/core';
import {
  RegistrationDocumentControllerService,
  RegistrationDocumentResponse
} from '../../../../core/api';

@Component({
  selector: 'app-registration-document',
  imports: [],
  templateUrl: './registration-document.html',
  styleUrl: './registration-document.css'
})
export class RegistrationDocument {
  // Inject the Swagger-generated service
  private documentService = inject(RegistrationDocumentControllerService);

  // Signals to manage the UI state
  documents = signal<RegistrationDocumentResponse[]>([]);
  isLoading = signal<boolean>(true);

  // Compute the total number of documents
  totalDocuments = computed(() => this.documents().length);

  // Compute the number of documents that have "isMandatory" set to true
  mandatoryDocumentsCount = computed(() => this.documents().filter(doc => doc.mandatory).length);

  ngOnInit(): void {
    this.fetchDocuments();
  }

  fetchDocuments(): void {
    this.isLoading.set(true);

    this.documentService.getAllDocuments().subscribe({
      next: response => {
        this.documents.set(response.data || []);
        this.isLoading.set(false);
      },
      error: error => {
        console.error('Error while fetching documents', error);
        this.isLoading.set(false);
      }
    });
  }
}
