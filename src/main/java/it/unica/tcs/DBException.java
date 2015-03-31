package it.unica.tcs;

public class DBException extends Exception {
	
	private static final long serialVersionUID = 5361792563207812803L;
	private String outputMessage;
	
	public DBException() {
		
	}

	public DBException(String message) {

		outputMessage = message;
	}
	
	@Override
	public String getMessage() {

		return outputMessage;
	}
}
