package search.objective;

import connection.ResponseObject;
import search.Generator;
import search.Individual;
import test_drivers.TestDriver;
import util.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This ResponseStructureFitness uses the stripValues function from Fitness.
 */
public class ResponseStructureFitness extends Fitness {
    private static String STANDARD_STRING = "";
    private static Boolean STANDARD_BOOLEAN = true;
    private static Integer STANDARD_NUMBER = 0;

    private Map<String, Integer> structureFrequencyTable;

    public ResponseStructureFitness() {
        super();
        this.structureFrequencyTable = new HashMap<>();
    }

    @Override
    public void evaluate(Generator generator, List<Individual> population) {
        // Fill in hashmap with structure frequency
        for (Individual individual : population) {

            String structureString = stripValues(individual.toTotalJSONObject(), individual.getResponseObject().getResponseObject()).toString();

            if (!structureFrequencyTable.containsKey(structureString)) {
                structureFrequencyTable.put(structureString, 0);
            }
            structureFrequencyTable.put(structureString, structureFrequencyTable.get(structureString) + 1);

            // Evaluate individual compared to the map
            double fitness = (double) 1 / structureFrequencyTable.get(stripValues(individual.toTotalJSONObject(), individual.getResponseObject().getResponseObject()).toString());
            individual.setFitness(fitness);

//            ARCHIVE_THRESHOLD = Math.min((100 / structureFrequencyTable.size()), ARCHIVE_THRESHOLD); // if structure is relatively rare, add to archive.
            // decide whether to add individual to the archive
            if (individual.getResponseObject().getResponseCode() > 499 && !getArchive().contains(individual)) {
                this.addToArchive(individual);
            } else if (fitness >= Configuration.ARCHIVE_THRESHOLD && !getArchive().contains(individual)) {
                this.addToArchive(individual);
            }
        }

    }

    @Override
    public ArrayList<String>  storeInformation() {
        ArrayList<String> info = new ArrayList<>();
        info.add("Map: " + structureFrequencyTable.keySet().size());
        return info;
    }

}
