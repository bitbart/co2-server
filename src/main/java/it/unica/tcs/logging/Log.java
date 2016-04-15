package it.unica.tcs.logging;

import static org.apache.commons.lang.StringEscapeUtils.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import it.unica.tcs.Tools;

@Deprecated
public class Log {
		 
    public static final Logger log = Logger.getLogger("Co2log");
	
	static {
			
	    File logdir = new File("/srv/http/log/middleware/");
	    logdir.mkdirs();

	    // Removes log files of previous WAR versions
		Tools.callApplication("rm " + logdir + "/*", null);
		
		FileHandler myFileHandler = null;
		
		try {
			
			String fileName = "serverlog_" + Long.toString(System.currentTimeMillis()) + ".txt";
			
			
			myFileHandler = new FileHandler(logdir +"/"+ fileName , true);
			
			PrintWriter writer;
			
			writer = new PrintWriter(logdir + "/log_position.txt", "UTF-8");
			writer.print(fileName);
			writer.close();
		
			myFileHandler.setFormatter(new LogFormatter());
			
			log.addHandler(myFileHandler);
			log.setUseParentHandlers(false);
			log.setLevel(Level.ALL);

		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
			
	}
	
	public static Logger message() {
		
		return log; 
	}
	
	public static String format(String s) {
	    
	    if (s == null)
	        return "null";
		
		int sLength = s.length() < 50 ? s.length() : 50;
		String dots = s.length() == sLength ? "" : "...";
		
		return "'<i>" + escapeHtml(s.substring(0, sLength)).replaceAll("\n", "") + dots + "</i>'";
	}
	
}