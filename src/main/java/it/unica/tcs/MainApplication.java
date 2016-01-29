package it.unica.tcs;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class MainApplication implements ServletContextListener {

	private static Random rng; 
	private static Mutex mutex;
	private static Cache<String, Boolean> credentialsCache;
	private static Cache<String, Boolean> permissionsCache;	
	private static Cache<String, Integer[]> latentCache;
	
	public static void mutexAcquire(Integer cID) {
	    
	    mutex.acquire(cID);
	}
	
	public static void mutexRelease(Integer cID) {
	    
	    mutex.release(cID);
	}
	
	public static Long getRand() {
	    
	    return rng.nextLong();
	}
	
	public static String getCtuID() {
	    
	    return (rng.nextInt(4) + 1) + "";
	}
	
	public static Cache<String, Boolean> getCredentialsCache() {
	    
	    return credentialsCache;
	}
	
	public static Cache<String, Boolean> getPermissionsCache() {
	    
	    return permissionsCache;
	}
	
	public static Cache<String, Integer[]> getLatentCache() {
	    
	    return latentCache;
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {


		Log.message().severe("The webservice has been killed by another process.");
		
	}
	
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		
		Integer latent_cs, cs, active_ss;
		
	    // Initializes PRNG
	    rng = new Random();
	    mutex = new Mutex();
	    DatabaseInterface db = DatabaseInterface.getInstance();
	    
	    try {
	        db.open();
	    }
	    catch (SQLException e) {
	        
	        Log.message().severe("Failed opening the database connection, the webservice is down!");
	        Log.message().warning("SQL says: " + e.getMessage());
	    }
	    
	    try (
		    ResultSet rs = db.select("SELECT COUNT(*) FROM contract WHERE state=0");
		    ResultSet rs2 = db.select("SELECT COUNT(*) FROM contract");
		    ResultSet rs3 = db.select("SELECT COUNT(*) FROM session");
		    ){
			rs.next();
			latent_cs = rs.getInt(1);
			
			rs2.next();
			cs = rs2.getInt(1);
			
			rs3.next();
			active_ss = rs3.getInt(1);
			
			Log.message().info("New .WAR loaded. When starting, there were " + cs + " contracts in the database (" + latent_cs + " latents), and " + active_ss + " sessions.");
		
	    } catch (SQLException e) {
	    	
	    	Log.message().warning("New .WAR loaded, can't read the number of active contracts/sessions in the database.");
	    	Log.message().warning("SQL says: " + e.getMessage());
		}
	    
	    if (!SessionMonitor.MONITOR_ENABLED)
	    	Log.message().warning("The execution monitor is currently disabled.");
	    
	    // Creating caches
	    
	    credentialsCache = new Cache<String, Boolean>(24*60*60, 24*60*60, 10000);
	    permissionsCache = new Cache<String, Boolean>(24*60*60, 24*60*60, 10000);
	    latentCache = new Cache<String, Integer[]>(24*60*60, 24*60*60, 10000);
	}
}