package it.unica.tcs;


import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unica.tcs.InternalException.ErrorTypes;
import it.unica.tcs.database.DBException;
import it.unica.tcs.database.DatabaseInterface;

/** Provides API to start a session between two contracts: 1) Insert a new contract; 2) Check if contract sent is fused. */
@Path(value = "/handling")
public class SessionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SessionHandler.class);
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
    	Long randomLong = MainApplication.getRand();
    	Integer delay = postData.getDelay();
    	boolean prv = postData.getPrivate();
    	
        Integer contractID, compliantID;
        String contractHash, compliantContract, typePreCheck, firstOutputOcaml, secondOutputOcaml;
        String[] input = new String[1];
        Integer contextID;
        boolean areFused;
        int userID;
        
        if (Tools.isNotValid(username, Tools.USERNAME_REGEX) || Tools.isNotValid(pass, Tools.PASSWORD_REGEX) || Tools.isNotValid(contractXML, Tools.XML_CONTRACT_REGEX)) {// TODO: to be completed
            
            logger.warn("The tellContract() was called with wrong parameters.");
            return new ResponsePacket(-1, "The TELLCONTRACT api was called with wrong parameters.");
        }

        // 1) Connecting to db
        
        try {
            // 2) Checking for valid auth data
            if (!DatabaseInterface.getInstance().authenticate(username, pass)) {
                logger.warn(
                        "Authentication error. Cannot accept USERNAME=" + username + " and hashed PASSWORD="
                                + Tools.hash256(pass) + "");
    
                return new ResponsePacket(-1, Messages.AUTH_FAILED);
            }
        }catch (SQLException e) {

            logger.error("Thrown SQL exception while opening database: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }

        // 3) Checking if contract is valid
        try {
            if (!Validator.localValidateXML(contractXML)) {
         
                logger.warn("Invalid contract passed to tell().");
                return new ResponsePacket(-1, Messages.CONTRACT_INVALID);
            }
        }
        catch (InternalException iie) {

            logger.error("IllegalInputException thrown in localValidateXML: " + iie.getMessage());
            
            return new ResponsePacket(iie.getType(), iie.getMessage());
        }
        catch (FileNotFoundException e) {

            logger.error("FileNotFoundException thrown in localValidateXML: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
        }
        
        // 4) Checking if the contract admits a compliant
        if (!Dualizer.localAdmitsCompliant(contractXML)){

            logger.info("The contract C1= " + StringEscapeUtils.escapeXml(contractXML.replaceAll("\n", "")) + " does not admit a compliant!");
            
            return new ResponsePacket(-1, Messages.CONTRACT_DOESNT_ADMITS_COMPLIANT + " and cannot be registered.");
        }
        
        // 5) Checking the type of contract for the future PreCheck
        typePreCheck = ComplianceChecker.getContractType(contractXML);


        // 6) Asking CTU for the partial UPPAAL template/mapping to be stored into the database
        input[0] = contractXML + "\n";
        
        
        // TODO: new part: check it
        AppResponse outputCTU;
        
        outputCTU = Tools.callApplication(Tools.getCtuPath()+ Tools.CTU_PARAM_BUILD_AUTOMATON, input);
        
        
        // Sanity check of the CTU's response
        if (outputCTU.isEmpty()) {
        	
        	logger.warn("CTU returns an empty mapping for a contract. Retrying.");
        	
        	try {
				Thread.sleep(500);
			} catch (InterruptedException e) {}
        	
        	// Second attempt
        	outputCTU = Tools.callApplication(Tools.getCtuPath()+ Tools.CTU_PARAM_BUILD_AUTOMATON, input);
        	
            if (outputCTU.isEmpty()) {
            	
            	logger.trace("UPPAAL errors:" + outputCTU.getErrors());
            		
            	logger.error("CTU still returns an empty mapping for a contract. Rejecting tell.");
            	return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
            }
        }
        
        // 7) Asking CTU for the labels to be stored into the database
        
        AppResponse outputCTU_second;
        outputCTU_second = Tools.callApplication(Tools.getCtuPath()+ Tools.CTU_PARAM_GET_LABELS, input);
        
        
        // Sanity check of the CTU's response
        if (outputCTU_second.isEmpty()) {
        	
        	logger.warn("CTU returns an empty getLabel for a contract. Retrying.");
        	
        	try {
				Thread.sleep(500);
			} catch (InterruptedException e) {}
        	
        	// Second attempt
        	outputCTU_second = Tools.callApplication(Tools.getCtuPath()+ Tools.CTU_PARAM_GET_LABELS, input);
        	
            if (outputCTU_second.isEmpty()) {
            		
            	logger.error("CTU still returns an empty getLabel for a contract. Rejecting tell.");
            	return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
            }
        }
        
        firstOutputOcaml = outputCTU.getOutput();
        secondOutputOcaml = outputCTU_second.getOutput();
        
        // TODO: tests for the validity of firstOutputOcaml and second

        // 8) Adding contracts to database
        // 8a) Loads owner data
        try {
            userID = DatabaseInterface.getInstance().selectUserId(username);
        }
        catch (SQLException e) {

            logger.warn("Failed SELECT when loading owner data in tell(). SQL says: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }

        // 8b) Saves contract data
        try {
        	if (delay == null)
        		delay = 0;
        	
            contractHash = Tools.hashContract(contractXML, randomLong);
            contextID = DatabaseInterface.getInstance().getIDFromContext(Tools.getDeclaredStringContext(contractXML));
            contractID = DatabaseInterface.getInstance().insertContract(contractHash, contractXML, userID, contextID, DatabaseInterface.CONTRACT_ROLE_LATENT, DatabaseInterface.CONTRACT_HANDLED, new Long(randomLong), typePreCheck, firstOutputOcaml, secondOutputOcaml, delay, prv); 
            
            // The contract is under processing (not latent)
            logger.info("Added new contract with ID=" + contractID + ", HASH=" + contractHash + ", OWNER=" + userID + " and CONTEXT=" + contextID);
        }
        catch (SQLException e) {
        	
        	
            logger.warn("Cannot add a contract to DB. SQL says: " + e.getMessage());
            return new ResponsePacket(-1, Messages.DB_INSERT_FAILED);
        }
                
        boolean endWhile = true;
        areFused = false;
        
        while (endWhile) {
            // 9) Checking contract compliance
            try {
                BasicPair<Integer, String> compliantData = ComplianceChecker.getCompliant(DatabaseInterface.getInstance(), contractXML,firstOutputOcaml, secondOutputOcaml, contextID, typePreCheck);
                
                // 9a) Checks if compliant ID exists
                if (!compliantData.isEmpty()) {
                    
                    compliantID = compliantData.getFirst();
                    compliantContract = compliantData.getSecond();
                }
                // Else, no compliant is found
                else {
                	
                    DatabaseInterface.getInstance().setContractState(contractID, DatabaseInterface.CONTRACT_LATENT); // Now it is really latent
    
                    logger.trace("No compliant contract found for C1=" + contractID + "");
                    
                    // 9b) Decrements the user's reputation
                    try {
            			User.build(userID).decrementRepAndStore();
            			
            		} catch (SQLException sqle) {
            			
            			logger.error("SQLException thrown while decrementing the reputation in 'tell()': " + sqle.getMessage());
            		}
                    
                    return new ResponsePacket(0, Messages.CONTRACT_REGISTERED + ". " + Messages.SESSION_COMPLIANT_NO, contractHash);
                }
            }
            catch (SQLException sqle) {
    
                logger.warn("Cannot find a compliant contract. SQL says: " + sqle.getMessage());
                
                return new ResponsePacket(-1, Messages.DB_INSERT_FAILED);
            }
            catch (FileNotFoundException fnfe) {
    
                logger.error("FileNotFoundException thrown in tell(): " + fnfe.getMessage());
                
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
                logger.error("Cannot retrieve compliant contract data. SQL says: " + sqle.getMessage());
                
                return new ResponsePacket(-1, Messages.DB_INSERT_FAILED);
            }
            
            // 10) Merging contracts
            if (compliant.getState() == DatabaseInterface.CONTRACT_LATENT) {
            	
                areFused = fuse(DatabaseInterface.getInstance(), contractXML, compliantContract, contractID, compliantID);
                endWhile = false;
                
            }
            
            // RELEASES THE MUTEX (end critical section)
            MainApplication.mutexRelease(compliantID);
        }
        
        // 11) Decrements the user's reputation
        try {
			User.build(userID).decrementRepAndStore();
			
		} catch (SQLException sqle) {
			
			logger.error("SQLException thrown while decrementing the reputation in 'tell()': " + sqle.getMessage());
		}

        // 12) Returning response
        if (areFused) {
            return new ResponsePacket(1, Messages.CONTRACT_REGISTERED +". " + Messages.SESSION_COMPLIANT_YES, contractHash);
        }
        else{
            logger.error("Cannot fuse two compliant contracts, unknown cause.");

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
    	String type = postData.getContractType();

    	Contract original;
    	Integer originalState;
        String originalXML, dualXML, dualHash, originalType;
        Integer contextID, originalID, dualID;
        boolean areFused;
        int userID;
        
        if (type==null || !type.equals(Contract.TYPE_TST)) { // TODO: handle other contract types here
        	
        	logger.trace("A user tried to accept a contract of an unknown or null contract type: " + type);
        	return new ResponsePacket(-1, "The specified contract type doesn't exist or you haven't specified the type."); // serialize this message
        }
        
        AppResponse firstOutput, secondOutput;

        // 1) Connecting to db
        DatabaseInterface db = DatabaseInterface.getInstance();

        try {
            // 2) Checking for valid auth data
            if (!DatabaseInterface.getInstance().authenticate(username, pass)) {
                logger.warn("Authentication error. Cannot accept USERNAME=" + username + " and hashed PASSWORD=" + Tools.hash256(pass) + "");
                
                return new ResponsePacket(-1, Messages.AUTH_FAILED);
            }
        }catch (SQLException e) {
            
            logger.error("Thrown SQL exception while opening database: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }
        
        try {
        	
        	// Loads contractID
			original = new Contract().loadFromHash(originalHash);
            originalID = original.getContractID();
        }
        catch (SQLException e) {
		    
            logger.warn("Failed while checking if the requested CONTRACT='"+ originalHash +"' exists and is latent: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
		}
            
        // 3) Checking if requested contract exists and is latent.
        try {
            
        	MainApplication.mutexAcquire(originalID);
        	
        	// Loads all data (after mutex acquiring)
        	original = new Contract().loadFromID(originalID);
        	originalState = original.getState();
            originalID = original.getContractID();
            originalXML = original.getContractXML();
            originalType = original.getType();
            
            if (!originalType.equals(type)) {
            	
            	logger.trace("The contract that a user tried to accept is not of the specified type (" + type + ").");
            	MainApplication.mutexRelease(originalID);
                return new ResponsePacket(0, "The contract you tried to accept is not of the specified type (" + type + ")."); // serialize this message
            }

	        if (originalState != DatabaseInterface.CONTRACT_LATENT) {
	        	
	        	// logger.info("Checked if the contract with HASH=" + originalHash + " is latent: NO!");
	        	MainApplication.mutexRelease(originalID);
	        	
	        	return new ResponsePacket(-1, Messages.CONTRACT_NOT_PUBLISHED);
	        }
	        
	        if (original.isExpired()) {
	        	
	        	db.setContractState(originalID, DatabaseInterface.CONTRACT_EXPIRED);
            	logger.info("Contract with ID=" + originalID + " is declared expired.");
	        	MainApplication.mutexRelease(originalID);
	        	
	        	return new ResponsePacket(-1, Messages.CONTRACT_EXPIRED_MESSAGE);
	        }
	        
			logger.info("Contract loaded from hash; ID: " + originalID + ", HASH: " + originalHash + ", XML: " + originalXML + ", STATE: " + originalState);
	        
		} catch (SQLException e) {
		    
			MainApplication.mutexRelease(originalID);
            logger.warn("Failed while checking if the requested CONTRACT='"+ originalHash +"' exists and is latent: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
		}
        
        // 4) Create the XML dual contract and adding contracts to database
        // 4a) Loads owner data
        try {
            userID = db.selectUserId(username);
        }
        catch (SQLException e) {
            
        	MainApplication.mutexRelease(originalID);
            logger.warn("Failed SELECT when loading owner data in accept(). SQL says: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }

        // 4b) Loads contract data
        try {
        	// Load dual contract data
            logger.info("Calling dual");
        	dualXML = Dualizer.getXMLDual(originalXML);
            dualHash = Tools.hashContract(dualXML, randLong);
            contextID = DatabaseInterface.getInstance().getIDFromContext(Tools.getDeclaredStringContext(dualXML));  
            
            // TODO: is it the following part necessary for dual contracts?
            
            // 6) Asking CTU for the partial UPPAAL template/mapping to be stored into the database
    		String[] input = new String[1];
    		input[0] = dualXML + "\n";
            firstOutput = Tools.callApplication(Tools.getCtuPath()+ Tools.CTU_PARAM_BUILD_AUTOMATON, input);
            
            // 7) Asking CTU for the labels to be stored into the database
            secondOutput = Tools.callApplication(Tools.getCtuPath()+ Tools.CTU_PARAM_GET_LABELS, input);
            
            String firstOutputOcaml = firstOutput.getOutput();
            String secondOutputOcaml = secondOutput.getOutput();
            
            dualID = db.insertContract(dualHash, dualXML, userID, contextID, DatabaseInterface.CONTRACT_ROLE_LATENT, DatabaseInterface.CONTRACT_HANDLED, new Long(randLong), "5", firstOutputOcaml, secondOutputOcaml, 0);

            logger.info("Added new contract with XML=" + dualXML + ", ID=" + dualID + ", HASH=" + dualHash + ", OWNER=" + userID + " and CONTEXT=" + contextID);
      
        } catch (SQLException e) {
            
            logger.warn("Cannot add a contract to DB. SQL says: " + e.getMessage());
            MainApplication.mutexRelease(originalID);
            return new ResponsePacket(-1, Messages.DB_INSERT_FAILED);
            
        } catch (Exception e){
            
            logger.warn("XML error: " + e.getMessage());
            MainApplication.mutexRelease(originalID);
            return new ResponsePacket(-1, Messages.DB_INSERT_FAILED);
        }

        // 5) Compliance contract certainly exists: merge contracts.
        areFused = fuse(db, originalXML, dualXML, originalID, dualID);
        MainApplication.mutexRelease(originalID);
        
        if (areFused) {
        	
            return new ResponsePacket(1, Messages.CONTRACT_REGISTERED +"." + Messages.SESSION_COMPLIANT_YES, dualHash);
        }
        else {
            logger.warn("Cannot fuse two compliant contracts, unknown cause.");
            return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
        }
    }
    
    @POST
    @Path(value = "/retract")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket retract(QueryPacket postData) {
    	
    	String username = postData.getUsername();
    	String pass = postData.getPassword();
    	String contractHash = postData.getContractHash();
    	
        DatabaseInterface db = DatabaseInterface.getInstance();

        try {
            // 2) Checking for valid auth data
            if (!DatabaseInterface.getInstance().authenticate(username, pass)) {
                logger.warn("Authentication error. Cannot accept USERNAME=" + username + " and hashed PASSWORD=" + Tools.hash256(pass) + "");
                
                return new ResponsePacket(-1, Messages.AUTH_FAILED);
            }
        }catch (SQLException e) {
            
            logger.error("Thrown SQL exception while opening database: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }

    	
    	try {
			Contract c = new Contract().loadFromHash(contractHash);
			
			MainApplication.mutexAcquire(c.getContractID());
			
			c.loadFromHash(contractHash); // reload the contract again (to be atomic)
			
			Integer state = c.getState();
			
			if (state == DatabaseInterface.CONTRACT_LATENT) {
				
				db.updateContractState(c.getContractHash(), DatabaseInterface.CONTRACT_EXPIRED);
				
				MainApplication.mutexRelease(c.getContractID());
				
				return new ResponsePacket(1, "The contract has been successfully retracted.");
			}
			else {
				
				MainApplication.mutexRelease(c.getContractID());
				
				return new ResponsePacket(-1, "The specified contract is actually not latent and cannot be retracted.");
			}
				
			
		} catch (SQLException e) {
			
			logger.error("Thrown SQL exception while trying to retract the contract with hash " + contractHash+ ": " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
		}
    }
    
    public static ResponsePacket isFused(String contractHash) {
    	
    	String key = contractHash;
    	
    	DatabaseInterface db = DatabaseInterface.getInstance();
    	
    	Cache<String, Integer[]> lc = MainApplication.getLatentCache();
    	
    	Integer[] response = lc.get(key);
    	Integer actualTime = (int) (System.currentTimeMillis()/1000); // here it is approximated to 1 second

        // Retrieves contract state
        try {
        	
        	if (response != null) {
        		
        		switch(response[0]) {
        		
    	    		case DatabaseInterface.CONTRACT_LATENT: 
    	    		case DatabaseInterface.CONTRACT_HANDLED:
    	    			//logger.trace("Checked if contract with hash=" + Log.format(contractHash) + " is fused (in cache). Expire time is '" + response[1] + "' "
    	    			//		+ "and actual time is '" + actualTime + "'.");
    	    			if (response[1] == -1 || response[1] > actualTime)
    	    				return new ResponsePacket(0, Messages.CONTRACT_LATENT_MESSAGE);
    	    			else {
    	    				logger.trace("Updated state in db and cache for contract with HASH=" + contractHash + ": " + "EXPIRED");
    	    				lc.put(key, new Integer[] {DatabaseInterface.CONTRACT_EXPIRED});
    	    				db.updateContractState(contractHash, DatabaseInterface.CONTRACT_EXPIRED);
    	    				
    	    				return new ResponsePacket(-2, Messages.CONTRACT_EXPIRED_MESSAGE);
    	    			}
    	    		case DatabaseInterface.CONTRACT_OFF_DUTY: 
    	    		case DatabaseInterface.CONTRACT_ON_DUTY: 
    	    			return new ResponsePacket(1, Messages.CONTRACT_FUSED_MESSAGE);
    	    		case DatabaseInterface.CONTRACT_EXPIRED:
    	    			return new ResponsePacket(-2, Messages.CONTRACT_EXPIRED_MESSAGE);
    	    		case DatabaseInterface.CONTRACT_INNOCENT:
    	    		case DatabaseInterface.CONTRACT_CULPABLE:
    	    			new ResponsePacket(1, Messages.CONTRACT_COMPLETED_MESSAGE);
    	    		default:
    	    			return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
    	    			
        		}
        	}
        	
        	Contract c = new Contract().loadFromHash(contractHash);
            Integer state;
            Integer expireTime = c.getDelay() == 0 ? -1 : (int) (c.getTimestamp()/1000) + c.getDelay()/1000;
            String logmsg = "Checked state for contract with HASH=" + contractHash + ": ";
        	
            state = c.getState();

            switch (state) {
                case DatabaseInterface.CONTRACT_LATENT:
                    logger.trace(logmsg + "LATENT");
                    lc.put(key, new Integer[] { DatabaseInterface.CONTRACT_LATENT , expireTime});
                    return new ResponsePacket(0, Messages.CONTRACT_LATENT_MESSAGE);

                case DatabaseInterface.CONTRACT_ON_DUTY:
                    logger.trace(logmsg + "ON_DUTY");
                    lc.put(key, new Integer[] {DatabaseInterface.CONTRACT_ON_DUTY});
                    return new ResponsePacket(1, Messages.CONTRACT_FUSED_MESSAGE);

                case DatabaseInterface.CONTRACT_OFF_DUTY:
                    logger.trace(logmsg + "OFF_DUTY");
                    lc.put(key, new Integer[] {DatabaseInterface.CONTRACT_OFF_DUTY});
                    return new ResponsePacket(1, Messages.CONTRACT_FUSED_MESSAGE);

                case DatabaseInterface.CONTRACT_INNOCENT:
                    logger.trace(logmsg + "INNOCENT");
                    lc.put(key, new Integer[] {DatabaseInterface.CONTRACT_INNOCENT});
                    return new ResponsePacket(1, Messages.CONTRACT_COMPLETED_MESSAGE);

                case DatabaseInterface.CONTRACT_CULPABLE:
                    logger.trace(logmsg + "CULPABLE");
                    lc.put(key, new Integer[] {DatabaseInterface.CONTRACT_CULPABLE});
                    return new ResponsePacket(1, Messages.CONTRACT_COMPLETED_MESSAGE);
                    
                case DatabaseInterface.CONTRACT_EXPIRED:
                    logger.trace(logmsg + "EXPIRED");
                    lc.put(key, new Integer[] {DatabaseInterface.CONTRACT_EXPIRED});
                    return new ResponsePacket(-2, Messages.CONTRACT_EXPIRED_MESSAGE);

                default:
                    logger.trace(logmsg + "ERROR");
                    return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
            }

        }
        catch (SQLException e) {

            logger.warn("Database exception thrown when checking fusion: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
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
    	
        try {
            // 2) Checking for valid auth data
            if (!DatabaseInterface.getInstance().authenticate(username, pass)) {
                logger.warn("Authentication error. Cannot accept USERNAME=" + username + " and hashed PASSWORD=" + Tools.hash256(pass) + "");
                
                return new ResponsePacket(-1, Messages.AUTH_FAILED);
            }
        }catch (SQLException e) {
            
            logger.error("Thrown SQL exception while opening database: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.DB_SELECT_FAILED);
        }
    	
    	return isFused(contractHash);
    }
    
    /*
    @POST
    @Produces(value="text/xml")
    @Path(value = "/depositTell") 
    public String tellContract(@FormParam("username") String username, @FormParam("pass") String pass,
            @FormParam("contract") String contractXML, @FormParam("randlong") String randLong, @FormParam("deposit") String deposit) {
        
        return null;
    }*/

    /** Given a contract and a compliant, fuses the two contracts.
     * 
     * @param db Database
     * @param contract XML of the user contract
     * @param compliant Identifier of the compliant contract found by server, it will be sent to the client via xml
     *            response
     * @return true if contracts are fused */
    private boolean fuse(DatabaseInterface db, String contract, String compliant, Integer contractID, Integer compliantID) {

        String input[] = new String[2], path, fileName, sessionHash;
        AppResponse ocamlResults;
        Integer contextID, sessionID;
        
        Contract c1, c2;
        try {
            c1 = new Contract().loadFromID(contractID);
            c2 = new Contract().loadFromID(compliantID);
        }
        catch (SQLException e) {

            logger.error(
                    "Cannot retrieve data for C1=" + contractID + " and C2=" + compliantID + ". SQL says: "
                            + e.getMessage());

            return false;
        }

        // 1) Creates Ocaml process to start session
        // 1a) Ocaml CTU needs a file where write output
        fileName = Tools.getTempFile(Tools.CTU_NETWORKS_PREFIX, Tools.EXTENSION_NETS);

        // 1b) Creates process
        path = Tools.getCtuPath()+ Tools.CTU_PARAM_START + " " + fileName;
        input[0] = compliant + "\n";
        input[1] = contract;
        ocamlResults = Tools.callApplication(path, input);
        
        if (ocamlResults.hasErrors()) {
        	
            logger.error(
                    "Error while creating the initial state for a session. CTU error: " + ocamlResults.getErrors());
            logger.trace("Ocaml filename: " + fileName);
            Tools.rm(fileName);
            return false;
        }
        
        sessionHash = Tools.hash256(c1.getContractHash() + c2.getContractHash());

        // 2) Adds new session to DB
        try {
            contextID = DatabaseInterface.getInstance().getIDFromContext(Tools.getDeclaredStringContext(contract));
            sessionID = db.insertSession(sessionHash, (Integer) DatabaseInterface.SESSION_ACTIVE, fileName, contextID);
        }
        catch (SQLException e) {

            logger.error("Cannot insert new session in database (when fusing), SQL says: " + e.getMessage());
            logger.trace("Ocaml filename: " + fileName);
            return false;
        }
        finally {
            Tools.rm(fileName);
        }

        // 3) Update state of contract and compliant contract.
        try {
            boolean c1_result, c2_result;
            Integer c1_progress = DatabaseInterface.CONTRACT_OFF_DUTY, c2_progress = DatabaseInterface.CONTRACT_OFF_DUTY;

            db.updateContract(contractID, sessionID, DatabaseInterface.CONTRACT_ROLE_1, c1_progress);
            db.updateContract(compliantID, sessionID, DatabaseInterface.CONTRACT_ROLE_0, c2_progress);

            // 6b) Checks who is on duty
            c1_result = new SessionMonitor().monitorContractProgress(db, c1.getContractHash(), Tools.CTU_PARAM_DUTY);
            logger.trace("First monitorContractProgress() completed without problems.");
            c2_result = new SessionMonitor().monitorContractProgress(db, c2.getContractHash(), Tools.CTU_PARAM_DUTY);
            logger.trace("Second monitorContractProgress() completed without problems.");
            
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

            db.setContractState(contractID, c1_progress);
            db.setContractState(compliantID, c2_progress);
            
            Cache<String, Integer[]> lc = MainApplication.getLatentCache();
            
            lc.put(c1.getContractHash(), new Integer[]{c1_progress});
            lc.put(c2.getContractHash(), new Integer[]{c2_progress});

            logger.info(
                    "Contract with ID=" + contractID + " and contract with ID=" + compliantID
                            + " have been fused in a new session with ID=" + sessionID + " and HASH="
                            + sessionHash);

            return true;
        }
        catch (InternalException e) {

        }
        catch (DBException e) {

            logger.error(
                    "Error while checking participant status in monitorContractProgress: " + e.getMessage());
        }
        catch (SQLException e) {

            logger.error("Failed updating a contract. SQL says: " + e.getMessage());
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

        try{
        // 2) Checks for valid user & pwd
            if (!DatabaseInterface.getInstance().authenticate(username, pass)) {
                logger.warn(
                        "Authentication error. Cannot accept USERNAME=" + username + " and hashed PASSWORD="
                                + Tools.hash256(pass) + "");
                
                throw new DBException(Messages.AUTH_FAILED);
            }
        } catch (SQLException e) {
        
            logger.error("Thrown SQL exception while opening database: " + e.getMessage());
            
            throw new DBException(Messages.AUTH_FAILED);
        }

        // 3) Verifies contract's owner
        if (!Tools.permissionContract(db, username, contractHash)) {
            logger.warn(
                    "Access denied: user with USERNAME=" + username
                            + " tried to access contract with CONTRACT_HASH=" + contractHash);

            throw new DBException(Messages.PERMISSION_DENIED);
        }

        // 4) Retrieves contract state
        String query = "SELECT state FROM contract WHERE contract_hash = '" + contractHash + "';";
        
        try (
                Connection connection = db.getDatasource().getConnection();
                Statement stmt = connection.createStatement();
                ) {
            ResultSet rs = stmt.executeQuery(query);
            rs.next();
            
            Integer result = rs.getInt(1);
            
            rs.close();
            return result;
        }
        catch (SQLException e) {

            logger.error(
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

	        if (!DatabaseInterface.getInstance().authenticate(username, pass)) {
	        	logger.warn(
	        			"Authentication error. Cannot accept USERNAME=" + username + " and hashed PASSWORD="
	                    + Tools.hash256(pass) + "");
	    
	            return new ResponsePacket(-1, Messages.AUTH_FAILED);
	        }

	        return new ResponsePacket(1, "Correct username and password pair.");
			
		} catch (SQLException e) {
			
            logger.error("SQL Exception thrown when verifying credentials: " + e.getMessage());
            
            return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
		}
    }
}