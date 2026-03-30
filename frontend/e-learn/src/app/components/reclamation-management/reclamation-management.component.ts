import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ReclamationService } from '../../services/reclamation.service';
import { Reclamation, ReclamationStatus } from '../../models/reclamation.model';
import jsPDF from 'jspdf';

@Component({
  selector: 'app-reclamation-management',
  templateUrl: './reclamation-management.component.html',
  styleUrl: './reclamation-management.component.css',
  standalone: false
})
export class ReclamationManagementComponent implements OnInit {
  reclamations: Reclamation[] = [];
  loading = false;
  errorMessage = '';
  successMessage = '';
  editingId: number | null = null;
  speaking = false;
  ttsSupported = typeof window !== 'undefined' && 'speechSynthesis' in window;

  readonly statuses: ReclamationStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'REJECTED'];

  formModel: Reclamation = {
    userName: '',
    userEmail: '',
    subject: '',
    imageUrl: '',
    description: '',
    status: 'OPEN'
  };

  constructor(
    private readonly reclamationService: ReclamationService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadReclamations();
  }

  loadReclamations(): void {
    this.loading = true;
    this.errorMessage = '';
    this.reclamationService.getAll().subscribe({
      next: (data) => {
        this.reclamations = data;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les reclamations.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  saveReclamation(): void {
    this.errorMessage = '';
    this.successMessage = '';

    const payload: Reclamation = {
      userName: this.formModel.userName.trim(),
      userEmail: this.formModel.userEmail.trim(),
      subject: this.formModel.subject.trim(),
      imageUrl: this.formModel.imageUrl?.trim(),
      description: this.formModel.description.trim(),
      status: this.formModel.status
    };

    if (!payload.userName || !payload.userEmail || !payload.subject || !payload.description) {
      this.errorMessage = 'Veuillez remplir tous les champs de la reclamation.';
      return;
    }

    if (this.editingId === null) {
      this.reclamationService.create(payload).subscribe({
        next: () => {
          this.successMessage = 'Reclamation ajoutee avec succes.';
          this.resetForm();
          this.loadReclamations();
        },
        error: () => {
          this.errorMessage = "Erreur lors de l'ajout de la reclamation.";
        }
      });
      return;
    }

    this.reclamationService.update(this.editingId, payload).subscribe({
      next: () => {
        this.successMessage = 'Reclamation modifiee avec succes.';
        this.resetForm();
        this.loadReclamations();
      },
      error: () => {
        this.errorMessage = 'Erreur lors de la modification de la reclamation.';
      }
    });
  }

  startEdit(item: Reclamation): void {
    if (item.id === undefined) {
      return;
    }

    this.editingId = item.id;
    this.formModel = {
      userName: item.userName,
      userEmail: item.userEmail,
      subject: item.subject,
      imageUrl: item.imageUrl,
      description: item.description,
      status: item.status
    };
  }

  deleteReclamation(id: number | undefined): void {
    if (id === undefined) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.reclamationService.delete(id).subscribe({
      next: () => {
        this.successMessage = 'Reclamation supprimee avec succes.';
        this.loadReclamations();
      },
      error: () => {
        this.errorMessage = 'Erreur lors de la suppression de la reclamation.';
      }
    });
  }

  resetForm(): void {
    this.editingId = null;
    this.formModel = {
      userName: '',
      userEmail: '',
      subject: '',
      imageUrl: '',
      description: '',
      status: 'OPEN'
    };
  }

  speakReclamation(item: Reclamation): void {
    if (!this.ttsSupported) {
      this.errorMessage = 'Text-to-speech non supporte sur ce navigateur.';
      return;
    }

    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(
      `Reclamation de ${item.userName}. Sujet: ${item.subject}. Description: ${item.description}. Statut: ${item.status}.`
    );
    utterance.lang = 'fr-FR';
    utterance.onstart = () => {
      this.speaking = true;
      this.cdr.detectChanges();
    };
    utterance.onend = () => {
      this.speaking = false;
      this.cdr.detectChanges();
    };
    utterance.onerror = () => {
      this.errorMessage = 'Erreur lors de la lecture vocale.';
      this.speaking = false;
      this.cdr.detectChanges();
    };
    window.speechSynthesis.speak(utterance);
  }

  stopSpeech(): void {
    if (!this.ttsSupported) {
      return;
    }

    window.speechSynthesis.cancel();
    this.speaking = false;
  }

  shareOnWhatsapp(item: Reclamation): void {
    const text = encodeURIComponent(this.buildShareText(item));
    window.open(`https://wa.me/?text=${text}`, '_blank', 'noopener');
  }

  shareOnTelegram(item: Reclamation): void {
    const text = encodeURIComponent(this.buildShareText(item));
    const url = encodeURIComponent(window.location.href);
    window.open(`https://t.me/share/url?url=${url}&text=${text}`, '_blank', 'noopener');
  }

  exportPdf(item: Reclamation): void {
    const doc = new jsPDF();
    doc.setFontSize(16);
    doc.text('Reclamation', 14, 18);

    doc.setFontSize(12);
    const lines = [
      `ID: ${item.id ?? '-'}`,
      `Nom: ${item.userName}`,
      `Email: ${item.userEmail}`,
      `Sujet: ${item.subject}`,
      `Statut: ${item.status}`,
      `Image URL: ${item.imageUrl ?? '-'}`,
      `Description: ${item.description}`
    ];

    let y = 30;
    for (const line of lines) {
      const wrapped = doc.splitTextToSize(line, 180);
      doc.text(wrapped, 14, y);
      y += wrapped.length * 7;
    }

    doc.save(`reclamation-${item.id ?? 'item'}.pdf`);
  }

  private buildShareText(item: Reclamation): string {
    return `Reclamation: ${item.subject}\nAuteur: ${item.userName}\nStatut: ${item.status}\nDescription: ${item.description}`;
  }
}
