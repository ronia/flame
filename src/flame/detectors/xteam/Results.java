package flame.detectors.xteam;

import java.util.HashSet;
import java.util.Set;

/**
 * XTEAM simulation results
 * 
 * @author 					<a href=mailto:jaeyounb@usc.edu>Jae young Bang</a>
 * @version					2013.05
 */
public class Results {
	
	/**
	 * Simulation results per analysis type
	 */
	private Set<Result> results = new HashSet<>();

	/**
	 * Default constructor
	 */
	public Results() {
		
	}
	
	/**
	 * Gets Result with the analysis type
	 * 
	 * @param analysisType	Analysis type
	 * @return				Result with the analysis type
	 */
	public Result getResult(String analysisType) {
		
		// Iterates through the Results
		for(Result result : results) {
			if(result.getAnalysisType().equals(analysisType)) {
				return result;
			}
		}
		
		return createResult(analysisType);
	}
	
	/**
	 * Creates new Result with the analysis type
	 * 
	 * @param analysisType	Analysis type
	 * @return				New Result
	 */
	public Result createResult(String analysisType) {
		// If the Result does not exist
		Result newResult = new Result(analysisType);
		results.add(newResult);
		
		return newResult;
	}
	
	/**
	 * Gets all Results
	 * 
	 * @return				All Results	
	 */
	public Set<Result> getResults() {
		return results;
	}
}
