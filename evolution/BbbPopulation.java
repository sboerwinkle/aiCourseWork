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

	private static int populationCap = 30;
	private ArrayList<BbbIndividual> individuals;
	private int populationIndex;
	private static final String graphDataFile = "GA_graph_data.csv";
	
	public BbbPopulation(){
		super();
		individuals = new ArrayList<BbbIndividual> (populationCap);
		populationIndex = 0;
	}
	
	
	/**
	 * Read in csv and recreate list of individuals
	 * @param fileName
	 */
	public void readFromFile(String fileName){
		// we could start with a blank list or just append
		//individuals.clear();
		String input;
		String vals[] = new String[5];
		BbbIndividual ind;
		try {
			BufferedReader buffR = new BufferedReader(new FileReader(fileName));
			//first line should be index
			input = buffR.readLine();
			populationIndex = Integer.valueOf(input);
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
				add(ind);
			}
			
			buffR.close();
		} catch (FileNotFoundException e) {
			// then just make some random ones
			BbbIndividual id;
			for (int j = 0; j < populationCap; j++){
				id = new BbbIndividual();
				add(id);
			};
		} catch (IOException e){
			//problem closing file
			e.printStackTrace();
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
			//write index first
			fw.append(populationIndex + "\n");
			//now write all chromosomes
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
	

	/**
	 * Function to write this generation for analysis later
	 * Uses simple csv format.
	 * @param fileName
	 */
	public void writeGraphData(){
		try {
			FileWriter fw = new FileWriter(graphDataFile, true);
			//now append all individuals in this generation
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
			//but first save stats on this generation
			writeGraphData();
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
	 *  producing three children
	 */
	private void doCrossover(){
		//get current size to do since we will be adding individuals
		int popSize = individuals.size();
		//pairwise crossover individuals
		for (int i = 0; i < popSize / 2 ; i++){
			individuals.get(i * 2).getChromosome().crossover(
					individuals.get(i * 2 + 1).getChromosome());
			//create third child
			BbbIndividual parent = new BbbIndividual(individuals.get(i*2));
			BbbIndividual third = new BbbIndividual(individuals.get(i*2 + 1));
			parent.getChromosome().crossover(third.getChromosome());
			//append third child to population
			add(third);
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
		
		//perform crossover
		doCrossover();
		
		//sort again because we added members
		Collections.sort(individuals);
		
		//truncate the population so we only take the top best
		while(individuals.size() > populationCap){
			individuals.remove(populationCap);
		}
		
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
