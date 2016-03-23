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
	 * Constructor that reads data in from file
	 * @param fileName
	 */
	public BbbPopulation(String fileName){
		this();
		readFromFile(fileName);
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
			}
			
			
			buffR.close();
		} catch (FileNotFoundException e) {
			// file not found
			e.printStackTrace();
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
		System.out.println("Sorted");
		System.out.println(this);
		//truncate the population so we only take the top best
		while(individuals.size() > populationCap){
			individuals.remove(populationCap);
		}
		System.out.println("Truncated");
		System.out.println(this);
		//perform crossover
		doCrossover();
		System.out.println("Crossover");
		System.out.println(this);
		//mutate
		for (BbbIndividual i : individuals){
			i.getChromosome().mutate();
		}
		System.out.println("Mutated");
		System.out.println(this);
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
