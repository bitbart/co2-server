package it.unica.tcs;

import it.unica.tcs.Log;
import it.unica.tcs.Tools;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// TODO: Create all javadocs

/** */
public class DatabaseInterface {

	// Connection to database.
	private Connection connection;

	// Database's credentials.
	public final static String DB_NAME = "co2_data";
	public static final String DB_PATH = "localhost";
	public static final String DB_USER = "root";
	public static final String DB_PASS = "nNEp1Ak6ii";

	// Database's tables
	public final static String TABLE_USER = "user";
	public final static String TABLE_CONTRACT = "contract";
	public final static String TABLE_TRACE = "trace";
	public final static String TABLE_PARTICIPANT = "partecipant";
	public final static String TABLE_SESSION = "session";
	public final static String TABLE_CONTEXT = "context";
	public final static String TABLE_ACTION = "action";
	public final static String TABLE_CONTEXT_ACTION = "context_action";

	// Contract states
	public final static int CONTRACT_HANDLED = -1;
	public final static int CONTRACT_LATENT = 0;
	public final static int CONTRACT_ON_DUTY = 1;
	public final static int CONTRACT_OFF_DUTY = 2;
	public final static int CONTRACT_INNOCENT = 3;
	public final static int CONTRACT_CULPABLE = 4;
	public final static int CONTRACT_COMPLETED = 5;

	// Roles of participants during interaction
	public final static int CONTRACT_ROLE_LATENT = -1;
	public final static int CONTRACT_ROLE_0 = 0;
	public final static int CONTRACT_ROLE_1 = 1;

	// Participant states
	public final static int PARTICIPANT_FAIR = 0;
	public final static int PARTICIPANT_GUILTY = 1;

	// Session states
	public final static int SESSION_ACTIVE = 0;
	public final static int SESSION_COMPLETED_CORRECTLY = 1;
	public final static int SESSION_COMPLETED_UNCORRECTLY = 2;

	// Timestamp
	public final static int SESSION_STARTING_DELAY = 0; // 0 secs

	// Error state
	public final static int ERROR_DB = -1;

	// Context
	public final static String CONTEXT_EMPTY_NAME = "contextempty";
	public final static Integer CONTEXT_EMPTY_ID = 0;
	
	// Action types
	public final static int ACTION_TYPE_INT = 0;
    public final static int ACTION_TYPE_STRING = 1;
    public final static int ACTION_TYPE_FILE = 2;

	// Public query functions called by the server.

	/** */
	public void open() throws SQLException {
		
		if (this.connection == null) {
		    
		    this.registerConnector();
		    this.connection = DriverManager.getConnection("jdbc:mysql://localhost/" + DB_NAME + "?autoReconnect=true", DB_USER, DB_PASS);
		}
	}

	/** */
	public void close() {

		
        try {
            if (this.connection != null)
                connection.close();
        }
        catch (SQLException e) {
            
            Log.message().warning("Can't close a opened connection. SQL says: " + e);
        }
		
		connection = null;
	}

	/** @throws SQLException */
	public ResultSet select(String query) throws SQLException {

		ResultSet resultQuery;

		resultQuery = this.throwSelect(query);

		return resultQuery;
	}

	/** @throws SQLException */
	public void insertUser(String firstName, String lastName, String email, String password) throws SQLException {

		String query;
		String[] cols = new String[4];
		String[] vals = new String[4];

		cols[0] = "first_name";
		cols[1] = "last_name";
		cols[2] = "email";
		cols[3] = "password";

		vals[0] = firstName;
		vals[1] = lastName;
		vals[2] = email;
		vals[3] = Tools.hash256(password);

		query = generateInsertQuery(TABLE_USER, cols, vals);

		this.throwUpdate(query);
	}

	/** @throws SQLException */
	public Integer insertContract(String contractHash, String contractXML, Integer ownerID, Integer contextID,
	        Integer role, Integer state, Long timestamp, String typePreCheck, String mapping, String aux) throws SQLException {

		String insertQuery, selectQuery;
		Integer identifier;
		ResultSet rs;

		String[] cols = new String[10];
		String[] vals = new String[10];

		cols[0] = "contract_hash";
		cols[1] = "contract_xml";
		cols[2] = "owner_id";
		cols[3] = "context_id";
		cols[4] = "role";
		cols[5] = "state";
		cols[6] = "timestamp";
		cols[7] = "type_pre_check";
		cols[8] = "mapping";
		cols[9] = "aux";


		vals[0] = contractHash;
		vals[1] = contractXML;
		vals[2] = ownerID + "";
		vals[3] = contextID + "";
		vals[4] = role + "";
		vals[5] = state + "";
		vals[6] = timestamp.toString();
		vals[7] = typePreCheck;
		vals[8] = mapping;
		vals[9] = aux;

		insertQuery = generateInsertQuery(TABLE_CONTRACT, cols, vals);

		this.throwUpdate(insertQuery);

		// Returning ID of the new contract added.
		selectQuery = "SELECT `contract_id` FROM contract WHERE `contract_hash` = '" + contractHash + "'";
		rs = this.select(selectQuery);
		rs.next();
		identifier = rs.getInt(1);

		return identifier;
	}
	
