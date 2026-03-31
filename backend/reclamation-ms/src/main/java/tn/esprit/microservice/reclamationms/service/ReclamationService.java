package tn.esprit.microservice.reclamationms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import tn.esprit.microservice.reclamationms.entity.Reclamation;
import tn.esprit.microservice.reclamationms.exception.ResourceNotFoundException;
import tn.esprit.microservice.reclamationms.repository.ReclamationRepository;

@Service
public class ReclamationService {

    private static final int QR_SIZE = 320;

    private final ReclamationRepository reclamationRepository;
    private final ObjectMapper objectMapper;

    public ReclamationService(ReclamationRepository reclamationRepository, ObjectMapper objectMapper) {
        this.reclamationRepository = reclamationRepository;
        this.objectMapper = objectMapper;
    }

    public List<Reclamation> findAll() {
        return reclamationRepository.findAll();
    }

    public Reclamation findById(Long id) {
        return reclamationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reclamation with id " + id + " not found"));
    }

    public Reclamation create(Reclamation reclamation) {
        return reclamationRepository.save(reclamation);
    }

    public Reclamation update(Long id, Reclamation request) {
        Reclamation reclamation = findById(id);
        reclamation.setUserName(request.getUserName());
        reclamation.setUserEmail(request.getUserEmail());
        reclamation.setSubject(request.getSubject());
        reclamation.setImageUrl(request.getImageUrl());
        reclamation.setDescription(request.getDescription());
        reclamation.setStatus(request.getStatus());
        return reclamationRepository.save(reclamation);
    }

    public void delete(Long id) {
        Reclamation reclamation = findById(id);
        reclamationRepository.delete(reclamation);
    }

    public String buildLectureText(Long id) {
        Reclamation reclamation = findById(id);
        return String.format(
                Locale.ROOT,
                "Reclamation de %s. Sujet: %s. Description: %s. Statut: %s.",
                safe(reclamation.getUserName()),
                safe(reclamation.getSubject()),
                safe(reclamation.getDescription()),
                reclamation.getStatus() == null ? "-" : reclamation.getStatus().name()
        );
    }

    public ReclamationDictationResult processDictation(String transcript) {
        String cleanedText = normalizeTranscript(transcript);
        if (cleanedText.isEmpty()) {
            return new ReclamationDictationResult("", "", "");
        }

        int splitIndex = cleanedText.indexOf(". ");
        String suggestedSubject;
        String suggestedDescription;

        if (splitIndex > 0) {
            suggestedSubject = cleanedText.substring(0, splitIndex + 1).trim();
            suggestedDescription = cleanedText.substring(splitIndex + 2).trim();
        } else {
            suggestedSubject = cleanedText.length() > 80
                    ? cleanedText.substring(0, 80).trim() + "..."
                    : cleanedText;
            suggestedDescription = cleanedText;
        }

        return new ReclamationDictationResult(cleanedText, suggestedSubject, suggestedDescription);
    }

    public byte[] generateQrCode(Long id) {
        Reclamation reclamation = findById(id);
        return encodeQrCode(buildQrPayload(reclamation));
    }

    public byte[] generatePdf(Long id) {
        Reclamation reclamation = findById(id);
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 16);
                content.newLineAtOffset(50, 780);
                content.showText("Reclamation");
                content.newLineAtOffset(0, -26);
                content.setFont(PDType1Font.HELVETICA, 12);

                for (String line : buildPdfLines(reclamation)) {
                    for (String wrappedLine : wrapText(line, 92)) {
                        content.showText(wrappedLine);
                        content.newLineAtOffset(0, -16);
                    }
                }
                content.endText();
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to generate reclamation PDF", ex);
        }
    }

    private List<String> buildPdfLines(Reclamation reclamation) {
        List<String> lines = new ArrayList<>();
        lines.add("ID: " + (reclamation.getId() == null ? "-" : reclamation.getId()));
        lines.add("Nom: " + safe(reclamation.getUserName()));
        lines.add("Email: " + safe(reclamation.getUserEmail()));
        lines.add("Sujet: " + safe(reclamation.getSubject()));
        lines.add("Image URL: " + safe(reclamation.getImageUrl()));
        lines.add("Statut: " + (reclamation.getStatus() == null ? "-" : reclamation.getStatus().name()));
        lines.add("Description: " + safe(reclamation.getDescription()));
        lines.add("Date creation: " + (reclamation.getCreatedAt() == null ? "-" : reclamation.getCreatedAt()));
        return lines;
    }

    private String buildQrPayload(Reclamation reclamation) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", reclamation.getId());
        payload.put("userName", reclamation.getUserName());
        payload.put("userEmail", reclamation.getUserEmail());
        payload.put("subject", reclamation.getSubject());
        payload.put("status", reclamation.getStatus());
        payload.put("description", reclamation.getDescription());
        payload.put("createdAt", reclamation.getCreatedAt());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize reclamation QR payload", ex);
        }
    }

    private byte[] encodeQrCode(String payload) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            BitMatrix matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new IllegalStateException("Unable to generate reclamation QR code", ex);
        }
    }

    private List<String> wrapText(String text, int maxLineLength) {
        String normalized = normalizeTranscript(text);
        if (normalized.isEmpty()) {
            return List.of("-");
        }

        List<String> wrappedLines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        for (String word : normalized.split(" ")) {
            if (currentLine.isEmpty()) {
                currentLine.append(word);
                continue;
            }

            if (currentLine.length() + 1 + word.length() > maxLineLength) {
                wrappedLines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                currentLine.append(' ').append(word);
            }
        }

        if (!currentLine.isEmpty()) {
            wrappedLines.add(currentLine.toString());
        }
        return wrappedLines;
    }

    private String normalizeTranscript(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String safe(String value) {
        String normalized = normalizeTranscript(value);
        return normalized.isEmpty() ? "-" : normalized;
    }

    public record ReclamationDictationResult(String cleanedText, String suggestedSubject,
                                             String suggestedDescription) {
    }
}
