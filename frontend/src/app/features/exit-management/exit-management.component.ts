import { Component, OnInit, OnDestroy, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { OperatorService } from 'src/app/core/services/operator.service';
import { OperatorNavbarComponent } from 'src/app/shared/components/operator-navbar/operator-navbar.component';
export interface TicketSummary {
  ticketId: string;
  slotNumber: string;
  vehicleNumber: string;
  vehicleType: string;
  entryTime: string; // ISO string
}

export interface RecentExit {
  ticketId: string;
  vehicleNo: string;
  amount: number;
  time: string;
}

@Component({
  selector: 'app-exit-management',
  standalone: true,
  imports: [CommonModule, FormsModule, OperatorNavbarComponent],
  templateUrl: './exit-management.component.html',
  styleUrls: ['./exit-management.component.scss']
})
export class ExitManagementComponent implements OnInit, OnDestroy {

  private operatorApi = inject(OperatorService);
  private router = inject(Router);

  // ── State signals ──────────────────────────────────────────────────────────
  view = signal<'search' | 'summary' | 'done'>('search');
  ticket = signal<TicketSummary | null>(null);
  selectedPayment = signal<'CASH' | 'CARD' | 'UPI'>('CASH');
  isSearching = signal(false);
  isProcessing = signal(false);
  searchError = signal('');
  currentTime = signal(new Date().toISOString());

  // ── Search state ───────────────────────────────────────────────────────────
  searchQuery = '';

  // ── Pricing ───────────────────────────────────────────────────────────────
  readonly ratePerHour = 5;
  readonly serviceFee = 1.50;

  // ── Gate info (from auth) ──────────────────────────────────────────────────
  gateName = 'Gate 2';

  // ── Today's stats ──────────────────────────────────────────────────────────
  todayStats = signal({
    exited: 0,
    revenue: 0,
    avgDuration: '—',
    cashPayments: 0,
    digitalPayments: 0
  });

  recentExits = signal<RecentExit[]>([]);

  // ── Clock ──────────────────────────────────────────────────────────────────
  private clockInterval?: ReturnType<typeof setInterval>;

  // ── Computed ───────────────────────────────────────────────────────────────

  durationMs = computed(() => {
    const t = this.ticket();
    if (!t) return 0;
    return new Date(this.currentTime()).getTime() - new Date(t.entryTime).getTime();
  });

  durationHours = computed(() => Math.max(1, Math.ceil(this.durationMs() / (1000 * 60 * 60))));

  durationDisplay = computed(() => {
    const ms = this.durationMs();
    const h = Math.floor(ms / (1000 * 60 * 60));
    const m = Math.floor((ms % (1000 * 60 * 60)) / (1000 * 60));
    if (h === 0) return `${m}m`;
    if (m === 0) return `${h}h`;
    return `${h}h ${m}m`;
  });

  parkingFee = computed(() => this.durationHours() * this.ratePerHour);
  totalAmount = computed(() => this.parkingFee() + this.serviceFee);

  // ── Lifecycle ──────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.clockInterval = setInterval(() => {
      this.currentTime.set(new Date().toISOString());
    }, 30_000); // update every 30s

    this.loadGateInfo();
    this.loadTodayStats();
  }

  ngOnDestroy(): void {
    if (this.clockInterval) clearInterval(this.clockInterval);
  }

  // ── Gate / operator info ───────────────────────────────────────────────────

  private loadGateInfo(): void {
    try {
      // OperatorService stores operator payload under 'op_info'
      const operator = JSON.parse(localStorage.getItem('op_info') || '{}');
      if (operator?.gateName) {
        this.gateName = operator.gateName;
      }
    } catch { /* ignore */ }
  }

  // ── Stats ──────────────────────────────────────────────────────────────────

  private loadTodayStats(): void {
    // Backend provides /api/operator/exit/stats as { totalExits, revenueCollected, avgDurationHours }
    // UI expects a richer shape; map what we have.
    this.operatorApi.getExitStats().subscribe({
      next: (stats) => {
        this.todayStats.set({
          exited: stats.totalExits,
          revenue: stats.revenueCollected,
          avgDuration: `${stats.avgDurationHours}h`,
          cashPayments: 0,
          digitalPayments: 0,
        });
      },
      error: () => { /* keep defaults */ }
    });

    // this.api.getRecentExits(5).subscribe({
    //   next: (exits) => this.recentExits.set(exits),
    //   error: () => { /* keep empty */ }
    // });
  }

  // ── Search ─────────────────────────────────────────────────────────────────

  searchTicket(): void {
    const query = this.searchQuery.trim();
    if (!query) {
      this.searchError.set('Please enter a Ticket ID or Vehicle Number.');
      return;
    }

    this.isSearching.set(true);
    this.searchError.set('');

    // Backend expects query param name 'query' and returns a full TicketResponse.
    // Map into the lightweight TicketSummary used by this screen.
    this.operatorApi.searchTicket(query).subscribe({
      next: (ticket) => {
        this.isSearching.set(false);
        this.ticket.set({
          ticketId: ticket.ticketId,
          slotNumber: ticket.slotNumber,
          vehicleNumber: ticket.vehicleNumber,
          vehicleType: ticket.vehicleType,
          // Backend splits entryDate + entryTime; UI expects ISO-ish string.
          entryTime: ticket.entryTime ? `${ticket.entryDate ?? ''}T${ticket.entryTime}` : '',
        });
        this.currentTime.set(new Date().toISOString());
        this.selectedPayment.set('CASH');
        this.view.set('summary');
      },
      error: (err) => {
        this.isSearching.set(false);
        const msg = err?.error?.message || 'Ticket not found. Please check the ID or vehicle number.';
        this.searchError.set(msg);
      }
    });
  }

  // ── Summary actions ────────────────────────────────────────────────────────

  cancelSummary(): void {
    this.ticket.set(null);
    this.searchQuery = '';
    this.searchError.set('');
    this.view.set('search');
  }

  completeExit(): void {
    const t = this.ticket();
    if (!t) return;

    this.isProcessing.set(true);

    // Backend ExitRequest: { ticketId, paymentMethod, operatorId }.
    // OperatorService adds operatorId from logged-in operator.
    this.operatorApi.completeExit(t.ticketId, this.selectedPayment()).subscribe({
      next: () => {
        this.isProcessing.set(false);
        this.view.set('done');
        this.loadTodayStats(); // refresh sidebar stats
      },
      error: (err) => {
        this.isProcessing.set(false);
        alert(err?.error?.message || 'Failed to complete exit. Please try again.');
      }
    });
  }

  // ── Done actions ───────────────────────────────────────────────────────────

  printReceipt(): void {
    window.print();
  }

  startNew(): void {
    this.ticket.set(null);
    this.searchQuery = '';
    this.searchError.set('');
    this.selectedPayment.set('CASH');
    this.view.set('search');
  }

  // ── Alternative methods ───────────────────────────────────────────────────

  onQrScan(): void {
    alert('QR scanner integration coming soon. Please use manual entry.');
  }

  onBarcodeScan(): void {
    alert('Barcode scanner integration coming soon. Please use manual entry.');
  }
}