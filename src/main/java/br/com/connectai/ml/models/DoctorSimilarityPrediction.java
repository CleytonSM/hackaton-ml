package br.com.connectai.ml.models;

public class DoctorSimilarityPrediction {
    private int doctorId;
    private double probability;

    public DoctorSimilarityPrediction() {
    }

    public DoctorSimilarityPrediction(int doctorId, double probability) {
        this.doctorId = doctorId;
        this.probability = probability;
    }

    public int getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(int doctorId) {
        this.doctorId = doctorId;
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }
}
