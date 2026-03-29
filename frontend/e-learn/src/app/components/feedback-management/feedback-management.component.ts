import { Component, OnInit } from '@angular/core';
import { FeedbackService } from '../../services/feedback.service';
import { Feedback } from '../../models/feedback.model';

@Component({
  selector: 'app-feedback-management',
  templateUrl: './feedback-management.component.html',
  styleUrl: './feedback-management.component.css',
  standalone: false
})
export class FeedbackManagementComponent implements OnInit {
  feedbacks: Feedback[] = [];
  loading = false;
  errorMessage = '';
  successMessage = '';
  editingId: number | null = null;

  formModel: Feedback = {
    userName: '',
    userEmail: '',
    rating: 5,
    comment: ''
  };

  constructor(private readonly feedbackService: FeedbackService) {}

  ngOnInit(): void {
    this.loadFeedbacks();
  }

  loadFeedbacks(): void {
    this.loading = true;
    this.errorMessage = '';
    this.feedbackService.getAll().subscribe({
      next: (data) => {
        this.feedbacks = data;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les feedbacks.';
        this.loading = false;
      }
    });
  }

  saveFeedback(): void {
    this.errorMessage = '';
    this.successMessage = '';

    const payload: Feedback = {
      userName: this.formModel.userName.trim(),
      userEmail: this.formModel.userEmail.trim(),
      rating: this.formModel.rating,
      comment: this.formModel.comment.trim()
    };

    if (!payload.userName || !payload.userEmail || !payload.comment) {
      this.errorMessage = 'Veuillez remplir tous les champs du feedback.';
      return;
    }

    if (payload.rating < 1 || payload.rating > 5) {
      this.errorMessage = 'La note doit etre entre 1 et 5.';
      return;
    }

    if (this.editingId === null) {
      this.feedbackService.create(payload).subscribe({
        next: () => {
          this.successMessage = 'Feedback ajoute avec succes.';
          this.resetForm();
          this.loadFeedbacks();
        },
        error: () => {
          this.errorMessage = "Erreur lors de l'ajout du feedback.";
        }
      });
      return;
    }

    this.feedbackService.update(this.editingId, payload).subscribe({
      next: () => {
        this.successMessage = 'Feedback modifie avec succes.';
        this.resetForm();
        this.loadFeedbacks();
      },
      error: () => {
        this.errorMessage = 'Erreur lors de la modification du feedback.';
      }
    });
  }

  startEdit(item: Feedback): void {
    if (item.id === undefined) {
      return;
    }

    this.editingId = item.id;
    this.formModel = {
      userName: item.userName,
      userEmail: item.userEmail,
      rating: item.rating,
      comment: item.comment
    };
  }

  deleteFeedback(id: number | undefined): void {
    if (id === undefined) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.feedbackService.delete(id).subscribe({
      next: () => {
        this.successMessage = 'Feedback supprime avec succes.';
        this.loadFeedbacks();
      },
      error: () => {
        this.errorMessage = 'Erreur lors de la suppression du feedback.';
      }
    });
  }

  resetForm(): void {
    this.editingId = null;
    this.formModel = {
      userName: '',
      userEmail: '',
      rating: 5,
      comment: ''
    };
  }
}
