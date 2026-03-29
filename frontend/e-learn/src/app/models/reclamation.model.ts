export type ReclamationStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED';

export interface Reclamation {
  id?: number;
  userName: string;
  userEmail: string;
  subject: string;
  imageUrl?: string;
  description: string;
  status: ReclamationStatus;
  createdAt?: string;
}