	public void insertTrace(Integer actionID, String actionName, Integer role, Integer sessionID, String value, boolean isFile) throws SQLException {

        String insertQuery;

        String[] cols = new String[6];
        String[] vals = new String[6];

        cols[0] = "action_id";
        cols[1] = "action_name";
        cols[2] = "role";
        cols[3] = "session_id";
        cols[4] = "timestamp";
        cols[5] = "data_string_value";
        
        if (isFile)
            cols[4] = "data_file_value";

        vals[0] = actionID + "";
        vals[1] = actionName;
        vals[2] = role + "";
        vals[3] = sessionID + "";
        vals[4] = Long.toString(System.currentTimeMillis());
        vals[5] = value;

        if (isFile)
            insertQuery = generateInsertQuery(TABLE_TRACE, cols, vals, true);
        else
            insertQuery = generateInsertQuery(TABLE_TRACE, cols, vals);

        this.throwUpdate(insertQuery);
	}
	
    public void insertTrace(Integer actionID, String actionName, Integer role, Integer sessionID) throws SQLException {

        String insertQuery;

        String[] cols = new String[5];
        String[] vals = new String[5];

        cols[0] = "action_id";
        cols[1] = "action_name";
        cols[2] = "role";
        cols[3] = "session_id";
        cols[4] = "timestamp";

        vals[0] = actionID + "";
        vals[1] = actionName;
        vals[2] = role + "";
        vals[3] = sessionID + "";
        vals[4] = Long.toString(System.currentTimeMillis());

        insertQuery = generateInsertQuery(TABLE_TRACE, cols, vals);

        this.throwUpdate(insertQuery);
    }

	/** @throws SQLException */
	public void insertTrace(Integer actionID, String actionName, Integer role, Integer sessionID, Integer value) throws SQLException {

		String insertQuery;

		String[] cols = new String[6];
		String[] vals = new String[6];

		cols[0] = "action_id";
		cols[1] = "action_name";
		cols[2] = "role";
		cols[3] = "session_id";
		cols[4] = "timestamp";
		cols[5] = "data_int_value";

		vals[0] = actionID + "";
		vals[1] = actionName;
		vals[2] = role + "";
		vals[3] = sessionID + "";
		vals[4] = Long.toString(System.currentTimeMillis());
		vals[5] = value + "";

		insertQuery = generateInsertQuery(TABLE_TRACE, cols, vals);

		this.throwUpdate(insertQuery);
	}

	/** @throws SQLException */
	public void insertParticipant(Integer userID, Integer state) throws SQLException {

		String insertQuery;

		String[] cols = new String[2];
		String[] vals = new String[2];

		cols[0] = "user_id";
		cols[1] = "state";

		vals[0] = Integer.toString(userID);
		vals[1] = Integer.toString(state);

		insertQuery = generateInsertQuery(TABLE_PARTICIPANT, cols, vals);

		this.throwUpdate(insertQuery);
	}

	/** @throws SQLException */
	public Integer insertSession(String sessionHash, Integer state, String lastState, Integer contextID)
	        throws SQLException {

		String insertQuery, selectQuery;
		ResultSet rs;
		Integer identifier;

		String[] cols = new String[6];
		String[] vals = new String[6];

		cols[0] = "session_hash";
		cols[1] = "state";
		cols[2] = "context_id";
		cols[3] = "start_timestamp";
		cols[4] = "last_timestamp";
		cols[5] = "last_state"; // A "load_file" column must be the last in the string array

		vals[0] = sessionHash;
		vals[1] = state + "";
		vals[2] = contextID + "";
		vals[3] = (System.currentTimeMillis() + SESSION_STARTING_DELAY) + "";
		vals[4] = (System.currentTimeMillis() + SESSION_STARTING_DELAY) + "";
		vals[5] = lastState;

		insertQuery = generateInsertQuery(TABLE_SESSION, cols, vals, true);

		this.throwUpdate(insertQuery);

		// Returning ID of the new contract added.
		selectQuery = "SELECT `session_id` FROM session WHERE `session_hash` = '" + sessionHash + "'";
		rs = this.select(selectQuery);
		rs.next();
		identifier = rs.getInt(1);

		return identifier;
	}

