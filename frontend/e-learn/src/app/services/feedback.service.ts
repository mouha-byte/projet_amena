import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Feedback } from '../models/feedback.model';
import { API_GATEWAY_URL } from './api.config';

@Injectable({
  providedIn: 'root'
})
export class FeedbackService {
  private readonly baseUrl = `${API_GATEWAY_URL}/feedbacks`;

  constructor(private readonly http: HttpClient) {}

  getAll(): Observable<Feedback[]> {
    return this.http.get<Feedback[]>(this.baseUrl);
  }

  getById(id: number): Observable<Feedback> {
    return this.http.get<Feedback>(`${this.baseUrl}/${id}`);
  }

  create(payload: Feedback): Observable<Feedback> {
    return this.http.post<Feedback>(this.baseUrl, payload);
  }

  update(id: number, payload: Feedback): Observable<Feedback> {
    return this.http.put<Feedback>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
