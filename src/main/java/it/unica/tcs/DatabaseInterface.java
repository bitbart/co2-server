package it.unica.tcs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

// TODO: Create all javadocs

/** */
public class DatabaseInterface {

    // Connection to database.
    private Connection connection;

    // Database's credentials.
    public final static String DB_NAME = "co2_data";
    public static final String DB_PATH = "localhost";
    public static final String DB_USER = "root";
    public static final String DB_PASS = "Hj94kld*(";

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
    public final static int CONTRACT_EXPIRED = 6;

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

    
    private static DatabaseInterface instance;
    
    private DataSource datasource;
    
    private DatabaseInterface() {
        
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            this.datasource = (DataSource) envCtx.lookup("jdbc/co2datasource");
            
        } catch (NamingException e) {
            Log.message().severe("error istantiating the datasource: "+e.getMessage());
        }
        
    }
    
    public static DatabaseInterface getInstance() {
        if (instance==null)
            instance = new DatabaseInterface();
        
        return instance;
    }
    
    /** */
    public void open() throws SQLException {

        if (this.connection == null) {
            Log.message().fine("opening a new connection");
            
            if (datasource!=null) {
                Log.message().fine("using datasource");
                this.connection = datasource.getConnection();
            }
            else {
                Log.message().fine("using old method");
                this.registerConnector();
                this.connection = DriverManager.getConnection("jdbc:mysql://localhost/" + DB_NAME + "?autoReconnect=true",
                        DB_USER, DB_PASS);
            }
        }
    }

    /** */
    public void close() {

        try {
            if (this.connection != null)
                connection.close();
        } catch (SQLException e) {

            Log.message().warning("Can't close a opened connection. SQL says: " + e);
        }

        connection = null;
    }

    /**
     * @throws SQLException
     */
    @SuppressWarnings("resource")
    public ResultSet select(String query) throws SQLException {

        ResultSet resultQuery;
        this.open(); // Re-creates the connection, if lost

        Statement stmt = this.connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        resultQuery = rs;

        return resultQuery;
    }

    /**
     * @throws SQLException
     */
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

    /**
     * @throws SQLException
     */
    public void updatePassword(String username, String newPassword) throws SQLException {

        String query, condition;
        String[] cols = new String[1];
        String[] vals = new String[1];

        cols[0] = "password";
        vals[0] = Tools.hash256(newPassword);

        condition = "`email` = '" + username + "'";

        query = generateUpdateQuery(TABLE_USER, cols, vals, condition);

        this.throwUpdate(query);
    }

    public Integer insertContract(String contractHash, String contractXML, Integer ownerID, Integer contextID,
            Integer role, Integer state, Long randomLong, String typePreCheck, String mapping, String aux,
            Integer delay) throws SQLException {

        return insertContract(contractHash, contractXML, ownerID, contextID, role, state, randomLong, typePreCheck,
                mapping, aux, delay, false);
    }

    /**
     * @throws SQLException
     */
    public Integer insertContract(String contractHash, String contractXML, Integer ownerID, Integer contextID,
            Integer role, Integer state, Long randomLong, String typePreCheck, String mapping, String aux,
            Integer delay, boolean prv) throws SQLException {

        String insertQuery, selectQuery;
        Integer identifier;

        String[] cols = new String[14];
        String[] vals = new String[14];

        cols[0] = "contract_hash";
        cols[1] = "contract_xml";
        cols[2] = "owner_id";
        cols[3] = "context_id";
        cols[4] = "role";
        cols[5] = "state";
        cols[6] = "random_long";
        cols[7] = "type_pre_check";
        cols[8] = "mapping";
        cols[9] = "aux";
        cols[10] = "delay";
        cols[11] = "tell_timestamp";
        cols[12] = "type";
        cols[13] = "private";

        vals[0] = contractHash;
        vals[1] = contractXML;
        vals[2] = ownerID + "";
        vals[3] = contextID + "";
        vals[4] = role + "";
        vals[5] = state + "";
        vals[6] = randomLong.toString();
        vals[7] = typePreCheck;
        vals[8] = mapping;
        vals[9] = aux;
        vals[10] = delay + "";
        vals[11] = System.currentTimeMillis() + "";
        vals[12] = Contract.TYPE_TST; // TODO: handle different contract types
        vals[13] = prv ? "1" : "0";

        insertQuery = generateInsertQuery(TABLE_CONTRACT, cols, vals);

        this.throwUpdate(insertQuery);

        // Returning ID of the new contract added.
        selectQuery = "SELECT `contract_id` FROM contract WHERE `contract_hash` = '" + contractHash + "'";

        try (ResultSet rs = this.select(selectQuery);) {
            rs.next();
            identifier = rs.getInt(1);

            return identifier;
        }
    }

    public void insertTrace(Integer actionID, String actionName, Integer role, Integer sessionID, String value,
            boolean isFile) throws SQLException {

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

    /**
     * @throws SQLException
     */
    public void insertTrace(Integer actionID, String actionName, Integer role, Integer sessionID, Integer value)
            throws SQLException {

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

    /**
     * @throws SQLException
     */
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

    /**
     * @throws SQLException
     */
    public Integer insertSession(String sessionHash, Integer state, String lastState, Integer contextID)
            throws SQLException {

        String insertQuery, selectQuery;
        Integer identifier;

        String[] cols = new String[6];
        String[] vals = new String[6];

        cols[0] = "session_hash";
        cols[1] = "state";
        cols[2] = "context_id";
        cols[3] = "start_timestamp";
        cols[4] = "last_timestamp";
        cols[5] = "last_state"; // A "load_file" column must be the last in the
        // string array

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

        try (ResultSet rs = this.select(selectQuery);) {
            rs.next();
            identifier = rs.getInt(1);

            return identifier;
        }
    }

    /**
     * @throws SQLException
     */
    public void updateUser(Integer userID, String firstName, String lastName, String email, String password,
            Integer reputation, Integer credit, Double tv, Double[] ftv) throws SQLException {

        String updateQuery, condition;

        String[] cols = new String[7];
        String[] vals = new String[7];

        cols[0] = "first_name";
        cols[1] = "last_name";
        cols[2] = "email";
        cols[3] = "password";
        cols[4] = "reputation";
        cols[5] = "credit";
        cols[6] = "tv";

        vals[0] = firstName;
        vals[1] = lastName;
        vals[2] = email;
        vals[3] = password;
        vals[4] = reputation + "";
        vals[5] = credit + "";
        vals[6] = tv + "";

        condition = "user_id = '" + userID + "'";

        updateQuery = generateUpdateQuery(TABLE_USER, cols, vals, condition);

        this.throwUpdate(updateQuery);

        saveFTV(userID, ftv);
    }

    /**
     * @throws SQLException
     */
    public void setTraceRead(Integer traceID) throws SQLException {

        String updateQuery, condition;

        String[] cols = new String[1];
        String[] vals = new String[1];

        cols[0] = "read";

        vals[0] = 1 + "";

        condition = "trace_id = '" + traceID + "'";

        updateQuery = generateUpdateQuery(TABLE_TRACE, cols, vals, condition);

        this.throwUpdate(updateQuery);
    }

    /**
     * @throws SQLException
     */
    public void saveNetwork(Integer sessionID, String last_state) throws SQLException {

        String updateQuery, condition;

        String[] cols = new String[2];
        String[] vals = new String[2];

        cols[0] = "last_timestamp";
        cols[1] = "last_state"; // A "load_file" column must be the last in the
        // string array

        vals[0] = Long.toString(System.currentTimeMillis());
        vals[1] = last_state;

        condition = "session_id = '" + sessionID + "'";

        updateQuery = generateUpdateQuery(TABLE_SESSION, cols, vals, condition, true);

        this.throwUpdate(updateQuery);
    }

    public void setSessionState(Integer sessionID, Integer state) throws SQLException {

        String updateQuery, condition;

        String[] cols = new String[1];
        String[] vals = new String[1];

        cols[0] = "state";

        vals[0] = state + "";

        condition = "session_id = '" + sessionID + "'";

        updateQuery = generateUpdateQuery(TABLE_SESSION, cols, vals, condition);

        this.throwUpdate(updateQuery);
    }

    /**
     * @throws SQLException
     */
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

    /**
     * @throws SQLException
     */
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

    /**
     * @throws SQLException
     */
    public void updateContract(String contractHash, Integer sessionID, Integer role, Integer state)
            throws SQLException {

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

    public void updateContractState(String contractHash, Integer state) throws SQLException {

        String updateQuery, condition;

        String[] cols = new String[1];
        String[] vals = new String[1];

        cols[0] = "state";
        vals[0] = state + "";

        condition = "contract_hash = '" + contractHash + "'";

        updateQuery = generateUpdateQuery(TABLE_CONTRACT, cols, vals, condition);

        this.throwUpdate(updateQuery);
    }

    /**
     * @throws SQLException
     */
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

    /**
     * @throws SQLException
     */
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

    // Methods for handling array storing

    /** This method will help to convert any object into byte array */
    public byte[] convertObjectToByteArray(Object obj) throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(obj);
        return byteArrayOutputStream.toByteArray();
    }

    /** This method will help to save java objects into database */
    public long saveFTV(Integer userID, Object javaObject2Persist) throws SQLException {

        byte[] byteArray = null;
        String SQLQUERY_TO_SAVE_JAVAOBJECT = "UPDATE user SET ftv=? WHERE user_id=" + userID;
        int persistObjectID = -1;
        try (PreparedStatement preparedStatement = connection.prepareStatement(SQLQUERY_TO_SAVE_JAVAOBJECT,
                PreparedStatement.RETURN_GENERATED_KEYS)) {

            byteArray = convertObjectToByteArray(javaObject2Persist);
            preparedStatement.setBytes(1, byteArray);
            preparedStatement.executeUpdate();

            @SuppressWarnings("resource") // closing the PreparedStatement
            // automatically close the ResultSet
            ResultSet rs = preparedStatement.getGeneratedKeys();

            if (rs.next()) {
                persistObjectID = rs.getInt(1);
            }

        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException(e.getMessage());
        }
        return persistObjectID;
    }

    /** This method will help to read java objects from database */
    public Double[] getFTV(String userID) throws SQLException {

        Blob blob = null;
        byte[] bytes = null;

        try (ResultSet resultSet = select("SELECT ftv FROM user WHERE user_id = " + userID)) {
            resultSet.next();
            blob = resultSet.getBlob("ftv");

            if (blob != null)
                bytes = blob.getBytes(1, (int) (blob.length()));

        } catch (SQLException e) {

            throw e;
        } catch (Exception e) {

            e.printStackTrace();

            Log.message().severe("Unknown exception in getFTV: " + e.getMessage());
            throw new SQLException("unknown exception.");

        }

        ObjectInputStream objectInputStream = null;

        try {

            Double[] res;

            if (bytes != null) {
                objectInputStream = new ObjectInputStream(new ByteArrayInputStream(bytes));

                Object retrievingObject = objectInputStream.readObject();

                res = (Double[]) retrievingObject;

                if (res == null || res.length != 4) {

                    res = new Double[4];
                    res[0] = 0.;
                    res[1] = 0.;
                    res[2] = 0.;
                    res[3] = 0.;
                }
            } else {

                res = new Double[4];
                res[0] = 0.;
                res[1] = 0.;
                res[2] = 0.;
                res[3] = 0.;
            }

            return res;
        } catch (IOException | ClassNotFoundException e) {

            throw new SQLException("IOException or ClassNotFoundException in getFTV: " + e.getMessage());
        }

    }

    /*
     * Example of usage
     * 
     * @SuppressWarnings("unchecked") public static void main(String args[])
     * throws Exception { Connection connection = null; byte[]
     * retrievedArrayObject = null; try { connection = getConnection();
     * 
     * List<Object> listToSaveInDB = new ArrayList<Object>();
     * listToSaveInDB.add(new Date()); listToSaveInDB.add(new String(
     * "KUMAR GAURAV")); listToSaveInDB.add(new Integer(55));
     * 
     * long persistObjectID = saveBlob(connection, listToSaveInDB);
     * System.out.println(listToSaveInDB + " Object is saved sucessfully");
     * 
     * retrievedArrayObject = getBlob(connection, persistObjectID);
     * 
     * ObjectInputStream objectInputStream = null; if (retrievedArrayObject !=
     * null) objectInputStream = new ObjectInputStream(new
     * ByteArrayInputStream(retrievedArrayObject));
     * 
     * Object retrievingObject = objectInputStream.readObject();
     * 
     * List<Object> dataListFromDB = (List<Object>) retrievingObject; for
     * (Object object : dataListFromDB) { System.out.println(
     * "Retrieved Data is :->" + object.toString()); }
     * 
     * System.out.println("Successfully retrieved java Object from Database");
     * 
     * } catch (Exception e) { e.printStackTrace(); } finally {
     * connection.close(); } }
     */

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

        if (cols.length != vals.length)
            throw new SQLException();

        for (int i = 0; i < cols.length; i++) {

            if (loadfile && i == cols.length - 1)
                sets += "`" + cols[i] + "`=" + "LOAD_FILE('" + vals[i] + "')";
            else
                sets += "`" + cols[i] + "`=" + "'" + vals[i] + "'";

            if (i < cols.length - 1)
                sets += ",";
        }

        updateQuery = "UPDATE `" + table + "` SET " + sets + " WHERE " + condition + " ;";

        return updateQuery;
    }

    /** */
    private void registerConnector() {

        // The following section of Java code shows how you might register MySQL
        // Connector/J
        try {
            // The newInstance() call is a work around for some broken Java
            // implementations
            Class.forName("com.mysql.jdbc.Driver").newInstance();

        } catch (Exception ex) {

            Log.message().severe("Error when loading JDBC driver!");
        }
    }

    /** */
    private Integer throwUpdate(String queryUpdate) throws SQLException {

        this.open(); // Re-creates the connection, if lost

        try (Statement stmt = connection.createStatement()) {
            Integer rs = stmt.executeUpdate(queryUpdate);
            return rs;
        }
    }

    public Integer deleteContracts() throws SQLException {

        this.open(); // Re-creates the connection, if lost

        try (Statement stmt = connection.createStatement();) {
            Integer rs = stmt.executeUpdate("DELETE FROM " + TABLE_CONTRACT + " WHERE context_id<>4");
            rs = stmt.executeUpdate("DELETE FROM " + TABLE_SESSION + " WHERE context_id<>4");

            return rs;
        }
    }

}
