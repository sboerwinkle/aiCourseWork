package barn1474.evolution;

import java.util.Random;

public class BbbChromosome {

	static private double mutationFactor = 1.1;
	private static int geneSize = 4;
	private static double lowerGeneBound = .01;
	private static double upperGeneBound = 4.0;
	private double[] genes;

	/**
	 * Default constructor initializes a random gene
	 */
	public BbbChromosome() {
		super();
		genes = new double[geneSize];
		Random rand = new Random();
		genes[0] = rand.nextDouble() * (upperGeneBound - lowerGeneBound) + lowerGeneBound;
		genes[1] = rand.nextDouble() * (upperGeneBound - lowerGeneBound) + lowerGeneBound;
		genes[2] = rand.nextDouble() * (upperGeneBound - lowerGeneBound) + lowerGeneBound;
		genes[3] = rand.nextDouble() * (upperGeneBound - lowerGeneBound) + lowerGeneBound;

	}
	
	/**
	 * Initialize gene to parameter list
	 * @param g1
	 * @param g2
	 * @param g3
	 * @param g4
	 */
	public BbbChromosome(double g1, double g2, double g3, double g4){
		super();
		genes = new double[geneSize];
		genes[0] = g1;
		genes[1] = g2;
		genes[2] = g3;
		genes[3] = g4;
		
	}
	
	/**
	 * Copy constructor
	 * @param other
	 */
	public BbbChromosome(BbbChromosome other){
		this(other.getGene(0), other.getGene(1), other.getGene(2), other.getGene(3));
	}
	
	/**
	 * Mutation: pick one random gene and change it
	 * by a random, scaled, Gaussian distributed term
	 */
	public void mutate(){
		Random rand = new Random();
		//get randomly which gene to change
		int g = rand.nextInt(geneSize);
		//use Gaussian random to mutate
		double noise = rand.nextGaussian();
		genes[g] = genes[g] + noise * mutationFactor;
		
	}
	
	public void crossover(BbbChromosome otherChrome){
		Random rand = new Random();
		//get random crossover point
		int g = rand.nextInt(geneSize - 1);
		//swap genes up to crossover
		double temp;
		for (int i = 0; i <= g; i++){
			temp = genes[i];
			genes[i] = otherChrome.genes[i];
			otherChrome.genes[i] = temp;
		}
	}
	
	/**
	 * Get one of the genes from this chromosome
	 * @param gindex index of the gene to return
	 * @return double
	 */
	public double getGene(int gindex){
		if (gindex < 0 || gindex >= geneSize) throw new IndexOutOfBoundsException();
		return genes[gindex];
	}
	
	/**
	 * Set the value of one of the genes
	 * @param gindex index of the gene to set
	 * @param val double value to store in gene
	 */
	public void setGene(int gindex, double val){
		if (gindex < 0 || gindex >= geneSize) throw new IndexOutOfBoundsException();
		genes[gindex] = val;
	}
	
}
