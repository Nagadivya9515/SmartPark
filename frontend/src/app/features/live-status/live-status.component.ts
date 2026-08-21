import { Component, OnInit, OnDestroy, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OperatorService } from 'src/app/core/services/operator.service';
import { FloorDto, SectionDto, SlotDto } from 'src/app/shared/models/operator.models';
import { OperatorNavbarComponent } from 'src/app/shared/components/operator-navbar/operator-navbar.component';

@Component({
  selector: 'app-live-status',
  standalone: true,
  imports: [CommonModule, OperatorNavbarComponent],
  templateUrl: './live-status.component.html',
  styleUrls: ['./live-status.component.scss']
})
export class LiveStatusComponent implements OnInit, OnDestroy {

  private api = inject(OperatorService);

  // ── State ──────────────────────────────────────────────────────────────────
  stats = signal({ totalSlots: 0, occupied: 0, available: 0, occupancyRate: 0, todayEntries: 0 });
  floors = signal<FloorDto[]>([]);
  selectedFloorIndex = signal(0);
  isLoading = signal(true);
  autoRefresh = signal(true);
  lastRefreshed = signal<Date>(new Date());

  // ── Auto-refresh ───────────────────────────────────────────────────────────
  private refreshInterval?: ReturnType<typeof setInterval>;
  private readonly REFRESH_INTERVAL_MS = 30_000;

  // ── Computed ───────────────────────────────────────────────────────────────
  selectedFloor = computed(() => {
    const fs = this.floors();
    return fs[this.selectedFloorIndex()] ?? null;
  });

  occupancyColor = computed(() => {
    const rate = this.stats().occupancyRate;
    if (rate < 50) return '#16a34a';
    if (rate < 80) return '#d97706';
    return '#dc2626';
  });

  // ── Lifecycle ──────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.fetchData();
    this.startAutoRefresh();
  }

  ngOnDestroy(): void {
    this.stopAutoRefresh();
  }

  // ── Data ───────────────────────────────────────────────────────────────────

  fetchData(): void {
    this.isLoading.set(true);
    this.api.getLiveStatus().subscribe({
      next: (data) => {
        // Backend /api/operator/live-status returns fields directly, not nested under "stats".
        this.stats.set({
          totalSlots: data.totalSlots,
          occupied: data.occupied,
          available: data.available,
          occupancyRate: data.occupancyRate,
          todayEntries: data.todayEntries,
        });
        this.floors.set(data.floors);
        this.lastRefreshed.set(new Date());
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        // Keep previous data on error
      }
    });
  }

  onRefresh(): void {
    this.fetchData();
  }

  toggleAutoRefresh(): void {
    this.autoRefresh.update(v => !v);
    if (this.autoRefresh()) {
      this.startAutoRefresh();
    } else {
      this.stopAutoRefresh();
    }
  }

  private startAutoRefresh(): void {
    this.stopAutoRefresh();
    this.refreshInterval = setInterval(() => {
      if (this.autoRefresh()) this.fetchData();
    }, this.REFRESH_INTERVAL_MS);
  }

  private stopAutoRefresh(): void {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
      this.refreshInterval = undefined;
    }
  }

  // ── UI helpers ─────────────────────────────────────────────────────────────

  selectFloor(index: number): void {
    this.selectedFloorIndex.set(index);
  }

  // Slot model comes from backend OperatorDtos.SlotDto; use id as stable key.
  trackBySlot(_: number, slot: SlotDto): number {
    return slot.id;
  }

  trackBySection(_: number, section: SectionDto): string {
    return section.sectionName;
  }

  vehicleIcon(type: string): string {
    switch (type) {
      case 'CAR': return '🚗';
      case 'BIKE': return '🚲';
      case 'TRUCK': return '🚛';
      default: return '🚗';
    }
  }

  occupancyBarWidth(floor: FloorDto): string {
    return `${floor.occupancyRate}%`;
  }

  occupancyBarColor(floor: FloorDto): string {
    if (floor.occupancyRate < 50) return '#16a34a';
    if (floor.occupancyRate < 80) return '#d97706';
    return '#dc2626';
  }

  formatLastRefreshed(): string {
    return this.lastRefreshed().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }
}