package it.unica.tcs;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {
	
    @Override
    public String format(LogRecord record) {
    	
    	String logmsg = new String();
    	
    	logmsg = "<div style=\"font-family:Arial; font-size:12pt; margin-bottom:15px; padding:0px;\">";
    	
    	logmsg += "<span style=\"font-size:8pt; color:#666666\">" + new Date(record.getMillis()) + " | "  + record.getSourceClassName() + " &raquo; "  + record.getSourceMethodName() +  "()</span>\n";
    	
    	if (record.getLevel() == Level.INFO && record.getMessage().contains("*** New"))
    		logmsg += "<span style=\"color:#333333\"><b>&middot;</b> " + record.getLevel() + ": <span style=\"color:#0000CC\">" + record.getMessage() + "</span>";
    	else if (record.getLevel() == Level.INFO)
    		logmsg += "<span style=\"color:#333333\"><b>&middot;</b> " + record.getLevel() + ": " + record.getMessage()+ "</span>";
    	else if (record.getLevel() == Level.FINE)
            logmsg += "<span style=\"color:#04B404\"><b>&middot;</b> " + record.getLevel() + ": " + record.getMessage()+ "</span>";
    	else if (record.getLevel() == Level.WARNING)
    		logmsg += "<span style=\"color:#CC0000\"><b>&middot; " + record.getLevel() + ": " + record.getMessage()+ "</b></span>";
    	else if (record.getLevel() == Level.SEVERE)
    		logmsg += "<span style=\"color:#990000\"><b>&middot; " + record.getLevel() + ": " + record.getMessage()+ "</b></span>";
    	
    	logmsg += "</div>";
    	
        return logmsg;
    }
}
