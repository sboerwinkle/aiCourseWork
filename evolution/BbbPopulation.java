package barn1474.evolution;

/**
 * Implement a population for evolution, which is a collection of individuals
 * @author barnett
 *
 */
public class BbbPopulation {

	private static int populationCap = 50;
	private BbbIndividual[] individuals;
	private int populationIndex;
	
	public BbbPopulation(){
		super();
		individuals = new BbbIndividual[populationCap];
		populationIndex = 0;
		
	}
	
}
