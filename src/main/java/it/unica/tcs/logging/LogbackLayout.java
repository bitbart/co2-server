package it.unica.tcs.logging;

import java.util.Date;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.LayoutBase;

public class LogbackLayout extends LayoutBase<ILoggingEvent> {

    @Override
    public String doLayout(ILoggingEvent event) {
        
        StringBuilder logmsg = new StringBuilder();
        StringBuilder info = new StringBuilder();
        
        String className = "";
        String methodName = "";
        
        if (event.hasCallerData()) {
            StackTraceElement caller = event.getCallerData()[0];
            className = caller.getClassName();
            methodName = caller.getMethodName();
        }
        
        info
            .append("<div class=\"summary\">").append(new Date(event.getTimeStamp()))
            .append(" | ").append(className)
            .append(" &raquo; ").append(methodName).append("()</div>");
        
        if (event.getLevel() == Level.INFO && event.getFormattedMessage().contains("New .WAR")) {
            logmsg.append("<tr class=\"newtr\"><td class=\"new\">").append(event.getLevel()).append("</td><td class=\"newmsg\">").append(info).append("<span class=\"newspan\">").append(event.getFormattedMessage()).append("</span></td>");
        }
        else if (event.getLevel() == Level.INFO) {
            logmsg.append("<tr class=\"infotr\"><td class=\"info\">").append(event.getLevel()).append("</td><td class=\"infomsg\">").append(info).append("<span class=\"infospan\">").append(event.getFormattedMessage()+ "</span></td>");
        }
        else if (event.getLevel() == Level.DEBUG) {
            logmsg.append("<tr class=\"finetr\"><td class=\"fine\">").append(event.getLevel()).append("</td><td class=\"finemsg\">").append(info).append("<span class=\"finespan\">").append(event.getFormattedMessage()+ "</span></td>");
        }
        else if (event.getLevel() == Level.TRACE) {
            logmsg.append("<tr class=\"finetr\"><td class=\"fine\">").append(event.getLevel()).append("</td><td class=\"finemsg\">").append(info).append("<span class=\"finespan\">").append(event.getFormattedMessage()+ "</span></td>");
        }
        else if (event.getLevel() == Level.WARN) {
            logmsg.append("<tr class=\"warningtr\"><td class=\"warning\">").append(event.getLevel()).append("</td><td class=\"warningmsg\">").append(info).append("<span class=\"warningspan\">").append(event.getFormattedMessage()+ "</b></span></td>");
        }
        else if (event.getLevel() == Level.ERROR) {
            logmsg.append("<tr class=\"severetr\"><td class=\"severe\">").append(event.getLevel()).append("</td><td class=\"severemsg\">").append(info).append("<span class=\"severespan\">").append(event.getFormattedMessage()+ "</b></span></td>");
        }
        
        logmsg.append("</tr><tr class=\"spacer\"></tr>");
        logmsg.append(CoreConstants.LINE_SEPARATOR);
        
        return logmsg.toString();
    }

}
