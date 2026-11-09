import { RoleCode } from '../auth/auth.models';
import { IconName } from '../../shared/icon/icon';

export interface NavItem {
  label: string;
  path: string;
  icon: IconName;
  description: string;
  /** Rôles autorisés. Absent = visible par tous les utilisateurs connectés. */
  roles?: RoleCode[];
}

export interface NavSection {
  title: string;
  items: NavItem[];
}

/**
 * Menu de l'application. Chaque membre de l'équipe ajoute ici les entrées de son domaine.
 * Le menu masque les pages selon les rôles ; la vraie sécurité reste dans le backend.
 */
export const NAVIGATION: NavSection[] = [
  {
    title: 'Général',
    items: [
      { label: 'Tableau de bord', path: '/', icon: 'home', description: 'Vue d’ensemble' },
    ],
  },
  {
    title: 'Administration',
    items: [
      { label: 'Comptes', path: '/comptes', icon: 'users', description: 'Utilisateurs et rôles', roles: ['ADMIN'] },
      { label: 'Calendrier', path: '/calendrier', icon: 'calendar', description: 'Années, semestres et périodes', roles: ['ADMIN'] },
      { label: 'Organisation', path: '/organisation', icon: 'layers', description: 'Départements, filières, groupes', roles: ['ADMIN'] },
            { label: 'Étudiants', path: '/etudiants', icon: 'user', description: 'Profils et inscriptions aux groupes', roles: ['ADMIN', 'CHEF_FILIERE'] },
      { label: 'Journal d’audit', path: '/audit', icon: 'shield', description: 'Traçabilité des actions', roles: ['ADMIN'] },
    ],
  },
  {
    title: 'Pédagogie',
    items: [
      { label: 'Modules et éléments', path: '/modules', icon: 'book', description: 'Descriptifs et objectifs', roles: ['ADMIN', 'CHEF_FILIERE', 'CHEF_DEPARTEMENT', 'ENSEIGNANT'] },
      { label: 'Affectations', path: '/affectations', icon: 'clipboard', description: 'Enseignants, groupes, volumes', roles: ['ADMIN', 'CHEF_FILIERE', 'CHEF_DEPARTEMENT'] },
      { label: 'Emploi du temps', path: '/emploi-du-temps', icon: 'clock', description: 'Séances de la semaine' },
      { label: 'Absences', path: '/absences', icon: 'check', description: 'Appel et justificatifs' },
      { label: 'Ressources', path: '/ressources', icon: 'folder', description: 'Cours et supports' },
      { label: 'Assistant IA', path: '/assistant', icon: 'sparkles', description: 'Questions sur vos cours' },
    ],
  },
];

export function visibleSections(roles: RoleCode[]): NavSection[] {
  return NAVIGATION
    .map(section => ({
      ...section,
      items: section.items.filter(item => !item.roles || item.roles.some(r => roles.includes(r))),
    }))
    .filter(section => section.items.length > 0);
}