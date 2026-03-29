package tn.esprit.microservice.reclamationms.service;

import java.util.List;
import org.springframework.stereotype.Service;
import tn.esprit.microservice.reclamationms.entity.Reclamation;
import tn.esprit.microservice.reclamationms.exception.ResourceNotFoundException;
import tn.esprit.microservice.reclamationms.repository.ReclamationRepository;

@Service
public class ReclamationService {

    private final ReclamationRepository reclamationRepository;

    public ReclamationService(ReclamationRepository reclamationRepository) {
        this.reclamationRepository = reclamationRepository;
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
}
