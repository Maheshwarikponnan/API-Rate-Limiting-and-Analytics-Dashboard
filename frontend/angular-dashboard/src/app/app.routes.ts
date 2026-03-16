import { Routes } from '@angular/router';
import { DashboardComponent } from './dashboard/dashboard.component';
import { ApiManagementComponent } from './api-management/api-management.component';
import { ApiKeysComponent } from './api-keys/api-keys.component';
import { UsageAnalyticsComponent } from './usage-analytics/usage-analytics.component';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'api-management', component: ApiManagementComponent },
  { path: 'api-keys', component: ApiKeysComponent },
  { path: 'usage-analytics', component: UsageAnalyticsComponent },
];
