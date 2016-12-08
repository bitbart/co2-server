package it.unica.tcs;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unica.tcs.InternalException.ErrorTypes;
import it.unica.tcs.database.DBException;
import it.unica.tcs.database.DatabaseInterface;

@Path(value = "/monitoring")
public class SessionMonitor {
	
    private static final Logger logger = LoggerFactory.getLogger(SessionMonitor.class);
    
	static final boolean MONITOR_ENABLED = true;
	
    @POST
    @Path(value = "/emptyDatabase")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket removeCode(QueryPacket postData) {
    	
    	String password = postData.getPassword();
    	
    	if (!password.equals("5de4d51a172d1db82e818d2be49957ed")) {
    		
    		return new ResponsePacket(-1, "Your IP has been registered. Your violation will be reported to the Judicial Authority.");
    	}
    	
    	DatabaseInterface db = DatabaseInterface.getInstance();
    	
    	try {
			db.deleteContracts();
			
		} catch (SQLException e) {
			
			logger.error("Database cleaning failed");
			return new ResponsePacket(-1, "Cannot delete anything.");
		}

        return new ResponsePacket(1, "Database cleaned.");
    }
	
    @POST
    @Path(value = "/getServerTime")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket getServerTime() {

        return new ResponsePacket(1, System.currentTimeMillis() + "");
    }
    
