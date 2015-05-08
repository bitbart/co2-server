package it.unica.tcs;


import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;
import it.unica.tcs.InternalException.ErrorTypes;

import java.io.FileNotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

/** Provides API to start a session between two contracts: 1) Insert a new contract; 2) Check if contract sent is fused. */
@Path(value = "/handling")
public class SessionHandler {

    // TODO: Add comments for timestamp param (in all the sources).
	
    /** Receives contracts from client: decides if accept or reject a contract.
     * 
     * @param username Client username
     * @param pass Client password
     * @param contractXML Xml contract sent by client
     * @return Xml response that communicates if the contract is: accepted, accepted and fused, rejected */
    @POST
    @Path(value = "/tellContract")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket tellContract(QueryPacket postData) {

    	// 0) Picking inputs
    	String username = postData.getUsername();
    	String pass = postData.getPassword();
    	String contractXML = postData.getFirstContract();
    	Long timestamp = MainApplication.getRand();
    	
        Integer contractID, compliantID;
        String contractHash, compliantContract, typePreCheck, firstOutputOcaml, secondOutputOcaml;
        String[] input = new String[1];
        Integer contextID;
        boolean areFused;
        int userID;
        
        if (Tools.isNotValid(username, Tools.USERNAME_REGEX) || Tools.isNotValid(pass, Tools.PASSWORD_REGEX) || Tools.isNotValid(contractXML, Tools.XML_CONTRACT_REGEX)) {// TODO: to be completed
            
            Log.message().warning("The tellContract() was called with wrong parameters.");
            return new ResponsePacket(-1, "The TELLCONTRACT api was called with wrong parameters.");
        }

        // 1) Connecting to db
        DatabaseInterface db = MainApplication.getDBConnection();

        try {
            // 2) Checking for valid auth data
            if (!Tools.authenticate(db, username, pass)) {
                Log.message().warning(
                        "Authentication error. Cannot accept USERNAME=" + Log.format(username) + " and hashed PASSWORD="
                                + Log.format(Tools.hash256(pass)) + "");
    
                return new ResponsePacket(-1, Messages.AUTH_FAILED);
            }
        }catch (SQLException e) {

            Log.message().severe("Thrown SQL exception while opening database: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }

        // 3) Checking if contract is valid
        try {
            if (!Validator.localValidateXML(contractXML)) {
         
                Log.message().warning("Invalid contract passed to tell().");
                return new ResponsePacket(-1, Messages.CONTRACT_INVALID);
            }
        }
        catch (InternalException iie) {

            Log.message().severe("IllegalInputException thrown in localValidateXML: " + iie.getMessage());
            
            return new ResponsePacket(iie.getType(), iie.getMessage());
        }
        catch (FileNotFoundException e) {

            Log.message().severe("FileNotFoundException thrown in localValidateXML: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
        }
        
        // 4) Checking if the contract admits a compliant
        if (!Dualizer.localAdmitsCompliant(contractXML)){

            Log.message().info("The contract C1= " + escapeHtml(contractXML.replaceAll("\n", "")) + " does not admit a compliant!");
            
            return new ResponsePacket(-1, Messages.CONTRACT_DOESNT_ADMITS_COMPLIANT + " and cannot be registered.");
        }
        
        // 5) Checking the type of contract for the future PreCheck
        typePreCheck = ComplianceChecker.getContractType(contractXML);


        // 6) Asking CTU for the partial UPPAAL template/mapping to be stored into the database
        input[0] = contractXML + "\n";
        firstOutputOcaml = Tools.callApplication(Tools.PATH_CTU + Tools.CTU_PARAM_BUILD_AUTOMATON, input, false);
        
        // 7) Asking CTU for the labels to be stored into the database
        secondOutputOcaml = Tools.callApplication(Tools.PATH_CTU + Tools.CTU_PARAM_GET_LABELS, input, false);
        
        // TODO: tests for the validity of firstOutputOcaml and second

        // 8) Adding contracts to database
        // 8a) Loads owner data
        try {
            ResultSet rs = db.select("SELECT user_id FROM user WHERE email = '" + username + "';");
            rs.next();
            userID = rs.getInt(1);
        }
        catch (SQLException e) {

            Log.message().warning("Failed SELECT when loading owner data in tell(). SQL says: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }

        // 8b) Saves contract data
        try {
            contractHash = Tools.hashContract(contractXML, timestamp);
            contextID = Tools.getIDFromContext(db, Tools.getDeclaredStringContext(contractXML));
            contractID = db.insertContract(contractHash, contractXML, userID, contextID, DatabaseInterface.CONTRACT_ROLE_LATENT, DatabaseInterface.CONTRACT_HANDLED, new Long(timestamp), typePreCheck, firstOutputOcaml, secondOutputOcaml); 
            
            // The contract is under processing (not latent)
            Log.message().info("Added new contract with ID=" + contractID + ", HASH=" + Log.format(contractHash) + ", OWNER=" + userID + " and CONTEXT=" + contextID);
        }
        catch (SQLException e) {
        	
            Log.message().warning("Cannot add a contract to DB. SQL says: " + e.getMessage());
            return new ResponsePacket(-1, Messages.DB_INSERT_FAILED);
        }
                
        boolean endWhile = true;
        areFused = false;
        
        while (endWhile) {
            // 9) Checking contract compliance
            try {
                BasicPair<Integer, String> compliantData = ComplianceChecker.getCompliant(db, contractXML,firstOutputOcaml, secondOutputOcaml, contextID, typePreCheck);
                
                // 9a) Checks if compliant ID exists
                if (!compliantData.isEmpty()) {
                    
                    compliantID = compliantData.getFirst();
                    compliantContract = compliantData.getSecond();
                }
                // Else, no compliant is found
                else {
                	
                    db.updateContractState(contractID, DatabaseInterface.CONTRACT_LATENT); // Now it is really latent
    
                    Log.message().fine("No compliant contract found for C1=" + contractID + "");
                    
                    return new ResponsePacket(0, Messages.CONTRACT_REGISTERED + ". " + Messages.SESSION_COMPLIANT_NO, contractHash);
                }
            }
            catch (SQLException sqle) {
    
                Log.message().warning("Cannot find a compliant contract. SQL says: " + sqle.getMessage());
                
                return new ResponsePacket(-1, Messages.DB_INSERT_FAILED);
            }
            catch (FileNotFoundException fnfe) {
    
                Log.message().severe("FileNotFoundException thrown in tell(): " + fnfe.getMessage());
                
                return new ResponsePacket(-1, Messages.TYPE_GENERIC_ERROR);
            }
            
            Contract compliant = new Contract();
            
            // ACQUIRES THE MUTEX ON THE COMPLIANT CONTRACT
            MainApplication.mutexAcquire(compliantID);
    
            try {
                
                compliant.loadFromID(compliantID);
            }
            catch (SQLException sqle) {
    
                MainApplication.mutexRelease(compliantID);
                Log.message().severe("Cannot retrieve compliant contract data. SQL says: " + sqle.getMessage());
                
                return new ResponsePacket(-1, Messages.DB_INSERT_FAILED);
            }
            
            // 10) Merging contracts
            if (compliant.getState() == DatabaseInterface.CONTRACT_LATENT) {
                areFused = fuse(db, contractXML, compliantContract, contractID, compliantID);
                endWhile = false;
                
            }
            
            // RELEASES THE MUTEX (end critical section)
            MainApplication.mutexRelease(compliantID);
        }
        
        // 11) Decrements the user's reputation
        try {
			User tmp = User.build(username);
			tmp.decrementRepAndStore();
			
		} catch (SQLException sqle) {
			
			Log.message().severe("SQLException thrown while decrementing the reputation in 'tell()': " + sqle.getMessage());
		}

        // 12) Returning response
        if (areFused) {
            return new ResponsePacket(1, Messages.CONTRACT_REGISTERED +". " + Messages.SESSION_COMPLIANT_YES, contractHash);
        }
        else{
            Log.message().severe("Cannot fuse two compliant contracts, unknown cause.");

            return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
        }
    }

    /** Asks to create a dual of a contract C stored in the middleware (with the given hash). 
     * If C exists and is latent, then the middleware creates the dual of C and merges the two contracts.
     * Else the middleware rejects the request.
     * 
     * @param username Client username
     * @param pass Client password
     * @param originalHash Hash of the contract stored in the middleware
     * @return Xml response that communicates if the contract is: accepted, accepted and fused, rejected */
    @POST
    @Path(value = "/acceptContract")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket acceptContract(QueryPacket postData) {
    	
    	String username = postData.getUsername();
    	String pass = postData.getPassword();
    	String originalHash = postData.getContractHash();
    	Long randLong = MainApplication.getRand();

    	Contract original;
    	Integer originalState;
        String originalXML, dualXML, dualHash;
        Integer contextID, originalID, dualID;
        boolean areFused;
        int userID;

        // 1) Connecting to db
        DatabaseInterface db = MainApplication.getDBConnection();

        try {
            // 2) Checking for valid auth data
            if (!Tools.authenticate(db, username, pass)) {
                Log.message().warning("Authentication error. Cannot accept USERNAME=" + Log.format(username) + " and hashed PASSWORD=" + Log.format(Tools.hash256(pass)) + "");
                
                return new ResponsePacket(-1, Messages.AUTH_FAILED);
            }
        }catch (SQLException e) {
            
            Log.message().severe("Thrown SQL exception while opening database: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }

        // 3) Checking if requested contract exists and is latent.
        try {
        	// Load original contract data
			original = new Contract().loadFromHash(originalHash);
	        originalState = original.getState();
            originalID = original.getContractID();
            originalXML = original.getContractXML();

	        if (originalState != DatabaseInterface.CONTRACT_LATENT) {
	        	
	        	Log.message().info("Checked if the contract with HASH=" + originalHash + " is latent: NO!");
	        	new ResponsePacket(-1, Messages.PERMISSION_DENIED);
	        }
	        
			Log.message().info("Contract loaded from hash; ID: " + originalID + ", HASH: " + originalHash + ", XML: " + Log.format(originalXML) + ", STATE: " + originalState);
	        
		} catch (SQLException e) {
		    
            Log.message().warning("Failed while checking if the requested CONTRACT='"+ originalHash +"' exists and is latent: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
		}
        
        // 4) Create the XML dual contract and adding contracts to database
        // 4a) Loads owner data
        try {
            ResultSet rs = db.select("SELECT user_id FROM user WHERE email = '" + username + "';");
            rs.next();
            userID = rs.getInt(1);
        }
        catch (SQLException e) {
            
            Log.message().warning("Failed SELECT when loading owner data in accept(). SQL says: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }

        // 4b) Loads contract data
        try {
        	// Load dual contract data
            Log.message().info("Calling dual");
        	dualXML = Dualizer.getXMLDual(originalXML);
            dualHash = Tools.hashContract(dualXML, randLong);
            contextID = Tools.getIDFromContext(db, Tools.getDeclaredStringContext(dualXML));  
            
            // TODO: is it the following part necessary for dual contracts?
            
            // 6) Asking CTU for the partial UPPAAL template/mapping to be stored into the database
    		String[] input = new String[1];
    		input[0] = dualXML + "\n";
            String firstOutputOcaml = Tools.callApplication(Tools.PATH_CTU + Tools.CTU_PARAM_BUILD_AUTOMATON, input, false);
            
            // 7) Asking CTU for the labels to be stored into the database
            String secondOutputOcaml = Tools.callApplication(Tools.PATH_CTU + Tools.CTU_PARAM_GET_LABELS, input, false);
            
            dualID = db.insertContract(dualHash, dualXML, userID, contextID, DatabaseInterface.CONTRACT_ROLE_LATENT, DatabaseInterface.CONTRACT_HANDLED, new Long(randLong), "5", firstOutputOcaml, secondOutputOcaml);

            Log.message().info("Added new contract with XML=" + Log.format(dualXML) + ", ID=" + dualID + ", HASH=" + Log.format(dualHash) + ", OWNER=" + userID + " and CONTEXT=" + contextID);
      
        } catch (SQLException e) {
            
            Log.message().warning("Cannot add a contract to DB. SQL says: " + e.getMessage());
            return new ResponsePacket(-1, Messages.DB_INSERT_FAILED);
            
        } catch (Exception e){
            
            Log.message().warning("XML error: " + e.getMessage());
            return new ResponsePacket(-1, Messages.DB_INSERT_FAILED);
        }

        // 5) Compliance contract certainly exists: merge contracts.
        areFused = fuse(db, originalXML, dualXML, originalID, dualID);
        
        if (areFused) {

            return new ResponsePacket(1, Messages.CONTRACT_REGISTERED +"." + Messages.SESSION_COMPLIANT_YES, dualHash);
        }
        else {
            Log.message().warning("Cannot fuse two compliant contracts, unknown cause.");
            return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
        }
    }

    
    /** 
     * Given a contract it communicates if is fused.
     * @param username Client username
     * @param pass Client password
     * @param contractHash Xml contract sent by client
     * @return Xml response that communicates if the contract is fused 
     */
    @POST
    @Path(value = "/isFused")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket checkFused(QueryPacket postData) {
    	
    	String username = postData.getUsername();
    	String pass = postData.getPassword();
    	String contractHash = postData.getContractHash();

        DatabaseInterface db = MainApplication.getDBConnection();
        
        Integer state;
        String logmsg = "Checked state for contract with HASH=" + Log.format(contractHash) + ": ";

        // Retrieves contract state
        try {
            state = getContractState(db, username, pass, contractHash);

            switch (state) {
                case DatabaseInterface.CONTRACT_LATENT:
                    Log.message().fine(logmsg + "LATENT");
                    return new ResponsePacket(0, Messages.CONTRACT_LATENT_MESSAGE);

                case DatabaseInterface.CONTRACT_ON_DUTY:
                    Log.message().fine(logmsg + "ON_DUTY");
                    return new ResponsePacket(1, Messages.CONTRACT_FUSED_MESSAGE);

                case DatabaseInterface.CONTRACT_OFF_DUTY:
                    Log.message().fine(logmsg + "OFF_DUTY");
                    return new ResponsePacket(1, Messages.CONTRACT_FUSED_MESSAGE);

                case DatabaseInterface.CONTRACT_INNOCENT:
                    Log.message().fine(logmsg + "INNOCENT");
                    return new ResponsePacket(1, Messages.CONTRACT_COMPLETED_MESSAGE);

                case DatabaseInterface.CONTRACT_CULPABLE:
                    Log.message().fine(logmsg + "CULPABLE");
                    return new ResponsePacket(1, Messages.CONTRACT_COMPLETED_MESSAGE);

                default:
                    Log.message().fine(logmsg + "ERROR");
                    return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
            }

        }
        catch (DBException e) {

            Log.message().warning("Database exception thrown when checking fusion: " + e.getMessage());
            
            return new ResponsePacket(-1, e.getMessage());
        }
    }
    
    @POST
    @Produces(value="text/xml")
    @Path(value = "/depositTell") 
    public String tellContract(@FormParam("username") String username, @FormParam("pass") String pass,
            @FormParam("contract") String contractXML, @FormParam("randlong") String randLong, @FormParam("deposit") String deposit) {
        
        return null;
    }

    /** Given a contract and a compliant, fuses the two contracts.
     * 
     * @param db Database
     * @param contract XML of the user contract
     * @param compliant Identifier of the compliant contract found by server, it will be sent to the client via xml
     *            response
     * @return true if contracts are fused */
    private boolean fuse(DatabaseInterface db, String contract, String compliant, Integer contractID, Integer compliantID) {

        String input[] = new String[2], path, fileName, sessionHash;
        Integer contextID, sessionID;
        
        Contract c1, c2;
        try {
            c1 = new Contract().loadFromID(contractID);
            c2 = new Contract().loadFromID(compliantID);
        }
        catch (SQLException e) {

            Log.message().severe(
                    "Cannot retrieve data for C1=" + contractID + " and C2=" + compliantID + ". SQL says: "
                            + e.getMessage());

            return false;
        }

        // 1) Creates Ocaml process to start session
        // 1a) Ocaml CTU needs a file where write output
        fileName = Tools.getFile(Tools.CTU_PARAM_START + compliant + contract, Tools.PATH_CTU_NETS, Tools.EXTENSION_NETS, true);

        // 1b) Creates process
        path = Tools.PATH_CTU + Tools.CTU_PARAM_START + " " + fileName;
        input[0] = compliant + "\n";
        input[1] = contract;
        Tools.callApplication(path, input, false);
        
        Tools.callApplication("chown mysql " + fileName, null, false); // Change the proprietary of the file (mysql) due to an error bug

        sessionHash = Tools.hash256(c1.getContractHash() + c2.getContractHash());

        // 2) Adds new session to DB
        try {
            contextID = Tools.getIDFromContext(db, Tools.getDeclaredStringContext(contract));
            sessionID = db.insertSession(sessionHash, (Integer) DatabaseInterface.SESSION_ACTIVE, fileName, contextID);

        }
        catch (SQLException e) {

            Log.message().severe(
                    "Cannot insert new session in database (when fusing), SQL says: " + e.getMessage());
            Log.message().fine("Ocaml filename: " + fileName);
            return false;
        }

        // 3) Update state of contract and compliant contract.
        try {
            boolean c1_result, c2_result;
            Integer c1_progress = DatabaseInterface.CONTRACT_OFF_DUTY, c2_progress = DatabaseInterface.CONTRACT_OFF_DUTY;

            db.updateContract(contractID, sessionID, DatabaseInterface.CONTRACT_ROLE_1, c1_progress);
            db.updateContract(compliantID, sessionID, DatabaseInterface.CONTRACT_ROLE_0, c2_progress);

            // 6b) Checks who is on duty
            c1_result = new SessionMonitor().monitorContractProgress(db, c1.getContractHash(), Tools.CTU_PARAM_DUTY);
            Log.message().finest("First monitorContractProgress() completed without problems.");
            c2_result = new SessionMonitor().monitorContractProgress(db, c2.getContractHash(), Tools.CTU_PARAM_DUTY);
            Log.message().finest("Second monitorContractProgress() completed without problems.");
            
            if (c1_result) {
                c1_progress = DatabaseInterface.CONTRACT_ON_DUTY;
                c2_progress = DatabaseInterface.CONTRACT_OFF_DUTY;
            }
            else if (c2_result) {
                c2_progress = DatabaseInterface.CONTRACT_ON_DUTY;
                c1_progress = DatabaseInterface.CONTRACT_OFF_DUTY;
            }
            else if (!c1_result && !c2_result) {
                c1_progress = DatabaseInterface.CONTRACT_OFF_DUTY;
                c2_progress = DatabaseInterface.CONTRACT_OFF_DUTY;
            }
            else
                throw new InternalException(ErrorTypes.TYPE_TOO_ONDUTY);

            db.updateContractState(contractID, c1_progress);
            db.updateContractState(compliantID, c2_progress);

            Log.message().info(
                    "Contract with ID=" + contractID + " and contract with ID=" + compliantID
                            + " have been fused in a new session with ID=" + sessionID + " and HASH="
                            + Log.format(sessionHash));

            return true;
        }
        catch (InternalException e) {

        }
        catch (DBException e) {

            Log.message().severe(
                    "Error while checking participant status in monitorContractProgress: " + e.getMessage());
        }
        catch (SQLException e) {

            Log.message().severe("Failed updating a contract. SQL says: " + e.getMessage());
        }

        return false;
    }

    /** Given a contract, return its state.
     * 
     * @param username Client username
     * @param pass Client password
     * @param contractHash Hash contract sent by client
     * @return An integer value that indicates the state of the contract
     * @throws DBException if authentication, or permission, or queries fail */
    public static int getContractState(DatabaseInterface db, String username, String pass, String contractHash) throws DBException {

        ResultSet rs;
        
        try{
        // 2) Checks for valid user & pwd
            if (!Tools.authenticate(db, username, pass)) {
                Log.message().warning(
                        "Authentication error. Cannot accept USERNAME=" + Log.format(username) + " and hashed PASSWORD="
                                + Log.format(Tools.hash256(pass)) + "");
                
                throw new DBException(Messages.AUTH_FAILED);
            }
        } catch (SQLException e) {
        
            Log.message().severe("Thrown SQL exception while opening database: " + e.getMessage());
            
            throw new DBException(Messages.AUTH_FAILED);
        }

        // 3) Verifies contract's owner
        if (!Tools.permissionContract(db, username, contractHash)) {
            Log.message().warning(
                    "Access denied: user with USERNAME=" + Log.format(username)
                            + " tried to access contract with CONTRACT_HASH=" + Log.format(contractHash));

            throw new DBException(Messages.PERMISSION_DENIED);
        }

        // 4) Retrieves contract state
        try {
            rs = db.select("SELECT state FROM contract WHERE contract_hash = '" + contractHash + "';");
            rs.next();
            
            Integer result = rs.getInt(1);
            
            return result;

        }
        catch (SQLException e) {

            Log.message().severe(
                    "Cannot retrieve 'state' from contract with CONTRACT_HASH=" + contractHash + ". SQL says: "
                            + e.getMessage());

            throw new DBException(Messages.DB_CONN_FAILED);
        }
    }
    
    // TODO: insert comments
    /** */
    @POST
    @Path(value = "/verifyCredentials")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket verifyCredentials(QueryPacket postData){
    	
    	// 0) Picking inputs
    	String username = postData.getUsername();
    	String pass = postData.getPassword();
    	
    	try {
    		
			Thread.sleep(2000);
			
		} catch (InterruptedException e1) {}

        try {
            DatabaseInterface db = MainApplication.getDBConnection();

	        if (!Tools.authenticate(db, username, pass)) {
	        	Log.message().warning(
	        			"Authentication error. Cannot accept USERNAME=" + Log.format(username) + " and hashed PASSWORD="
	                    + Log.format(Tools.hash256(pass)) + "");
	    
	            return new ResponsePacket(-1, Messages.AUTH_FAILED);
	        }

	        return new ResponsePacket(1, "Correct username and password pair.");
			
		} catch (SQLException e) {
			
            Log.message().severe("SQL Exception thrown when verifying credentials: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
		}
    }
}