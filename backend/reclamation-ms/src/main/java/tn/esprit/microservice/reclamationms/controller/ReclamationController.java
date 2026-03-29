package tn.esprit.microservice.reclamationms.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.microservice.reclamationms.entity.Reclamation;
import tn.esprit.microservice.reclamationms.service.ReclamationService;

@RestController
@RequestMapping("/reclamations")
public class ReclamationController {

    private final ReclamationService reclamationService;

    public ReclamationController(ReclamationService reclamationService) {
        this.reclamationService = reclamationService;
    }

    @GetMapping
    public List<Reclamation> getAll() {
        return reclamationService.findAll();
    }

    @GetMapping("/{id}")
    public Reclamation getById(@PathVariable Long id) {
        return reclamationService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Reclamation> create(@RequestBody Reclamation reclamation) {
        return ResponseEntity.ok(reclamationService.create(reclamation));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reclamation> update(@PathVariable Long id, @RequestBody Reclamation reclamation) {
        return ResponseEntity.ok(reclamationService.update(id, reclamation));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reclamationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
