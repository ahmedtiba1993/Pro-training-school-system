import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

export interface Toast {
  id: number;
  type: ToastType;
  message: string;
}

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  private counter = 0;

  toasts = signal<Toast[]>([]);

  show(type: ToastType, message: string) {
    const id = ++this.counter;

    this.toasts.update((t) => [...t, { id, type, message }]);

    setTimeout(() => {
      this.remove(id);
    }, 4000);
  }

  success(message: string) {
    this.show('success', message);
  }

  error(message: string) {
    this.show('error', message);
  }

  remove(id: number) {
    this.toasts.update((t) => t.filter((x) => x.id !== id));
  }
}
