package br.com.connectai.ml.repositories;


import br.com.connectai.ml.models.db.entities.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAnswersRepository extends JpaRepository<UserAnswer, Integer> {
    List<UserAnswer> findByIdPatientInOrderByIdQuestion(List<Integer> patientIds);

    List<UserAnswer> findByIdDoctorInOrderByIdQuestion(List<Integer> doctorIds);

    List<UserAnswer> findByIdPatientOrderByIdQuestion(Integer patientId);

    List<UserAnswer> findByIdDoctorOrderByIdQuestion(Integer id);
}
