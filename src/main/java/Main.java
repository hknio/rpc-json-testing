import connection.Client;
import org.json.JSONObject;
import search.BasicEA;
import search.Generator;
import search.Individual;
import search.objective.DiversityBasedFitness;
import search.objective.Fitness;
//import search.objective.PairwiseFitness;
import search.objective.RandomFitness;
import search.objective.ResponseFitnessClustering;
import search.objective.ResponseFitnessClustering2;
import search.objective.ResponseFitnessPredefinedTypes;
import search.objective.ResponseStructureFitness2;
import search.objective.ResponseStructureFitness3;
import search.objective.StatusCodeFitness;
import search.openRPC.Specification;
import test_drivers.GanacheTestDriver;
import test_drivers.RippledTestDriver;
import test_drivers.RippledTestDriverTestNet;
import test_drivers.TestDriver;
import test_generation.TestWriter;
import util.IO;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Main {

    private static ArrayList<Double> bestFitness = new ArrayList<>();

    public static void main(String args[]) {
        String fitnessFunction = "";
        int runningTime = 60; //default value
        try {
            fitnessFunction = args[0]; // 1, 2, 3, 4, 5, 6, 7 or 8, default is 1
            runningTime = Integer.parseInt(args[1]); // time in minutes, default is 1 hour
        }
        catch (ArrayIndexOutOfBoundsException e){
            System.out.println("Argument(s) not specified. Default value(s) used.");
        }
        catch (NumberFormatException e){
            System.out.println("Time limit argument is not an integer. Default time limit used: 24 hours.");
        }

        File jar = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        String directory = jar.getParentFile().getAbsolutePath();

//        String filepath = directory + System.getProperty("file.separator") + "ripple-openrpc.json";
        String filepath = directory + System.getProperty("file.separator") + "ethereum-openrpc.json";

        Specification specification = null;

        try {
            String data = IO.readFile(filepath);
            specification = new Specification(new JSONObject(data));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Generator generator = new Generator(specification);

        try {
            // The url for the Ripple JSON-RPC API ledger (testnet)
//            String url_ripple = "https://s.altnet.rippletest.net:51234";
//            String url_ripple = "http://127.0.0.1:5005";
            String url_ganache = "http://127.0.0.1:8545";

//            URL url = new URL(url_ripple);
            URL url = new URL(url_ganache);

            Client client = new Client(url);
//            TestDriver testDriver = new RippledTestDriver(client);
//            TestDriver testDriver = new RippledTestDriverTestNet(client);
            TestDriver testDriver = new GanacheTestDriver(client);
            Fitness fitness;

            switch (fitnessFunction) {
                case "1":
                    System.out.println("Using 1: RandomFitness");
                    fitness = new RandomFitness(testDriver);
                    break;
                case "2":
                    System.out.println("Using 2: StatusCodeFitness");
                    fitness = new StatusCodeFitness(testDriver);
                    break;
                case "3":
                    System.out.println("Using 3: ResponseFitnessPredefinedTypes");
                    fitness = new ResponseFitnessPredefinedTypes(testDriver);
                    break;
                case "4":
                    System.out.println("Using 4: ResponseFitnessClustering");
                    fitness = new ResponseFitnessClustering(testDriver);
                    break;
                case "5":
                    System.out.println("Using 5: ResponseFitnessClustering2");
                    fitness = new ResponseFitnessClustering2(testDriver);
                    break;
                case "6":
                    System.out.println("Using 6: ResponseStructureFitness2");
                    fitness = new ResponseStructureFitness2(testDriver);
                    break;
                case "7":
                    System.out.println("Using 7: ResponseStructureFitness3");
                    fitness = new ResponseStructureFitness3(testDriver);
                    break;
                case "8":
                    System.out.println("Using 8: DiversityBasedFitness");
                    fitness = new ResponseStructureFitness3(testDriver);
                    break;
                default:
                    System.out.println("No or invalid argument specified. Using default fitness: RandomFitness");
                    fitness = new RandomFitness(testDriver);
            }
            System.out.println("Experiment will run for " + runningTime + " minute(s) = " + ((double) runningTime/60) + " hour(s)");

            BasicEA ea = new BasicEA(fitness, generator);
            List<Individual> population = ea.generatePopulation(50);

            // stopping criterium: time
            Long startTime = System.currentTimeMillis();
            int generation = 0;
            while (System.currentTimeMillis() - startTime < (runningTime*60*1000)) {
                generation += 1;
                long start = System.nanoTime();
                population = ea.nextGeneration(population);

                // keeping records of the highest fitness in each generation
                double maxFitness = 0;
                for (Individual ind : population) {
                    if (ind.getFitness() > maxFitness) {
                        maxFitness = ind.getFitness();
                    }
                }
                bestFitness.add(maxFitness);
                System.out.println("Generation: " + generation + " : " + maxFitness);

//                System.out.println("Generation time: " + ((System.nanoTime() - start) / 1000000d));

            }

            // stopping criterium: generations
//            for (int i = 0; i < 20; i++) {
//                System.out.println("Generation: " + i);
//                population = ea.nextGeneration(population);
//            }

            // Write tests for the best individuals
            String testDirectory = System.getProperty("user.dir") + "/src/test/java/generated";
            TestWriter testWriter = new TestWriter(url_ganache, testDirectory, "GanacheTestDriver");

            // Write archive size and best fitness values of each generation to file
            FileWriter writer = new FileWriter("archiveSize_bestFitnessValues.txt");

            File directory_tests = new File(testDirectory);
            int fileCount = Objects.requireNonNull(directory_tests.list()).length;
            writer.write("Amount of tests in the archive: " + fileCount + "\n");

            for(Double fit: bestFitness) {
                writer.write(fit + System.lineSeparator());
            }
            writer.close();

            // Delete old test files
            for (File file : new java.io.File(testDirectory).listFiles()) {
                if (!file.isDirectory())
                    file.delete();
            }

            List<Individual> archive = fitness.getArchive();
            System.out.println("Tests in the archive: " + archive.size());
            for (int i = 0; i < archive.size(); i++) {
                testWriter.writeTest(archive.get(i), "ind" + i + "Test");
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
