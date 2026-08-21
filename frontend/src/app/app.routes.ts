import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { operatorAuthGuard } from './core/guards/operator-auth.guard';
import { adminGuard } from './admin/admin.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(m => m.LoginComponent),
    canActivate: [guestGuard], // redirect to dashboard if already logged in
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then(m => m.RegisterComponent),
    canActivate: [guestGuard],
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./dashboard/parking-dashboard.component').then(m => m.ParkingDashboardComponent),
    canActivate: [authGuard], // protected route
  },
  {
    path: 'admin',
    loadComponent: () => import('./admin/admin-layout.component').then(m => m.AdminLayoutComponent),
    canActivate: [adminGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./admin/pages/admin-dashboard.component').then(m => m.AdminDashboardComponent),
      },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
    ],
  },
  {
    path: 'bookings',
    loadComponent: () => 
      import('./features/my-bookings/my-bookings.component')
        .then(m => m.MyBookingsComponent)
  },
  {
    path: 'confirmation',
    loadComponent: () => 
      import('./features/booking-confirm/booking-confirm.component')
        .then(m => m.BookingConfirmComponent)
  },
  {
  path: 'booking/:id', // Define the ID parameter here
  loadComponent: () => import('./features/book-slot/book-slot.component').then(m => m.BookSlotComponent),
  canActivate: [authGuard]
  },
      // NOTE: an unguarded '/entry' route used to sit here, reaching the
      // same EntryComponent that generates real parking tickets — with no
      // operatorAuthGuard, anyone (logged in or not) could open it. Gate
      // entry now only exists behind '/operator/entry' below.
      {
        path: 'live-status',
        loadComponent: () =>
          import('./features/live-status/live-status.component').then(m => m.LiveStatusComponent),
      },
      {
        path: 'operator/live-status',
        loadComponent: () =>
          import('./features/live-status/live-status.component').then(m => m.LiveStatusComponent),
        canActivate: [operatorAuthGuard],
      },
      {
        path: 'operator/login',
        loadComponent: () =>
          import('./features/operator-login/operator-login.component').then(m => m.OperatorLoginComponent),
        canActivate: [guestGuard],
      },
      {
        path: 'operator/entry',
        loadComponent: () =>
          import('./features/entry/entry.component').then(m => m.EntryComponent),
        canActivate: [operatorAuthGuard],
      },
      {
        path: 'operator/exit',
        loadComponent: () =>
          import('./features/exit-management/exit-management.component').then(m => m.ExitManagementComponent),
        canActivate: [operatorAuthGuard],
      },
      // {
      //   path: 'operator/exit',
      //   loadComponent: () =>
      //     import('./features/exit/exit.component').then(m => m.ExitComponent),
      //   canActivate: [authGuard],
      // }
     
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
