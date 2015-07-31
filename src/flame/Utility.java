package flame;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

public class Utility {
	
	/**
	 * To Camel-case
	 * @param s
	 * @return
	 */
	public static String toCamelCase(String s) {
	    return s.substring(0, 1).toUpperCase() +
	               s.substring(1).toLowerCase();
	}
	
	public static final String flameDateFormat = "yyyyMMdd_HHmm_ss";
	public static final String excelDateFormat = "MM/dd/yyyy HH:mm:ss";
	
	/**
	 * Converts the time from milliseconds to human-readable format
	 * @param millisecs			Current system time in milliseconds
	 * @return					Human-readable time in "yyyyMMdd_kkmm_ss" format
	 */
	public static String convertDate (long millisecs) {
		SimpleDateFormat date_format = new SimpleDateFormat(flameDateFormat);
		
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(millisecs);
		
	    return date_format.format(c.getTime());
	}
	
	/**
	 * Reverts the time from human-readable format to milliseconds
	 * @param timestamp			A timestamp string
	 * @return					The timestamp time in milliseconds
	 * @throws ParseException	Invalid timestamp
	 */
	public static long revertDate (String timestamp) throws ParseException {
		SimpleDateFormat date_format = new SimpleDateFormat(flameDateFormat);
		
		Calendar c = Calendar.getInstance();
		c.setTime(date_format.parse(timestamp));
		
		return c.getTimeInMillis();
	}
	
	/**
	 * Converts the time from FLAME format to Excel-friendly format
	 * @param timestamp			A FLAME timestamp string 
	 * @return					An Excel-friendly timestamp string
	 * @throws ParseException	Invalid timestamp
	 */
	public static String convertDateForExcel (String timestamp) throws ParseException {
		SimpleDateFormat date_format 	= new SimpleDateFormat(flameDateFormat);
		SimpleDateFormat excel_format	= new SimpleDateFormat(excelDateFormat);
		
		Calendar c = Calendar.getInstance();
		c.setTime(date_format.parse(timestamp));
		
		return excel_format.format(c.getTime());
	}
	
	/**
	 * Gets a property (in String) from config.properties
	 * 
	 * @param propName		Property name
	 * @return				Property value
	 * @throws Exception	Property is not specified
	 */
	public static String getProperty(Properties props, String propName) throws Exception {
		String value = props.getProperty(propName);
		
		// if the propName is not specified
		if(value == null) {
			throw new Exception (propName + " must be specified in config.properties.");
		}
		
		return value;
	}
	
	/**
	 * Gets a property (in int) from config.properties
	 * 
	 * @param propName		Property name
	 * @return				Property value in int
	 * @throws Exception	Property is either not specified or ill-formatted
	 */
	public static int getIntProperty(Properties props, String propName) throws Exception {
		String 	strValue;
		int		value;
		
		// gets the string property
		try {
			strValue = getProperty(props, propName);
		} catch (Exception e) {
			throw e;
		}
		
		// converts the string property value to integer
		try {
			value = Integer.parseInt(strValue);
		} catch (NumberFormatException nfe) {
			throw new Exception (propName + " property in config.properties is ill-formatted.");
		}
		
		return value;
	}
	
	/**
	 * Gets a property (in double) from config.properties
	 * 
	 * @param propName		Property name
	 * @return				Property value in double
	 * @throws Exception	Property is either not specified or ill-formatted
	 */
	public static double getDoubleProperty(Properties props, String propName) throws Exception {
		String 	strValue;
		double	value;
		
		// gets the string property
		try {
			strValue = getProperty(props, propName);
		} catch (Exception e) {
			throw e;
		}
		
		// converts the string property value to integer
		try {
			value = Double.parseDouble(strValue);
		} catch (NumberFormatException nfe) {
			throw new Exception (propName + " property in config.properties is ill-formatted.");
		}
		
		return value;
	}
	
	/**
	 * Gets a property (in Path) from config.properties
	 * 
	 * @param propName		Property name
	 * @return				Property path
	 * @throws Exception	Property is either not specified or the path is invalid
	 */
	public static Path getPathProperty(Properties props, String propName) throws Exception {
		String 	strValue;
		Path	path;
		
		// gets the string property
		try {
			strValue = getProperty(props, propName);
		} catch (Exception e) {
			throw e;
		}
		
		// initializes the path
		try {
			path = Paths.get(strValue);
		} catch (InvalidPathException ipe) {
			throw new Exception (propName + " is not a valid path.");
		}
		
		return path;
	}
	
	/**
	 * Gets a property (in boolean) from config.properties
	 * 
	 * @param propName		Property name
	 * @return				Property boolean
	 * @throws Exception	Property is not specified
	 */
	public static boolean getBooleanProperty(Properties props, String propName) throws Exception {
		String 	strValue;
		boolean	ret 		= false;
		
		// gets the string property
		try {
			strValue = getProperty(props, propName);
		} catch (Exception e) {
			throw e;
		}
		
		if(strValue.toLowerCase().equals("on") || strValue.toLowerCase().equals("true")) {
			ret = true;
		}
		
		return ret;
	}
	
	/**
	 * Converts FLAME timestamp to Calendar
	 * @param time_string
	 * @return
	 * @throws ParseException
	 */
	public	static Calendar convertTimeStoC (String time_string) throws ParseException {
		SimpleDateFormat 	date_format 	= new SimpleDateFormat(flameDateFormat);
		Calendar c 							= Calendar.getInstance();
		c.setTime(date_format.parse(time_string));
		return c;
	}
	
	/**
	 * Converts Calendar to FLAME timestamp
	 * @param time_calendar
	 * @return
	 */
	public	static String convertTimeCtoS (Calendar time_calendar) {
		SimpleDateFormat 	date_format 	= new SimpleDateFormat(flameDateFormat);
		return date_format.format(time_calendar.getTime());
	}

	/**
	 * Diffs two Calendar instances (C2 - C1)
	 * 
	 * @param c1
	 * @param c2
	 * @return			Difference in milliseconds (C2 - C1)
	 */
	public static long timeDiff(Calendar c1, Calendar c2) {
		return (c2.getTimeInMillis() - c1.getTimeInMillis()) / 1000;
	}

}
