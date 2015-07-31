package flame.analyzer;

import java.nio.file.Path;

public class LogFilePaths {
	/**
	 * Path to the directory at which the log files are located
	 */
	private			Path						dataPath;
		
	/**
	 * Path to the parent result directory that contains all sub-result directories
	 */
	private			Path						allResultPath;
	
	/**
	 * Path to the directory where the output result is going to be stored
	 */
	private			Path						resultPath;
	
	/**
	 * Path to the default values file
	 */
	private			Path						defaultValuePath;
	
	/**
	 * Path to the option values file
	 */
	private			Path						optionValuePath;
	
	/**
	 * Path to the FLAME log file that has events
	 */
	private			Path						logEventPath;
	
	/**
	 * Path to the FLAME log file that has analyses
	 */
	private			Path						logAnalysisPath;
	
	/**
	 * Path to the FLAME log file that has snapshots
	 */
	private			Path						logSnapshotPath;
	
	/**
	 * Path to the analysis output file that has combined analysis/snapshot
	 */
	private			Path						outCombinedAnalysisPath;
	
	/**
	 * Path to the analysis output file that has combined event/snapshot
	 */
	private			Path						outCombinedEventPath;
	
	/**
	 * Path to the analysis output file that has inconsistency analysis
	 */
	private			Path						outInconsistencyPath;
	
	/**
	 * Path to the analysis output file that has unhandled inconsistencies at snapshots 
	 */
	private			Path						outSnapshotPath;
	
	/**
	 * Path to the analysis output file that has unhandled inconsistencies at updates
	 */
	private			Path						outUpdatePath;
	
	/**
	 * Path to the analysis output file that has the human-readable value changes
	 */
	private			Path						outValueChangePath;
	
	/**
	 * Path to the analysis output file that has the human-readable option changes
	 */
	private			Path						outOptionChangePath;
	
	/**
	 * Path to the all analysis file 
	 */
	private			Path						allAnalysesPath;
	
	/**
	 * Path to the all events file
	 */
	private			Path						allEventsPath;
	
	/**
	 * Path to the all conflicts file
	 */
	private			Path						allConflictsPath;
	
	/**
	 * Path to the all conflicts at commits file
	 */
	private			Path						allConflictsAtCommitsPath;
	
	/**
	 * Path to the all conflicts at updates file
	 */
	private			Path						allConflictsAtUpdatesPath;
	
	/**
	 * Path to the all value changes file
	 */
	private			Path						allValueChanges;
	
	/**
	 * Path to the all option changes file
	 */
	private			Path						allOptionChanges;
	
	/**
	 * Default creator 
	 * 
	 * @param dataPath							Path to the directory at which the log files are located
	 * @param allResultPath						Path to the parent result directory that contains all sub-result directories
	 * @param resultPath						Path to the directory where the output result is going to be stored
	 * @param datDefault						Path to the default values file
	 * @param datOption							Path to the option values file
	 * @param logEventPath						Path to the FLAME log file that has events
	 * @param logAnalysisPath					Path to the FLAME log file that has analyses
	 * @param logSnapshotPath					Path to the FLAME log file that has snapshots
	 * @param outCombinedAnalysisPath			Path to the analysis output file that has combined analysis/snapshot
	 * @param outCombinedEventPath				Path to the analysis output file that has combined event/snapshot
	 * @param outInconsistencyPath				Path to the analysis output file that has inconsistency analysis
	 * @param outSnapshotPath					Path to the analysis output file that has unhandled inconsistencies at snapshots
	 * @param outUpdatePath						Path to the analysis output file that has unhandled inconsistencies at updates
	 * @param outOptionPath						Path to the analysis output file that has the human-readable option switches
	 * @param outAllAnalyses					Path to the all analysis file
	 * @param outAllEvents						Path to the all events file
	 * @param outAllConflicts					Path to the all conflicts file
	 * @param outAllConflictsAtCommits			Path to the all conflicts at commits file
	 * @param outAllConflictsAtUpdates			Path to the all conflicts at updates file
	 * @param outAllValueChanges				Path to the all value changes file
	 * @param outAllOptionChanges				Path to the all option changes file
	 */
	public 			LogFilePaths (				Path dataPath, 
												Path allResultPath,
												Path resultPath,
												Path datDefault,
												Path datOption,
												Path logEventPath,
												Path logAnalysisPath,
												Path logSnapshotPath,
												Path outCombinedAnalysisPath,
												Path outCombinedEventPath,
												Path outInconsistencyPath,
												Path outSnapshotPath,
												Path outUpdatePath,
												Path outValueChangePath,
												Path outOptionChangePath,
												Path outAllAnalyses,
												Path outAllEvents,
												Path outAllConflicts,
												Path outAllConflictsAtCommits,
												Path outAllConflictsAtUpdates,
												Path outAllValueChanges,
												Path outAllOptionChanges) {
		// stores the paths
		this.dataPath 						= dataPath;
		this.allResultPath					= allResultPath;
		this.resultPath						= resultPath;
		
		this.defaultValuePath				= datDefault;
		this.optionValuePath				= datOption;
		
		this.logEventPath 					= logEventPath;
		this.logAnalysisPath				= logAnalysisPath;
		this.logSnapshotPath				= logSnapshotPath;
		
		this.outCombinedAnalysisPath		= outCombinedAnalysisPath;
		this.outCombinedEventPath			= outCombinedEventPath;
		this.outInconsistencyPath			= outInconsistencyPath;
		this.outSnapshotPath				= outSnapshotPath;
		this.outUpdatePath					= outUpdatePath;
		this.outValueChangePath				= outValueChangePath;
		this.outOptionChangePath			= outOptionChangePath;
		
		this.allAnalysesPath				= outAllAnalyses;
		this.allEventsPath					= outAllEvents;
		this.allConflictsPath				= outAllConflicts;
		this.allConflictsAtCommitsPath		= outAllConflictsAtCommits;
		this.allConflictsAtUpdatesPath		= outAllConflictsAtUpdates;
		this.allValueChanges				= outAllValueChanges;
		this.allOptionChanges				= outAllOptionChanges;
	}
	
