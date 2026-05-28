import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import {
  RoomControllerService,
  RoomStatusStatsResponse,
  RoomResponse,
  RoomRequest
} from '../../../../core/api';
import { ToastService } from '../../../../shared/services/toast.service';

export enum RoomType {
  CLASSROOM = 'CLASSROOM',
  COMPUTER_LAB = 'COMPUTER_LAB',
  WORKSHOP = 'WORKSHOP',
  MEETING_ROOM = 'MEETING_ROOM'
}

export enum RoomStatus {
  DRAFT = 'DRAFT',
  ACTIVE = 'ACTIVE',
  MAINTENANCE = 'MAINTENANCE',
  ARCHIVED = 'ARCHIVED'
}

@Component({
  selector: 'app-room',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './room.html',
  styleUrl: './room.css'
})
export class Room implements OnInit {
  private roomsService = inject(RoomControllerService);
  private fb = inject(FormBuilder);
  private toastService = inject(ToastService);

  // --- SIGNALS D'ÉTAT PRINCIPAUX ---
  roomStats = signal<RoomStatusStatsResponse | null>(null);
  rooms = signal<RoomResponse[]>([]);

  isModalOpen = signal<boolean>(false);
  isEditModalOpen = signal<boolean>(false);
  editingRoomId = signal<number | null>(null);
  isSubmitting = signal<boolean>(false);

  isConfirmModalOpen = signal<boolean>(false);
  pendingRoomId = signal<number | null>(null);
  pendingStatus = signal<RoomStatus | null>(null);

  searchQuery = signal<string>('');
  selectedType = signal<string>('');
  selectedStatus = signal<string>('');

  roomTypeOptions = [
    { value: RoomType.CLASSROOM, label: 'Salle de Cours Standard' },
    { value: RoomType.COMPUTER_LAB, label: 'Laboratoire Informatique' },
    { value: RoomType.WORKSHOP, label: 'Atelier Technique' },
    { value: RoomType.MEETING_ROOM, label: 'Salle de Réunion / Soutenance' }
  ];

  roomStatusOptions = [
    { value: RoomStatus.DRAFT, label: "En cours d'aménagement" },
    { value: RoomStatus.ACTIVE, label: 'Disponible' },
    { value: RoomStatus.MAINTENANCE, label: 'En Maintenance' },
    { value: RoomStatus.ARCHIVED, label: 'Archivée' }
  ];

  roomForm = this.fb.nonNullable.group({
    name: ['', [Validators.required]],
    emplacement: ['', [Validators.required]],
    capacity: [30, [Validators.required, Validators.min(1)]],
    type: [RoomType.CLASSROOM, [Validators.required]],
    status: [RoomStatus.ACTIVE, [Validators.required]]
  });

  filteredRooms = computed(() => {
    let list = this.rooms();
    const query = this.searchQuery()
      .toLowerCase()
      .trim();
    const type = this.selectedType();
    const status = this.selectedStatus();

    if (query) {
      list = list.filter(
        room =>
          room.name?.toLowerCase().includes(query) ||
          room.code?.toLowerCase().includes(query) ||
          room.emplacement?.toLowerCase().includes(query)
      );
    }
    if (type) list = list.filter(room => room.type === type);
    if (status) list = list.filter(room => room.status === status);

    return list;
  });

  pendingStatusLabel = computed(() => {
    const status = this.pendingStatus();
    return this.roomStatusOptions.find(o => o.value === status)?.label ?? '';
  });

  ngOnInit(): void {
    this.refreshDashboard();
  }

  refreshDashboard(): void {
    this.roomsService.getRoomStatusStats().subscribe({
      next: response => {
        if (response?.data) this.roomStats.set(response.data);
      },
      error: err => console.error('Erreur stats:', err)
    });

    this.roomsService.getAllRooms().subscribe({
      next: response => {
        if (response?.data) this.rooms.set(response.data);
      },
      error: err => console.error('Erreur liste salles:', err)
    });
  }

  triggerStatusChange(id: number, newStatus: string): void {
    this.pendingRoomId.set(id);
    this.pendingStatus.set(newStatus as RoomStatus);
    this.isConfirmModalOpen.set(true);
  }

  cancelStatusChange(): void {
    this.isConfirmModalOpen.set(false);
    this.pendingRoomId.set(null);
    this.pendingStatus.set(null);
    this.rooms.set([...this.rooms()]);
  }

  confirmStatusChange(): void {
    const id = this.pendingRoomId();
    const newStatus = this.pendingStatus();
    if (id === null || !newStatus) return;

    this.roomsService.changeRoomStatus(id, newStatus as any).subscribe({
      next: () => {
        this.isConfirmModalOpen.set(false);
        this.pendingRoomId.set(null);
        this.pendingStatus.set(null);
        this.refreshDashboard();
        this.toastService.success('Le statut de la salle a été modifié.');
      },
      error: err => {
        this.isConfirmModalOpen.set(false);
        this.handleBackendError(err, 'Impossible de modifier le statut de la salle.');
      }
    });
  }

  openModal(): void {
    this.roomForm.reset({
      name: '',
      emplacement: '',
      capacity: 30,
      type: RoomType.CLASSROOM,
      status: RoomStatus.ACTIVE
    });
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
  }

  handleFormSubmit(): void {
    if (this.roomForm.invalid || this.isSubmitting()) return;
    this.isSubmitting.set(true);
    const requestPayload = (this.roomForm.getRawValue() as unknown) as RoomRequest;

    this.roomsService.createRoom(requestPayload).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.closeModal();
        this.refreshDashboard();
        this.toastService.success('La salle a été créée avec succès.');
      },
      error: err => {
        this.isSubmitting.set(false);
        this.handleBackendError(err, 'Une erreur est survenue lors de la création.');
      }
    });
  }

  openEditModal(room: RoomResponse): void {
    this.editingRoomId.set(room.id ?? null);
    this.roomForm.patchValue({
      name: room.name ?? '',
      emplacement: room.emplacement ?? '',
      capacity: room.capacity ?? 30,
      type: (room.type as RoomType) ?? RoomType.CLASSROOM,
      status: (room.status as RoomStatus) ?? RoomStatus.ACTIVE
    });
    this.isEditModalOpen.set(true);
  }

  closeEditModal(): void {
    this.isEditModalOpen.set(false);
    this.editingRoomId.set(null);
  }

  handleEditFormSubmit(): void {
    const roomId = this.editingRoomId();
    if (this.roomForm.invalid || this.isSubmitting() || roomId === null) return;

    this.isSubmitting.set(true);
    const requestPayload = (this.roomForm.getRawValue() as unknown) as RoomRequest;

    this.roomsService.updateRoom(roomId, requestPayload).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.closeEditModal();
        this.refreshDashboard();
        this.toastService.success('La salle a été modifiée avec succès.');
      },
      error: err => {
        this.isSubmitting.set(false);
        this.handleBackendError(err, 'Une erreur est survenue lors de la modification.');
      }
    });
  }

  getFriendlyType(type?: string): string {
    return this.roomTypeOptions.find(o => o.value === type)?.label ?? type ?? 'Inconnu';
  }

  /**
   * Centralise et traduit les messages d'erreurs techniques en français pour l'utilisateur
   */
  private handleBackendError(err: any, fallbackMessage: string): void {
    const backendMessage = err.error?.message;

    if (backendMessage === 'ROOM_NAME_ALREADY_EXISTS') {
      this.toastService.error('Une salle avec ce nom existe déjà.');
    } else {
      this.toastService.error(fallbackMessage);
    }
  }
}
