package flame.detectors.xteam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * XTEAM simulation result for an analysis type
 * 
 * @author 				<a href=mailto:jaeyounb@usc.edu>Jae young Bang</a>
 * @version				2013.05
 */
public class Result {
	
	/**
	 * Analysis type for this Result
	 */
	private String 					analysisType;
	
	private double					overall_totalValue				= 0;
	private double					overall_maxValue				= 0;
	private int						overall_numberOfValues			= 0;
	private int						overall_numberOfSuccesses		= 0;
	private int						numberOfComponents				= 0;
	
	private	Map<String, Double>		perComponentTotalValues			= new TreeMap<>();
	private Map<String, MaxValue>	perComponentMaxValues			= new TreeMap<>();
	private Map<String, Double>		perComponentAvgValues			= new TreeMap<>();
	private Map<String, Integer>	perComponentNumberOfValues		= new TreeMap<>();
	private Map<String, Integer>	perComponentNumberOfSuccesses	= new TreeMap<>();
	
	/**
	 * Pair of time and value that represent a maximum value
	 * 
	 * @author 				<a href=mailto:jaeyounb@usc.edu>Jae young Bang</a>
	 * @version				2013.10
	 */
	public class MaxValue {
		private double	time;
		private	double	value;
		
		public MaxValue(double time, double value) {
			this.time 	= time;
			this.value 	= value;
		}
		
		public double 	getMaxTime() 	{ return time; }
		public double 	getMaxValue() 	{ return value; }
	}
	
	/**
	 * Default constructor
	 * @param analysisType	Analysis type
	 */
	public Result (String analysisType) {
		this.analysisType = analysisType;
	}
	
	/**
	 * Gets the analysis type of this Result
	 * @return				Analysis type
	 */
	public String getAnalysisType() {
		return analysisType;
	}
	
	/**
	 * Gets the total value of this Result
	 * @return				Total value
	 */
	public double getTotalValue() {
		return overall_totalValue;
	}
	
	/**
	 * Gets the maximum value of this Result
	 * @return				Maximum value
	 */
	public double getMaxValue() {
		return overall_maxValue;
	}
	
	/**
	 * Gets the average value of this Result
	 * @return				Average value
	 */
	public double getAvgValue() {
		if(numberOfComponents != 0) {
			return overall_totalValue / (double) overall_numberOfSuccesses;
		} else {
			return 0;
		}
	}
	
	/**
	 * Gets the overall number of values
	 * @return				Overall number of values
	 */
	public int getNumberOfValues() {
		return overall_numberOfValues;
	}
	
	/**
	 * Gets the overall number of successes
	 * @return				Overall number of successes
	 */
	public int getNumberOfSuccesses() {
		return overall_numberOfSuccesses;
	}
	
	/**
	 * Gets the per-component total values
	 * @return				Per-component total values
	 */
	public List<String> getPerComponentTotalValues() {
		return convertToString(perComponentTotalValues);
	}
	
	/**
	 * Gets the per-component maximum values
	 * @return				Per-component maximum values
	 */
	public List<String> getPerComponentMaxValues() {
		return convertMaxToString(perComponentMaxValues);
	}
	
	/**
	 * Gets the per-component average values
	 * @return				Per-component average values
	 */
	public List<String> getPerComponentAvgValues() {
		return convertToString(perComponentAvgValues);
	}
	
	public List<String> getPerComponentNumberOfSuccesses() {
		List<String> ret = new ArrayList<> ();
		
		for (String key : perComponentNumberOfSuccesses.keySet()) {
			int 	success 	= perComponentNumberOfSuccesses.get(key);
			int 	value 		= perComponentNumberOfValues.get(key);
			double 	ratio 		= (double) success / (double) value; 
			ret.add(key + ": " 	+ String.format("%2.2f", ratio * (double) 100) + "% (" + success + "/" + value + ")");
		}
		
		return ret;
	}
	
	private List<String> convertToString(Map<String, Double> values) {
		List<String> ret = new ArrayList<> ();
		
		for (String key : values.keySet()) {
			ret.add(key + ": " + values.get(key));
		}
		
		return ret;
	}
	
	private List<String> convertMaxToString(Map<String, MaxValue> values) {
		List<String> ret = new ArrayList<> ();
		
		for (String key : values.keySet()) {
			ret.add(key + "(at " + values.get(key).getMaxTime() + ")" + ": " + values.get(key).getMaxValue());
		}
		
		return ret;
	}
	
	/**
	 * Adds per-component analysis result
	 * @param componentName	Component name
	 * @param value			Analysis value
	 */
	public void addComponentAnalysis(	String 	componentName, 
										double 	totalValue, 
										double	maxTime,
										double 	maxValue,
										int		numberOfValues,
										int		numberOfSuccesses) {
		
		// adds the componentName-value pair
		perComponentTotalValues.put(componentName, totalValue);
		perComponentMaxValues.put(componentName, new MaxValue(maxTime, maxValue));
		if(numberOfSuccesses != 0) {
			perComponentAvgValues.put(componentName, totalValue/numberOfSuccesses);
		} else {
			perComponentAvgValues.put(componentName, new Double(0));
		}
		perComponentNumberOfValues.put(componentName, numberOfValues);
		perComponentNumberOfSuccesses.put(componentName, numberOfSuccesses);
			
		// adds the value to the total value
		overall_totalValue += totalValue;
		
		// finds the max. value
		if(overall_maxValue < maxValue) {
			overall_maxValue = maxValue;
		}
		
		// increases the analysis number
		numberOfComponents++;
		
		// increases the overall number of values
		overall_numberOfValues += numberOfValues;
		
		// increases the overall number of successes
		overall_numberOfSuccesses += numberOfSuccesses;
	}
}
