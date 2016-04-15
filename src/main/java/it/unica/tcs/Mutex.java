package it.unica.tcs;

import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mutex {
    
    private static final Logger logger = LoggerFactory.getLogger(Mutex.class);
    
    TreeSet<Integer> ts;
    
    public Mutex() {
        
        ts = new TreeSet<Integer>();
    }
    
    public void acquire(Integer cID) {
        
        while (true) {
            
            synchronized(this) {
                
                if (!ts.contains(cID)) {
                    
                    ts.add(cID);
                    break;
                }
                    
           
            }
        }
        
        logger.trace("Thread #" + Thread.currentThread().getId() +" acquired the mutex on: " + cID);
    }
    
    public void release(Integer cID) {
        
        ts.remove(cID);
        
        logger.trace("Thread #" + Thread.currentThread().getId() +" released the mutex: " + cID);
    }
}
