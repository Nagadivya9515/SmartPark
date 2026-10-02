import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  BookingResponse, CreateBookingRequest,
  MyBookingsSummary, VehicleDto
} from 'src/app/shared/models/booking.models';

@Injectable({ providedIn: 'root' })
export class BookingService {
  private readonly http = inject(HttpClient);
  private readonly API  = 'http://localhost:8080/api/bookings';

  createBooking(req: CreateBookingRequest): Observable<BookingResponse> {
    return this.http.post<BookingResponse>(this.API, req, { withCredentials: true });
  }

  getBooking(ref: string): Observable<BookingResponse> {
    return this.http.get<BookingResponse>(`${this.API}/${ref}`, { withCredentials: true });
  }

  getMyBookings(): Observable<MyBookingsSummary> {
    return this.http.get<MyBookingsSummary>(`${this.API}/my`, { withCredentials: true });
  }

  cancelBooking(ref: string): Observable<BookingResponse> {
    return this.http.patch<BookingResponse>(
      `${this.API}/${ref}/cancel`, {}, { withCredentials: true });
  }

  getVehicles(): Observable<VehicleDto[]> {
    return this.http.get<VehicleDto[]>(`${this.API}/vehicles`, { withCredentials: true });
  }
}
