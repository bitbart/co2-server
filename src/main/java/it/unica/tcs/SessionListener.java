package it.unica.tcs;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

@WebListener
public class SessionListener implements HttpSessionListener {

    private static int activeSessions;
    
    public SessionListener() {
    	
    	activeSessions = 0;
    	
    	//Log.message().severe("SessionListener created");
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        activeSessions++;
        
        if (activeSessions > 1)
        	Log.message().severe("Too many sessions: " + activeSessions);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        activeSessions--;
    }
}