	/** @throws SQLException */
	public void updateUser(Integer userID, String firstName, String lastName, String email, String password, String reputation, String credit)
	        throws SQLException {

		String updateQuery, condition;

		String[] cols = new String[6];
		String[] vals = new String[6];

		cols[0] = "first_name";
		cols[1] = "last_name";
		cols[2] = "email";
		cols[3] = "password";
		cols[4] = "reputation";
		cols[5] = "credit";

		vals[0] = firstName;
		vals[1] = lastName;
		vals[2] = email;
		vals[3] = password;
		vals[4] = reputation;
		vals[5] = credit;

		condition = "user_id = '" + userID + "'";

		updateQuery = generateUpdateQuery(TABLE_USER, cols, vals, condition);

		this.throwUpdate(updateQuery);
	}

	/** @throws SQLException */
	public void setTraceRead(Integer traceID)
	        throws SQLException {

		String updateQuery, condition;

		String[] cols = new String[1];
		String[] vals = new String[1];

		cols[0] = "read";

		vals[0] = 1 + "";

		condition = "trace_id = '" + traceID + "'";

		updateQuery = generateUpdateQuery(TABLE_TRACE, cols, vals, condition);

		this.throwUpdate(updateQuery);
	}

	/** @throws SQLException */
	public void saveNetwork(Integer sessionID, String last_state) throws SQLException {

		String updateQuery, condition;

		String[] cols = new String[2];
		String[] vals = new String[2];

		cols[0] = "last_timestamp";
		cols[1] = "last_state"; // A "load_file" column must be the last in the string array

		vals[0] = Long.toString(System.currentTimeMillis());
		vals[1] = last_state;

		condition = "session_id = '" + sessionID + "'";

		updateQuery = generateUpdateQuery(TABLE_SESSION, cols, vals, condition, true);

		this.throwUpdate(updateQuery);
	}
	
	public void setSessionState(Integer sessionID, Integer state)
	        throws SQLException {

		String updateQuery, condition;

		String[] cols = new String[1];
		String[] vals = new String[1];

		cols[0] = "state";

		vals[0] = state + "";

		condition = "session_id = '" + sessionID + "'";

		updateQuery = generateUpdateQuery(TABLE_SESSION, cols, vals, condition);

		this.throwUpdate(updateQuery);
	}

	/** @throws SQLException */
	public void updateParticipant(Integer participantID, Integer userID, Integer state) throws SQLException {

		String updateQuery, condition;

		String[] cols = new String[2];
		String[] vals = new String[2];

		cols[0] = "user_id";
		cols[1] = "state";

		vals[0] = userID + "";
		vals[1] = state + "";

		condition = "participant_id = '" + participantID + "'";

		updateQuery = generateUpdateQuery(TABLE_PARTICIPANT, cols, vals, condition);

		this.throwUpdate(updateQuery);
	}

	/** @throws SQLException */
	public void updateContract(Integer contractID, Integer sessionID, Integer role, Integer state) throws SQLException {

		String updateQuery, condition;

		String[] cols = new String[3];
		String[] vals = new String[3];

		cols[0] = "session_id";
		cols[1] = "role";
		cols[2] = "state";

		vals[0] = sessionID + "";
		vals[1] = role + "";
		vals[2] = state + "";

		condition = "contract_id = '" + contractID + "'";

		updateQuery = generateUpdateQuery(TABLE_CONTRACT, cols, vals, condition);

		this.throwUpdate(updateQuery);
	}
	
	public void setContractState(Integer contractID, Integer state) throws SQLException {

		String updateQuery, condition;

		String[] cols = new String[1];
		String[] vals = new String[1];

		cols[0] = "state";

		vals[0] = state + "";

		condition = "contract_id = '" + contractID + "'";

		updateQuery = generateUpdateQuery(TABLE_CONTRACT, cols, vals, condition);

		this.throwUpdate(updateQuery);
	}

	/** @throws SQLException */
	public void updateContract(String contractHash, Integer sessionID, Integer role, Integer state) throws SQLException {

		String updateQuery, condition;

		String[] cols = new String[3];
		String[] vals = new String[3];

		cols[0] = "session_id";
		cols[1] = "role";
		cols[2] = "state";

		vals[0] = sessionID + "";
		vals[1] = role + "";
		vals[2] = state + "";

		condition = "contract_hash = '" + contractHash + "'";

		updateQuery = generateUpdateQuery(TABLE_CONTRACT, cols, vals, condition);

		this.throwUpdate(updateQuery);
	}

