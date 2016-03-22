package barn1474.evolution;

import java.util.Random;

/**
 * Class for storing a chromosome and its fitness
 * @author barnett
 *
 */
public class BbbIndividual {

	static private double mutationFactor = 1.1;
	
	private BbbChromosome chromosome;
	private double fitness;
	
	/**
	 * Constructor
	 */
	public BbbIndividual(BbbChromosome chrome){
		super();
		chromosome = new BbbChromosome(chrome);
		fitness = -1;
	}
	
	/**
	 * Change some genes with some randomness
	 */
	public void mutate(){
		Random rand = new Random();
		//get randomly which gene to change
		int g = rand.nextInt(4);
		//change the gene by the mutation factor
		double newGene = chromosome.getGene(g) * mutationFactor;
		chromosome.setGene(g, newGene);
		
	}
	
	
	public BbbChromosome getChromosome() {
		return chromosome;
	}
	public void setChromosome(BbbChromosome chromosome) {
		this.chromosome = chromosome;
	}
	public double getFitness() {
		return fitness;
	}
	public void setFitness(double fitness) {
		this.fitness = fitness;
	}
	
	
	
	
}
