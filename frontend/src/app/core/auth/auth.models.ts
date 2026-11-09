export type RoleCode = 'ADMIN' | 'CHEF_DEPARTEMENT' | 'CHEF_FILIERE' | 'ENSEIGNANT' | 'ETUDIANT';

export const ROLE_LABELS: Record<RoleCode, string> = {
  ADMIN: 'Administrateur',
  CHEF_DEPARTEMENT: 'Chef de département',
  CHEF_FILIERE: 'Chef de filière',
  ENSEIGNANT: 'Enseignant',
  ETUDIANT: 'Étudiant',
};

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface CurrentUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  active: boolean;
  roles: RoleCode[];
}