package tn.esprit.microservice.feedbackms.service;

import java.util.List;
import org.springframework.stereotype.Service;
import tn.esprit.microservice.feedbackms.entity.Feedback;
import tn.esprit.microservice.feedbackms.exception.ResourceNotFoundException;
import tn.esprit.microservice.feedbackms.repository.FeedbackRepository;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
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
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());
        return feedbackRepository.save(feedback);
    }

    public void delete(Long id) {
        Feedback feedback = findById(id);
        feedbackRepository.delete(feedback);
    }
}
