import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { OperatorService } from 'src/app/core/services/operator.service';

@Component({
  selector: 'app-operator-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './operator-login.component.html',
  styleUrls: ['./operator-login.component.scss'],
})
export class OperatorLoginComponent {
  private router = inject(Router);
  private api    = inject(OperatorService);

  operatorId  = '';
  password    = '';
  rememberMe  = false;
  showPw      = false;
  loading     = signal(false);
  error       = signal<string | null>(null);

  onSubmit(): void {
    if (!this.operatorId.trim() || !this.password) {
      this.error.set('Please enter Operator ID and password');
      return;
    }
    this.loading.set(true);
    this.error.set(null);

    this.api.login({ operatorId: this.operatorId, password: this.password, rememberMe: this.rememberMe })
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.router.navigate(['/operator/entry']);
        },
        error: err => {
          this.loading.set(false);
          this.error.set(err.error?.error ?? 'Invalid credentials');
        }
      });
  }

  goBack(): void { this.router.navigate(['/']); }
}