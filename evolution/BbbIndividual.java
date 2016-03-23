package barn1474.evolution;

import java.util.Random;

/**
 * Class for storing a chromosome and its fitness together
 * @author barnett
 *
 */
public class BbbIndividual implements Comparable<BbbIndividual>{
	
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


	/**
	 * Comparison of individuals is based on fitness
	 */
	@Override
	public int compareTo(BbbIndividual other) {
		if (this.fitness == other.fitness) return 0;
		return this.fitness > other.fitness ? 1 : -1;
	}
	
	
	
	
}
