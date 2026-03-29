export interface Feedback {
  id?: number;
  userName: string;
  userEmail: string;
  title: string;
  imageUrl?: string;
  rating: number;
  comment: string;
  createdAt?: string;
}
