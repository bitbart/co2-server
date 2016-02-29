package it.unica.tcs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import it.unica.tcs.DatabaseInterface;

/** 
 */
// TODO: Add comments
public class Contract {
	
	public static String TYPE_TST = "tst";

	private DatabaseInterface db;
	private Integer contractID;
	private String contractHash;
	private String contractXML;
	private Integer ownerID;
	private Integer sessionID;
	private Integer contextID;
	private Integer role;
	private Integer state;
	private Long randomLong;
	private Long timestamp;
    private String typePreCheck;
    private Integer delay;
    private String type;
	private boolean initialized;
	private Integer prv;

	public Contract() {

		this.db = DatabaseInterface.getInstance();
		this.initialized = false;
	}

	/*
	public Contract initCkontract(String contractHash, String contractXML, Integer ownerID, Integer sessionID,
	        Integer contextID, Integer role, Integer state, Long timestamp, String typePreCheck, String mapping, String aux) throws SQLException {

		this.contractID = db.insertContract(contractHash, contractXML, ownerID, contextID, role, state, timestamp, typePreCheck, mapping, aux);
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
	} */

    public Contract loadFromID(Integer contractID) throws SQLException {

        String query = "SELECT * FROM contract WHERE contract_id = ?";

        try (
                Connection connection = db.getDatasource().getConnection();
                PreparedStatement stmt = connection.prepareStatement(query);
                ) {
            stmt.setInt(1, contractID);
            
            ResultSet result = stmt.executeQuery();
            result.next();

            this.contractID = contractID;
            this.contractHash = result.getString("contract_hash");
            this.contractXML = result.getString("contract_xml");
            this.ownerID = result.getInt("owner_id");
            this.sessionID = result.getInt("session_id"); // 0 if session_id is
            // NULL
            this.contextID = result.getInt("context_id");
            this.role = result.getInt("role");
            this.state = result.getInt("state");
            this.randomLong = result.getLong("random_long");
            this.delay = result.getInt("delay");
            this.timestamp = result.getLong("tell_timestamp");
            this.typePreCheck = result.getString("type_pre_check");
            this.type = result.getString("type");
            this.prv = result.getInt("private");

            this.initialized = true;

            result.close();
            return this;
        }
    }

    public Contract loadFromHash(String contractHash) throws SQLException {

        String query = "SELECT * FROM contract WHERE contract_hash = ?";

        try (
                Connection connection = db.getDatasource().getConnection();
                PreparedStatement stmt = connection.prepareStatement(query);
                ) {
            stmt.setString(1, contractHash);
            
            ResultSet result = stmt.executeQuery();
            result.next();

            this.contractHash = contractHash;
            this.contractID = result.getInt("contract_id");
            this.contractXML = result.getString("contract_xml");
            this.ownerID = result.getInt("owner_id");
            this.sessionID = result.getInt("session_id"); // 0 if session_id is
            // NULL
            this.contextID = result.getInt("context_id");
            this.role = result.getInt("role");
            this.state = result.getInt("state");
            this.randomLong = result.getLong("random_long");
            this.delay = result.getInt("delay");
            this.timestamp = result.getLong("tell_timestamp");
            this.typePreCheck = result.getString("type_pre_check");
            this.type = result.getString("type");
            this.prv = result.getInt("private");

            this.initialized = true;
            
            result.close();
            return this;
        }
    }

	public boolean isInitialized() {

		return this.initialized;
	}

    public Integer getCompliantID() throws SQLException {

        String query = "SELECT contract_id FROM contract WHERE session_id = '" + this.sessionID
                + "' AND contract_id <> '" + this.contractID + "';";

        try (
                Connection connection = db.getDatasource().getConnection();
                PreparedStatement stmt = connection.prepareStatement(query);
                ) {
            ResultSet result = stmt.executeQuery(query);
            result.next();

            Integer i = result.getInt(1);

            result.close();
            return i;
        }
    }

    public String getCompliantHash() throws SQLException {

        String query = "SELECT contract_hash FROM contract WHERE session_id = ? AND contract_id <> ?";

        try (
                Connection connection = db.getDatasource().getConnection();
                PreparedStatement stmt = connection.prepareStatement(query);
                ) {
            stmt.setInt(1, sessionID);
            stmt.setInt(2, contractID);
            
            ResultSet result = stmt.executeQuery();
            result.next();

            String s = result.getString(1);

            result.close();
            return s;
        }
    }
    
    public String getSessionHash() throws SQLException {

        if (sessionID<=0)
            return "";
                    
        String query = "SELECT session_hash FROM session WHERE session_id = ?";

        try (
                Connection connection = db.getDatasource().getConnection();
                PreparedStatement stmt = connection.prepareStatement(query);
                ) {
            stmt.setInt(1, sessionID);
            
            ResultSet result = stmt.executeQuery();
            result.next();

            String s = result.getString(1);

            result.close();
            return s;
        }
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
	
	public Integer getDelay() {

		return this.delay;
	}

	public Long getRandomLong() {

		return this.randomLong;
	}
	
	public String getTypePreCheck(){
	    
	    return this.typePreCheck;
	}
	
	public Long getTimestamp() {
		
		return this.timestamp;
	}
	
	public String getType() {
		
		return this.type;
	}
	
	public boolean isPrivate() {
		
		return this.prv == 1;
	}
	
	public boolean isExpired() {
		
		if (delay == 0)
			return false;
		
		if (randomLong + delay >= System.currentTimeMillis())
			return false;
		else
			return true;
	}
}
