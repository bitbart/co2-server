package it.unica.tcs;

import java.sql.ResultSet;
import java.sql.SQLException;
import it.unica.tcs.DatabaseInterface;

/** 
 */
// TODO: Add comments
public class Contract {

	private DatabaseInterface db;
	private Integer contractID;
	private String contractHash;
	private String contractXML;
	private Integer ownerID;
	private Integer sessionID;
	private Integer contextID;
	private Integer role;
	private Integer state;
	private Long timestamp;
    private String typePreCheck;
	private boolean initialized;

	public Contract(DatabaseInterface db) {

		this.db = db;
		this.initialized = false;
	}

	public Contract initContract(String contractHash, String contractXML, Integer ownerID, Integer sessionID,
	        Integer contextID, Integer role, Integer state, Long timestamp, String typePreCheck) throws SQLException {

		this.contractID = db.insertContract(contractHash, contractXML, ownerID, contextID, role, state, timestamp, typePreCheck);
		this.contractHash = contractHash;
		this.contractXML = contractXML;
		this.ownerID = ownerID;
		this.sessionID = sessionID;
		this.role = role;
		this.state = state;
		this.timestamp = timestamp;
		this.typePreCheck = typePreCheck;

		this.initialized = true;

		return this;
	}

	public Contract loadFromID(Integer contractID) throws SQLException {

		String query = "SELECT * FROM contract WHERE contract_id = '" + contractID + "';";
		ResultSet result;

		result = db.select(query);
		result.next();

		this.contractID = contractID;
		this.contractHash = result.getString("contract_hash");
		this.contractXML = result.getString("contract_xml");
		this.ownerID = result.getInt("owner_id");
		this.sessionID = result.getInt("session_id"); // 0 if session_id is NULL
		this.contextID = result.getInt("context_id");
		this.role = result.getInt("role");
		this.state = result.getInt("state");
		this.timestamp = result.getLong("timestamp");
		this.typePreCheck = result.getString("type_pre_check");

		this.initialized = true;

		return this;
	}

	public Contract loadFromHash(String contractHash) throws SQLException {

		String query = "SELECT * FROM contract WHERE contract_hash = '" + contractHash + "';";
		ResultSet result;

		result = db.select(query);
		result.next();
		
		this.contractHash = contractHash;
		this.contractID = result.getInt("contract_id");
		this.contractXML = result.getString("contract_xml");
		this.ownerID = result.getInt("owner_id");
		this.sessionID = result.getInt("session_id"); // 0 if session_id is NULL
		this.contextID = result.getInt("context_id");
		this.role = result.getInt("role");
		this.state = result.getInt("state");
		this.timestamp = result.getLong("timestamp");
        this.typePreCheck = result.getString("type_pre_check");

		this.initialized = true;
		
		return this;
	}

	public boolean isInitialized() {

		return this.initialized;
	}

	public Integer getCompliantID() throws SQLException {

		String query = "SELECT contract_id FROM contract WHERE session_id = '" + this.sessionID
		        + "' AND contract_id <> '" + this.contractID + "';";
		ResultSet result;

		result = db.select(query);
		result.next();
		
		Integer i = result.getInt(1);
		
		return i;
	}

	public String getCompliantHash() throws SQLException {

		String query = "SELECT contract_hash FROM contract WHERE session_id = '" + this.sessionID
		        + "' AND contract_id <> '" + this.contractID + "';";
		ResultSet result;

		result = db.select(query);
		result.next();
		
		String s = result.getString(1);
		
		return s;
	}

	public Integer getContractID() {

		return this.contractID;
	}

	public String getContractHash() {

		return this.contractHash;
	}

	public String getContractXML() {

		return this.contractXML;
	}

	public Integer getOwnerID() {

		return this.ownerID;
	}

	public Integer getSessionID() {

	    if (this.sessionID > 0)
	        return this.sessionID;
	    else
	        return -1;
	}

	public Integer getContextID() {

		return this.contextID;
	}

	public Integer getRole() {

		return this.role;
	}

	public Integer getState() {

		return this.state;
	}

	public Long getTimestamp() {

		return this.timestamp;
	}
	
	public String getTypePreCheck(){
	    
	    return this.typePreCheck;
	}
}
