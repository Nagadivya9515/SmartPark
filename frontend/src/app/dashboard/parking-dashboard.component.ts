
import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { ParkingApiService } from '../core/services/parking.service';
import { DashboardResponse } from '../shared/models/parking.models';
import { interval, Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-parking-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './parking-dashboard.component.html',
  styleUrls: ['./parking-dashboard.component.scss'],
})
export class ParkingDashboardComponent implements OnInit, OnDestroy {
  private readonly parkingService = inject(ParkingApiService);
  private readonly router = inject(Router);


  data = signal<DashboardResponse | null>(null);
  loading = signal(true);
  // Matches the floor names the backend actually seeds ("Floor 1"/"Floor 2")
  // — this used to say 'GROUND' | 'FIRST', which matched the dashboard
  // module's own (now-removed) seeder but not the unified inventory's.
  activeFloor = signal<'Floor 1' | 'Floor 2'>('Floor 1');

  private pollSub?: Subscription;

  // Use a computed signal for filtered sections to keep the template clean
  filteredSections = computed(() => {
    const currentData = this.data();
    if (!currentData) return [];
    return currentData.sections.filter(s => s.floor === this.activeFloor());
  });

  ngOnInit(): void {
    this.load();
    // Live updates every 30s
    const lotId = 1;
    this.pollSub = interval(30000)
      .pipe(switchMap(() => this.parkingService.getDashboard(lotId)))
      .subscribe(res => this.data.set(res));
      
  }
  

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }

  load(): void {
    this.loading.set(true);

    // Single lot for now — there's only ever one seeded lot; a lot picker
    // would need a "list lots" endpoint this app doesn't have yet.
    const lotId = 1;

    this.parkingService.getDashboard(lotId).subscribe({
      next: res => {
        this.data.set(res);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  setFloor(floor: 'Floor 1' | 'Floor 2'): void {
    this.activeFloor.set(floor);
  }

  /**
   * A free slot takes the user to booking. An occupied slot does nothing —
   * this used to call the slot-toggle endpoint to forcibly free it up,
   * which meant any logged-in end user could clear (or occupy) any slot in
   * the lot, including one someone else had booked. That's now an
   * admin-only override on the backend (see DashboardController) and isn't
   * exposed in the end-user dashboard at all.
   */
  selectSlot(slotId: number, event: Event): void {
    event.stopPropagation();

    const allSlots = this.data()?.sections.flatMap(s => s.slots) || [];
    const targetSlot = allSlots.find(s => s.id === slotId);
    if (!targetSlot || targetSlot.occupied) return;

    this.router.navigate(['/booking', slotId]);
  }

  vehicleIcon(type: string): string {
    const icons: Record<string, string> = { 'CAR': '🚗', 'BIKE': '🚲', 'TRUCK': '🚛' };
    return icons[type] || '🅿️';
  }

  progressWidth(available: number, total: number): string {
    if (total === 0) return '0%';
    const occupancy = ((total - available) / total) * 100;
    return `${occupancy}%`;
  }

  progressColor(type: string): string {
    const colors: Record<string, string> = { 'CAR': '#22c55e', 'BIKE': '#3b82f6', 'TRUCK': '#8b5cf6' };
    return colors[type] || '#6366f1';
  }
  
}