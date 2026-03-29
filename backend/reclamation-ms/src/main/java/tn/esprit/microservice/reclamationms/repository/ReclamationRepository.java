package tn.esprit.microservice.reclamationms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.microservice.reclamationms.entity.Reclamation;

public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {
}
