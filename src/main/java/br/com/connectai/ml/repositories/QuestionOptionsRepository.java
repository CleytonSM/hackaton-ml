package br.com.connectai.ml.repositories;

import br.com.connectai.ml.models.db.entities.QuestionOptions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionOptionsRepository extends JpaRepository<QuestionOptions, Integer> {
    List<QuestionOptions> findAllByQuestionIdIn(List<Integer> userAnsweredQuestionIds);
}
