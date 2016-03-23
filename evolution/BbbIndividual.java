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
	 * Default constructor uses random chromosome
	 */
	public BbbIndividual(){
		super();
		chromosome = new BbbChromosome();
		fitness = -1;
	}
	
	/**
	 * Constructor to specify chromosome
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
	
	@Override
	public String toString(){
		return "CHROM:" + chromosome + " FIT:" + String.format("%f",fitness);
	}
	
	
}
