import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pagination.html',
})
export class PaginationComponent {
  // --- INPUTS ---
  @Input() currentPage: number = 0; // 0-based index
  @Input() total: number = 0; // Total number of items
  @Input() limit: number = 10; // Items per page

  // --- OUTPUTS ---
  @Output() changePage = new EventEmitter<number>();

  /**
   * Calculates the total number of pages based on total items and limit.
   */
  get totalPages(): number {
    return Math.ceil(this.total / this.limit) || 1;
  }

  /**
   * Generates a smart page array for the UI, e.g., [1, 2, '...', 10].
   */
  get pages(): (number | string)[] {
    const total = this.totalPages;
    const current = this.currentPage + 1; // Convert to 1-based index for display logic
    const delta = 1; // Number of pages to show around the current page

    const range: number[] = [];
    const rangeWithDots: (number | string)[] = [];
    let lastValue: number | undefined;

    // Determine which page numbers should be visible
    for (let i = 1; i <= total; i++) {
      if (i === 1 || i === total || (i >= current - delta && i <= current + delta)) {
        range.push(i);
      }
    }

    // Insert ellipses ("...") where there are gaps between numbers
    for (const i of range) {
      if (lastValue) {
        if (i - lastValue === 2) {
          rangeWithDots.push(lastValue + 1);
        } else if (i - lastValue !== 1) {
          rangeWithDots.push('...');
        }
      }
      rangeWithDots.push(i);
      lastValue = i;
    }

    return rangeWithDots;
  }

  // --- DISPLAY HELPERS (e.g., "Showing 1 to 10 of 50") ---

  get startItem(): number {
    if (this.total === 0) return 0;
    return this.currentPage * this.limit + 1;
  }

  get endItem(): number {
    if (this.total === 0) return 0;
    const end = (this.currentPage + 1) * this.limit;
    return end > this.total ? this.total : end;
  }

  // --- ACTIONS ---

  /**
   * Triggered when a specific page number is clicked.
   */
  onPageClick(page: number | string) {
    // If the element is an ellipsis ("..."), do nothing
    if (typeof page === 'string') return;

    // Convert display page (1-based) back to backend index (0-based)
    const pageIndex = page - 1;

    if (pageIndex >= 0 && pageIndex < this.totalPages && pageIndex !== this.currentPage) {
      this.changePage.emit(pageIndex);
    }
  }

  /**
   * Triggered when Previous or Next buttons are clicked.
   */
  onNextPrev(newPage: number) {
    if (newPage >= 0 && newPage < this.totalPages) {
      this.changePage.emit(newPage);
    }
  }
}
