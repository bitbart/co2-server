package it.unica.tcs;

import java.sql.SQLException;
import java.util.Random;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class MainApplication implements ServletContextListener {

	private static DatabaseInterface db;
	private static Random rng; 
	private static Mutex mutex;
	
	public static void mutexAcquire(Integer cID) {
	    
	    mutex.acquire(cID);
	}
	
	public static void mutexRelease(Integer cID) {
	    
	    mutex.release(cID);
	}
	
	public static Long getRand() {
	    
	    return rng.nextLong();
	}
	
	public static DatabaseInterface getDBConnection() {
	    
	    return db;
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {


		Log.message().severe("The middleware has been killed.");
		
	}
	
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		
	    // Initializes PRNG
	    rng = new Random();
	    mutex = new Mutex();
	    db = new DatabaseInterface();
	    
	    try {
	        db.open();
	    }
	    catch (SQLException e) {
	        
	        Log.message().severe("Failed opening the database connection! ***SEVERE FAULT***");
	        Log.message().warning("SQL says: " + e.getMessage());
	    }
	    
	    Log.message().info("**************** New .WAR loaded - Log started ****************");
		
	}
}