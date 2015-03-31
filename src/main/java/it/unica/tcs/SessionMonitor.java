package it.unica.tcs;

import it.unica.tcs.InternalException.ErrorTypes;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SessionMonitor {
	
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
/*
	private String executeAction(DatabaseInterface db, String contractHash,
			String action, String value) throws SQLException {

		String beforeFileName, afterFileName, path;
		Integer sessionID, contextID, c1_progress = DatabaseInterface.CONTRACT_OFF_DUTY, c2_progress = c1_progress;
		boolean allowed, performed, c1_result, c2_result;

		Contract c1 = new Contract(db).loadFromHash(contractHash);
		Contract c2 = new Contract(db).loadFromHash(c1.getCompliantHash());

		String[] types, msgs;
		types = new String[2];
		msgs = new String[2];
		types[0] = types[1] = Messages.TYPE_GENERIC_ERROR;
		msgs[0] = Messages.SESSION_ACTION_DENIED;

		contextID = c1.getContextID();

		if (contextID != DatabaseInterface.CONTEXT_EMPTY_ID) {

			// 1) Checks if action is allowed in this context
			allowed = Tools.actionAllowed(db, contractHash, action);
			if (!allowed) {
				msgs[1] = Messages.CONTRACT_ACTION_CONTEXT;
				return printMessage(types, msgs);
			}

			// 2) Checks if action is done
			performed = Tools.actionPerformed(db, action);
			if (!performed) {
				msgs[1] = Messages.SESSION_ACTION_NOT_PERFORMED;
				return printMessage(types, msgs);
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

			msgs[1] = Messages.ERROR_GENERIC_INTERNAL;
			return printMessage(types, msgs);
		} catch (DBException e) {

			msgs[1] = Messages.ERROR_GENERIC_INTERNAL;
			return printMessage(types, msgs);
		} catch (InternalException iie) {

			return iie.getResponse();
		}

		db.updateContract(contractHash, sessionID, c1.getRole(), c1_progress);
		db.updateContract(c2.getContractHash(), sessionID, c2.getRole(),
				c2_progress);

		// 7) Returns results
		types[0] = types[1] = Messages.TYPE_SUCCESS;
		msgs[0] = Messages.CONTRACT_FUSED_MESSAGE;
		msgs[1] = Messages.SESSION_ACTION_DONE;

		return printMessage(types, msgs);
	}*/

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
