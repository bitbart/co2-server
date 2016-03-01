package it.unica.tcs;

import java.util.List;

public class QueryPacket {

	private String username;
	private String password;
	private String newPassword;
	private String firstContract;
	private String secondContract;
	private String contractHash;
	private String actionName;
	private String actionValue;
	private Integer delay;
	private String contractType;
	private boolean prv;
	private List<String> actions;
	
	// Must have no-argument constructor
	public QueryPacket() {

	}
	
	public boolean getPrivate() {
		return prv;
	}

	public void setPrivate(boolean prv) {
		this.prv = prv;
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
	
	public String getActionName() {
		return actionName;
	}
	
	public String getActionValue() {
		return actionValue;
	}
	
	public Integer getDelay() {
		return delay;
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
	
	public void setActionName(String actionName) {
		this.actionName = actionName;
	}
	
	public void setActionValue(String actionValue) {
		this.actionValue = actionValue;
	}
	
	public void setDelay(Integer delay) {
		this.delay = delay;
	}
	
	public void setContractPair(String firstContract, String secondContract) {
		this.firstContract = firstContract;
		this.secondContract = secondContract;
	}

	public String getContractType() {
		return contractType;
	}

	public void setContractType(String contractType) {
		this.contractType = contractType;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }
	
}