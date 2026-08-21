import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError } from 'rxjs';
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  UserInfo,
} from '../../shared/models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http   = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly API = 'http://localhost:8080/api/auth';

  // ── Reactive state via Angular Signals ────────────────────────────────
  private _user    = signal<UserInfo | null>(null);
  private _loading = signal<boolean>(false);
  private _error   = signal<string | null>(null);

  // Public read-only views
  readonly user       = this._user.asReadonly();
  readonly isLoggedIn = computed(() => this._user() !== null);
  readonly isLoading  = this._loading.asReadonly();
  readonly error      = this._error.asReadonly();

  // ── Register ───────────────────────────────────────────────────────────
  register(req: RegisterRequest): Observable<AuthResponse> {
    this._loading.set(true);
    this._error.set(null);
    return this.http.post<AuthResponse>(`${this.API}/register`, req).pipe(
      tap(() => this._loading.set(false)),
      catchError(err => {
        this._loading.set(false);
        const msg = err.error?.error || err.error?.message || 'Registration failed';
        this._error.set(msg);
        return throwError(() => new Error(msg));
      })
    );
  }

  // ── Login ──────────────────────────────────────────────────────────────
  login(req: LoginRequest): Observable<AuthResponse> {
    this._loading.set(true);
    this._error.set(null);
    return this.http
      .post<AuthResponse>(`${this.API}/login`, {
        ...req,
        deviceInfo: navigator.userAgent.substring(0, 100),
      }, { withCredentials: true })
      .pipe(
        tap(res => {
          this._user.set({ username: res.username, roles: res.role });
          this._loading.set(false);
          this.router.navigate(['/dashboard']);
        }),
        catchError(err => {
          this._loading.set(false);
          const msg = err.error?.error || 'Invalid username or password';
          this._error.set(msg);
          return throwError(() => new Error(msg));
        })
      );
  }

  // ── Refresh access token (called by interceptor on 401) ────────────────
  refreshToken(): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.API}/refresh`, {}, { withCredentials: true })
      .pipe(
        tap(res => this._user.set({ username: res.username, roles: res.role })),
        catchError(err => {
          this.clearSession();
          return throwError(() => err);
        })
      );
  }

  // ── Logout ─────────────────────────────────────────────────────────────
  logout(): void {
    this.http
      .post(`${this.API}/logout`, {}, { withCredentials: true })
      .subscribe({ complete: () => this.clearSession() });
  }

  logoutAll(): void {
    this.http
      .post(`${this.API}/logout-all`, {}, { withCredentials: true })
      .subscribe({ complete: () => this.clearSession() });
  }

  // ── Restore session on app load ────────────────────────────────────────
  // Called once in app initializer — tries /me to see if cookie is still valid
  restoreSession(): Observable<any> {
    return this.http
      .get<{ username: string; roles: string }>(`${this.API}/me`, { withCredentials: true })
      .pipe(
        tap(info => this._user.set(info)),
        catchError(() => {
          this._user.set(null);
          return throwError(() => null);
        })
      );
  }

  clearError(): void {
    this._error.set(null);
  }

  private clearSession(): void {
    this._user.set(null);
    this.router.navigate(['/login']);
  }
}
