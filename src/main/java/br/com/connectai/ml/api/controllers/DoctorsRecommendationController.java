package br.com.connectai.ml.api.controllers;

import br.com.connectai.ml.api.service.ModelService;
import br.com.connectai.ml.models.DoctorSimilarityPrediction;
import br.com.connectai.ml.models.db.entities.Doctor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ml")
@CrossOrigin("*")
public class DoctorsRecommendationController {
    @Autowired
    private ModelService modelService;

    @PostMapping("/train")
    public String trainModel() {
        modelService.trainModel();
        return "Model training finished";
    }

    @GetMapping("/top-matches/{patientId}/{specialtyId}")
    public List<DoctorSimilarityPrediction> getTopMatches(@PathVariable("patientId") int patientId, @PathVariable("specialtyId") int specialtyId) {
        return modelService.getTopMatches(patientId, 100, specialtyId);
    }

    @PostMapping("/top-matches/{patientId}")
    public List<DoctorSimilarityPrediction> getTopMatches(@PathVariable("patientId") int patientId, @RequestBody List<Doctor> doctors) {
        return modelService.getTopMatches2(patientId, 100, doctors);
    }
}
