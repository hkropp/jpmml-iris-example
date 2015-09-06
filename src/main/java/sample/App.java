package sample;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import javax.xml.bind.JAXBException;
import javax.xml.transform.sax.SAXSource;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningModel;
import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.ModelEvaluationContext;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.MiningModelEvaluator;
import org.jpmml.evaluator.ProbabilityClassificationMap;
import org.jpmml.model.ImportFilter;
import org.jpmml.model.JAXBUtil;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 */

public class App {

    public App(){}

    public static void main(String... args) throws Exception {
        App app = new App();
        app.run(args);
    }

    public void run(String ... args) throws Exception {
        PMML pmml = createPMMLfromFile("iris_rf.pmml");

        ModelEvaluator<MiningModel> modelEvaluator = new MiningModelEvaluator(pmml);
        printArgumentsOfModel(modelEvaluator);

        List<String> dataLines = Files.readAllLines(Paths.get(App.class.getResource("Iris.csv").toURI()));

        for(String dataLine : dataLines){
            // System.out.println(dataLine); // (sepal_length,sepal_width,petal_length,petal_width,class)
            if(dataLine.startsWith("sepal_length")) continue;

            Map<FieldName, FieldValue> arguments = readArgumentsFromLine(dataLine, modelEvaluator);

            modelEvaluator.verify();

            Map<FieldName, ?> results = modelEvaluator.evaluate(arguments);

            FieldName targetName = modelEvaluator.getTargetField();
            Object targetValue = results.get(targetName);

            ProbabilityClassificationMap nodeMap = (ProbabilityClassificationMap) targetValue;

            System.out.println("\n% 'setosa': " + nodeMap.getProbability("setosa"));
            System.out.println("% 'versicolor': " + nodeMap.getProbability("versicolor"));
            System.out.println("% 'virginica': " + nodeMap.getProbability("virginica"));

            System.out.println("== Result: " + nodeMap.getResult() +"\n");
        }
    }

    public Map<FieldName, FieldValue> readArgumentsFromLine(String line, ModelEvaluator<MiningModel> modelEvaluator) {
        Map<FieldName, FieldValue> arguments = new LinkedHashMap<FieldName, FieldValue>();
        String[] lineArgs = line.split(",");

        if( lineArgs.length != 5) return arguments;

        FieldValue sepalLength = modelEvaluator.prepare(new FieldName("Sepal.Length"), lineArgs[0].isEmpty() ? 0 : lineArgs[0]);
        FieldValue sepalWidth = modelEvaluator.prepare(new FieldName("Sepal.Width"), lineArgs[1].isEmpty() ? 0 : lineArgs[1]);
        FieldValue petalLength = modelEvaluator.prepare(new FieldName("Petal.Length"), lineArgs[2].isEmpty() ? 0 : lineArgs[2]);
        FieldValue petalWidth = modelEvaluator.prepare(new FieldName("Petal.Width"), lineArgs[3].isEmpty() ? 0 : lineArgs[3]);

        arguments.put(new FieldName("Sepal.Length"), sepalLength);
        arguments.put(new FieldName("Sepal.Width"), sepalWidth);
        arguments.put(new FieldName("Petal.Length"), petalLength);
        arguments.put(new FieldName("Petal.Width"), petalWidth);

        return arguments;
    }

    public void printArgumentsOfModel(ModelEvaluator<MiningModel> modelEvaluator){
        System.out.println("### Active Fields of Model ####");
        for(FieldName fieldName : modelEvaluator.getActiveFields()){
            System.out.println("Field Name: "+ fieldName);
        }
    }

    public PMML createPMMLfromFile(String fileName) throws SAXException, JAXBException, FileNotFoundException {
        File pmmlFile = new File(App.class.getResource(fileName).getPath());
        String pmmlString = new Scanner(pmmlFile).useDelimiter("\\Z").next();

        InputStream is = new ByteArrayInputStream(pmmlString.getBytes());

        InputSource source = new InputSource(is);
        SAXSource transformedSource = ImportFilter.apply(source);

        return JAXBUtil.unmarshalPMML(transformedSource);
    }
}
