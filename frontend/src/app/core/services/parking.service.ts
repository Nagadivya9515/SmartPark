import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { DashboardResponse, SlotDetail } from 'src/app/shared/models/parking.models';

// This service previously also declared operator auth/entry/exit/live-status
// methods against `/api/parking/...` paths that never existed on the
// backend (those flows are served under `/api/operator/...` and are
// implemented for real in OperatorService). Trimmed to what this service
// actually owns: the end-user dashboard and slot lookup.
@Injectable({ providedIn: 'root' })
export class ParkingApiService {

  private http = inject(HttpClient);
  private readonly BASE_URL = 'http://localhost:8080/api/parking';

  private get authHeaders(): HttpHeaders {
    const token = localStorage.getItem('authToken') || '';
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  getDashboard(lotId: number = 1): Observable<DashboardResponse> {
    return this.http
      .get<DashboardResponse>(`${this.BASE_URL}/dashboard/${lotId}`, { withCredentials: true })
      .pipe(catchError(this.handleError));
  }

  // Slot + lot detail — used by the booking page when it's opened directly
  // (no dashboard router state to read from). Previously the frontend asked
  // for this at a bookings-service endpoint the backend never implemented.
  getSlotById(slotId: number | string): Observable<SlotDetail> {
    return this.http
      .get<SlotDetail>(`${this.BASE_URL}/slots/${slotId}`, { withCredentials: true })
      .pipe(catchError(this.handleError));
  }

  // Admin-only manual override — see DashboardController.
  toggleSlot(slotId: string): Observable<any> {
    return this.http.patch(
      `${this.BASE_URL}/slots/${slotId}/toggle`,
      {},
      { headers: this.authHeaders, withCredentials: true }
    );
  }

  private handleError(error: any): Observable<never> {
    console.error('API Error:', error);
    return throwError(() => error);
  }
}
