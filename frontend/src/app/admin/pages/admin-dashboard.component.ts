import { Component, OnInit, inject, signal, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminApiService } from '../admin-api.service';
import { OverviewResponse } from '../../shared/models/admin.models';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss'],
})
export class AdminDashboardComponent implements OnInit {
  private api = inject(AdminApiService);

  data    = signal<OverviewResponse | null>(null);
  loading = signal(true);

  ngOnInit(): void {
    this.api.getOverview().subscribe({
      next: d => { this.data.set(d); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  get revenuePct(): number {
    const d = this.data();
    if (!d || !d.yesterdayRevenue) return 8.3;
    return Math.round(((d.todayRevenue - d.yesterdayRevenue) / d.yesterdayRevenue) * 1000) / 10;
  }

  get revenuePctPositive(): boolean { return this.revenuePct >= 0; }

  get totalOperatorsDisplay(): string {
    const d = this.data();
    return d ? `${d.activeOperators}/${d.totalOperators}` : '–';
  }

  // Chart helpers — build inline SVG path from data points
  buildLinePath(points: {value: number}[], w: number, h: number, pad = 20): string {
    if (!points?.length) return '';
    const max   = Math.max(...points.map(p => p.value), 1);
    const min   = Math.min(...points.map(p => p.value));
    const range = max - min || 1;
    const xs    = points.map((_, i) => pad + (i / (points.length - 1)) * (w - pad * 2));
    const ys    = points.map(p => h - pad - ((p.value - min) / range) * (h - pad * 2));
    return xs.map((x, i) => `${i === 0 ? 'M' : 'L'}${x.toFixed(1)},${ys[i].toFixed(1)}`).join(' ');
  }

  buildAreaPath(points: {value: number}[], w: number, h: number, pad = 20): string {
    const line = this.buildLinePath(points, w, h, pad);
    if (!line) return '';
    const lastX = pad + w - pad * 2;
    return `${line} L${lastX},${h - pad} L${pad},${h - pad} Z`;
  }

  yTicks(points: {value: number}[], count = 5): number[] {
    if (!points?.length) return [];
    const max = Math.max(...points.map(p => p.value), 1);
    return Array.from({length: count}, (_, i) => Math.round((max / (count - 1)) * i));
  }

  formatDate(d: string): string {
    const dt = new Date(d);
    return dt.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }

  get carsOccupied():   number { return this.data()?.occupancyByType?.['CAR']   ?? 27; }
  get bikesOccupied():  number { return this.data()?.occupancyByType?.['BIKE']  ?? 8;  }
  get trucksOccupied(): number { return this.data()?.occupancyByType?.['TRUCK'] ?? 5;  }
  get totalOccupied():  number { return this.carsOccupied + this.bikesOccupied + this.trucksOccupied; }

  // Donut chart arc
  buildArc(value: number, total: number, offset: number, r = 60): string {
    const circ = 2 * Math.PI * r;
    const pct  = total > 0 ? value / total : 0;
    return `${(pct * circ).toFixed(2)} ${circ.toFixed(2)}`;
  }
}