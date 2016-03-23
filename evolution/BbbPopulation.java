package barn1474.evolution;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

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
	 * Read in csv and recreate list of individuals
	 * @param fileName
	 */
	public void readFromFile(String fileName)throws FileNotFoundException{
		// we could start with a blank list or just append
		//individuals.clear();
		String input;
		String vals[] = new String[5];
		BbbIndividual ind;
		try {
			BufferedReader buffR = new BufferedReader(new FileReader(fileName));
			while (buffR.ready()){
				input = buffR.readLine();
				vals = input.split(",");
				ind = new BbbIndividual(new BbbChromosome(
						Double.valueOf(vals[0]),
						Double.valueOf(vals[1]),
						Double.valueOf(vals[2]),
						Double.valueOf(vals[3])
						));
				ind.setFitness(Double.valueOf(vals[4]));
				individuals.add(ind);
			}
			
			buffR.close();
		} catch (FileNotFoundException e) {
			throw new FileNotFoundException();
		} catch (IOException e){
			//problem closing file
		}
		
	}
	
	/**
	 * Function to write population out to file
	 * Uses simple csv format.
	 * @param fileName
	 */
	public void writeToFile(String fileName){
		try {
			FileWriter fw = new FileWriter(fileName);
			for (BbbIndividual i : individuals){
				for (int j = 0; j < BbbChromosome.geneSize; j++){
					fw.append("" + i.getChromosome().getGene(j));
					fw.append(",");
				}
				fw.append("" + i.getFitness());
				fw.append("\n");
			}
			fw.flush();
			fw.close();
		} catch (IOException e) {
			// oh no what can we do
			e.printStackTrace();
		}
		
	}
	
	public void add(BbbIndividual ind){
		individuals.add(ind);
	}
	
	/**
	 * Returns individuals in order from population.
	 * If there are no more to return, the next population
	 * is automatically generated and its first individual is returned.
	 * @return an individual from the population
	 */
	public BbbIndividual getNextIndividual(){
		if (populationIndex == individuals.size()){
			//time for next generation
			nextGeneration();
			populationIndex = 0;
		}
		return individuals.get(populationIndex++);
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
	private void doCrossover(){
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
			//the fitness of this chromosome has not been measured yet
			i.setFitness(-1);
		}

	}
	
	@Override
	public String toString(){
		String out = "";
		for (BbbIndividual i : individuals){
			out = out + i + "\n";
		}
		return out;
	}
	
}
