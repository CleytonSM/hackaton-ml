package br.com.connectai.ml.api.service;

import br.com.connectai.ml.models.DoctorSimilarityPrediction;
import br.com.connectai.ml.models.db.entities.Doctor;
import br.com.connectai.ml.models.db.entities.Patient;
import br.com.connectai.ml.models.db.entities.QuestionOptions;
import br.com.connectai.ml.models.db.entities.UserAnswer;
import br.com.connectai.ml.models.enums.SpecialtiesEnum;
import br.com.connectai.ml.repositories.DoctorRepository;
import br.com.connectai.ml.repositories.PatientRepository;
import br.com.connectai.ml.repositories.QuestionOptionsRepository;
import br.com.connectai.ml.repositories.UserAnswersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ModelService {
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private UserAnswersRepository userAnswersRepository;
    @Autowired
    private QuestionOptionsRepository questionOptionsRepository;

    public void trainModel() {
        try {
            List<Doctor> doctors = doctorRepository.findAll();
            List<Integer> doctorIds = doctors.stream().map(Doctor::getId).collect(Collectors.toList());

            List<Patient> patients = patientRepository.findAll();
            List<Integer> patientIds = patients.stream().map(Patient::getId).collect(Collectors.toList());

            List<UserAnswer> userAnswers = userAnswersRepository.findByIdPatientInOrderByIdQuestion(patientIds);
            userAnswers.addAll(userAnswersRepository.findByIdDoctorInOrderByIdQuestion(doctorIds));
            final List<UserAnswer> userAnswersFinal = userAnswers.stream().filter(userAnswer -> userAnswer.getIdQuestionOption() != null).collect(Collectors.toList());
            List<QuestionOptions> questionOptionsList = questionOptionsRepository.findAll();
            List<Integer> questionIds = questionOptionsList.stream()
                    .map(QuestionOptions::getQuestionId)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            Instances trainingData = createWekaDatasetForClustering(doctors, patients, questionIds);

            doctors.forEach(doctor -> addDoctorsToDataset(doctor, userAnswersFinal, trainingData));
            patients.forEach(patient -> addPatientToDataset(patient, userAnswersFinal, trainingData));
            SimpleKMeans trainedModel = trainKMeansModel(trainingData);
            new File("models").mkdirs();
            try {
                // 1. Salvar o MODELO com extensão .model
                String modelPath = "models/vitalink.model";
                SerializationHelper.write(modelPath, trainedModel);

                // 2. Salvar a ESTRUTURA DO DATASET com extensão .arff
                String structurePath = "models/dataset_structure.arff";
                ArffSaver saver = new ArffSaver();
                saver.setInstances(trainingData);
                saver.setFile(new File(structurePath));
                saver.writeBatch();

                System.out.println("Modelo salvo em: " + modelPath);
                System.out.println("Estrutura salva em: " + structurePath);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } catch (Exception e) {
            System.err.println("Erro no treinamento do modelo: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Falha no treinamento do modelo", e);
        }
    }

    private SimpleKMeans trainKMeansModel(Instances trainingData) throws Exception {
        SimpleKMeans kmeans = new SimpleKMeans();

        // Configurações
        kmeans.setNumClusters(5); // ajustar baseado no número de usuários
        kmeans.setMaxIterations(500);
        kmeans.setSeed(1); // para resultados reproduzíveis

        // Treinar
        kmeans.buildClusterer(trainingData);

        System.out.println("Número de usuários: " + trainingData.numInstances());
        System.out.println("Número de perguntas: " + trainingData.numAttributes());
        System.out.println("Número de clusters: " + kmeans.getNumClusters());
        System.out.println("SSE: " + kmeans.getSquaredError());
        System.out.println("Sum of squared errors: " + kmeans.getSquaredError());

        return kmeans;
    }

    private Instances createWekaDatasetForClustering(List<Doctor> doctors, List<Patient> patients, List<Integer> questionIds) {
        ArrayList<Attribute> attributes = new ArrayList<>();

        for (Integer questionId : questionIds) {
            attributes.add(new Attribute("question_" + questionId));
        }

        return new Instances("UserProfiles", attributes, doctors.size() + patients.size());
    }

    private void addDoctorsToDataset(Doctor doctor, List<UserAnswer> otherUserAnswers, Instances trainingData) {
        double[] values = new double[trainingData.numAttributes()];
        Arrays.fill(values, 0.0);

        List<UserAnswer> userAnswers = otherUserAnswers.stream()
                .filter(ans -> ans.getIdDoctor() == doctor.getId())
                .collect(Collectors.toList());

        List<Integer> optionIds = userAnswers.stream()
                .map(UserAnswer::getIdQuestionOption)
                .collect(Collectors.toList());

        List<QuestionOptions> chosenOptions = questionOptionsRepository.findAllById(optionIds);

        for (QuestionOptions qo : chosenOptions) {
            String attributeName = "question_" + qo.getQuestionId();
            Attribute attr = trainingData.attribute(attributeName);
            if (attr != null) {
                values[attr.index()] = qo.getOptionValue();
            }
        }

        trainingData.add(new DenseInstance(1.0, values));
    }

    private void addPatientToDataset(Patient patient, List<UserAnswer> otherUserAnswers, Instances trainingData) {
        double[] values = new double[trainingData.numAttributes()];
        Arrays.fill(values, 0.0);

        List<UserAnswer> userAnswers = otherUserAnswers.stream()
                .filter(ans -> ans.getIdPatient() == patient.getId())
                .collect(Collectors.toList());

        List<Integer> optionIds = userAnswers.stream()
                .map(UserAnswer::getIdQuestionOption)
                .collect(Collectors.toList());

        List<QuestionOptions> chosenOptions = questionOptionsRepository.findAllById(optionIds);

        for (QuestionOptions qo : chosenOptions) {
            String attributeName = "question_" + qo.getQuestionId();
            Attribute attr = trainingData.attribute(attributeName);
            if (attr != null) {
                values[attr.index()] = qo.getOptionValue();
            }
        }

        trainingData.add(new DenseInstance(1.0, values));
    }

    public List<DoctorSimilarityPrediction> getTopMatches(int patientId, int topN, int specialtyId) {
        List<DoctorSimilarityPrediction> predictions = new ArrayList<>();
        SpecialtiesEnum specialtiesEnum = SpecialtiesEnum.fromCode(specialtyId);
        // 1. Carregar modelo e estrutura
        SimpleKMeans model = loadModel();
        Instances instances = loadDatasetStructure();

        // 2. Criar instância do usuário âncora
        Patient anchorPatient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário âncora não encontrado"));
        Instance anchorInstance = createInstanceForPatient(anchorPatient, instances);

        anchorInstance.setDataset(instances);

        // 3. Descobrir cluster do usuário âncora
        int anchorCluster;
        try {
            anchorCluster = model.clusterInstance(anchorInstance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 4. Buscar outros doutores do evento
        List<Doctor> doctors = doctorRepository.findAll().stream().filter(doctor -> doctor.getSpecialty().equals(
                specialtiesEnum.name())).collect(Collectors.toList());

        // 5. Filtrar doutores do MESMO CLUSTER + calcular similaridade
        double maxPossibleDistance = calculateMaxDistance(instances);

        int finalAnchorCluster = anchorCluster;
        doctors.forEach(doctor -> {
            Instance currentUserInstance = createInstanceForDoctor(doctor, instances);
            currentUserInstance.setDataset(instances);
            try {
                int userCluster = model.clusterInstance(currentUserInstance);

                // Só processar se estiver no mesmo cluster
                double similarityPercentage = calculateSimilarityPercentage(
                        anchorInstance, currentUserInstance, maxPossibleDistance);

                DoctorSimilarityPrediction prediction = new DoctorSimilarityPrediction();
                prediction.setId(doctor.getId());
                prediction.setProbability(similarityPercentage);
                predictions.add(prediction);
                // Usuários de outros clusters são ignorados (não aparecem na lista)

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // 6. Ordenar e retornar top N
        return predictions.stream()
                .sorted((a, b) -> Double.compare(b.getProbability(), a.getProbability()))
                .limit(topN)
                .collect(Collectors.toList());
    }



    private double calculateMaxDistance(Instances datasetStructure) {
        double maxDistance = 0.0;

        // Para cada atributo, pegar valor min e max dos dados reais
        for (int i = 0; i < datasetStructure.numAttributes(); i++) {
            double min = datasetStructure.attributeStats(i).numericStats.min;
            double max = datasetStructure.attributeStats(i).numericStats.max;
            double range = max - min;
            maxDistance += range * range; // Soma dos quadrados das diferenças máximas
        }

        return Math.sqrt(maxDistance);
    }

    private double calculateSimilarityPercentage(Instance anchor, Instance other, double maxDistance) {
        double distance = calculateEuclideanDistance(anchor, other);
        double similarity = 1.0 - (distance / maxDistance);

        // Garantir que fica entre 0 e 1
        similarity = Math.max(0.0, Math.min(1.0, similarity));

        return similarity * 100.0; // Converter para porcentagem
    }

    private Instances loadDatasetStructure() {
        Instances structure = null;
        try {
            structure = ConverterUtils.DataSource.read("models/dataset_structure.arff");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //structure.delete(); // Remove todas as instâncias, mantém só os atributos
        return structure;
    }

    private SimpleKMeans loadModel() {
        try {
            String modelPath = "models/vitalink.model";
            return (SimpleKMeans) SerializationHelper.read(modelPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Instance createInstanceForPatient(Patient patient, Instances datasetStructure) {
        // Criar array com tamanho igual ao número de atributos
        double[] values = new double[datasetStructure.numAttributes()];
        Arrays.fill(values, 0.0); // Valores padrão

        // Buscar respostas do usuário
        List<UserAnswer> userAnswers = userAnswersRepository.findByIdPatientOrderByIdQuestion(patient.getId());

        // Buscar as opções escolhidas
        List<Integer> optionIds = userAnswers.stream()
                .map(UserAnswer::getIdQuestionOption)
                .collect(Collectors.toList());

        List<QuestionOptions> chosenOptions = questionOptionsRepository.findAllById(optionIds);

        // Preencher valores nas posições corretas
        for (QuestionOptions option : chosenOptions) {
            String attributeName = "question_" + option.getQuestionId();
            Attribute attr = datasetStructure.attribute(attributeName);

            if (attr != null) {
                values[attr.index()] = option.getOptionValue();
            }
        }

        // Criar e retornar a instância
        return new DenseInstance(1.0, values);
    }

    private Instance createInstanceForDoctor(Doctor doctor, Instances datasetStructure) {
        // Criar array com tamanho igual ao número de atributos
        double[] values = new double[datasetStructure.numAttributes()];
        Arrays.fill(values, 0.0); // Valores padrão

        // Buscar respostas do usuário
        List<UserAnswer> userAnswers = userAnswersRepository.findByIdDoctorOrderByIdQuestion(doctor.getId());

        // Buscar as opções escolhidas
        List<Integer> optionIds = userAnswers.stream()
                .map(UserAnswer::getIdQuestionOption)
                .collect(Collectors.toList());

        List<QuestionOptions> chosenOptions = questionOptionsRepository.findAllById(optionIds);

        // Preencher valores nas posições corretas
        for (QuestionOptions option : chosenOptions) {
            String attributeName = "question_" + option.getQuestionId();
            Attribute attr = datasetStructure.attribute(attributeName);

            if (attr != null) {
                values[attr.index()] = option.getOptionValue();
            }
        }

        // Criar e retornar a instância
        return new DenseInstance(1.0, values);
    }

    private double calculateEuclideanDistance(Instance user1, Instance user2) {
        double sum = 0.0;
        for (int i = 0; i < user1.numAttributes(); i++) {
            double diff = user1.value(i) - user2.value(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    public List<DoctorSimilarityPrediction> getTopMatches2(int patientId, int n, List<Doctor> doctors) {
        List<DoctorSimilarityPrediction> predictions = new ArrayList<>();

        // 1. Carregar modelo e estrutura
        SimpleKMeans model = loadModel();
        Instances instances = loadDatasetStructure();

        // 2. Criar instância do usuário âncora
        Patient anchorPatient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário âncora não encontrado"));
        Instance anchorInstance = createInstanceForPatient(anchorPatient, instances);

        anchorInstance.setDataset(instances);

        // 3. Descobrir cluster do usuário âncora
        int anchorCluster;
        try {
            anchorCluster = model.clusterInstance(anchorInstance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 5. Filtrar doutores do MESMO CLUSTER + calcular similaridade
        double maxPossibleDistance = calculateMaxDistance(instances);

        int finalAnchorCluster = anchorCluster;
        doctors.forEach(doctor -> {
            Instance currentUserInstance = createInstanceForDoctor(doctor, instances);
            currentUserInstance.setDataset(instances);
            try {
                int userCluster = model.clusterInstance(currentUserInstance);

                // Só processar se estiver no mesmo cluster
                double similarityPercentage = calculateSimilarityPercentage(
                        anchorInstance, currentUserInstance, maxPossibleDistance);

                DoctorSimilarityPrediction prediction = new DoctorSimilarityPrediction();
                prediction.setId(doctor.getId());
                prediction.setProbability(similarityPercentage);
                prediction.setActive(doctor.getActive());
                prediction.setCrm(doctor.getCrm());
                prediction.setEmail(doctor.getEmail());
                prediction.setCreatedAt(doctor.getCreatedAt());
                prediction.setUpdatedAt(doctor.getUpdatedAt());
                prediction.setName(doctor.getName());
                prediction.setSpecialtyId(SpecialtiesEnum.valueOf(doctor.getSpecialty()).getCode());
                prediction.setDeletedAt(doctor.getDeletedAt());
                predictions.add(prediction);
                // Usuários de outros clusters são ignorados (não aparecem na lista)

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // 6. Ordenar e retornar top N
        return predictions.stream()
                .sorted((a, b) -> Double.compare(b.getProbability(), a.getProbability()))
                .limit(n)
                .collect(Collectors.toList());
    }
}