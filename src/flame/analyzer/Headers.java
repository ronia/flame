package flame.analyzer;

public class Headers {
	
	/**
	 * Events and snapshots file headers
	 */
	public static final String[]					events						= { "time_finish",
																					"event_type",
																					"origin_component",
																					"username",
																					"event_id",
																					"value",
																					"value_change",
																					"option_change",
																					"blank_1",
																					"blank_2",
																					"blank_3",
																					"blank_4",
																					"blank_5",
																					"blank_6",
																					"blank_7",
																					"blank_8",
																					"time_begin" };
	
	/**
	 * Analyses and snapshots file headers
	 */
	public static final String[]					analyses					= { "time_finish",
																					"event_type",
																					"origin_component",
																					"username",
																					"event_id",
																					"analysis_type",
																					"syntactic_conflict",
																					"semantic_warning",
																					"overall_total",
																					"overall_max",
																					"overall_avg",
																					"overall_success",
																					"per_component_total",
																					"per_component_max",
																					"per_component_avg",
																					"per_component_success",
																					"time_begin",
																					"open_syntactic_con",
																					"open_semantic_warning",
																					"open_semantic_con_overall",
																					"open_semantic_con_per_com",
																					"open_con_total",
																					"session_begin" };
	
	/**
	 * Conflicts file headers
	 */
	public static final	String[] 					conflicts					= {	"session_begin",
																					"conflict_type", 
																					"username",
																					"time_created",
																					"number_of_commits",
																					"time_first_committed",
																					"number_of_updates",
																					"time_first_updated",
																					"time_resolved",
																					"resolved",
																					"lifetime_in_sec",
																					"value" };
	/**
	 * Unhandled inconsistencies at Snapshots file headers
	 */
	public static final	String[]					conflicts_at_sync			= {	"conflict_type",
																					"engine_name",
																					"username",
																					"time_created",
																					"sync_time",
																					"lifetime_until_sync",
																					"value" };
	
	/**
	 * Human-readable value changes file headers
	 */
	public static final String[]					value_changes				= { "time_created",
																					"username",
																					"event_id",
																					"parent_obj_name",
																					"obj_name",
																					"attr_name",
																					"current_value",
																					"new_value" };
	/**
	 * Human-readable option changes file headers
	 */
	public static final String[]					option_changes				= { "time_created",
																					"username",
																					"event_id",
																					"task_number",
																					"participant_number",
																					"object_number",
																					"current_option",
																					"new_option" };
	
	/**
	 * Extra headers for the "all" output files
	 */
	public static final String[]					extra_headers_for_all		= {	"team_no",
																					"session_no",
																					"pcd" };
	
	
	/**
	 * Headers for the "all" analyses
	 */
	public static final	String[]					all_analyses				= {	"team_no",	
																					"session_no",	
																					"pcd",
																					"analysis_type",	
																					"analysis_start_time",	
																					"analysis_end_time" };
	/**
	 * Headers for the "all" events
	 */
	public static final	String[]					all_events					= {	"team_no",	
																					"session_no",	
																					"pcd",	
																					"username",	
																					"event_time",	
																					"change_detail",	
																					"decision_user_number",	
																					"decision_obj_number",
																					"decision_option_number",	
																					"decision_option_chosen" };
	
	public static final String[]					all_conflicts				= {	"team_no",
																					"session_no",
																					"pcd",	
																					"analysis_type",
																					"username",	
																					"time_create",
																					"time_resolved",
																					"lifetime_in_seconds",
																					"conflict_detail" };
	
	public static final String[]					all_conflicts_at_commits	= { "team_no",
																					"session_no",
																					"pcd",
																					"analysis_type",
																					"username",
																					"time_create",
																					"time_commit",
																					"lifetime_until_commit",
																					"conflict_detail" };
	
	public static final String[]					all_value_changes			= { "team_no",
																					"session_no",
																					"pcd",
																					"time_created",
																					"username",
																					"event_id",
																					"parent_obj_name",
																					"obj_name",
																					"attr_name",
																					"current_value",
																					"new_value" };
	
	public static final String[]					all_option_changes			= { "team_no",
																					"session_no",
																					"pcd",
																					"time_created",
																					"username",
																					"event_id",
																					"task_number",
																					"participant_number",
																					"object_number",
																					"current_option",
																					"new_option" };
}
