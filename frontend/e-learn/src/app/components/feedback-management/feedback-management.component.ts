import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FeedbackService } from '../../services/feedback.service';
import { Feedback } from '../../models/feedback.model';

type SpeechTargetField = 'title' | 'comment';

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
  listening = false;
  speechSupported = typeof window !== 'undefined' && ('SpeechRecognition' in window || 'webkitSpeechRecognition' in window);
  qrPayload = '';

  private speechRecognition: SpeechRecognition | null = null;

  formModel: Feedback = {
    userName: '',
    userEmail: '',
    title: '',
    imageUrl: '',
    rating: 5,
    comment: ''
  };

  constructor(
    private readonly feedbackService: FeedbackService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadFeedbacks();
  }

  loadFeedbacks(): void {
    this.loading = true;
    this.errorMessage = '';
    this.feedbackService.getAll().subscribe({
      next: (data) => {
        this.feedbacks = data;
        this.refreshQrPayload();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les feedbacks.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  saveFeedback(): void {
    this.errorMessage = '';
    this.successMessage = '';

    const payload: Feedback = {
      userName: this.formModel.userName.trim(),
      userEmail: this.formModel.userEmail.trim(),
      title: this.formModel.title.trim(),
      imageUrl: this.formModel.imageUrl?.trim(),
      rating: this.formModel.rating,
      comment: this.formModel.comment.trim()
    };

    if (!payload.userName || !payload.userEmail || !payload.title || !payload.comment) {
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
      title: item.title,
      imageUrl: item.imageUrl,
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
      title: '',
      imageUrl: '',
      rating: 5,
      comment: ''
    };
  }

  startSpeechInput(target: SpeechTargetField): void {
    if (!this.speechSupported) {
      this.errorMessage = 'Speech-to-text non supporte sur ce navigateur.';
      return;
    }

    const SpeechRecognitionCtor = (window as Window & {
      SpeechRecognition?: new () => SpeechRecognition;
      webkitSpeechRecognition?: new () => SpeechRecognition;
    }).SpeechRecognition
      ?? (window as Window & {
        webkitSpeechRecognition?: new () => SpeechRecognition;
      }).webkitSpeechRecognition;

    if (!SpeechRecognitionCtor) {
      this.errorMessage = 'Speech-to-text non supporte sur ce navigateur.';
      return;
    }

    this.errorMessage = '';
    this.listening = true;

    this.speechRecognition = new SpeechRecognitionCtor();
    this.speechRecognition.lang = 'fr-FR';
    this.speechRecognition.interimResults = false;
    this.speechRecognition.maxAlternatives = 1;

    this.speechRecognition.onresult = (event: SpeechRecognitionEvent) => {
      const transcript = event.results[0][0].transcript.trim();
      if (!transcript) {
        return;
      }

      if (target === 'title') {
        this.formModel.title = transcript;
      } else {
        this.formModel.comment = transcript;
      }

      this.cdr.detectChanges();
    };

    this.speechRecognition.onerror = () => {
      this.errorMessage = 'Erreur de reconnaissance vocale.';
      this.listening = false;
      this.cdr.detectChanges();
    };

    this.speechRecognition.onend = () => {
      this.listening = false;
      this.cdr.detectChanges();
    };

    this.speechRecognition.start();
  }

  stopSpeechInput(): void {
    this.speechRecognition?.stop();
    this.listening = false;
  }

  buildShareText(item: Feedback): string {
    return `Forum Post: ${item.title}\nAuteur: ${item.userName}\nCommentaire: ${item.comment}\nNote: ${item.rating}/5`;
  }

  shareOnWhatsapp(item: Feedback): void {
    const text = encodeURIComponent(this.buildShareText(item));
    window.open(`https://wa.me/?text=${text}`, '_blank', 'noopener');
  }

  shareOnTelegram(item: Feedback): void {
    const text = encodeURIComponent(this.buildShareText(item));
    window.open(`https://t.me/share/url?text=${text}`, '_blank', 'noopener');
  }

  private refreshQrPayload(): void {
    if (this.feedbacks.length === 0) {
      this.qrPayload = JSON.stringify({ message: 'Aucun post forum' });
      return;
    }

    const latest = this.feedbacks[0];
    this.qrPayload = JSON.stringify({
      id: latest.id,
      title: latest.title,
      author: latest.userName,
      comment: latest.comment,
      rating: latest.rating,
      createdAt: latest.createdAt
    });
  }
}
