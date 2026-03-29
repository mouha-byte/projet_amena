import { Component, OnInit } from '@angular/core';
import { ReclamationService } from '../../services/reclamation.service';
import { Reclamation, ReclamationStatus } from '../../models/reclamation.model';

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

  readonly statuses: ReclamationStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'REJECTED'];

  formModel: Reclamation = {
    userName: '',
    userEmail: '',
    subject: '',
    description: '',
    status: 'OPEN'
  };

  constructor(private readonly reclamationService: ReclamationService) {}

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
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les reclamations.';
        this.loading = false;
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
      description: '',
      status: 'OPEN'
    };
  }
}
