import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import {
  LoginRequest, LoginResponse, TicketResponse,
  LiveStatusResponse, ExitStatsResponse, EntryRequest, VehicleType
} from 'src/app/shared/models/operator.models';

const TOKEN_KEY    = 'op_token';
const OPERATOR_KEY = 'op_info';

@Injectable({ providedIn: 'root' })
export class OperatorService {
  private readonly http   = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly API    = 'http://localhost:8080/api/operator';

  currentOperator = signal<LoginResponse | null>(this.loadStoredOperator());

  // ── Auth ───────────────────────────────────────────────────────────────
  login(req: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.API}/auth/login`, req).pipe(
      tap(res => {
        localStorage.setItem(TOKEN_KEY, res.token);
        localStorage.setItem(OPERATOR_KEY, JSON.stringify(res));
        this.currentOperator.set(res);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(OPERATOR_KEY);
    this.currentOperator.set(null);
    this.router.navigate(['/operator/login']);
  }

  get isLoggedIn(): boolean {
    return !!localStorage.getItem(TOKEN_KEY);
  }

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem(TOKEN_KEY) ?? '';
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  // ── Entry ──────────────────────────────────────────────────────────────
generateEntry(request: EntryRequest): Observable<TicketResponse> {
  return this.http.post<TicketResponse>(`${this.API}/entry`,
    { 
      ...request, // spreads vehicleNumber, vehicleType, gateName
      operatorId: this.currentOperator()?.operatorId 
    },
    { headers: this.getHeaders() }
  );
}

  // ── Exit ───────────────────────────────────────────────────────────────
  searchTicket(query: string): Observable<TicketResponse> {
    return this.http.get<TicketResponse>(`${this.API}/exit/search?query=${query}`,
      { headers: this.getHeaders() });
  }

  completeExit(ticketId: string, paymentMethod: string): Observable<TicketResponse> {
    return this.http.post<TicketResponse>(`${this.API}/exit/complete`,
      { ticketId, paymentMethod,
        operatorId: this.currentOperator()?.operatorId },
      { headers: this.getHeaders() });
  }

  getExitStats(): Observable<ExitStatsResponse> {
    return this.http.get<ExitStatsResponse>(`${this.API}/exit/stats`,
      { headers: this.getHeaders() });
  }

  // ── Live status ────────────────────────────────────────────────────────
  getLiveStatus(): Observable<LiveStatusResponse> {
    return this.http.get<LiveStatusResponse>(`${this.API}/live-status`,
      { headers: this.getHeaders() });
  }

  private loadStoredOperator(): LoginResponse | null {
    try {
      const s = localStorage.getItem(OPERATOR_KEY);
      return s ? JSON.parse(s) : null;
    } catch { return null; }
  }

   
  
}