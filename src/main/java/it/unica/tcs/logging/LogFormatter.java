package it.unica.tcs.logging;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {
	
    @Override
    public String format(LogRecord record) {
    	
    	String logmsg = new String(), info = new String();
    	
    	info = "<div class=\"summary\">" + new Date(record.getMillis()) + " | "  + record.getSourceClassName() + " &raquo; "  + record.getSourceMethodName() +  "()</div>";
    	
    	if (record.getLevel() == Level.INFO && record.getMessage().contains("New .WAR"))
    		logmsg = "<tr class=\"newtr\"><td class=\"new\">" + record.getLevel() + "</td><td class=\"newmsg\">" + info + "<span class=\"newspan\">" + record.getMessage() + "</span></td>";
    	else if (record.getLevel() == Level.INFO)
    		logmsg = "<tr class=\"infotr\"><td class=\"info\">" + record.getLevel() + "</td><td class=\"infomsg\">" + info + "<span class=\"infospan\">" + record.getMessage()+ "</span></td>";
    	else if (record.getLevel() == Level.FINE)
            logmsg = "<tr class=\"finetr\"><td class=\"fine\">MINOR</td><td class=\"finemsg\">" + info + "<span class=\"finespan\">" + record.getMessage()+ "</span></td>";
    	else if (record.getLevel() == Level.FINEST)
            logmsg = "<tr class=\"finetr\"><td class=\"fine\">TEMP</td><td class=\"finemsg\">" + info + "<span class=\"finespan\">" + record.getMessage()+ "</span></td>";
    	else if (record.getLevel() == Level.WARNING)
    		logmsg = "<tr class=\"warningtr\"><td class=\"warning\">" + record.getLevel() + "</td><td class=\"warningmsg\">" + info + "<span class=\"warningspan\">" + record.getMessage()+ "</b></span></td>";
    	else if (record.getLevel() == Level.SEVERE)
    		logmsg = "<tr class=\"severetr\"><td class=\"severe\">ERROR</td><td class=\"severemsg\">" + info + "<span class=\"severespan\">" + record.getMessage()+ "</b></span></td>";
    	
    	logmsg += "</tr><tr class=\"spacer\"></tr>";
    	
        return logmsg;
    }
}
