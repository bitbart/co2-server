package it.unica.tcs;

import cern.jet.random.Normal;
import cern.jet.random.engine.RandomEngine;

/**Generates normal distributed random integers  */
public class NormalGenerator {
	private RandomEngine randomEngine;
	private double standardDeviation;
	
	/** Instantiates a random numbers generator object 
	 * 
	 * @param sd standard deviation of the normal distribution
	 */
	public NormalGenerator(double sd){
		randomEngine = RandomEngine.makeDefault();
		this.standardDeviation = sd;
	}
	/**Generates a number in the range [0, limit) (limit not included).
	 * 
	 * @param limit 
	 * @return integer value
	 */
	public int next(int limit){
		
		if (limit == 0)
			return 0;
		
		double standardDeviation = this.standardDeviation;
		
		double correctionFactor = Math.log10(limit);
		if(correctionFactor > 1){
		    standardDeviation = standardDeviation / correctionFactor;
		}
		
		Normal normalGen = new Normal(0, standardDeviation, randomEngine);;
		
		double n=-1;		
		while(n<0 || n>=limit){
			n = normalGen.nextDouble()*limit;
			Log.message().fine("Extracted: " + n);
		}
		
		return (int) Math.floor(n);		
	}

}