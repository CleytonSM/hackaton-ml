package br.com.connectai.ml.repositories;

import br.com.connectai.ml.models.db.entities.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, Integer> {
}
