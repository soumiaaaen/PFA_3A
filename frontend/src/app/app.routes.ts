import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login').then(m => m.Login),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./core/layout/shell/shell').then(m => m.Shell),
    children: [
      {
        path: '',
        loadComponent: () => import('./features/dashboard/dashboard').then(m => m.Dashboard),
      },
      {
        path: 'comptes',
        canActivate: [roleGuard('ADMIN')],
        loadComponent: () => import('./features/accounts/accounts').then(m => m.Accounts),
      },
            {
        path: 'calendrier',
        canActivate: [roleGuard('ADMIN')],
        loadComponent: () => import('./features/calendar/calendar').then(m => m.Calendar),
      },
            {
        path: 'organisation',
        canActivate: [roleGuard('ADMIN')],
        loadComponent: () => import('./features/organization/organization').then(m => m.Organization),
      },
            {
        path: 'etudiants',
        canActivate: [roleGuard('ADMIN', 'CHEF_FILIERE')],
        loadComponent: () => import('./features/students/students').then(m => m.Students),
      },
            {
        path: 'modules',
        canActivate: [roleGuard('ADMIN', 'CHEF_FILIERE', 'CHEF_DEPARTEMENT', 'ENSEIGNANT')],
        loadComponent: () => import('./features/catalog/catalog').then(m => m.Catalog),
      },
            {
        path: 'affectations',
        canActivate: [roleGuard('ADMIN', 'CHEF_FILIERE', 'CHEF_DEPARTEMENT')],
        loadComponent: () => import('./features/assignments/assignments').then(m => m.Assignments),
      },
            {
        path: 'audit',
        canActivate: [roleGuard('ADMIN')],
        loadComponent: () => import('./features/audit/audit').then(m => m.Audit),
      },
      // Chaque membre de l'équipe ajoute ses pages ici, sur le même modèle.
      {
        path: '**',
        loadComponent: () => import('./features/coming-soon/coming-soon').then(m => m.ComingSoon),
      },
    ],
  },
];