import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'missions', pathMatch: 'full' },
  {
    path: 'missions',
    loadComponent: () =>
      import('./pages/missions/missions.component').then((m) => m.MissionsComponent)
  },
  {
    path: 'missions/new',
    loadComponent: () =>
      import('./pages/mission-form/mission-form.component').then((m) => m.MissionFormComponent)
  },
  {
    path: 'missions/:id/edit',
    loadComponent: () =>
      import('./pages/mission-form/mission-form.component').then((m) => m.MissionFormComponent)
  },
  {
    path: 'missions/:id/candidatures',
    loadComponent: () =>
      import('./pages/mission-candidatures/mission-candidatures.component').then(
        (m) => m.MissionCandidaturesComponent
      )
  },
  {
    path: 'missions/:id',
    loadComponent: () =>
      import('./pages/mission-detail/mission-detail.component').then((m) => m.MissionDetailComponent)
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./pages/dashboard/dashboard.component').then((m) => m.DashboardComponent)
  },
  {
    path: 'ia',
    loadComponent: () =>
      import('./pages/ia-hub/ia-hub.component').then((m) => m.IaHubComponent)
  },
  { path: '**', redirectTo: 'missions' }
];
