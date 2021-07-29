package search.objective;

import connection.ResponseObject;
import search.Generator;
import search.Individual;
import test_drivers.TestDriver;
import util.Configuration;

import java.util.ArrayList;
import java.util.List;

import static util.RandomSingleton.getRandom;

/**
 * RandomFitness creates random fitness values for individuals (based on Gaussian distribution).
 */
public class RandomFitness extends Fitness {

    public RandomFitness(TestDriver testDriver) {
        super(testDriver);
    }

    @Override
    public void evaluate(Generator generator, List<Individual> population) {
        // Call methods
        List<ResponseObject> responseObjects = getResponses(population);

        if (getTestDriver().shouldContinue()) {

            for (int i = 0; i < population.size(); i++) {
                Individual individual = population.get(i);

                double fitness = getRandom().nextGaussian();
                individual.setFitness(fitness);

                // decide whether to add individual to the archive
                if (fitness >= Configuration.getARCHIVE_THRESHOLD_RANDOM() && !getArchive().contains(individual)) {
                    this.addToArchive(individual, responseObjects.get(i));
                }
            }

        }

    }

    @Override
    public ArrayList<String> storeInformation() {
        return new ArrayList<>();
    }

}
