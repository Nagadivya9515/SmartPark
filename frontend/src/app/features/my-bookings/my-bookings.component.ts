import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { BookingService } from '../../core/services/booking.service';
import { BookingResponse, MyBookingsSummary } from 'src/app/shared/models/booking.models';

@Component({
  selector: 'app-my-bookings',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-bookings.component.html',
  styleUrls: ['./my-bookings.component.scss'],
})
export class MyBookingsComponent implements OnInit {
  private router         = inject(Router);
  private bookingService = inject(BookingService);

  data      = signal<MyBookingsSummary | null>(null);
  loading   = signal(true);
  activeTab = signal<'active' | 'history'>('active');
  showQrFor = signal<string | null>(null);
  cancelling = signal<string | null>(null);

  qrSvgMap: Record<string, string> = {};

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.bookingService.getMyBookings().subscribe({
      next: d => { this.data.set(d); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  cancelBooking(ref: string): void {
    if (!confirm('Cancel this booking?')) return;
    this.cancelling.set(ref);
    this.bookingService.cancelBooking(ref).subscribe({
      next: () => { this.cancelling.set(null); this.load(); },
      error: () => this.cancelling.set(null),
    });
  }

  toggleQr(ref: string, qrData: string): void {
    if (this.showQrFor() === ref) {
      this.showQrFor.set(null);
      return;
    }
    if (!this.qrSvgMap[ref]) {
      this.qrSvgMap[ref] = this.buildQrSvg(qrData ?? ref);
    }
    this.showQrFor.set(ref);
  }

  private buildQrSvg(data: string): string {
    const size = 10;
    const seed = this.hashCode(data);
    let cells: boolean[][] = [];
    for (let r = 0; r < size; r++) {
      cells[r] = [];
      for (let c = 0; c < size; c++) {
        cells[r][c] = ((seed * (r + 1) * (c + 1) * 31) % 7) < 4;
      }
    }
    [[0,0],[0,1],[1,0],[0,size-1],[0,size-2],[1,size-1],
     [size-1,0],[size-2,0],[size-1,1]].forEach(([r,c]) => cells[r][c] = true);
    let svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${size*14} ${size*14}">
      <rect width="100%" height="100%" fill="white"/>`;
    for (let r = 0; r < size; r++)
      for (let c = 0; c < size; c++)
        if (cells[r][c])
          svg += `<rect x="${c*14}" y="${r*14}" width="12" height="12" rx="2" fill="#1a1d23"/>`;
    return svg + '</svg>';
  }

  private hashCode(str: string): number {
    let h = 0;
    for (let i = 0; i < str.length; i++)
      h = (Math.imul(31, h) + str.charCodeAt(i)) | 0;
    return Math.abs(h);
  }

  formatDate(d: string): string {
    return new Date(d).toLocaleDateString('en-GB', { day:'2-digit', month:'2-digit', year:'numeric' });
  }

  goDashboard(): void { this.router.navigate(['/dashboard']); }
}