package it.unica.tcs;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Log {
		 
	public static final Logger log = Logger.getLogger("Co2log");
	private static boolean init = false;
	
	private static void init() {
		
		if (init == false) {
			
			// Removes log files of previous WAR versions
			Tools.callApplication("rm " + Tools.HOME_DIR + "/logs/*", null);
			
			FileHandler myFileHandler = null;
			
			try {
				
				String fileName = "serverlog_" + Long.toString(System.currentTimeMillis()) + ".txt";
				
				myFileHandler = new FileHandler(Tools.HOME_DIR + "/logs/" + fileName , true);
				
				PrintWriter writer;
				
				writer = new PrintWriter(Tools.HOME_DIR + "/upload/log_position.txt", "UTF-8");
				writer.print(fileName);
				writer.close();
			
				myFileHandler.setFormatter(new LogFormatter());
				
				log.addHandler(myFileHandler);
				log.setUseParentHandlers(false);
				log.setLevel(Level.INFO);
				
				init = true;

			} catch (SecurityException | IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	public static Logger message() {
		
		init();
		return log; 
	}
	
	public static String format(String s) {
	    
	    if (s == null)
	        return "null";
		
		int sLength = s.length() < 50 ? s.length() : 50;
		String dots = s.length() == sLength ? "" : "...";
		
		return "'<i>" + escapeHtml(s.substring(0, sLength)).replaceAll("\n", "") + dots + "</i>'";
	}
	
	public static boolean isInitialized() {
		return init;
	}
}