import { Component, inject, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { OperatorService } from 'src/app/core/services/operator.service';

export type OperatorNavActive = 'entry' | 'exit' | 'live';

@Component({
  selector: 'app-operator-navbar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './operator-navbar.component.html',
  styleUrls: ['./operator-navbar.component.scss'],
})
export class OperatorNavbarComponent {
  private readonly router = inject(Router);
  private readonly operatorService = inject(OperatorService);

  active = input<OperatorNavActive>('entry');

  goEntry(): void { this.router.navigate(['/operator/entry']); }
  goExit(): void { this.router.navigate(['/operator/exit']); }
  goLive(): void { this.router.navigate(['/operator/live-status']); }

  logout(): void { this.operatorService.logout(); }
}

