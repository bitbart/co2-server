package it.unica.tcs;

import it.unica.tcs.InternalException.ErrorTypes;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

public class SessionMonitor {
	
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
            c = new Contract(db).loadFromHash(contractHash);
            role = c.getRole();

            // Gets a new filename
            fileName = Tools.getFile(contractHash + role, Tools.PATH_CTU_NETS, Tools.EXTENSION_NETS, false);

            // Loads the state of the session
            Tools.loadNetworkFromDB(db, contractHash, fileName);
                
            // Gets a second new filename
            newFileName = Tools.getFile(contractHash + role + 5, Tools.PATH_CTU_NETS, Tools.EXTENSION_NETS, false);
            
            // Updates the state of the session with the delay
            Tools.callApplication(Tools.PATH_CTU + " -delay " + calculateDelay(db,c.getSessionID()) + " " + fileName + " " + newFileName, null, true);
            
            // Saves the updated network
            db.saveNetwork(c.getSessionID(), newFileName);
         
            
            String path = Tools.PATH_CTU + "-pa" + " " + role + " " + newFileName;
            String ocamlResult = Tools.callApplication(path, null, false);
            
            //Tools.callApplication(path, null, true);
            
            if (ocamlResult.equals(""))
                return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
            else
                return new ResponsePacket(1, ocamlResult); // TODO: output must be formatted and it is necessary to handle the time

        }
        catch (SQLException e) {

            Log.message().warning(
                    "Error in loadFromHash while checking if the owner of a contract with HASH="
                            + Log.format(contractHash) + " is on duty.");

            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }
    }
    
    @POST
    @Path(value = "/isOnDuty")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket isOnDuty(QueryPacket postData) {
    	
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

        // 2) Does query
        try {
            c = new Contract(db).loadFromHash(contractHash);
            contractState = c.getState();
            
            // TODO: does it need to query UPPAAL for checking the state? I think yes!

            if (contractState == DatabaseInterface.CONTRACT_ON_DUTY) {
                Log.message().info(
                        "Checked if the owner of the contract with HASH=" + Log.format(contractHash)
                                + " is on duty: YES!");
                
                return new ResponsePacket(1, Messages.PROPERTY_YES);
            }
            else {
                Log.message().info(
                        "Checked if the owner of the contract with HASH=" + Log.format(contractHash)
                                + " is on duty: NO!");
                
                return new ResponsePacket(0, Messages.PROPERTY_NO);
            }

        }
        catch (SQLException e) {

            Log.message().warning(
                    "Error in loadFromHash while checking if the owner of a contract with HASH="
                            + Log.format(contractHash) + " is on duty.");

            return new ResponsePacket(-1, Messages.PERMISSION_DENIED);
        }
    }

    @POST
    @Path(value = "/isCulpable")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket isCulpable(QueryPacket postData) {
    	
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

        // 2) Does query
        try {

            c = new Contract(db).loadFromHash(contractHash);
            contractState = c.getState();

            if (contractState == DatabaseInterface.CONTRACT_CULPABLE) {
                Log.message().info(
                        "Checked if the owner of the contract with HASH=" + Log.format(contractHash)
                                + " is culpable: YES!");
                
                return new ResponsePacket(1, Messages.PROPERTY_YES);
            }
            else {
                
                if (monitorContractProgress(db, contractHash, Tools.CTU_PARAM_CULPABLE)) { // the user is became culpable (because of time elapsing)
                    
                    Log.message().info(
                            "Checked if the owner of the contract with HASH=" + Log.format(contractHash)
                                    + " is culpable: YES!");
                    
                    return new ResponsePacket(1, Messages.PROPERTY_YES);
                }
                
                Log.message().info(
                        "Checked if the owner of the contract with HASH=" + Log.format(contractHash)
                                + " is culpable: NO!");
                
                return new ResponsePacket(0, Messages.PROPERTY_NO);
            }
        }
        catch (InternalException e) {
            
            Log.message().warning("InternalException in isCulpable: " + e.getMessage());
            
            return new ResponsePacket(e.getType(), e.getMessage());
        }
        catch (DBException e) {
            
            Log.message().warning("DBException in isCulpable: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
        }
        catch (SQLException e) {

            Log.message().warning(
                    "Error in loadFromHash while checking if the owner of a contract with HASH="
                            + Log.format(contractHash) + " is culpable.");

            return new ResponsePacket(-1, Messages.PERMISSION_DENIED);
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
            
            c = new Contract(db).loadFromHash(contractHash);
            
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
    public ResponsePacket send(QueryPacket postData) {
    	
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

        // 2) Retrieves contract state and decides if can do the action
        try {

            // 2a) Checks timestamp
            c = new Contract(db).loadFromHash(contractHash);
            sessionID = c.getSessionID();
            query = "SELECT start_timestamp FROM session WHERE session_id = " + sessionID;
            rs = db.select(query);
            rs.next();
            timestamp = rs.getLong(1);

            // 2b) Checks state
            state = SessionHandler.getContractState(db, username, pass, contractHash);

            if ((state == DatabaseInterface.CONTRACT_LATENT) || (timestamp > System.currentTimeMillis())) {
                
                return new ResponsePacket(-1, Messages.SESSION_MOVE_BEFORE_START);
            }

            if (!Tools.CONF_MOVE_AFTER_CONTRACT_END) {
            	
                if ((state == DatabaseInterface.CONTRACT_ON_DUTY) || (state == DatabaseInterface.CONTRACT_OFF_DUTY)) {
                 
                    return executeAction(db, contractHash, action, value);
                }
                else {
                	return new ResponsePacket(-1, Messages.SESSION_MOVE_AFTER_END);
                }
            }
            else {
            	
                return executeAction(db, contractHash, action, value);
            }
        }
        catch (DBException | SQLException e) {
        	
            Log.message().warning("Database exception thrown when executing DO: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_CONN_FAILED);
        }
    }
    
    @POST
    @Path(value = "/receive")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket receive(QueryPacket postData) {
    	
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
            Log.message().warning("Failed opening database. SQL says: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_CONN_FAILED);
        }

        // 2) Retrieves contract state and decides if does the action
        try {
            
            Integer count, dataType, actionID;
            String actionName;

            // 2a) Checks timestamp
            c = new Contract(db).loadFromHash(contractHash);
            sessionID = c.getSessionID();
            
            query = "SELECT a.action_id,action_name,data_type,data_int_value,data_string_value,data_file_value,COUNT(*),trace_id "
                    + "FROM `trace` AS t LEFT JOIN action AS a ON t.action_id = a.action_id WHERE session_id = " + sessionID + " "
                    + "AND `read`=0 AND role=" + (1 - c.getRole()) + " ORDER BY timestamp;";  // counterparty's role
            rs = db.select(query);
            rs.next();
            count = rs.getInt(7); // returns COUNT(*)
            
            if (count < 1) {
                
                return new ResponsePacket(0, "Nothing to receive (the buffer is empty)");
            }
            
            actionName = rs.getString(2);
            actionID = rs.getInt(1);
            dataType = rs.getInt(3);
            
            ResponsePacket response = new ResponsePacket(1, "Action received (check the actionName and actionValue fields)");
            response.setActionName(actionName);
            
            if (actionID == -1) {
                
                db.setTraceRead(rs.getInt(8)); // traceID (set it as read)
                
                response.setActionName("Action received");
                return response;
            }
            
            if (dataType == 0) {
                
                Integer value = rs.getInt(4);
                db.setTraceRead(rs.getInt(8)); // traceID (set it as read)
                
                response.setActionValue(value + "");
                return response;
            }
            else if (dataType == 1) {
                
                String value = rs.getString(5);
                db.setTraceRead(rs.getInt(8)); // traceID (set it as read)
                
                response.setActionValue(value);
                return response;
            }
            else if (dataType == 2) {
                
                String value = rs.getString(5);
                db.setTraceRead(rs.getInt(8)); // traceID (set it as read)
                
                response.setActionValue(value);
                return response;
            }
        }
        catch (SQLException e) {
            
            Log.message().warning("Can't select data in receive(). SQL says: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_CONN_FAILED);
        }
        
        return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
    }
    
	public boolean monitorContractProgress(DatabaseInterface db, String contractHash, String queryType) throws DBException, InternalException {

        String path, ocamlResult, fileName = "", newFileName = "", sessionHash = "";
        Integer role;
        Contract c;

        // 1) Verifies contract
        try {
            c = new Contract(db).loadFromHash(contractHash);

        }
        catch (SQLException e) {

            Log.message().warning("Error in loadFromHash: " + e.getMessage());
            throw new InternalException(ErrorTypes.TYPE_PERMISSION_DENIED);
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
            Tools.callApplication(Tools.PATH_CTU + " -delay " + calculateDelay(db,c.getSessionID()) + " " + fileName + " " + newFileName, null, true);
            //Log.message().info("CTU Output: " + output);

        }
        catch (SQLException e) {

            Log.message().warning("SQLException thrown in loadNetworkFromDB or calculateDelay: " + e.getMessage());
            throw new DBException(Messages.DB_SELECT_FAILED);
        }
      
        // 3) Creates Ocaml process
        path = Tools.PATH_CTU + queryType + " " + role + " " + newFileName;
        ocamlResult = Tools.callApplication(path, null, false);
        Tools.callApplication(path, null, true);
        
        //Log.message().info("Filename name: " + newFileName + " || Ocaml response: " + ocamlError);

        try {
            db.saveNetwork(c.getSessionID(), newFileName);
        }
        catch (SQLException e) {
            
            throw new DBException("Cannot save the updated network. SQL says: " + e.getMessage());
        }
        
        // Remove the temp file
        Tools.callApplication("rm " + fileName, null, false);
        Tools.callApplication("rm " + newFileName, null, false);
        String logmsg = "Checked if contract with HASH=" + Log.format(contractHash);
        logmsg += queryType.equals(Tools.CTU_PARAM_CULPABLE) ? " is culpable: " : " is on duty: ";

        // 4) Analyzes output application
        if (ocamlResult.contains(Messages.TYPE_YES)) {
            Log.message().info(logmsg + "yes!");
            return true;
        }
        else {
            Log.message().info(logmsg + "no!");
            return false;
        }
    }

	private ResponsePacket executeAction(DatabaseInterface db, String contractHash, String action, String value) throws SQLException {

		// TODO: I'm not sure that a private method has to build the ResponsePacket, maybe it should be a task of an interface method (the caller)
		String beforeFileName, afterFileName, path;
		Integer sessionID, contextID, c1_progress = DatabaseInterface.CONTRACT_OFF_DUTY, c2_progress = c1_progress;
		boolean allowed, performed, c1_result, c2_result;

		Contract c1 = new Contract(db).loadFromHash(contractHash);
		Contract c2 = new Contract(db).loadFromHash(c1.getCompliantHash());

		contextID = c1.getContextID();

		if (contextID != DatabaseInterface.CONTEXT_EMPTY_ID) {

			// 1) Checks if action is allowed in this context
			allowed = Tools.actionAllowed(db, contractHash, action);
			
			if (!allowed) {
				
				return new ResponsePacket(-1, Messages.CONTRACT_ACTION_CONTEXT);
			}

			// 2) Checks if action is done
			performed = Tools.actionPerformed(db, action);
			
			if (!performed) {
				
				return new ResponsePacket(-1, Messages.SESSION_ACTION_NOT_PERFORMED);
			}
		}

		// 3) Loads data from db to do the action
		beforeFileName = Tools.getFile(contractHash + action,
				Tools.PATH_CTU_NETS, Tools.EXTENSION_NETS, false);
		Tools.loadNetworkFromDB(db, contractHash, beforeFileName);
		afterFileName = Tools.getFile(contractHash + action,
				Tools.PATH_CTU_NETS, Tools.EXTENSION_NETS, false);

		// 4) Calls CTU and does action

		sessionID = c1.getSessionID();
		path = Tools.PATH_CTU + Tools.CTU_PARAM_STEP + " " + c1.getRole() + " "
				+ action + " " + calculateDelay(db, sessionID) + " " // TODO:
																		// Check
																		// if
																		// delay
																		// works
				+ beforeFileName + " " + afterFileName + " " + 0;
		Tools.callApplication(path, null, false);

		// 5) Saves new network in db.
		db.saveNetwork(sessionID, afterFileName);

		// 6) Update state of contract and compliant contract.
		try {

			// if user became culpable with the current action, network must be
			// rebuilt to avoid extaction
			// from the counterparty (otherwise, both participant will be
			// culpable)
			if (monitorContractProgress(db, contractHash,
					Tools.CTU_PARAM_CULPABLE)) {

				path = Tools.PATH_CTU + Tools.CTU_PARAM_STEP + " "
						+ c1.getRole() + " " + action + " " + 0 + " "
						+ beforeFileName + " " + afterFileName + " " + 1; // note
																			// the
																			// 1
				Tools.callApplication(path, null, false);

				db.saveNetwork(sessionID, afterFileName);
			}

			// 6a) Checks culpability
			c1_result = monitorContractProgress(db, contractHash,
					Tools.CTU_PARAM_CULPABLE);
			c2_result = monitorContractProgress(db, c2.getContractHash(),
					Tools.CTU_PARAM_CULPABLE);

			if (c1_result && c2_result) {
				c1_progress = DatabaseInterface.CONTRACT_CULPABLE;
				c2_progress = DatabaseInterface.CONTRACT_CULPABLE;
			} else if (c1_result) {
				c1_progress = DatabaseInterface.CONTRACT_CULPABLE;
				c2_progress = DatabaseInterface.CONTRACT_INNOCENT;
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
				db.insertTrace(actionID, action, c1.getRole(), sessionID);
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

			return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
		
		} catch (DBException e) {

			return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
			
		} catch (InternalException iie) {

			return new ResponsePacket(iie.getType(), iie.getMessage());
		}

		db.updateContract(contractHash, sessionID, c1.getRole(), c1_progress);
		db.updateContract(c2.getContractHash(), sessionID, c2.getRole(), c2_progress);

		return new ResponsePacket(1, Messages.SESSION_ACTION_DONE);
	}

	private Float calculateDelay(DatabaseInterface db, Integer sessionID)
			throws SQLException {

		String query;
		ResultSet rs;
		Long timestamp, elapsedTime;

		query = "SELECT start_timestamp FROM session WHERE session_id="
				+ sessionID + ";"; // counterparty's role
		rs = db.select(query);
		rs.next();
		timestamp = rs.getLong(1);

		elapsedTime = ((System.currentTimeMillis() - timestamp) / 1000) / 60;

		return new Float(elapsedTime); // CHECK IF IT IS CORRECT
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
}
