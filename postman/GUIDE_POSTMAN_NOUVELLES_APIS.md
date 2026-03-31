# Guide Postman Step by Step - Nouvelles APIs

Ce guide explique pas a pas comment tester les nouvelles APIs backend de:
- feedbacks
- reclamations

APIs concernees:
- lecture
- dictee
- qrcode
- pdf

## 0) Prerequis

1. Java et Maven doivent etre installes.
2. Postman doit etre installe.
3. Les microservices doivent etre demarres.

Ordre recommande:
1. eureka-server (8761)
2. config-server (8888)
3. feedback-ms (8090)
4. reclamation-ms (8091)
5. gateway (8085)

## 1) Importer la collection Postman

1. Ouvre Postman.
2. Clique sur Import.
3. Selectionne le fichier:
   - e-learn-main/postman/e-learn-new-apis.postman_collection.json
4. Valide l import.

## 2) Configurer les variables

Tu peux utiliser les variables de collection deja presentes:
- baseUrl = http://localhost:8085
- feedbackId = vide au depart
- reclamationId = vide au depart

Si besoin, modifie baseUrl selon ton environnement.

## 3) Test complet Feedbacks

Dossier Postman: Feedbacks - Nouvelles APIs

### Etape 1 - Creer un feedback

1. Lance: POST Create Feedback (set feedbackId)
2. Verifie status 200.
3. Le script Postman enregistre automatiquement feedbackId.

### Etape 2 - Tester lecture

1. Lance: GET Feedback Lecture Text
2. Verifie:
   - status 200
   - Content-Type contient text/plain
3. Le body doit contenir une phrase de lecture du feedback.

### Etape 3 - Tester dictee

1. Lance: POST Feedback Dictation
2. Verifie:
   - status 200
   - reponse JSON avec:
     - cleanedText
     - suggestedTitle
     - suggestedComment

### Etape 4 - Tester QR code

1. Lance: GET Feedback QR Code (PNG)
2. Verifie:
   - status 200
   - Content-Type contient image/png
3. Dans Postman, tu peux voir Preview pour afficher l image.

### Etape 5 - Tester PDF

1. Lance: GET Feedback PDF
2. Verifie:
   - status 200
   - Content-Type contient application/pdf
3. Clique Save Response pour enregistrer le PDF si necessaire.

## 4) Test complet Reclamations

Dossier Postman: Reclamations - Nouvelles APIs

### Etape 1 - Creer une reclamation

1. Lance: POST Create Reclamation (set reclamationId)
2. Verifie status 200.
3. Le script Postman enregistre automatiquement reclamationId.

### Etape 2 - Tester lecture

1. Lance: GET Reclamation Lecture Text
2. Verifie:
   - status 200
   - Content-Type contient text/plain
3. Le body doit contenir une phrase de lecture de la reclamation.

### Etape 3 - Tester dictee

1. Lance: POST Reclamation Dictation
2. Verifie:
   - status 200
   - reponse JSON avec:
     - cleanedText
     - suggestedSubject
     - suggestedDescription

### Etape 4 - Tester QR code

1. Lance: GET Reclamation QR Code (PNG)
2. Verifie:
   - status 200
   - Content-Type contient image/png
3. Affiche l image via Preview dans Postman.

### Etape 5 - Tester PDF

1. Lance: GET Reclamation PDF
2. Verifie:
   - status 200
   - Content-Type contient application/pdf
3. Sauvegarde le fichier si tu veux verifier le contenu.

## 5) Endpoints backend utilises

Feedbacks:
- GET /feedbacks/{id}/lecture
- POST /feedbacks/dictation
- GET /feedbacks/{id}/qrcode
- GET /feedbacks/{id}/pdf

Reclamations:
- GET /reclamations/{id}/lecture
- POST /reclamations/dictation
- GET /reclamations/{id}/qrcode
- GET /reclamations/{id}/pdf

## 6) Erreurs frequentes et solutions

1. 404 Not Found
- Verifie que le service gateway est demarre.
- Verifie baseUrl.
- Verifie que feedbackId/reclamationId ne sont pas vides.

2. 500 Internal Server Error
- Verifie les logs de feedback-ms et reclamation-ms.
- Verifie que l ID existe vraiment en base.

3. Variables non remplies
- Relance les requetes de creation (POST Create Feedback / POST Create Reclamation).

## 7) Ordre de test rapide conseille

1. POST Create Feedback
2. GET Feedback Lecture
3. POST Feedback Dictation
4. GET Feedback QR Code
5. GET Feedback PDF
6. POST Create Reclamation
7. GET Reclamation Lecture
8. POST Reclamation Dictation
9. GET Reclamation QR Code
10. GET Reclamation PDF
