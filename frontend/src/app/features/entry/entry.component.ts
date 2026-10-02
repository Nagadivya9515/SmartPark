import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TicketResponse, VehicleType, LiveStatusResponse } from 'src/app/shared/models/operator.models';
import { OperatorService } from 'src/app/core/services/operator.service';
import { OperatorNavbarComponent } from 'src/app/shared/components/operator-navbar/operator-navbar.component';

interface VehicleOption {
  type: VehicleType;
  icon: string;
  label: string;
  freeSlots: number;
}

@Component({
  selector: 'app-entry',
  standalone: true,
  imports: [CommonModule, FormsModule, OperatorNavbarComponent],
  templateUrl: './entry.component.html',
  styleUrls: ['./entry.component.scss'],
})
export class EntryComponent implements OnInit {
  private router          = inject(Router);
  private operatorService = inject(OperatorService);

  activeTab     = signal<'entry' | 'ticket'>('entry');
  loading       = signal(false);
  error         = signal<string | null>(null);
  ticket        = signal<TicketResponse | null>(null);
  liveData      = signal<LiveStatusResponse | null>(null);
  qrSvg         = signal('');

  vehicleNumber    = '';
  selectedType     = signal<VehicleType>('CAR');
  gateName         = 'Gate 1';

  vehicleOptions: VehicleOption[] = [
    { type: 'CAR',   icon: 'car',   label: 'Car',   freeSlots: 45 },
    { type: 'BIKE',  icon: 'bike',  label: 'Bike',  freeSlots: 20 },
    { type: 'TRUCK', icon: 'truck', label: 'Truck', freeSlots: 5  },
  ];

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.operatorService.getLiveStatus().subscribe({
      next: d => {
        this.liveData.set(d);
        this.vehicleOptions[0].freeSlots = d.availableByType?.['CAR'] ?? 45;
        this.vehicleOptions[1].freeSlots = d.availableByType?.['BIKE'] ?? 20;
        this.vehicleOptions[2].freeSlots = d.availableByType?.['TRUCK'] ?? 5;
      }
    });
  }

  generateTicket(): void {
    if (!this.vehicleNumber.trim()) {
      this.error.set('Please enter a vehicle number');
      return;
    }
    this.loading.set(true);
    this.error.set(null);

    this.operatorService.generateEntry({
      vehicleNumber: this.vehicleNumber,
      vehicleType:   this.selectedType(),
      gateName:      this.gateName,
    }).subscribe({
      next: t => {
        this.ticket.set(t);
        this.qrSvg.set(this.buildQr(t.qrData ?? t.ticketId));
        this.loading.set(false);
        this.activeTab.set('ticket');
        this.loadStats();
      },
      error: err => {
        this.loading.set(false);
        this.error.set(err.error?.error ?? 'Failed to generate ticket');
      }
    });
  }

  processNext(): void {
    this.vehicleNumber = '';
    this.ticket.set(null);
    this.qrSvg.set('');
    this.error.set(null);
    this.activeTab.set('entry');
    this.loadStats();
  }

  goToLiveStatus(): void { this.router.navigate(['/operator/live-status']); }
  goToExit(): void { this.router.navigate(['/operator/exit']); }

  get todayEntries(): number { return this.liveData()?.todayEntries ?? 0; }
  get currentOccupancy(): number { return this.liveData()?.occupancyRate ?? 0; }

  // ── QR generator ──────────────────────────────────────────────────────
  buildQr(data: string): string {
    const size = 12;
    const seed = this.hash(data);
    const cells: boolean[][] = [];
    for (let r = 0; r < size; r++) {
      cells[r] = [];
      for (let c = 0; c < size; c++)
        cells[r][c] = ((seed * (r + 1) * (c + 1) * 31) % 7) < 4;
    }
    // finder patterns
    [[0,0],[0,1],[1,0],[0,size-1],[0,size-2],[1,size-1],
     [size-1,0],[size-2,0],[size-1,1]].forEach(([r,c]) => cells[r][c] = true);
    let svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${size*12} ${size*12}">
      <rect width="100%" height="100%" fill="white"/>`;
    for (let r = 0; r < size; r++)
      for (let c = 0; c < size; c++)
        if (cells[r][c])
          svg += `<rect x="${c*12}" y="${r*12}" width="10" height="10" rx="1" fill="#111827"/>`;
    return svg + '</svg>';
  }

  private hash(str: string): number {
    let h = 0;
    for (let i = 0; i < str.length; i++)
      h = (Math.imul(31, h) + str.charCodeAt(i)) | 0;
    return Math.abs(h);
  }
}