package it.unica.tcs;


public class BasicPair<F, S> {
    private F first; //first member of pair
    private S second; //second member of pair
    private boolean isEmpty;
    
    public BasicPair() {
    	
    	isEmpty = true;
    }

    public BasicPair(F first, S second) {
        this.first = first;
        this.second = second;
        
        isEmpty = false;
    }

    public void set(F first, S second) {
        this.first = first;
        this.second = second;
        
        isEmpty = false;
    }
    
    public F getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }
    
    public boolean isEmpty() {
    	
    	return isEmpty;
    }
}