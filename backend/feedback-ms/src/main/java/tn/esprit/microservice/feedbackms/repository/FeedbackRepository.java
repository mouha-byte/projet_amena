package tn.esprit.microservice.feedbackms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.microservice.feedbackms.entity.Feedback;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}