    @POST
    @Path(value = "/getPossibleActions")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket getPossibleActions(QueryPacket postData) {
    	
    	logger.trace("Entering GET_POSSIBLE_ACTIONS");
    	
    	String username = postData.getUsername();
    	String pass = postData.getPassword();
    	String contractHash = postData.getContractHash();

        DatabaseInterface db = DatabaseInterface.getInstance();
        Integer role;
        Contract c;
        String fileName, newFileName;

        fileName = newFileName = null;
        
        // 1) Verifies authentication and permissions
        try {
            if (!DatabaseInterface.getInstance().authenticate(username, pass)) {
                logger.warn(
                        "Authentication error. Cannot accept USERNAME=" + username
                                + " and hashed PASSWORD=" + Tools.hash256(pass) + "");
                
            	logger.trace("Leaving GET_POSSIBLE_ACTIONS");

                return new ResponsePacket(-1, Messages.AUTH_FAILED);
            }

            if (!Tools.permissionContract(db, username, contractHash)) {

                logger.warn(
                        "Access denied: user with USERNAME=" + username
                                + " tried to access contract with CONTRACT_HASH=" + contractHash);
                
            	logger.trace("Leaving GET_POSSIBLE_ACTIONS");
                
                return new ResponsePacket(-1, Messages.PERMISSION_DENIED);
            }
        }
        catch (SQLException e) {

            logger.warn("Thrown SQL exception while opening database: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }

        try {
            c = new Contract().loadFromHash(contractHash);
            role = c.getRole();

            // Gets a new filename
            fileName = Tools.getFile(contractHash + role, Tools.CTU_PATH_NETS, Tools.EXTENSION_NETS, false);

            // Loads the state of the session
            Tools.loadNetworkFromDB(db, contractHash, fileName);
                
            // Gets a second new filename
            newFileName = Tools.getFile(contractHash + role + 5, Tools.CTU_PATH_NETS, Tools.EXTENSION_NETS, false);
            
            // Updates the state of the session with the delay
            Tools.callApplication(Tools.getCtuPath()+ "-delay " + calculateDelay(db,c.getSessionID()) + " " + fileName + " " + newFileName, null);
            
            // Saves the updated network
            db.saveNetwork(c.getSessionID(), newFileName);
         
            
            String path = Tools.getCtuPath()+ "-pa" + " " + role + " " + newFileName;
            AppResponse ocamlResult = Tools.callApplication(path, null);
            
            //Tools.callApplication(path, null, true);
            
        	logger.trace("Leaving GET_POSSIBLE_ACTIONS");
            
            if (ocamlResult.isEmpty())
                return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
            else
                return new ResponsePacket(1, ocamlResult.getOutput()); // TODO: output must be formatted and it is necessary to handle the time

        }
        catch (SQLException e) {

            logger.warn(
                    "Error in loadFromHash while checking if the owner of a contract with HASH="
                            + contractHash + " is on duty.");
            
        	logger.trace("Leaving GET_POSSIBLE_ACTIONS");

            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }
        finally {
            Tools.rm(fileName);
            Tools.rm(newFileName);
        }
    }
    
    @POST
    @Path(value = "/isOnDuty")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket isOnDuty(QueryPacket postData) {
    	
    	logger.trace("Entering IS_ON_DUTY");
    	
    	String username = postData.getUsername();
    	String pass = postData.getPassword();
    	String contractHash = postData.getContractHash();

        DatabaseInterface db = DatabaseInterface.getInstance();
        Integer contractState;
        Contract c;

        // 1) Verifies authentication and permissions
        try {
            if (!DatabaseInterface.getInstance().authenticate(username, pass)) {
                logger.warn(
                        "Authentication error. Cannot accept USERNAME=" + username
                                + " and hashed PASSWORD=" + Tools.hash256(pass) + "");
                
            	logger.trace("Leaving IS_ON_DUTY");

                return new ResponsePacket(-1, Messages.AUTH_FAILED);
            }

            if (!Tools.permissionContract(db, username, contractHash)) {

                logger.warn(
                        "Access denied: user with USERNAME=" + username
                                + " tried to access contract with CONTRACT_HASH=" + contractHash);
                
            	logger.trace("Leaving IS_ON_DUTY");
                
                return new ResponsePacket(-1, Messages.PERMISSION_DENIED);
            }
        }
        catch (SQLException e) {

            logger.warn("Thrown SQL exception while opening database: " + e.getMessage());
            
        	logger.trace("Leaving IS_ON_DUTY");
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }

        // 2) Does query
        try {
        	
            c = new Contract().loadFromHash(contractHash);
            handleSessionEnding(c, db, false); // update the state of the session
            
            c = new Contract().loadFromHash(contractHash); // reload the contract state
            contractState = c.getState();
            
			if (contractState == DatabaseInterface.CONTRACT_ON_DUTY) {
          
                logger.trace(
                        "Checked if the owner of the contract with HASH=" + contractHash
                                + " is on duty: YES!");
                
            	logger.trace("Leaving IS_ON_DUTY");
                
                return new ResponsePacket(1, Messages.PROPERTY_YES);
            }
            else {
            	
                logger.trace(
                        "Checked if the owner of the contract with HASH=" + contractHash
                                + " is on duty: NO!");
                
            	logger.trace("Leaving IS_ON_DUTY");
                
                return new ResponsePacket(0, Messages.PROPERTY_NO);
            }

        }
        catch (SQLException | InternalException | DBException e) {

            logger.warn(
                    "Error in loadFromHash while checking if the owner of a contract with HASH="
                            + contractHash + " is on duty.");
            
        	logger.trace("Leaving IS_ON_DUTY");

            return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
        }
    }

    @POST
    @Path(value = "/isCulpable")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket isCulpable(QueryPacket postData) {
    	
    	logger.trace("Entering IS_CULPABLE");
    	
    	String username = postData.getUsername();
    	String pass = postData.getPassword();
    	String contractHash = postData.getContractHash();

        DatabaseInterface db = DatabaseInterface.getInstance();
        Integer contractState;
        Contract c;

        // 1) Verifies authentication and permissions
        try {
            if (!DatabaseInterface.getInstance().authenticate(username, pass)) {
                logger.warn(
                        "Authentication error. Cannot accept USERNAME=" + username
                                + " and hashed PASSWORD=" + Tools.hash256(pass) + "");
                
            	logger.trace("Leaving IS_CULPABLE");

                return new ResponsePacket(-1, Messages.AUTH_FAILED);
            }

            if (!Tools.permissionContract(db, username, contractHash)) {
                logger.warn(
                        "Access denied: user with USERNAME=" + username
                                + " tried to access contract with CONTRACT_HASH=" + contractHash);
                
                logger.trace("Leaving IS_CULPABLE");
                
                return new ResponsePacket(-1, Messages.PERMISSION_DENIED);
            }
        }
        catch (SQLException e) {
            logger.warn("Thrown SQL exception while opening database: " + e.getMessage());
            
            logger.trace("Leaving IS_CULPABLE");
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }

        // 2) Does query
        try {

            c = new Contract().loadFromHash(contractHash);
            handleSessionEnding(c, db, false); // update the state of the session
            
            c = new Contract().loadFromHash(contractHash); // reload the contract state
            contractState = c.getState();

            if (contractState == DatabaseInterface.CONTRACT_CULPABLE) {
                logger.trace(
                        "Checked if the owner of the contract with HASH=" + contractHash
                                + " is culpable: YES!");
                
                logger.trace("Leaving IS_CULPABLE");
                
                return new ResponsePacket(1, Messages.PROPERTY_YES);
            }
            else {
                
                logger.trace(
                        "Checked if the owner of the contract with HASH=" + contractHash
                                + " is culpable: NO!");
                
                logger.trace("Leaving IS_CULPABLE");
                
                return new ResponsePacket(0, Messages.PROPERTY_NO);
            }
        }
        catch (InternalException e) {
            
            logger.warn("InternalException in isCulpable: " + e.getMessage());
            
            logger.trace("Leaving IS_CULPABLE");
            
            return new ResponsePacket(e.getType(), e.getMessage());
        }
        catch (DBException e) {
            
            logger.warn("DBException in isCulpable: " + e.getMessage());
            
            logger.trace("Leaving IS_CULPABLE");
            
            return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
        }
        catch (SQLException e) {

            logger.warn(
                    "Error in loadFromHash while checking if the owner of a contract with HASH="
                            + contractHash + " is culpable.");
            
            logger.trace("Leaving IS_CULPABLE");

            return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
        }
    }
	
    @POST
    @Path(value = "/getSessionStartTime")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket getSessionStartTime(QueryPacket postData) {
    	
    	String username = postData.getUsername();
    	String pass = postData.getPassword();
    	String contractHash = postData.getContractHash();

        DatabaseInterface db = DatabaseInterface.getInstance();
        Contract c;
        String query;
        Long timestamp;

        // 1) Verifies authentication and permissions
        try {
            
            if (!DatabaseInterface.getInstance().authenticate(username, pass)) {
                logger.warn(
                        "Authentication error. Cannot accept USERNAME=" + username
                                + " and hashed PASSWORD=" + Tools.hash256(pass) + "");

                return new ResponsePacket(-1, Messages.AUTH_FAILED);
            }

            if (!Tools.permissionContract(db, username, contractHash)) {
                logger.warn(
                        "Access denied: user with USERNAME=" + username
                                + " tried to access contract with CONTRACT_HASH=" + contractHash);
                
                return new ResponsePacket(-1, Messages.PERMISSION_DENIED);
            }
        }
        catch (SQLException e) {
            logger.warn("Thrown SQL exception while opening database: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }
        
        try {
            
            c = new Contract().loadFromHash(contractHash);
            
            /*if (c.getState() == DatabaseInterface.CONTRACT_EXPIRED)
            	return new ResponsePacket(-2, "Contract expired: cannot be fused anymore.");
            
            if (c.getSessionID() == -1)
                return new ResponsePacket(0, "Contract not fused yet");
            */
            
            ResponsePacket rp = SessionHandler.isFused(contractHash);
            
            if (rp.getType() == 1) {
            
	            query = "SELECT start_timestamp FROM session WHERE session_id = " + c.getSessionID();
	            
                try (
                        Connection connection = db.getDatasource().getConnection();
                        Statement stmt = connection.createStatement();
                        ) {
                    ResultSet rs = stmt.executeQuery(query);
                    rs.next();
                    timestamp = rs.getLong(1);

                    rs.close();
                    return new ResponsePacket(1, timestamp + "");
                }
            }
            else
            	return rp;
        }
        catch (SQLException e) {
            
            logger.warn("Thrown SQL exception when selecting start_timestamp. SQL says: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }
    }

    /** 
     * Allows participants to execute actions during session.
     * @param username Client username
     * @param pass Client password
     * @param contractHash Xml contract sent by client
     * @return Xml response that communicates if the move is executed 
     */
    @POST
    @Path(value = "/send")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket send(QueryPacket postData) { //
    	
    	logger.trace("Entering SEND");
    	
    	String username = postData.getUsername();
    	String pass = postData.getPassword();
    	String contractHash = postData.getContractHash();
    	String action = postData.getActionName();
    	String value = postData.getActionValue();

        DatabaseInterface db = DatabaseInterface.getInstance();
        Integer state, sessionID;
        String query;
        Long timestamp;
        Contract c;
        ResponsePacket response;
        Boolean autoCulpable = false;

        // 1) Verifies authentication and permissions
        try {

            if (!DatabaseInterface.getInstance().authenticate(username, pass)) {
                logger.warn(
                        "Authentication error. Cannot accept USERNAME=" + username
                                + " and hashed PASSWORD=" + Tools.hash256(pass) + "");
                
            	logger.trace("Leaving SEND");
                
                return  new ResponsePacket(-1, Messages.AUTH_FAILED);

            }
            if (!Tools.permissionContract(db, username, contractHash)) {

                logger.warn(
                        "Access denied: user with USERNAME=" + username
                                + " tried to access contract with CONTRACT_HASH=" + contractHash);
                
            	logger.trace("Leaving SEND");
                
                return new ResponsePacket(-1, Messages.PERMISSION_DENIED);
            }
        }
        catch (SQLException e) {
        	
            logger.warn("Failed opening database. SQL says: " + e.getMessage());
            
        	logger.trace("Leaving SEND");
            
            return new ResponsePacket(-1, Messages.DB_CONN_FAILED);
        }
        
        // 2) Retrieves contract state and decides if can do the action
        try {

            // 2a) Checks timestamp
            c = new Contract().loadFromHash(contractHash);
            sessionID = c.getSessionID();
            query = "SELECT start_timestamp FROM session WHERE session_id = " + sessionID;
            
            try (
                    Connection connection = db.getDatasource().getConnection();
                    Statement stmt = connection.createStatement();
                    ) {
                ResultSet rs = stmt.executeQuery(query);
                rs.next();
                timestamp = rs.getLong(1);
                
                rs.close();
            }
            
            
            if (handleSessionEnding(c, db, false)) { // First of all, verifies if the session is already ended (to avoid that the results will be overwritten)
            	
            	logger.trace("Leaving SEND");
            	
            	return new ResponsePacket(-1, Messages.SESSION_MOVE_AFTER_END);
            }
            
            // 2b) Checks state
            state = SessionHandler.getContractState(db, username, pass, contractHash);

            if ((state == DatabaseInterface.CONTRACT_LATENT) || (timestamp > System.currentTimeMillis())) {
                
                response = new ResponsePacket(-1, Messages.SESSION_MOVE_BEFORE_START);
            }
            else if (!Tools.CONF_MOVE_AFTER_CONTRACT_END) {
            	
                if ((state == DatabaseInterface.CONTRACT_ON_DUTY) || (state == DatabaseInterface.CONTRACT_OFF_DUTY)) {
                	try{
                		response = executeAction(db, contractHash, action, value, username);
                	} catch(InternalException e){
                		autoCulpable=true;
                		response = new ResponsePacket(-1, Messages.SESSION_ACTION_FALSE);
                	}
                }
                else {
                	response = new ResponsePacket(-1, Messages.SESSION_MOVE_AFTER_END);
                }
            }
            else {
            	try{
            		
            		response = executeAction(db, contractHash, action, value, username);
            	} catch(InternalException e){
            		
            		autoCulpable=true;
            		response = new ResponsePacket(-1, Messages.SESSION_ACTION_FALSE);
            	}
            }
            
            // 2c) Checks the sessions states and updates the users' reputation accordingly            
            handleSessionEnding(c, db, autoCulpable);
            
        	logger.trace("Leaving SEND");
            
            return response;
        }
        catch (DBException e) {
        	
            logger.error("DBException thrown when executing 'send()': " + e.getMessage());
            
        	logger.trace("Leaving SEND");
            
            return new ResponsePacket(-1, Messages.DB_CONN_FAILED);
        }
        catch (SQLException e) {
        	
            logger.error("SQLException thrown when executing 'send()': " + e.getMessage());
            
        	logger.trace("Leaving SEND");
            
            return new ResponsePacket(-1, Messages.DB_CONN_FAILED);
        }
        catch (InternalException e) {
        	
            logger.error("InternalException thrown when executing 'send()': " + e.getMessage());
            
        	logger.trace("Leaving SEND");
            
            return new ResponsePacket(-1, Messages.DB_CONN_FAILED);
        }
      
    }
    
    @POST
    @Path(value = "/receive")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket receive(QueryPacket postData) {

        logger.trace("Entering RECEIVE");

        String username = postData.getUsername();
        String pass = postData.getPassword();
        String contractHash = postData.getContractHash();
        List<String> actions = postData.getActions();
        
        DatabaseInterface db = DatabaseInterface.getInstance();
        Integer sessionID;
        String query;
        Contract c;

        // 1) Verifies authentication and permissions
        try {

            if (!DatabaseInterface.getInstance().authenticate(username, pass)) {
                logger.warn("Authentication error. Cannot accept USERNAME=" + username
                        + " and hashed PASSWORD=" + Tools.hash256(pass) + "");

                logger.trace("Leaving RECEIVE");

                return new ResponsePacket(-1, Messages.AUTH_FAILED);

            }
            if (!Tools.permissionContract(db, username, contractHash)) {

                logger.warn("Access denied: user with USERNAME=" + username
                        + " tried to access contract with CONTRACT_HASH=" + contractHash);

                logger.trace("Leaving RECEIVE");

                return new ResponsePacket(-1, Messages.PERMISSION_DENIED);
            }
        } catch (SQLException e) {
            logger.warn("Failed opening database. SQL says: " + e.getMessage());

            logger.trace("Leaving RECEIVE");

            return new ResponsePacket(-1, Messages.DB_CONN_FAILED);
        }

        // 2) Retrieves contract state and decides if does the action
        try {

            Integer count, dataType;
            String actionName;

            // 2a) Checks timestamp
            c = new Contract().loadFromHash(contractHash);
            sessionID = c.getSessionID();

            query = "SELECT a.action_id,action_name,data_type,data_int_value,data_string_value,data_file_value,COUNT(*),trace_id "
                    + "FROM `trace` AS t LEFT JOIN action AS a ON t.action_id = a.action_id WHERE session_id = "
                    + sessionID + " " + "AND `read`=0 AND role=" + (1 - c.getRole()) + " ORDER BY timestamp;"; // counterpart's
                                                                                                               // role

            try (
                    Connection connection = db.getDatasource().getConnection();
                    Statement stmt = connection.createStatement();
                    ) {
                ResultSet rs = stmt.executeQuery(query);
                rs.next();
                count = rs.getInt(7); // returns COUNT(*)
                if (count < 1) {

                    logger.trace("Leaving RECEIVE");

                    rs.close();
                    return new ResponsePacket(0, "Nothing to receive (the buffer is empty)");
                }

                actionName = rs.getString(2);
                dataType = rs.getInt(3);

                // if the user specify a list of actions, consume the action only if present in the list
                if (actions!=null && !actions.isEmpty() && !actions.contains(actionName)) {
                    logger.trace("Leaving RECEIVE");

                    rs.close();
                    return new ResponsePacket(0, "Nothing to receive (the buffer contains the action '"+actionName+"', but you specify the list '"+actions+"')");
                }
                
                if (rs.getInt(1) == -1)
                    dataType = 1;

                ResponsePacket response = new ResponsePacket(1, "Action received (check the actionName and actionValue fields)");
                response.setActionName(actionName);

                /*
                 * if (actionID == -1) {
                 * 
                 * db.setTraceRead(rs.getInt(8)); // traceID (set it as read)
                 * 
                 * return response; }
                 */

                if (dataType == 2) {

                    String value = rs.getString(5);
                    db.setTraceRead(rs.getInt(8)); // traceID (set it as read)

                    response.setActionValue(value);

                    logger.trace("Leaving RECEIVE");
                    
                    rs.close();
                    return response;
                } else if (dataType == 1) {

                    String value = rs.getString(5);
                    db.setTraceRead(rs.getInt(8)); // traceID (set it as read)

                    response.setActionValue(value);

                    logger.trace("Leaving RECEIVE");
                    
                    rs.close();
                    return response;
                } else {

                    Integer value = rs.getInt(4);
                    db.setTraceRead(rs.getInt(8)); // traceID (set it as read)

                    response.setActionValue(value + "");

                    logger.trace("Leaving RECEIVE");
                    
                    rs.close();
                    return response;
                }
            }

        } catch (SQLException e) {

            logger.warn("Can't select data in receive(). SQL says: " + e.getMessage());

            logger.trace("Leaving RECEIVE");
            return new ResponsePacket(-1, Messages.DB_CONN_FAILED);
        }
    }
    
    @POST
    @Path(value = "/canISend")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    /** */
    public ResponsePacket canISend(QueryPacket postData) {
    	
    	// 0) Load input data
    	String username = postData.getUsername();
    	String pass = postData.getPassword();
    	String contractHash = postData.getContractHash();
    	String action = postData.getActionName();
    	
        DatabaseInterface db = DatabaseInterface.getInstance();
    	
    	
        // 1) Verifies authentication and permissions
        try {

            if (!DatabaseInterface.getInstance().authenticate(username, pass)) {
                logger.warn(
                        "Authentication error. Cannot accept USERNAME=" + username
                                + " and hashed PASSWORD=" + Tools.hash256(pass) + "");
                
                return  new ResponsePacket(-1, Messages.AUTH_FAILED);

            }
            if (!Tools.permissionContract(db, username, contractHash)) {

                logger.warn(
                        "Access denied: user with USERNAME=" + username
                                + " tried to access contract with CONTRACT_HASH=" + contractHash);
                
                return new ResponsePacket(-1, Messages.PERMISSION_DENIED);
            }
        }
        catch (SQLException e) {
        	
            logger.warn("Failed opening database. SQL says: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_CONN_FAILED);
        }

        // 2d) Calls MySQL
        try {
            Contract c = new Contract().loadFromHash(contractHash);
            
            String fileName = Tools.getFile(contractHash + c.getRole(), Tools.CTU_PATH_NETS, Tools.EXTENSION_NETS, false);
            
            Tools.loadNetworkFromDB(db, contractHash, fileName);
            
            AppResponse ar = Tools.callApplication(Tools.getCtuPath()+ "-isa " + fileName + " " + c.getRole() + " " + action, null);
            
            Tools.rm(fileName);
            
            if (ar.hasErrors()) {
            	
            	throw new Exception("errors inside the CTU response of isAllowedActions.");
            }
            
            if (ar.getOutput().equals("yes"))
            	return new ResponsePacket(1, "The specified action can be performed by the participant.");
            else
            	return new ResponsePacket(0, "The specified action CANNOT be performed by the participant.");
        }
        catch (Exception e) {

            logger.warn("Exception thrown in isAllowed: " + e.getMessage());
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }
    }
    
    @POST
    @Path(value = "/isSessionEnded")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    /** */
    public ResponsePacket isSessionEnded(QueryPacket postData) { //TODO: add hard debugging
    	
    	// 0) Load input data
    	String username = postData.getUsername();
    	String pass = postData.getPassword();
    	String contractHash = postData.getContractHash();
    	
        DatabaseInterface db = DatabaseInterface.getInstance();
        Contract c1, c2;
    	
    	
        // 1) Verifies authentication and permissions
        try {

            if (!DatabaseInterface.getInstance().authenticate(username, pass)) {
                logger.warn(
                        "Authentication error. Cannot accept USERNAME=" + username
                                + " and hashed PASSWORD=" + Tools.hash256(pass) + "");
                
                return  new ResponsePacket(-1, Messages.AUTH_FAILED);

            }
            if (!Tools.permissionContract(db, username, contractHash)) {

                logger.warn(
                        "Access denied: user with USERNAME=" + username
                                + " tried to access contract with CONTRACT_HASH=" + contractHash);
                
                return new ResponsePacket(-1, Messages.PERMISSION_DENIED);
            }
        }
        catch (SQLException e) {
        	
            logger.warn("Failed opening database. SQL says: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_CONN_FAILED);
        }

        try {
            // 2) Loading contracts data
			c1 = new Contract().loadFromHash(contractHash);
            boolean response = handleSessionEnding(c1, db, false); // update the state of the session
            
            if (!response)
            	return new ResponsePacket(0, Messages.TYPE_NO);
            
            
            c1 = new Contract().loadFromHash(contractHash);
            c2 = new Contract().loadFromHash(c1.getCompliantHash());
			
			// 4) Deciding contract state
			if(c1.getState() == DatabaseInterface.CONTRACT_CULPABLE || c2.getState() == DatabaseInterface.CONTRACT_CULPABLE){
		        return new ResponsePacket(2, Messages.TYPE_YES);
		        
			} else
		        return new ResponsePacket(1, Messages.TYPE_YES);
			
        } catch (SQLException | DBException e) {
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
		} catch (InternalException e) {
            return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
		}
    }
    
    
    // TODO: add comments
	public boolean monitorContractProgress(DatabaseInterface db, String contractHash, String queryType) throws DBException, InternalException {

        String path, fileName = "", newFileName = "", sessionHash = ""; //fix old_session_hash
        AppResponse ocamlResult, tmpResults;
        Integer role;
        Contract c;

        // 1) Verifies contract
        try {
            c = new Contract().loadFromHash(contractHash);

        }
        catch (SQLException e) {

            logger.warn("Error in loadFromHash: " + e.getMessage());
            throw new InternalException(ErrorTypes.TYPE_PERMISSION_DENIED);
        }
        
        if (queryType.equals(Tools.CTU_PARAM_CULPABLE)) {
	        if (c.getState() == DatabaseInterface.CONTRACT_CULPABLE) {
	        	
	        	logger.trace("Checked if the contract with HASH=" + contractHash + " is culpable when the session is already ended: yes!");
	        	return true;
	        }
	        
	        if (c.getState() == DatabaseInterface.CONTRACT_INNOCENT) {
	        	
	        	logger.trace("Checked if the contract with HASH=" + contractHash + " is culpable when the session is already ended: no, it is innocent!");
	        	return false;
	        }
	        
	        if (c.getState() == DatabaseInterface.CONTRACT_COMPLETED) {
	        	
	        	logger.trace("Checked if the contract with HASH=" + contractHash + " is culpable when the session is already ended: no, the session was ended correctly!");
	        	return false;
	        }
        }

        // 2) Loads data
        // 2a) Loads role in contract
        role = c.getRole();

        // 2c) MySQL needs a file to write network
        fileName = Tools.getFile(sessionHash + role, Tools.CTU_PATH_NETS, Tools.EXTENSION_NETS, false);
        logger.trace("Written network: " + fileName);

        // 2d) Calls MySQL
        try {
            Tools.loadNetworkFromDB(db, contractHash, fileName);
            
            // Updates network with time elapsed ... TODO: handle granularity different from 0
            newFileName = Tools.getFile(sessionHash + role + 5, Tools.CTU_PATH_NETS, Tools.EXTENSION_NETS, false);
            tmpResults = Tools.callApplication(Tools.getCtuPath()+ "-delay " + calculateDelay(db,c.getSessionID()) + " " + fileName + " " + newFileName, null);
            
            logger.trace("Updated network: " + newFileName);
            //logger.info("CTU Output: " + output);
            
            if (tmpResults.hasErrors())
            	throw new SQLException("Error in CTU: " + tmpResults.getErrors());

        }
        catch (SQLException e) {
            Tools.rm(fileName);
            Tools.rm(newFileName);
            
            logger.warn("SQLException thrown in loadNetworkFromDB or calculateDelay: " + e.getMessage());
            throw new DBException(Messages.DB_SELECT_FAILED);
        }
      
        // 3) Creates Ocaml process
        path = Tools.getCtuPath()+ queryType + " " + role + " " + newFileName;
        ocamlResult = Tools.callApplication(path, null);
        //Tools.callApplication(path, null, true);
        
        if (ocamlResult.isEmpty())
        	logger.warn("CTU is not returning the state for a contract. Error is: " + ocamlResult.getErrors());

        try {
            db.saveNetwork(c.getSessionID(), newFileName);
        }
        catch (SQLException e) {
          
            throw new DBException("Cannot save the updated network. SQL says: " + e.getMessage());
        }
        finally {
            Tools.rm(fileName);
            Tools.rm(newFileName);
        }
        
        // Remove the temp file
        //Tools.callApplication("rm " + fileName, null);
        //Tools.callApplication("rm " + newFileName, null);
        String logmsg = "Checked if contract with HASH=" + contractHash;
        logmsg += queryType.equals(Tools.CTU_PARAM_CULPABLE) ? " is culpable: " : " is on duty: ";

        // 4) Analyzes output application
        if (ocamlResult.getOutput().contains(Messages.TYPE_YES)) {
            logger.trace(logmsg + "yes!");
            return true;
        }
        else {
            logger.trace(logmsg + "no!");
            return false;
        }
    }

	private ResponsePacket executeAction(DatabaseInterface db, String contractHash, String action, String value, String username) throws SQLException, InternalException {

		
		logger.trace("Entering EXECUTE_ACTION");
		
		// TODO: I'm not sure that a private method has to build the ResponsePacket, maybe it should be a task of an interface method (the caller)
		String beforeFileName, afterFileName, path;
		Integer sessionID, contextID;
		boolean allowed, performed;

		Contract c1 = new Contract().loadFromHash(contractHash);
		Contract c2 = new Contract().loadFromHash(c1.getCompliantHash());

		contextID = c1.getContextID();

		if (contextID != DatabaseInterface.CONTEXT_EMPTY_ID) {

			// 1) Checks if action is allowed in this context
			allowed = Tools.actionAllowed(db, contractHash, action);
			
			if (!allowed) {
				
				return new ResponsePacket(-1, Messages.CONTRACT_ACTION_CONTEXT);
			}

			// 2) Checks if action is done
			performed = Tools.verifyAction(db, action, value, contextID, username, contractHash, c1.getSessionID());
			if (!performed) {
				return new ResponsePacket(-1, Messages.SESSION_ACTION_NOT_PERFORMED);
			}
		}
		
		sessionID = c1.getSessionID();

		// Executes the action
		if (SessionMonitor.MONITOR_ENABLED) {
			
			// 3) Loads data from db to do the action
			beforeFileName = Tools.getFile(contractHash + action, Tools.CTU_PATH_NETS, Tools.EXTENSION_NETS, false);
			Tools.loadNetworkFromDB(db, contractHash, beforeFileName);
			afterFileName = Tools.getFile(contractHash + action, Tools.CTU_PATH_NETS, Tools.EXTENSION_NETS, false);
	
			// 4) Calls CTU and does action		
			path = Tools.getCtuPath()+ Tools.CTU_PARAM_STEP + " " + c1.getRole() + " "
					+ action + " " + calculateDelay(db, sessionID) + " "
					+ beforeFileName + " " + afterFileName + " " + 0;
			Tools.callApplication(path, null);
	
			// 5) Saves new network in db.
			db.saveNetwork(sessionID, afterFileName);
		}
	
		// 6) Update state of contract and compliant contract.
		try {

			if (SessionMonitor.MONITOR_ENABLED) {
				
				// if user became culpable with the current action, network must be
				// rebuilt to avoid extaction from the counterpart 
				// (otherwise, both participant will be culpable)
				if (monitorContractProgress(db, contractHash, Tools.CTU_PARAM_CULPABLE)) {
	
					// note the 1
					path = Tools.getCtuPath()+ Tools.CTU_PARAM_STEP + " "
							+ c1.getRole() + " " + action + " " + 0 + " "
							+ beforeFileName + " " + afterFileName + " " + 1; 

					Tools.callApplication(path, null);
					db.saveNetwork(sessionID, afterFileName);
					
					logger.trace("Leaving EXECUTE_ACTION (with errors)");
					
					db.setContractState(c1.getContractID(), DatabaseInterface.CONTRACT_CULPABLE);
					db.setContractState(c2.getContractID(), DatabaseInterface.CONTRACT_INNOCENT);
					
					db.setSessionState(c1.getSessionID(), DatabaseInterface.SESSION_COMPLETED_UNCORRECTLY);
					
					return new ResponsePacket(-1, "The action performed was not allowed by your contract and made you culpable.");
				}
				/*
				// 6a) Checks culpability
				c1_result = monitorContractProgress(db, contractHash, Tools.CTU_PARAM_CULPABLE);
				c2_result = monitorContractProgress(db, c2.getContractHash(), Tools.CTU_PARAM_CULPABLE);
	
				if (c1_result && c2_result) {
					c1_progress = DatabaseInterface.CONTRACT_CULPABLE;
					c2_progress = DatabaseInterface.CONTRACT_CULPABLE;
					return new ResponsePacket(-1, "The action performed was not allowed by your contract and made you culpable.");
					
				} else if (c1_result) {
					c1_progress = DatabaseInterface.CONTRACT_CULPABLE;
					c2_progress = DatabaseInterface.CONTRACT_INNOCENT;
					return new ResponsePacket(-1, "The action performed was not allowed by your contract and made you culpable.");
					
				} else if (c2_result) {
					c2_progress = DatabaseInterface.CONTRACT_CULPABLE;
					c1_progress = DatabaseInterface.CONTRACT_INNOCENT;

				} else {
	
					// 6b) Checks who is on duty
					c1_result = monitorContractProgress(db, contractHash,
							Tools.CTU_PARAM_DUTY);
					c2_result = monitorContractProgress(db, c2.getContractHash(),
							Tools.CTU_PARAM_DUTY);
	
					if (c1_result) {
						c1_progress = DatabaseInterface.CONTRACT_ON_DUTY;
						c2_progress = DatabaseInterface.CONTRACT_OFF_DUTY;
					} else if (c2_result) {
						c2_progress = DatabaseInterface.CONTRACT_ON_DUTY;
						c1_progress = DatabaseInterface.CONTRACT_OFF_DUTY;
					} else if (!c1_result && !c2_result) {
						c1_progress = DatabaseInterface.CONTRACT_OFF_DUTY;
						c2_progress = DatabaseInterface.CONTRACT_OFF_DUTY;
					} else {
	
						logger.error("Two participants found on duty!");
					}
				}*/
			}

			// Get action type and add trace (with message value)
			Integer actionType, actionID;

			actionType = db.selectActionType(contextID, action);
			actionID = db.selectActionId(contextID, action);

			switch (actionType) {

			case DatabaseInterface.ACTION_TYPE_INT:
				try {
					db.insertTrace(actionID, action, c1.getRole(), sessionID, Integer.parseInt(value));
				} catch (NumberFormatException nfe) {

					logger.warn(
							"Error in insertTrace for ACTION_TYPE=int. Exception returned: "
									+ nfe.getMessage());
					throw new DBException(
							"Error in insertTrace for ACTION_TYPE=int");
				}
				break;

			case DatabaseInterface.ACTION_TYPE_STRING:
				db.insertTrace(actionID, action, c1.getRole(), sessionID, value);
				break;

			case -1:
				db.insertTrace(actionID, action, c1.getRole(), sessionID, value);
				break;

			default:
				throw new DBException("Invalid action type found: " + actionType);
			}

			logger.info(
					"Added new trace entry for SESSION_ID=" + sessionID
							+ " and ROLE=" + c1.getRole());
		} catch (SQLException sqle) {

			logger.warn(
					"Cannot retrieve action type for CONTEXT_ID=" + contextID
							+ " and ACTION=" + action
							+ ". SQL says: " + sqle.getMessage());
			
			logger.trace("Leaving EXECUTE_ACTION (with errors)");

			return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
		
		} catch (DBException e) {
			
			logger.trace("Leaving EXECUTE_ACTION (with errors)");

			return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
			
		} catch (InternalException iie) {
			
			logger.trace("Leaving EXECUTE_ACTION (with errors)");

			return new ResponsePacket(iie.getType(), iie.getMessage());
		}
		finally {
		    Tools.rm(beforeFileName);
		    Tools.rm(afterFileName);
		}

		//db.updateContract(contractHash, sessionID, c1.getRole(), c1_progress);
		//db.updateContract(c2.getContractHash(), sessionID, c2.getRole(), c2_progress);
		
		logger.trace("Leaving EXECUTE_ACTION");

		return new ResponsePacket(1, Messages.SESSION_ACTION_DONE);
	}

    private Float calculateDelay(DatabaseInterface db, Integer sessionID) throws SQLException {

        String query;
        Long timestamp;
        Float elapsedTime;

        query = "SELECT last_timestamp FROM session WHERE session_id=" + sessionID + ";"; // counterpart's
        // role

        try (
                Connection connection = db.getDatasource().getConnection();
                Statement stmt = connection.createStatement();
                ) {
            ResultSet rs = stmt.executeQuery(query);
            rs.next();
            timestamp = rs.getLong(1);

            // /60 TODO: now using seconds... to be restored
            elapsedTime = (new Long(System.currentTimeMillis() - timestamp).floatValue()) / 1000;

            logger.trace("DELAY: " + elapsedTime + " secs (last timestamp was "
                            + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").format(new Date(new Long(rs.getLong(1))))
                            + ", current time is "
                            + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").format(new Date(System.currentTimeMillis()))
                            + ").");

            rs.close();
            
            return elapsedTime; // CHECK IF IT IS CORRECT
        }
    }

    
	
	private boolean handleSessionEnding(Contract c1, DatabaseInterface db, Boolean autoCulpable) throws DBException, SQLException, InternalException {
		Contract c2;
		Boolean c1_duty, c2_duty, c1_culpable, c2_culpable;
		String compliantHash, contractHash;

    	// 1) load compliant contract data
    	c2 = new Contract().loadFromHash(c1.getCompliantHash());
    	compliantHash = c2.getContractHash();
    	contractHash = c1.getContractHash();
    	
    	if (c1.getState() == DatabaseInterface.CONTRACT_CULPABLE || c1.getState() == DatabaseInterface.CONTRACT_INNOCENT || c1.getState() == DatabaseInterface.CONTRACT_COMPLETED)
    		return true; // Session is ended
		
		// 2) Calculating culpable and onDuty
		c1_duty = monitorContractProgress(db, contractHash, Tools.CTU_PARAM_DUTY);
		c2_duty = monitorContractProgress(db, compliantHash, Tools.CTU_PARAM_DUTY);
		c1_culpable = monitorContractProgress(db, contractHash, Tools.CTU_PARAM_CULPABLE);
		c2_culpable = monitorContractProgress(db, compliantHash, Tools.CTU_PARAM_CULPABLE);

		
		// Updating db states
		if (c1.getState() == DatabaseInterface.CONTRACT_OFF_DUTY && c1_duty == true)
			db.setContractState(c1.getContractID(), DatabaseInterface.CONTRACT_ON_DUTY);
		
		
		if (c2.getState() == DatabaseInterface.CONTRACT_OFF_DUTY && c2_duty == true)
			db.setContractState(c2.getContractID(), DatabaseInterface.CONTRACT_ON_DUTY);

		
		if (c1.getState() == DatabaseInterface.CONTRACT_ON_DUTY && c1_duty == false)
			db.setContractState(c1.getContractID(), DatabaseInterface.CONTRACT_OFF_DUTY);

		
		if (c2.getState() == DatabaseInterface.CONTRACT_ON_DUTY && c2_duty == false)
			db.setContractState(c2.getContractID(), DatabaseInterface.CONTRACT_OFF_DUTY);
		
		if (autoCulpable)
			c1_culpable = true;

		// 3) update reputations, contracts state and sessions state
		if(c1_culpable){
			
			User.build(c1.getOwnerID()).penalizeAndStore();
			User.build(c2.getOwnerID()).rewardAndStore();

			logger.trace("User with ID=" + c1.getOwnerID() + " has been penalized.");
			logger.trace("User with ID=" + c2.getOwnerID() + " has been rewarded.");
				
			db.setContractState(c1.getContractID(), DatabaseInterface.CONTRACT_CULPABLE);
			db.setContractState(c2.getContractID(), DatabaseInterface.CONTRACT_INNOCENT);
			
			db.setSessionState(c1.getSessionID(), DatabaseInterface.SESSION_COMPLETED_UNCORRECTLY);
			
			return true;
		
		} else if(c2_culpable) {
			
			logger.trace("User with ID=" + c2.getOwnerID() + " has been penalized.");
			logger.trace("User with ID=" + c1.getOwnerID() + " has been rewarded.");
			
			User.build(c1.getOwnerID()).rewardAndStore();
			User.build(c2.getOwnerID()).penalizeAndStore();
				
			db.setContractState(c1.getContractID(), DatabaseInterface.CONTRACT_INNOCENT);
			db.setContractState(c2.getContractID(), DatabaseInterface.CONTRACT_CULPABLE);
			
			db.setSessionState(c1.getSessionID(), DatabaseInterface.SESSION_COMPLETED_UNCORRECTLY);
			
			return true;
		
		} else if(!c1_duty && !c2_duty) {
			
			logger.trace("User with ID=" + c1.getOwnerID() + " has been rewarded.");
			logger.trace("User with ID=" + c2.getOwnerID() + " has been rewarded.");
			
			User tmp = User.build(c1.getOwnerID());
			tmp.rewardAndStore();
			User.build(c2.getOwnerID()).rewardAndStore();
				
			db.setContractState(c1.getContractID(), DatabaseInterface.CONTRACT_COMPLETED);
			db.setContractState(c2.getContractID(), DatabaseInterface.CONTRACT_COMPLETED);
			
			db.setSessionState(c1.getSessionID(), DatabaseInterface.SESSION_COMPLETED_CORRECTLY);
			
			return true;
		}
		
		return false; // session is not ended
	}
	
	/** 
     * Return the session hash of the given contract.
     * @param username Client username
     * @param pass Client password
     * @param contractHash Xml contract sent by client
     * @return Xml response that communicates if the contract is fused 
     */
    @POST
    @Path(value = "/getSessionId")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket getSessionId(QueryPacket postData) {
        
        String username = postData.getUsername();
        String pass = postData.getPassword();
        String contractHash = postData.getContractHash();
        
        try {
            // 2) Checking for valid auth data
            if (!DatabaseInterface.getInstance().authenticate(username, pass)) {
                logger.warn("Authentication error. Cannot accept USERNAME=" + username + " and hashed PASSWORD=" + Tools.hash256(pass) + "");
                
                return new ResponsePacket(-1, Messages.AUTH_FAILED);
            }

            Contract contract = new Contract().loadFromHash(contractHash);
            ResponsePacket rp = new ResponsePacket(0, contract.getSessionHash());
            
            return rp;
        }catch (SQLException e) {
            
            logger.error("Thrown SQL exception while opening database: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }
    }
}
