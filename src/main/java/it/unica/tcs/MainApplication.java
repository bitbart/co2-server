package it.unica.tcs;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebListener
public class MainApplication implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(MainApplication.class);
    
	private static Random rng; 
	private static Mutex mutex;
	private static Cache<String, Boolean> credentialsCache;
	private static Cache<String, Boolean> permissionsCache;	
	private static Cache<String, Integer[]> latentCache;
	private static ExecutorService executor;
	
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
	
	public static ExecutorService getExecutorService() {
	    
	    if (executor==null) {
	        logger.info("Starting the executor service");
	        executor = Executors.newCachedThreadPool();
	    }
	    
	    return executor;
	}
	
	
	@Override
	public void contextInitialized(ServletContextEvent arg0) {

        logger.info("Starting the middleware application");
        
	    rng = new Random();
	    mutex = new Mutex();
	    
	    if (!SessionMonitor.MONITOR_ENABLED)
	    	logger.warn("The execution monitor is currently disabled.");
	    
	    // Creating caches
	    credentialsCache = new Cache<String, Boolean>(24*60*60, 24*60*60, 10000);
	    permissionsCache = new Cache<String, Boolean>(24*60*60, 24*60*60, 10000);
	    latentCache = new Cache<String, Integer[]>(24*60*60, 24*60*60, 10000);
	    
	}
	
	@Override
    public void contextDestroyed(ServletContextEvent arg0) {

        logger.info("Destroying the application context");
        
        logger.info("Shutting down the executor service");
        executor.shutdownNow();
    }
}