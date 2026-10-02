import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { BookingResponse } from 'src/app/shared/models/booking.models';

@Component({
  selector: 'app-booking-confirm',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './booking-confirm.component.html',
  styleUrls: ['./booking-confirm.component.scss'],
})
export class BookingConfirmComponent implements OnInit {
  private router = inject(Router);

  booking = signal<BookingResponse | null>(null);
  qrSvg   = signal<string>('');

  ngOnInit(): void {
    const nav = this.router.getCurrentNavigation();
    const state = nav?.extras?.state as { booking: BookingResponse } | undefined;
    if (state?.booking) {
      this.booking.set(state.booking);
      this.generateQr(state.booking.qrData ?? state.booking.bookingRef);
    }
  }

  private generateQr(data: string): void {
    // Simple QR-like visual using the booking ref as seed
    // In production, use a library like angularx-qrcode
    const size = 10;
    const seed  = this.hashCode(data);
    let cells: boolean[][] = [];
    for (let r = 0; r < size; r++) {
      cells[r] = [];
      for (let c = 0; c < size; c++) {
        cells[r][c] = ((seed * (r + 1) * (c + 1) * 31) % 7) < 4;
      }
    }
    // Force finder patterns (corners)
    [[0,0],[0,1],[1,0],[0,size-1],[0,size-2],[1,size-1],
     [size-1,0],[size-2,0],[size-1,1]].forEach(([r,c]) => cells[r][c] = true);

    let svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${size*14} ${size*14}">`;
    svg += `<rect width="100%" height="100%" fill="white"/>`;
    for (let r = 0; r < size; r++) {
      for (let c = 0; c < size; c++) {
        if (cells[r][c]) {
          svg += `<rect x="${c*14}" y="${r*14}" width="12" height="12" rx="2" fill="#1a1d23"/>`;
        }
      }
    }
    svg += '</svg>';
    this.qrSvg.set(svg);
  }

  private hashCode(str: string): number {
    let h = 0;
    for (let i = 0; i < str.length; i++) {
      h = (Math.imul(31, h) + str.charCodeAt(i)) | 0;
    }
    return Math.abs(h);
  }

  formatDate(d: string): string {
    const date = new Date(d);
    return date.toLocaleDateString('en-US', { weekday: 'short', year: 'numeric', month: 'short', day: 'numeric' });
  }

  goToBookings(): void  { this.router.navigate(['/my-bookings']); }
  bookAnother(): void   { this.router.navigate(['/dashboard']); }
}