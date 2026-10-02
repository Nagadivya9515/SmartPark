// ── Request models ─────────────────────────────────────────────────────────
export interface LoginRequest {
  username: string;
  password: string;
  deviceInfo?: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

// ── Response models ────────────────────────────────────────────────────────
export interface AuthResponse {
  message: string;
  username: string;
  role: string;
}

export interface UserInfo {
  username: string;
  roles: string;
}

// ── App state ──────────────────────────────────────────────────────────────
export interface AuthState {
  user: UserInfo | null;
  isLoggedIn: boolean;
  isLoading: boolean;
  error: string | null;
}
