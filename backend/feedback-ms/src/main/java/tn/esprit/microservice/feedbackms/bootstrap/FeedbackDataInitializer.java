package tn.esprit.microservice.feedbackms.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tn.esprit.microservice.feedbackms.entity.Feedback;
import tn.esprit.microservice.feedbackms.repository.FeedbackRepository;

@Configuration
public class FeedbackDataInitializer {

    @Bean
    CommandLineRunner loadFeedbacks(FeedbackRepository feedbackRepository) {
        return args -> {
            if (feedbackRepository.count() > 0) {
                return;
            }

            Feedback f1 = new Feedback();
            f1.setUserName("Amena");
            f1.setUserEmail("amena@example.com");
            f1.setRating(5);
            f1.setComment("Excellent contenu de formation.");

            Feedback f2 = new Feedback();
            f2.setUserName("Karim");
            f2.setUserEmail("karim@example.com");
            f2.setRating(4);
            f2.setComment("Bonne plateforme, interface claire.");

            feedbackRepository.save(f1);
            feedbackRepository.save(f2);
        };
    }
}
