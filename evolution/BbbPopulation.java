package barn1474.evolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Implement a population for evolution, which is a collection of individuals
 * @author barnett
 *
 */
public class BbbPopulation {

	private static int populationCap = 50;
	private ArrayList<BbbIndividual> individuals;
	private int populationIndex;
	
	public BbbPopulation(){
		super();
		individuals = new ArrayList<BbbIndividual> (populationCap);
		populationIndex = 0;
	}
	
	/**
	 * Constructor that reads data in from file
	 * @param fileName
	 */
	public BbbPopulation(String fileName){
		this();
		readFromFile(fileName);
	}
	
	public void readFromFile(String fileName){
		
	}
	
	/**
	 * Function to write population out to file
	 * @param fileName
	 */
	public void writeToFile(String fileName){
		
	}
	
	/**
	 * Tell how many individuals are in the population
	 * @return integer size
	 */
	public int getPopulationSize(){
		return individuals.size();
	}
	
	/**
	 * Perform the crossover on this population
	 * The crossover method will be single-point random
	 *  producing two children
	 */
	public void doCrossover(){
		//pairwise crossover all individuals
		for (int i = 0; i < individuals.size() / 2 ; i++){
			individuals.get(i * 2).getChromosome().crossover(
					individuals.get(i * 2 + 1).getChromosome());
		}
	}
	
	/**
	 * Create the next generation by doing selection,
	 * crossover, and mutation.
	 * The selection method will be rank selection
	 */
	public void nextGeneration(){
		//sort so we can do by rank
		Collections.sort(individuals);
		
		//truncate the population so we only take the top best
		while(individuals.size() > populationCap){
			individuals.remove(populationCap);
		}
		
		//perform crossover
		doCrossover();
		
		//mutate
		for (BbbIndividual i : individuals){
			i.getChromosome().mutate();
		}
	}
	
}