	/**
	 * Returns the path to the directory at which the log files are located
	 * @return 				Path to the directory at which the log files are located
	 */
	public			Path						getDataPath() 						{ return dataPath; }

	/**
	 * Returns the path to the parent result directory that contains all sub-result directories
	 * @return				Path to the parent result directory that contains all sub-result directories
	 */
	public			Path						getAllResultPath() 					{ return allResultPath; }
	
	/**
	 * Returns the path to the directory where the output result is going to be stored
	 * @return				Path to the directory where the output result is going to be stored
	 */
	public			Path						getResultPath() 					{ return resultPath; }
	
	/**
	 * Returns the path to the default values file
	 * @return				Path to the default values file
	 */
	public			Path						getDefaultValuePath() 				{ return defaultValuePath; }
	
	/**
	 * Returns the path to the option values file
	 * @return				Path to the option values file
	 */
	public			Path						getOptionValuePath()				{ return optionValuePath; }
	
	/**
	 * Returns the path to the FLAME log file that has events
	 * @return				Path to the FLAME log file that has events
	 */
	public			Path						getLogEventPath()					{ return logEventPath; }
	
	/**
	 * Returns the path to the FLAME log file that has analyses
	 * @return				Path to the FLAME log file that has analyses
	 */
	public			Path						getLogAnalysisPath()				{ return logAnalysisPath; }
	
	/**
	 * Returns the path to the FLAME log file that has snapshots
	 * @return				Path to the FLAME log file that has snapshots
	 */
	public			Path						getLogSnapshotPath()				{ return logSnapshotPath; }
	
	/**
	 * Returns the path to the analysis output file that has combined analysis/snapshot
	 * @return				Path to the analysis output file that has combined analysis/snapshot
	 */
	public			Path						getOutCombinedAnalysisPath() 		{ return outCombinedAnalysisPath; }
	
	/**
	 * Returns the path to the analysis output file that has combined event/snapshot
	 * @return				Path to the analysis output file that has combined event/snapshot
	 */
	public			Path						getOutCombinedEventPath() 			{ return outCombinedEventPath; }
	
	/**
	 * Returns the path to the analysis output file that has inconsistency analysis
	 * @return				Path to the analysis output file that has inconsistency analysis
	 */
	public			Path						getOutInconsistencyPath()			{ return outInconsistencyPath; }
	
	/**
	 * Returns the path to the analysis output file that has unhandled inconsistencies at snapshots 
	 * @return				Path to the analysis output file that has unhandled inconsistencies at snapshots 
	 */
	public			Path						getOutSnapshotPath()				{ return outSnapshotPath; }

	/**
	 * Returns the path to the analysis output file that has unhandled inconsistencies at updates
	 * @return
	 */
	public			Path						getOutUpdatePath()					{ return outUpdatePath; }
	
	/**
	 * Returns the path to the analysis output file that has the human-readable value changes
	 * @return				Path to the analysis output file that has the human-readable value changes
	 */
	public			Path						getOutValueChangePath()				{ return outValueChangePath; }
	
	/**
	 * Returns the path to the analysis output file that has the human-readable option changes
	 * @return				Path to the analysis output file that has the human-readable option changes
	 */
	public			Path						getOutOptionChangePath()			{ return outOptionChangePath; }
	
	/**
	 * Returns the path to the all analysis file
	 * @return				Path to the all analysis file  
	 */
	public			Path						getAllAnalysesPath()				{ return allAnalysesPath; }
	
	/**
	 * Returns the path to the all events file
	 * @return				Path to the all events file
	 */
	public			Path						getAllEventsPath()					{ return allEventsPath; }
	
	/**
	 * Returns the path to the all conflicts file
	 * @return				Path to the all conflicts file
	 */
	public			Path						getAllConflictsPath()				{ return allConflictsPath; }
	
	/**
	 * Returns the path to the all conflicts at commits file
	 * @return				Path to the all conflicts at commits file
	 */
	public			Path						getAllConflictsAtCommitsPath()		{ return allConflictsAtCommitsPath; }
	
	/**
	 * Returns the path to the all conflicts at updates file
	 * @return				Path to the all conflicts at updates file
	 */
	public			Path						getAllConflictsAtUpdatesPath()		{ return allConflictsAtUpdatesPath; }
	
	/**
	 * Returns the path to the all value changes file
	 * @return				Path to the all value changes file
	 */
	public			Path						getAllValueChangesPath()			{ return allValueChanges; }
	
	/**
	 * Returns the path to the all option changes file
	 * @return				Path to the all option changes file
	 */
	public			Path						getAllOptionChangesPath()			{ return allOptionChanges; }
}
