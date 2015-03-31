package it.unica.tcs;

import java.util.TreeSet;

public class Mutex {
    
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
        
        Log.message().fine("Thread #" + Thread.currentThread().getId() +" acquired the mutex on: " + cID);
    }
    
    public void release(Integer cID) {
        
        ts.remove(cID);
        
        Log.message().fine("Thread #" + Thread.currentThread().getId() +" released the mutex: " + cID);
    }
}
