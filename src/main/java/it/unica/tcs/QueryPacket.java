package it.unica.tcs;


public class QueryPacket {

	private String username;
	private String password;
	private String firstContract;
	private String secondContract;

	// Must have no-argument constructor
	public QueryPacket() {

	}

	public QueryPacket(String username, String password, String firstContract, String secondContract) {
		this.username = username;
		this.password = password;
		this.firstContract = firstContract;
		this.secondContract = secondContract;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public String getFirstContract() {
		return firstContract;
	}
	
	public String getSecondContract() {
		return secondContract;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public void setFirstContract(String firstContract) {
		this.firstContract = firstContract;
	}
	
	public void setSecondContract(String secondContract) {
		this.secondContract = secondContract;
	}
}