	/** @throws SQLException */
	public void updateContext(Integer contextID, String name, String description) throws SQLException {

		String updateQuery, condition;

		String[] cols = new String[2];
		String[] vals = new String[2];

		cols[0] = "name";
		cols[1] = "description";

		vals[0] = name;
		vals[1] = description;

		condition = "context_id = '" + contextID + "'";

		updateQuery = generateUpdateQuery(TABLE_CONTEXT, cols, vals, condition);

		this.throwUpdate(updateQuery);
	}

	/** @throws SQLException */
	public void updateAction(Integer actionID, String name, String verificationLink) throws SQLException {

		String updateQuery, condition;

		String[] cols = new String[2];
		String[] vals = new String[2];

		cols[0] = "name";
		cols[1] = "verification_link";

		vals[0] = name;
		vals[1] = verificationLink;

		condition = "action_id = '" + actionID + "'";

		updateQuery = generateUpdateQuery(TABLE_ACTION, cols, vals, condition);

		this.throwUpdate(updateQuery);
	}

	// Private functions used by different public query functions.

	private String generateInsertQuery(String table, String[] cols, String[] vals) throws SQLException {

		return generateInsertQuery(table, cols, vals, false);
	}

	private String generateInsertQuery(String table, String[] cols, String[] vals, boolean loadfile)
	        throws SQLException {

		String insertQuery = new String();
		String columns = new String();
		String values = new String();

		if (cols.length != vals.length)
		    throw new SQLException("Columns and values number mismatch in generateInsertQuery [" + cols.length + "; "
		            + vals.length + "]");

		for (int i = 0; i < cols.length; i++) {
			columns += "`" + cols[i] + "`";

			if (loadfile && i == cols.length - 1)
				values += "LOAD_FILE ('" + vals[i] + "')";
			else
				values += "'" + vals[i] + "'";

			if (i < cols.length - 1) {
				columns += ",";
				values += ",";
			}
		}

		insertQuery = "INSERT INTO `" + DB_NAME + "`.`" + table + "` (" + columns + ") VALUES (" + values + ");";
		
	    Log.message().finest("Executed query: " + insertQuery);

		return insertQuery;
	}

	private String generateUpdateQuery(String table, String[] cols, String[] vals, String condition)
	        throws SQLException {

		return generateUpdateQuery(table, cols, vals, condition, false);
	}

	private String generateUpdateQuery(String table, String[] cols, String[] vals, String condition, boolean loadfile)
	        throws SQLException {

		String updateQuery = new String();
		String sets = new String();

		if (cols.length != vals.length) throw new SQLException();

		for (int i = 0; i < cols.length; i++) {

			if (loadfile && i == cols.length - 1)
				sets += "`" + cols[i] + "`=" + "LOAD_FILE('" + vals[i] + "')";
			else
				sets += "`" + cols[i] + "`=" + "'" + vals[i] + "'";

			if (i < cols.length - 1) sets += ",";
		}

		updateQuery = "UPDATE `" + table + "` SET " + sets + " WHERE " + condition + " ;";

		return updateQuery;
	}

	/** */
	private void registerConnector() {

		// The following section of Java code shows how you might register MySQL Connector/J
		try {
			// The newInstance() call is a work around for some broken Java implementations
			Class.forName("com.mysql.jdbc.Driver").newInstance();

		}
		catch (Exception ex) {

			Log.message().severe("Error when loading JDBC driver!");
		}
	}

	/** */
	private ResultSet throwSelect(String querySelect) throws SQLException {

	    this.open(); // Re-creates the connection, if lost
	    
		Statement stmt = null;
		ResultSet rs = null;

		stmt = connection.createStatement();
		rs = stmt.executeQuery(querySelect);

		return rs;
	}

	/** */
	private Integer throwUpdate(String queryUpdate) throws SQLException {

	    this.open(); // Re-creates the connection, if lost
	    
		Statement stmt = null;
		Integer rs = null;
		
		stmt = connection.createStatement();
		rs = stmt.executeUpdate(queryUpdate);
		
		return rs;
	}
	
	public Integer deleteContracts() throws SQLException {

	    this.open(); // Re-creates the connection, if lost
	    
		Statement stmt = null;
		Integer rs = null;
		
		stmt = connection.createStatement();
		rs = stmt.executeUpdate("DELETE FROM " + TABLE_CONTRACT + " WHERE context_id<>4");
		rs = stmt.executeUpdate("DELETE FROM " + TABLE_SESSION + " WHERE context_id<>4");
		
		return rs;
	}
	
	
}

