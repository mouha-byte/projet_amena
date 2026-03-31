package tn.esprit.microservice.feedbackms.service;

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
import tn.esprit.microservice.feedbackms.entity.Feedback;
import tn.esprit.microservice.feedbackms.exception.ResourceNotFoundException;
import tn.esprit.microservice.feedbackms.repository.FeedbackRepository;

@Service
public class FeedbackService {

    private static final int QR_SIZE = 320;

    private final FeedbackRepository feedbackRepository;
    private final ObjectMapper objectMapper;

    public FeedbackService(FeedbackRepository feedbackRepository, ObjectMapper objectMapper) {
        this.feedbackRepository = feedbackRepository;
        this.objectMapper = objectMapper;
    }

    public List<Feedback> findAll() {
        return feedbackRepository.findAll();
    }

    public Feedback findById(Long id) {
        return feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback with id " + id + " not found"));
    }

    public Feedback create(Feedback feedback) {
        return feedbackRepository.save(feedback);
    }

    public Feedback update(Long id, Feedback request) {
        Feedback feedback = findById(id);
        feedback.setUserName(request.getUserName());
        feedback.setUserEmail(request.getUserEmail());
        feedback.setTitle(request.getTitle());
        feedback.setImageUrl(request.getImageUrl());
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());
        return feedbackRepository.save(feedback);
    }

    public void delete(Long id) {
        Feedback feedback = findById(id);
        feedbackRepository.delete(feedback);
    }

    public String buildLectureText(Long id) {
        Feedback feedback = findById(id);
        String rating = feedback.getRating() == null ? "-" : feedback.getRating().toString();
        return String.format(
                Locale.ROOT,
                "Feedback de %s. Titre: %s. Commentaire: %s. Note: %s sur 5.",
                safe(feedback.getUserName()),
                safe(feedback.getTitle()),
                safe(feedback.getComment()),
                rating
        );
    }

    public FeedbackDictationResult processDictation(String transcript) {
        String cleanedText = normalizeTranscript(transcript);
        if (cleanedText.isEmpty()) {
            return new FeedbackDictationResult("", "", "");
        }

        int splitIndex = cleanedText.indexOf(". ");
        String suggestedTitle;
        String suggestedComment;

        if (splitIndex > 0) {
            suggestedTitle = cleanedText.substring(0, splitIndex + 1).trim();
            suggestedComment = cleanedText.substring(splitIndex + 2).trim();
        } else {
            suggestedTitle = cleanedText.length() > 70
                    ? cleanedText.substring(0, 70).trim() + "..."
                    : cleanedText;
            suggestedComment = cleanedText;
        }

        return new FeedbackDictationResult(cleanedText, suggestedTitle, suggestedComment);
    }

    public byte[] generateQrCode(Long id) {
        Feedback feedback = findById(id);
        return encodeQrCode(buildQrPayload(feedback));
    }

    public byte[] generatePdf(Long id) {
        Feedback feedback = findById(id);
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 16);
                content.newLineAtOffset(50, 780);
                content.showText("Feedback");
                content.newLineAtOffset(0, -26);
                content.setFont(PDType1Font.HELVETICA, 12);

                for (String line : buildPdfLines(feedback)) {
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
            throw new IllegalStateException("Unable to generate feedback PDF", ex);
        }
    }

    private List<String> buildPdfLines(Feedback feedback) {
        List<String> lines = new ArrayList<>();
        lines.add("ID: " + (feedback.getId() == null ? "-" : feedback.getId()));
        lines.add("Nom: " + safe(feedback.getUserName()));
        lines.add("Email: " + safe(feedback.getUserEmail()));
        lines.add("Titre: " + safe(feedback.getTitle()));
        lines.add("Image URL: " + safe(feedback.getImageUrl()));
        lines.add("Note: " + (feedback.getRating() == null ? "-" : feedback.getRating() + "/5"));
        lines.add("Commentaire: " + safe(feedback.getComment()));
        lines.add("Date creation: " + (feedback.getCreatedAt() == null ? "-" : feedback.getCreatedAt()));
        return lines;
    }

    private String buildQrPayload(Feedback feedback) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", feedback.getId());
        payload.put("userName", feedback.getUserName());
        payload.put("userEmail", feedback.getUserEmail());
        payload.put("title", feedback.getTitle());
        payload.put("rating", feedback.getRating());
        payload.put("comment", feedback.getComment());
        payload.put("createdAt", feedback.getCreatedAt());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize feedback QR payload", ex);
        }
    }

    private byte[] encodeQrCode(String payload) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            BitMatrix matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new IllegalStateException("Unable to generate feedback QR code", ex);
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

    public record FeedbackDictationResult(String cleanedText, String suggestedTitle, String suggestedComment) {
    }
}
