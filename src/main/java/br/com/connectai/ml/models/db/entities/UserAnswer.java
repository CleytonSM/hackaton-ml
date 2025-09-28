package br.com.connectai.ml.models.db.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity(name = "tb_user_answers")
@Table
public class UserAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "id_patient")
    private Integer idPatient;
    @Column(name = "id_doctor")
    private Integer idDoctor;
    @Column(name = "id_question")
    private Integer idQuestion;
    @Column(name = "id_question_option")
    private Integer idQuestionOption;
    @Column(name = "answer_value")
    private String answerValue;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIdPatient() {
        return idPatient;
    }

    public void setIdPatient(Integer idPatient) {
        this.idPatient = idPatient;
    }

    public Integer getIdDoctor() {
        return idDoctor;
    }

    public void setIdDoctor(Integer idDoctor) {
        this.idDoctor = idDoctor;
    }

    public Integer getIdQuestion() {
        return idQuestion;
    }

    public void setIdQuestion(Integer idQuestion) {
        this.idQuestion = idQuestion;
    }

    public Integer getIdQuestionOption() {
        return idQuestionOption;
    }

    public void setIdQuestionOption(Integer idQuestionOption) {
        this.idQuestionOption = idQuestionOption;
    }

    public String getAnswerValue() {
        return answerValue;
    }

    public void setAnswerValue(String answerValue) {
        this.answerValue = answerValue;
    }
}
