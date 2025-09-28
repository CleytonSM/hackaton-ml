package br.com.connectai.ml.repositories;

import br.com.connectai.ml.models.db.entities.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Integer> {
}
