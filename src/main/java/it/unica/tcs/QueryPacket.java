package it.unica.tcs;

public class QueryPacket {

	private String username;
	private String password;
	private String firstContract;
	private String secondContract;
	private String contractHash;

	// Must have no-argument constructor
	public QueryPacket() {

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
	
	public String getContractHash() {
		return contractHash;
	}
	
	public void setCredentials(String username, String password) {
		this.username = username;
		this.password = password;
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
	
	public void setContractHash(String contractHash) {
		this.contractHash = contractHash;
	}
	
	public void setContractPair(String firstContract, String secondContract) {
		this.firstContract = firstContract;
		this.secondContract = secondContract;
	}
}