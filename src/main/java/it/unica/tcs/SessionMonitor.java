package it.unica.tcs;

import it.unica.tcs.InternalException.ErrorTypes;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path(value = "/monitoring")
public class SessionMonitor {
	
	static final boolean HARD_DEBUGGING = true;
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
    	
    	DatabaseInterface db = MainApplication.getDBConnection();
    	
    	try {
			db.deleteContracts();
			
		} catch (SQLException e) {
			
			Log.message().severe("Database cleaning failed");
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
    	
    	if (HARD_DEBUGGING)
    		Log.message().severe("Entering GET_POSSIBLE_ACTIONS");
    	
    	String username = postData.getUsername();
    	String pass = postData.getPassword();
    	String contractHash = postData.getContractHash();

        DatabaseInterface db = MainApplication.getDBConnection();
        Integer role;
        Contract c;
        String fileName, newFileName;

        // 1) Verifies authentication and permissions
        try {
            if (!Tools.authenticate(db, username, pass)) {
                Log.message().warning(
                        "Authentication error. Cannot accept USERNAME=" + Log.format(username)
                                + " and hashed PASSWORD=" + Log.format(Tools.hash256(pass)) + "");
                
            	if (HARD_DEBUGGING)
            		Log.message().severe("Leaving GET_POSSIBLE_ACTIONS");

                return new ResponsePacket(-1, Messages.AUTH_FAILED);
            }

            if (!Tools.permissionContract(db, username, contractHash)) {

                Log.message().warning(
                        "Access denied: user with USERNAME=" + Log.format(username)
                                + " tried to access contract with CONTRACT_HASH=" + Log.format(contractHash));
                
            	if (HARD_DEBUGGING)
            		Log.message().severe("Leaving GET_POSSIBLE_ACTIONS");
                
                return new ResponsePacket(-1, Messages.PERMISSION_DENIED);
            }
        }
        catch (SQLException e) {

            Log.message().warning("Thrown SQL exception while opening database: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }

        try {
            c = new Contract().loadFromHash(contractHash);
            role = c.getRole();

            // Gets a new filename
            fileName = Tools.getFile(contractHash + role, Tools.PATH_CTU_NETS, Tools.EXTENSION_NETS, false);

            // Loads the state of the session
            Tools.loadNetworkFromDB(db, contractHash, fileName);
                
            // Gets a second new filename
            newFileName = Tools.getFile(contractHash + role + 5, Tools.PATH_CTU_NETS, Tools.EXTENSION_NETS, false);
            
            // Updates the state of the session with the delay
            Tools.callApplication(Tools.getCtuPath()+ "-delay " + calculateDelay(db,c.getSessionID()) + " " + fileName + " " + newFileName, null);
            
            // Saves the updated network
            db.saveNetwork(c.getSessionID(), newFileName);
         
            
            String path = Tools.getCtuPath()+ "-pa" + " " + role + " " + newFileName;
            AppResponse ocamlResult = Tools.callApplication(path, null);
            
            //Tools.callApplication(path, null, true);
            
        	if (HARD_DEBUGGING)
        		Log.message().severe("Leaving GET_POSSIBLE_ACTIONS");
            
            if (ocamlResult.isEmpty())
                return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
            else
                return new ResponsePacket(1, ocamlResult.getOutput()); // TODO: output must be formatted and it is necessary to handle the time

        }
        catch (SQLException e) {

            Log.message().warning(
                    "Error in loadFromHash while checking if the owner of a contract with HASH="
                            + Log.format(contractHash) + " is on duty.");
            
        	if (HARD_DEBUGGING)
        		Log.message().severe("Leaving GET_POSSIBLE_ACTIONS");

            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }
    }
    
    @POST
    @Path(value = "/isOnDuty")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket isOnDuty(QueryPacket postData) {
    	
    	if (HARD_DEBUGGING)
    		Log.message().severe("Entering IS_ON_DUTY");
    	
    	String username = postData.getUsername();
    	String pass = postData.getPassword();
    	String contractHash = postData.getContractHash();

        DatabaseInterface db = MainApplication.getDBConnection();
        Integer contractState;
        Contract c;

        // 1) Verifies authentication and permissions
        try {
            if (!Tools.authenticate(db, username, pass)) {
                Log.message().warning(
                        "Authentication error. Cannot accept USERNAME=" + Log.format(username)
                                + " and hashed PASSWORD=" + Log.format(Tools.hash256(pass)) + "");
                
            	if (HARD_DEBUGGING)
            		Log.message().severe("Leaving IS_ON_DUTY");

                return new ResponsePacket(-1, Messages.AUTH_FAILED);
            }

            if (!Tools.permissionContract(db, username, contractHash)) {

                Log.message().warning(
                        "Access denied: user with USERNAME=" + Log.format(username)
                                + " tried to access contract with CONTRACT_HASH=" + Log.format(contractHash));
                
            	if (HARD_DEBUGGING)
            		Log.message().severe("Leaving IS_ON_DUTY");
                
                return new ResponsePacket(-1, Messages.PERMISSION_DENIED);
            }
        }
        catch (SQLException e) {

            Log.message().warning("Thrown SQL exception while opening database: " + e.getMessage());
            
        	if (HARD_DEBUGGING)
        		Log.message().severe("Leaving IS_ON_DUTY");
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }

        // 2) Does query
        try {
        	
            c = new Contract().loadFromHash(contractHash);
            handleSessionEnding(c, db, false); // update the state of the session
            
            c = new Contract().loadFromHash(contractHash); // reload the contract state
            contractState = c.getState();
            
			if (contractState == DatabaseInterface.CONTRACT_ON_DUTY) {
          
                Log.message().fine(
                        "Checked if the owner of the contract with HASH=" + Log.format(contractHash)
                                + " is on duty: YES!");
                
            	if (HARD_DEBUGGING)
            		Log.message().severe("Leaving IS_ON_DUTY");
                
                return new ResponsePacket(1, Messages.PROPERTY_YES);
            }
            else {
            	
                Log.message().fine(
                        "Checked if the owner of the contract with HASH=" + Log.format(contractHash)
                                + " is on duty: NO!");
                
            	if (HARD_DEBUGGING)
            		Log.message().severe("Leaving IS_ON_DUTY");
                
                return new ResponsePacket(0, Messages.PROPERTY_NO);
            }

        }
        catch (SQLException | InternalException | DBException e) {

            Log.message().warning(
                    "Error in loadFromHash while checking if the owner of a contract with HASH="
                            + Log.format(contractHash) + " is on duty.");
            
        	if (HARD_DEBUGGING)
        		Log.message().severe("Leaving IS_ON_DUTY");

            return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
        }
    }

    @POST
    @Path(value = "/isCulpable")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket isCulpable(QueryPacket postData) {
    	
    	if (HARD_DEBUGGING)
    		Log.message().severe("Entering IS_CULPABLE");
    	
    	String username = postData.getUsername();
    	String pass = postData.getPassword();
    	String contractHash = postData.getContractHash();

        DatabaseInterface db = MainApplication.getDBConnection();
        Integer contractState;
        Contract c;

        // 1) Verifies authentication and permissions
        try {
            if (!Tools.authenticate(db, username, pass)) {
                Log.message().warning(
                        "Authentication error. Cannot accept USERNAME=" + Log.format(username)
                                + " and hashed PASSWORD=" + Log.format(Tools.hash256(pass)) + "");
                
            	if (HARD_DEBUGGING)
            		Log.message().severe("Leaving IS_CULPABLE");

                return new ResponsePacket(-1, Messages.AUTH_FAILED);
            }

            if (!Tools.permissionContract(db, username, contractHash)) {
                Log.message().warning(
                        "Access denied: user with USERNAME=" + Log.format(username)
                                + " tried to access contract with CONTRACT_HASH=" + Log.format(contractHash));
                
                if (HARD_DEBUGGING)
            		Log.message().severe("Leaving IS_CULPABLE");
                
                return new ResponsePacket(-1, Messages.PERMISSION_DENIED);
            }
        }
        catch (SQLException e) {
            Log.message().warning("Thrown SQL exception while opening database: " + e.getMessage());
            
            if (HARD_DEBUGGING)
        		Log.message().severe("Leaving IS_CULPABLE");
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }

        // 2) Does query
        try {

            c = new Contract().loadFromHash(contractHash);
            handleSessionEnding(c, db, false); // update the state of the session
            
            c = new Contract().loadFromHash(contractHash); // reload the contract state
            contractState = c.getState();

            if (contractState == DatabaseInterface.CONTRACT_CULPABLE) {
                Log.message().fine(
                        "Checked if the owner of the contract with HASH=" + Log.format(contractHash)
                                + " is culpable: YES!");
                
                if (HARD_DEBUGGING)
            		Log.message().severe("Leaving IS_CULPABLE");
                
                return new ResponsePacket(1, Messages.PROPERTY_YES);
            }
            else {
                
                Log.message().fine(
                        "Checked if the owner of the contract with HASH=" + Log.format(contractHash)
                                + " is culpable: NO!");
                
                if (HARD_DEBUGGING)
            		Log.message().severe("Leaving IS_CULPABLE");
                
                return new ResponsePacket(0, Messages.PROPERTY_NO);
            }
        }
        catch (InternalException e) {
            
            Log.message().warning("InternalException in isCulpable: " + e.getMessage());
            
            if (HARD_DEBUGGING)
        		Log.message().severe("Leaving IS_CULPABLE");
            
            return new ResponsePacket(e.getType(), e.getMessage());
        }
        catch (DBException e) {
            
            Log.message().warning("DBException in isCulpable: " + e.getMessage());
            
            if (HARD_DEBUGGING)
        		Log.message().severe("Leaving IS_CULPABLE");
            
            return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
        }
        catch (SQLException e) {

            Log.message().warning(
                    "Error in loadFromHash while checking if the owner of a contract with HASH="
                            + Log.format(contractHash) + " is culpable.");
            
            if (HARD_DEBUGGING)
        		Log.message().severe("Leaving IS_CULPABLE");

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

        DatabaseInterface db = MainApplication.getDBConnection();
        Contract c;
        String query;
        ResultSet rs;
        Long timestamp;

        // 1) Verifies authentication and permissions
        try {
            
            if (!Tools.authenticate(db, username, pass)) {
                Log.message().warning(
                        "Authentication error. Cannot accept USERNAME=" + Log.format(username)
                                + " and hashed PASSWORD=" + Log.format(Tools.hash256(pass)) + "");

                return new ResponsePacket(-1, Messages.AUTH_FAILED);
            }

            if (!Tools.permissionContract(db, username, contractHash)) {
                Log.message().warning(
                        "Access denied: user with USERNAME=" + Log.format(username)
                                + " tried to access contract with CONTRACT_HASH=" + Log.format(contractHash));
                
                return new ResponsePacket(-1, Messages.PERMISSION_DENIED);
            }
        }
        catch (SQLException e) {
            Log.message().warning("Thrown SQL exception while opening database: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }
        
        try {
            
            c = new Contract().loadFromHash(contractHash);
            
            if (c.getSessionID() == -1)
                return new ResponsePacket(0, "Contract not fused yet");
            
            query = "SELECT start_timestamp FROM session WHERE session_id = " + c.getSessionID();
            rs = db.select(query);
            rs.next();
            timestamp = rs.getLong(1);
            
            return new ResponsePacket(1, timestamp + "");
        }
        catch (SQLException e) {
            
            Log.message().warning("Thrown SQL exception when selecting start_timestamp. SQL says: " + e.getMessage());
            
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
    	
    	if (HARD_DEBUGGING)
    		Log.message().severe("Entering SEND");
    	
    	String username = postData.getUsername();
    	String pass = postData.getPassword();
    	String contractHash = postData.getContractHash();
    	String action = postData.getActionName();
    	String value = postData.getActionValue();

        DatabaseInterface db = MainApplication.getDBConnection();
        Integer state, sessionID;
        ResultSet rs;
        String query;
        Long timestamp;
        Contract c;
        ResponsePacket response;
        Boolean autoCulpable = false;

        // 1) Verifies authentication and permissions
        try {

            if (!Tools.authenticate(db, username, pass)) {
                Log.message().warning(
                        "Authentication error. Cannot accept USERNAME=" + Log.format(username)
                                + " and hashed PASSWORD=" + Log.format(Tools.hash256(pass)) + "");
                
            	if (HARD_DEBUGGING)
            		Log.message().severe("Leaving SEND");
                
                return  new ResponsePacket(-1, Messages.AUTH_FAILED);

            }
            if (!Tools.permissionContract(db, username, contractHash)) {

                Log.message().warning(
                        "Access denied: user with USERNAME=" + Log.format(username)
                                + " tried to access contract with CONTRACT_HASH=" + Log.format(contractHash));
                
            	if (HARD_DEBUGGING)
            		Log.message().severe("Leaving SEND");
                
                return new ResponsePacket(-1, Messages.PERMISSION_DENIED);
            }
        }
        catch (SQLException e) {
        	
            Log.message().warning("Failed opening database. SQL says: " + e.getMessage());
            
        	if (HARD_DEBUGGING)
        		Log.message().severe("Leaving SEND");
            
            return new ResponsePacket(-1, Messages.DB_CONN_FAILED);
        }
        
        // 2) Retrieves contract state and decides if can do the action
        try {

            // 2a) Checks timestamp
            c = new Contract().loadFromHash(contractHash);
            sessionID = c.getSessionID();
            query = "SELECT start_timestamp FROM session WHERE session_id = " + sessionID;
            rs = db.select(query);
            rs.next();
            timestamp = rs.getLong(1);
            
            if (handleSessionEnding(c, db, false)) { // First of all, verifies if the session is already ended (to avoid that the results will be overwritten)
            	
            	if (HARD_DEBUGGING) Log.message().severe("Leaving SEND");
            	
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
            
        	if (HARD_DEBUGGING)
        		Log.message().severe("Leaving SEND");
            
            return response;
        }
        catch (DBException | SQLException | InternalException e) {
        	
            Log.message().severe("A database exception was thrown when executing DO: " + e.getMessage());
            
        	if (HARD_DEBUGGING)
        		Log.message().severe("Leaving SEND");
            
            return new ResponsePacket(-1, Messages.DB_CONN_FAILED);
        }
      
    }
    
    @POST
    @Path(value = "/receive")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket receive(QueryPacket postData) {
    	
    	if (HARD_DEBUGGING)
    		Log.message().severe("Entering RECEIVE");
    	
    	String username = postData.getUsername();
    	String pass = postData.getPassword();
    	String contractHash = postData.getContractHash();

        DatabaseInterface db = MainApplication.getDBConnection();
        Integer sessionID;
        ResultSet rs;
        String query;
        Contract c;

        // 1) Verifies authentication and permissions
        try {

            if (!Tools.authenticate(db, username, pass)) {
                Log.message().warning(
                        "Authentication error. Cannot accept USERNAME=" + Log.format(username)
                                + " and hashed PASSWORD=" + Log.format(Tools.hash256(pass)) + "");
                
            	if (HARD_DEBUGGING)
            		Log.message().severe("Leaving RECEIVE");
                
                return new ResponsePacket(-1, Messages.AUTH_FAILED);

            }
            if (!Tools.permissionContract(db, username, contractHash)) {

                Log.message().warning(
                        "Access denied: user with USERNAME=" + Log.format(username)
                                + " tried to access contract with CONTRACT_HASH=" + Log.format(contractHash));
                
            	if (HARD_DEBUGGING)
            		Log.message().severe("Leaving RECEIVE");
                
                return new ResponsePacket(-1, Messages.PERMISSION_DENIED);
            }
        }
        catch (SQLException e) {
            Log.message().warning("Failed opening database. SQL says: " + e.getMessage());
            
        	if (HARD_DEBUGGING)
        		Log.message().severe("Leaving RECEIVE");
            
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
                    + "FROM `trace` AS t LEFT JOIN action AS a ON t.action_id = a.action_id WHERE session_id = " + sessionID + " "
                    + "AND `read`=0 AND role=" + (1 - c.getRole()) + " ORDER BY timestamp;";  // counterpart's role
            rs = db.select(query);
            rs.next();
            count = rs.getInt(7); // returns COUNT(*)
            
            if (count < 1) {
            	
            	if (HARD_DEBUGGING)
            		Log.message().severe("Leaving RECEIVE");
                
                return new ResponsePacket(0, "Nothing to receive (the buffer is empty)");
            }
            
            actionName = rs.getString(2);
            dataType = rs.getInt(3);
            
            if (rs.getInt(1) == -1)
            	dataType = 1;
            
            ResponsePacket response = new ResponsePacket(1, "Action received (check the actionName and actionValue fields)");
            response.setActionName(actionName);
            
            /*if (actionID == -1) {
                
                db.setTraceRead(rs.getInt(8)); // traceID (set it as read)
                
                return response;
            }*/
            
            if (dataType == 2) {
                
                String value = rs.getString(5);
                db.setTraceRead(rs.getInt(8)); // traceID (set it as read)
                
                response.setActionValue(value);
                
            	if (HARD_DEBUGGING)
            		Log.message().severe("Leaving RECEIVE");
                return response;
            }
            else if (dataType == 1) {
                
                String value = rs.getString(5);
                db.setTraceRead(rs.getInt(8)); // traceID (set it as read)
                
                response.setActionValue(value);
                
            	if (HARD_DEBUGGING)
            		Log.message().severe("Leaving RECEIVE");
                return response;
            }
            else {
                
                Integer value = rs.getInt(4);
                db.setTraceRead(rs.getInt(8)); // traceID (set it as read)
                
                response.setActionValue(value + "");
                
            	if (HARD_DEBUGGING)
            		Log.message().severe("Leaving RECEIVE");
                return response;
            }
        }
        catch (SQLException e) {
            
            Log.message().warning("Can't select data in receive(). SQL says: " + e.getMessage());
            
            
        	if (HARD_DEBUGGING)
        		Log.message().severe("Leaving RECEIVE");
            return new ResponsePacket(-1, Messages.DB_CONN_FAILED);
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
    	
        DatabaseInterface db = MainApplication.getDBConnection();
        Contract c1, c2;
    	
    	
        // 1) Verifies authentication and permissions
        try {

            if (!Tools.authenticate(db, username, pass)) {
                Log.message().warning(
                        "Authentication error. Cannot accept USERNAME=" + Log.format(username)
                                + " and hashed PASSWORD=" + Log.format(Tools.hash256(pass)) + "");
                
                return  new ResponsePacket(-1, Messages.AUTH_FAILED);

            }
            if (!Tools.permissionContract(db, username, contractHash)) {

                Log.message().warning(
                        "Access denied: user with USERNAME=" + Log.format(username)
                                + " tried to access contract with CONTRACT_HASH=" + Log.format(contractHash));
                
                return new ResponsePacket(-1, Messages.PERMISSION_DENIED);
            }
        }
        catch (SQLException e) {
        	
            Log.message().warning("Failed opening database. SQL says: " + e.getMessage());
            
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

        String path, fileName = "", newFileName = "", sessionHash = "";
        AppResponse ocamlResult;
        Integer role;
        Contract c;

        // 1) Verifies contract
        try {
            c = new Contract().loadFromHash(contractHash);

        }
        catch (SQLException e) {

            Log.message().warning("Error in loadFromHash: " + e.getMessage());
            throw new InternalException(ErrorTypes.TYPE_PERMISSION_DENIED);
        }
        
        if (queryType.equals(Tools.CTU_PARAM_CULPABLE)) {
	        if (c.getState() == DatabaseInterface.CONTRACT_CULPABLE) {
	        	
	        	Log.message().fine("Checked if the contract with HASH=" + Log.format(contractHash) + " is culpable when the session is already ended: yes!");
	        	return true;
	        }
	        
	        if (c.getState() == DatabaseInterface.CONTRACT_INNOCENT) {
	        	
	        	Log.message().fine("Checked if the contract with HASH=" + Log.format(contractHash) + " is culpable when the session is already ended: no, it is innocent!");
	        	return false;
	        }
	        
	        if (c.getState() == DatabaseInterface.CONTRACT_COMPLETED) {
	        	
	        	Log.message().fine("Checked if the contract with HASH=" + Log.format(contractHash) + " is culpable when the session is already ended: no, the session was ended correctly!");
	        	return false;
	        }
        }

        // 2) Loads data
        // 2a) Loads role in contract
        role = c.getRole();

        // 2c) MySQL needs a file to write network
        fileName = Tools.getFile(sessionHash + role, Tools.PATH_CTU_NETS, Tools.EXTENSION_NETS, false);

        // 2d) Calls MySQL
        try {
            Tools.loadNetworkFromDB(db, contractHash, fileName);
            
            // Updates network with time elapsed ... TODO: handle granularity different from 0
            newFileName = Tools.getFile(sessionHash + role + 5, Tools.PATH_CTU_NETS, Tools.EXTENSION_NETS, false);
            Tools.callApplication(Tools.getCtuPath()+ "-delay " + calculateDelay(db,c.getSessionID()) + " " + fileName + " " + newFileName, null);
            //Log.message().info("CTU Output: " + output);

        }
        catch (SQLException e) {

            Log.message().warning("SQLException thrown in loadNetworkFromDB or calculateDelay: " + e.getMessage());
            throw new DBException(Messages.DB_SELECT_FAILED);
        }
      
        // 3) Creates Ocaml process
        path = Tools.getCtuPath()+ queryType + " " + role + " " + newFileName;
        ocamlResult = Tools.callApplication(path, null);
        //Tools.callApplication(path, null, true);
        
        if (ocamlResult.isEmpty())
        	Log.message().warning("CTU is not returning the state for a contract.");

        try {
            db.saveNetwork(c.getSessionID(), newFileName);
        }
        catch (SQLException e) {
          
            throw new DBException("Cannot save the updated network. SQL says: " + e.getMessage());
        }
        
        // Remove the temp file
        Tools.callApplication("rm " + fileName, null);
        Tools.callApplication("rm " + newFileName, null);
        String logmsg = "Checked if contract with HASH=" + Log.format(contractHash);
        logmsg += queryType.equals(Tools.CTU_PARAM_CULPABLE) ? " is culpable: " : " is on duty: ";

        // 4) Analyzes output application
        if (ocamlResult.getOutput().contains(Messages.TYPE_YES)) {
            Log.message().fine(logmsg + "yes!");
            return true;
        }
        else {
            Log.message().fine(logmsg + "no!");
            return false;
        }
    }

	private ResponsePacket executeAction(DatabaseInterface db, String contractHash, String action, String value, String username) throws SQLException, InternalException {

		
		if (HARD_DEBUGGING)
			Log.message().severe("Entering EXECUTE_ACTION");
		
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
			beforeFileName = Tools.getFile(contractHash + action,
					Tools.PATH_CTU_NETS, Tools.EXTENSION_NETS, false);
			Tools.loadNetworkFromDB(db, contractHash, beforeFileName);
			afterFileName = Tools.getFile(contractHash + action,
					Tools.PATH_CTU_NETS, Tools.EXTENSION_NETS, false);
	
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
					
					if (HARD_DEBUGGING)
						Log.message().severe("Leaving EXECUTE_ACTION (with errors)");
					
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
	
						Log.message().severe("Two participants found on duty!");
					}
				}*/
			}

			// Get action type and add trace (with message value)
			Integer actionType, actionID;

			actionType = retrieveActionType(db, contextID, action);
			actionID = getActionID(db, contextID, action);

			switch (actionType) {

			case DatabaseInterface.ACTION_TYPE_INT:
				try {
					db.insertTrace(actionID, action, c1.getRole(), sessionID,
							Integer.parseInt(value));
				} catch (NumberFormatException nfe) {

					Log.message().warning(
							"Error in insertTrace for ACTION_TYPE=int. Exception returned: "
									+ nfe.getMessage());
					throw new DBException(
							"Error in insertTrace for ACTION_TYPE=int");
				}
				break;

			case DatabaseInterface.ACTION_TYPE_STRING:
				db.insertTrace(actionID, action, c1.getRole(), sessionID,
						value, false);
				break;

			case DatabaseInterface.ACTION_TYPE_FILE:
				db.insertTrace(actionID, action, c1.getRole(), sessionID,
						value, true);
				break;

			case -1:
				db.insertTrace(actionID, action, c1.getRole(), sessionID, value, false);
				break;

			default:
				throw new DBException("Invalid action type found: "
						+ actionType);
			}

			Log.message().info(
					"Added new trace entry for SESSION_ID=" + sessionID
							+ " and ROLE=" + c1.getRole());
		} catch (SQLException sqle) {

			Log.message().warning(
					"Cannot retrieve action type for CONTEXT_ID=" + contextID
							+ " and ACTION=" + Log.format(action)
							+ ". SQL says: " + sqle.getMessage());
			
			if (HARD_DEBUGGING)
				Log.message().severe("Leaving EXECUTE_ACTION (with errors)");

			return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
		
		} catch (DBException e) {
			
			if (HARD_DEBUGGING)
				Log.message().severe("Leaving EXECUTE_ACTION (with errors)");

			return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
			
		} catch (InternalException iie) {
			
			if (HARD_DEBUGGING)
				Log.message().severe("Leaving EXECUTE_ACTION (with errors)");

			return new ResponsePacket(iie.getType(), iie.getMessage());
		}

		//db.updateContract(contractHash, sessionID, c1.getRole(), c1_progress);
		//db.updateContract(c2.getContractHash(), sessionID, c2.getRole(), c2_progress);
		
		if (HARD_DEBUGGING)
			Log.message().severe("Leaving EXECUTE_ACTION");

		return new ResponsePacket(1, Messages.SESSION_ACTION_DONE);
	}

	private Float calculateDelay(DatabaseInterface db, Integer sessionID)
			throws SQLException {

		String query;
		ResultSet rs;
		Long timestamp;
		Float elapsedTime;

		query = "SELECT last_timestamp FROM session WHERE session_id="
				+ sessionID + ";"; // counterpart's role
		rs = db.select(query);
		rs.next();
		timestamp = rs.getLong(1);

		elapsedTime = (new Long(System.currentTimeMillis() - timestamp).floatValue()) / 1000; // /60 TODO: now using seconds... to be restored
		
		Log.message().fine("DELAY: " + elapsedTime + " secs (last timestamp was " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").format(new Date(new Long(rs.getLong(1)))) + 
				", current time is " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").format(new Date(System.currentTimeMillis())) + ").");

		return elapsedTime; // CHECK IF IT IS CORRECT
	}

	private Integer retrieveActionType(DatabaseInterface db, Integer contextID,
			String action) throws SQLException {

		String query;
		ResultSet rs;

		if (contextID == 0) // No data_type allowed for empty context's actions
			return -1;

		query = "SELECT action.data_type FROM action LEFT JOIN context_action ON action.action_id = context_action.action_id WHERE context_id = "
				+ contextID + " AND name = '" + action + "';";
		rs = db.select(query);
		rs.next();

		return rs.getInt(1);
	}

	private Integer getActionID(DatabaseInterface db, Integer contextID,
			String action) throws SQLException {

		String query;
		ResultSet rs;

		if (contextID == 0)
			return -1;

		query = "SELECT action.action_id FROM action LEFT JOIN context_action ON action.action_id = context_action.action_id WHERE context_id = "
				+ contextID + " AND name = '" + action + "';";
		rs = db.select(query);
		rs.next();

		return rs.getInt(1);
	}
	
	private boolean handleSessionEnding(Contract c1, DatabaseInterface db, Boolean autoCulpable) throws DBException, SQLException, InternalException{
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
		
		if (autoCulpable)
			c1_culpable = true;

		// 3) update reputations, contracts state and sessions state
		if(c1_culpable){
			
			User.build(c1.getOwnerID()).penalizeAndStore();
			User.build(c2.getOwnerID()).rewardAndStore();

			Log.message().fine("User with ID=" + c1.getOwnerID() + " has been penalized.");
			Log.message().fine("User with ID=" + c2.getOwnerID() + " has been rewarded.");
				
			db.setContractState(c1.getContractID(), DatabaseInterface.CONTRACT_CULPABLE);
			db.setContractState(c2.getContractID(), DatabaseInterface.CONTRACT_INNOCENT);
			db.setSessionState(c1.getSessionID(), DatabaseInterface.SESSION_COMPLETED_UNCORRECTLY);
			
			return true;
		
		} else if(c2_culpable) {
			
			Log.message().fine("User with ID=" + c2.getOwnerID() + " has been penalized.");
			Log.message().fine("User with ID=" + c1.getOwnerID() + " has been rewarded.");
			
			User.build(c1.getOwnerID()).rewardAndStore();
			User.build(c2.getOwnerID()).penalizeAndStore();
				
			db.setContractState(c1.getContractID(), DatabaseInterface.CONTRACT_INNOCENT);
			db.setContractState(c2.getContractID(), DatabaseInterface.CONTRACT_CULPABLE);
			db.setSessionState(c1.getSessionID(), DatabaseInterface.SESSION_COMPLETED_UNCORRECTLY);
			
			return true;
		
		} else if(!c1_duty && !c2_duty) {
			
			Log.message().fine("User with ID=" + c1.getOwnerID() + " has been rewarded.");
			Log.message().fine("User with ID=" + c2.getOwnerID() + " has been rewarded.");
			
			User.build(c1.getOwnerID()).rewardAndStore();
			User.build(c2.getOwnerID()).rewardAndStore();
				
			db.setContractState(c1.getContractID(), DatabaseInterface.CONTRACT_COMPLETED);
			db.setContractState(c2.getContractID(), DatabaseInterface.CONTRACT_COMPLETED);
			db.setSessionState(c1.getSessionID(), DatabaseInterface.SESSION_COMPLETED_CORRECTLY);
			
			return true;
		}
		
		return false; // session is not ended
	}
}
