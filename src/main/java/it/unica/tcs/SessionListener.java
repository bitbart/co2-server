package it.unica.tcs;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebListener
public class SessionListener implements HttpSessionListener {

    private static final Logger logger = LoggerFactory.getLogger(SessionListener.class);
    
    private static int activeSessions;
    
    public SessionListener() {
    	
    	activeSessions = 0;
    	
    	//logger.error("SessionListener created");
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        activeSessions++;
        
        if (activeSessions > 1)
        	logger.error("Too many sessions: " + activeSessions);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        activeSessions--;
    }
}
