import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AdminApiService } from '../admin-api.service';

@Component({
  selector: 'app-admin-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-login.component.html',
  styleUrls: ['./admin-login.component.scss'],
})
export class AdminLoginComponent {
  private router = inject(Router);
  private api    = inject(AdminApiService);

  username   = '';
  password   = '';
  rememberMe = false;
  showPw     = false;
  loading    = signal(false);
  error      = signal<string | null>(null);

  onSubmit(): void {
    if (!this.username || !this.password) {
      this.error.set('Please enter username and password');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.api.login({ username: this.username, password: this.password, rememberMe: this.rememberMe })
      .subscribe({
        next: () => { this.loading.set(false); this.router.navigate(['/admin/dashboard']); },
        error: err => { this.loading.set(false); this.error.set(err.error?.error ?? 'Invalid credentials'); }
      });
  }
}