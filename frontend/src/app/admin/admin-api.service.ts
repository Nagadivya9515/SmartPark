import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import {
  AdminLoginRequest, AdminLoginResponse, OverviewResponse,
  ParkingLotDto, OperatorSummaryResponse, OperatorDto,
  ReportSummaryResponse
} from '../shared/models/admin.models';

const TOKEN_KEY = 'admin_token';
const INFO_KEY  = 'admin_info';

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http   = inject(HttpClient);
  private readonly router = inject(Router);
  // Was pointed at :8081 — the backend (including AdminController) serves
  // everything, admin included, on :8080. Every admin call 404'd as shipped.
  private readonly API    = 'http://localhost:8080/api/admin';

  currentAdmin = signal<AdminLoginResponse | null>(this.loadStored());

  get isLoggedIn() { return !!localStorage.getItem(TOKEN_KEY); }

  private headers(): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY) ?? ''}` });
  }

  login(req: AdminLoginRequest): Observable<AdminLoginResponse> {
    return this.http.post<AdminLoginResponse>(`${this.API}/auth/login`, req).pipe(
      tap(res => {
        localStorage.setItem(TOKEN_KEY, res.token);
        localStorage.setItem(INFO_KEY, JSON.stringify(res));
        this.currentAdmin.set(res);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(INFO_KEY);
    this.currentAdmin.set(null);
    this.router.navigate(['/admin/login']);
  }

  getOverview(): Observable<OverviewResponse> {
    return this.http.get<OverviewResponse>(`${this.API}/overview`, { headers: this.headers() });
  }

  getParkingLots(): Observable<ParkingLotDto[]> {
    return this.http.get<ParkingLotDto[]>(`${this.API}/parking-lots`, { headers: this.headers() });
  }

  getOperators(): Observable<OperatorSummaryResponse> {
    return this.http.get<OperatorSummaryResponse>(`${this.API}/operators`, { headers: this.headers() });
  }

  createOperator(req: any): Observable<OperatorDto> {
    return this.http.post<OperatorDto>(`${this.API}/operators`, req, { headers: this.headers() });
  }

  toggleOperator(id: number): Observable<OperatorDto> {
    return this.http.patch<OperatorDto>(`${this.API}/operators/${id}/toggle`, {}, { headers: this.headers() });
  }

  deleteOperator(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/operators/${id}`, { headers: this.headers() });
  }

  getReports(days = 7): Observable<ReportSummaryResponse> {
    return this.http.get<ReportSummaryResponse>(`${this.API}/reports?days=${days}`, { headers: this.headers() });
  }

  private loadStored(): AdminLoginResponse | null {
    try { const s = localStorage.getItem(INFO_KEY); return s ? JSON.parse(s) : null; }
    catch { return null; }
  }
}
