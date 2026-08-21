import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { BookingService } from '../../core/services/booking.service';
import { ParkingApiService } from '../../core/services/parking.service';
import { CreateBookingRequest, VehicleDto } from '../../shared/models/booking.models';
import { SlotDetail } from '../../shared/models/parking.models';

/**
 * Booking now happens against a real registered vehicle rather than a
 * freeform plate/name typed on this page — the backend has always
 * validated that the vehicle belongs to the caller and matches the slot's
 * vehicle type, so a manually-typed plate that wasn't pre-registered would
 * fail with "Vehicle not found" every time. This page now lists the
 * caller's own vehicles (filtered to the slot's type) and lets them pick
 * one, the same way the "My Vehicles" list on the dashboard works.
 */
@Component({
  selector: 'app-book-slot',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './book-slot.component.html',
  styleUrls: ['./book-slot.component.scss'],
})
export class BookSlotComponent implements OnInit {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private bookingService = inject(BookingService);
  private parkingService = inject(ParkingApiService);

  slot = signal<SlotDetail | null>(null);
  vehicles = signal<VehicleDto[]>([]);
  selectedVehicleId = signal<number | null>(null);
  durationHours = signal(2);

  loading = signal(false);
  error = signal<string | null>(null);

  durationOptions = [1, 2, 3, 4, 6, 8, 12, 24];

  filteredVehicles = computed(() => {
    const type = this.slot()?.vehicleType;
    return this.vehicles().filter(v => v.vehicleType === type);
  });

  selectedVehicle = computed(() =>
    this.vehicles().find(v => v.id === this.selectedVehicleId()) ?? null
  );

  formattedSlotNumber = computed(() => this.slot()?.displayNumber ?? '...');

  totalAmount = computed(() => {
    const rate = this.slot()?.ratePerHour ?? 0;
    return rate * this.durationHours() + 1.50;
  });

  ngOnInit(): void {
    this.bookingService.getVehicles().subscribe(v => {
      this.vehicles.set(v);
      this.autoSelectVehicle();
    });

    // Prefer the slot the user just clicked on the dashboard; fall back to
    // fetching it from the backend if the page was opened directly by URL.
    const state = window.history.state as { slot?: SlotDetail };
    if (state?.slot) {
      this.slot.set(state.slot);
      return;
    }

    const slotId = this.route.snapshot.paramMap.get('id');
    if (!slotId) {
      this.error.set('No slot identifier found in URL.');
      return;
    }
    this.loading.set(true);
    this.parkingService.getSlotById(slotId).subscribe({
      next: (data) => {
        this.slot.set(data);
        this.loading.set(false);
        this.autoSelectVehicle();
      },
      error: () => {
        this.error.set('Failed to load slot details from server.');
        this.loading.set(false);
      },
    });
  }

  private autoSelectVehicle(): void {
    const match = this.filteredVehicles()[0];
    this.selectedVehicleId.set(match ? match.id : null);
  }

  confirmBooking(): void {
    const currentSlot = this.slot();
    const vehicle = this.selectedVehicle();

    if (!currentSlot) {
      this.error.set('Slot data is missing. Cannot proceed.');
      return;
    }
    if (!vehicle) {
      this.error.set('Select one of your registered vehicles to continue.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const req: CreateBookingRequest = {
      slotId: currentSlot.id,
      vehicleNumber: vehicle.vehicleNumber,
      durationHours: this.durationHours(),
    };

    this.bookingService.createBooking(req).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.router.navigate(['/confirmation'], { state: { booking: res } });
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.error || 'Booking failed.');
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}
