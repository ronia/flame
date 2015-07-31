package flame.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that stores localV analysis information
 * 
 * @author 					<a href=mailto:jaeyounb@usc.edu>Jae young Bang</a>
 * @version					2013.09
 */
public class LocalAnalysisInfo {
	private	java.util.List<String>				conflicts 			= new ArrayList<>();
	public	java.util.List<String>				getConflicts() 				{ return conflicts; }
	
	private	Map<String, java.util.List<String>>	warnings			= new HashMap<>();
	public	Map<String, java.util.List<String>>	getWarnings()				{ return warnings; }

	private String								arrivalTimeSCL		= "00000000_0000_00";
	public	String								getArrivalTimeSCL() 		{ return arrivalTimeSCL; }
	
	private Map<String, AnalysisInfo>			analysisTypeMap		= new HashMap<>();		
	public	Map<String, AnalysisInfo>			getAnalysisTypeMap()		{ return analysisTypeMap; }
	
	public class AnalysisInfo {
		private	String								overall 			= new String();
		public	String								getOverall() 				{ return overall; }
		
		private	java.util.List<String>				componentAnalysis 	= new ArrayList<>();
		public	java.util.List<String>				getComponentAnalysis() 		{ return componentAnalysis; }	
		
		private	String								arrivalTimeAnalysis	= "00000000_0000_00";
		public	String								getArrivalTimeAnalysis() 	{ return arrivalTimeAnalysis; }
		
		public	void setLocalAnalysis(	String									overall,
										java.util.List<String>					componentAnalysis,
										String									arrivalTime) {
			if(this.arrivalTimeAnalysis.compareTo(arrivalTime) < 0) {
				// copies the overall
				this.overall = new String (overall);
				
				// copies the component analysis results
				this.componentAnalysis = new ArrayList<>();
				if(componentAnalysis != null) {
					for(String component : componentAnalysis) {
						this.componentAnalysis.add(component);
					}
				}
				
				// copies arrival time
				this.arrivalTimeAnalysis = new String(arrivalTime);
			}
		}
	}
	
	
	/**
	 * Sets the local SCL
	 * 
	 * @param conflicts
	 * @param warnings
	 * @param arrivalTime
	 */
	public	void setLocalSCL(		java.util.List<String> 					conflicts,
									Map<String, java.util.List<String>>	 	warnings,
									String 									arrivalTime) {
		if(this.arrivalTimeSCL.compareTo(arrivalTime) < 0) {
			// copies the conflicts
			this.conflicts = new ArrayList<>();
			for(String conflict : conflicts) {
				this.conflicts.add(conflict);
			}
			
			// copies the warnings
			this.warnings = new HashMap<>();
			if(warnings != null) {
				for(String key : warnings.keySet()) {
					java.util.List<String> dst_warnings = new java.util.ArrayList<>();
					
					for(String warning : warnings.get(key)) {
						dst_warnings.add(warning);
					}
					
					this.warnings.put(key, dst_warnings);
				}
			}
			
			
			// copies arrival time
			this.arrivalTimeSCL = new String(arrivalTime);
		}
	}
	
	public	void setLocalAnalysis(	String 									type,
									String									overall,
									java.util.List<String>					componentAnalysis,
									String									arrivalTime) {
		if(analysisTypeMap.containsKey(type)) {
			AnalysisInfo oldInfo = analysisTypeMap.get(type);
			oldInfo.setLocalAnalysis(overall, componentAnalysis, arrivalTime);
		} else {
			AnalysisInfo newInfo = new AnalysisInfo();
			newInfo.setLocalAnalysis(overall, componentAnalysis, arrivalTime);
			analysisTypeMap.put(type, newInfo);
		}
	}
}