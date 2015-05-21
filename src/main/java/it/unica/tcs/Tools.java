package it.unica.tcs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class Tools {
	
	private static Random rng;
    
    public static final String HOME_DIR = "/home/debianadmin";
//  public static final String CO2_DIR = "/var/lib/mysql/tmp/co2_server";

	// CTU's (Convert To Uppaal) paths and files
	public static final String PATH_CTU = HOME_DIR + "/ctu";
//	public static final String PATH_CONS = CO2_DIR + "/cons";
	public static final String PATH_CTU_CONS = HOME_DIR + "/tmp/cons/OcamlContracts_";
//	public static final String PATH_NETS = CO2_DIR + "/nets";
	public static final String PATH_CTU_NETS = HOME_DIR + "/tmp/nets/OcamlNetworks_";
//	public static final String PATH_AUTOMATA = CO2_DIR + "/automata";
	public static final String PATH_CTU_AUTOMATA = HOME_DIR + "/tmp/nets/OcamlAutomata_";
//	public static final String PATH_LABELS = CO2_DIR + "/labels";
	public static final String PATH_CTU_LABELS = HOME_DIR + "/tmp/nets/OcamlLabels_";
	public static final String CTU_PARAM_TRANSLATE = "-s";
	public static final String CTU_PARAM_BINDING = "-v";
	public static final String CTU_PARAM_CULPABLE = "-ic";
	public static final String CTU_PARAM_DUTY = "-id";
	public static final String CTU_PARAM_START = "-start";
	public static final String CTU_PARAM_STEP = "-step";
	public static final String CTU_PARAM_ADMITS_COMPLIANT = "-da";
	public static final String CTU_PARAM_KIND_OF = "-dk";
	public static final String CTU_PARAM_DUAL_OF = "-dd";
	public static final String CTU_PARAM_BUILD_AUTOMATON = "-ba";
	public static final String CTU_PARAM_GET_LABELS = "-gl";

	// Uppaal's paths and files
	public static final String PATH_UPPAAL = HOME_DIR + "/uppaal/bin-Linux/verifyta ";
	public static final String UPPAAL_PARAMS = " " + HOME_DIR + "/uppaal/bin-Linux/test_compliance.q"; // | grep -e '--' | cut -d'-' -f 3"; // do not remove the initial space

	// Xmllint's path and files
	public static final String PATH_VALIDATOR = "xmllint --schema ";
	public static final String PATH_VALIDATOR_FILES = HOME_DIR + "/tmp/validatorFiles/";
	public static final String VALIDATOR_NAME_FILES = "ValidatorInput_";
	public static final String PATH_VALIDATOR_SCHEMA = HOME_DIR + "/validator_sources/ContractSchema.xsd";

	// File extensions
	public static final String EXTENSION_NETS = ".nets";
	public static final String EXTENSION_XML = ".xml";
	public static final String EXTENSION_TXT = ".txt";

	// TODO: Configuration properties should be written in a configuration file ...
	// Configuration
	public static final boolean CONF_MOVE_AFTER_CONTRACT_END = false;
	
	
	// Input checking
	public static final Integer USERNAME_REGEX = 0;
	public static final Integer PASSWORD_REGEX = 1;
	public static final Integer XML_CONTRACT_REGEX = 2;
	
	public static String getCtuPath() {
		
		return PATH_CTU + MainApplication.getCtuID() + " ";
	}
	
	public static boolean isNotValid(String param, Integer type) {
	    
	    switch (type) {
	        
	        case 0: return false;
	        case 1: return false;
	        case 2: return false;
	        default: return false;
	    }
	    
	}
	
	/* Chmods a file/dir */
	public static void chmod(String path) {
	    
	    callApplication("chmod 777 " + path, null);
	}
	
	/* Creates a directory in the server */
	public static void mkdir(String dir) {
	    
	    callApplication("mkdir " + dir, null);
	}
	
	/* Deletes all files and folders inside a specified directory */
	public static void rm(String dir) {
        
        callApplication("rm " + dir, null);
	}
	/* OLD 
	/** Launches a new process on server and returns process response. 
	 * Throws a TimeExpiredException if process takes too much time.
	 * 
	 * @param path Path and name of the process to be executed
	 * @param input Arguments array needed by the process.
	 * @param errorStream If set as true then the method returns the errorStream; otherwise returns the outputStream
	 * @return Process response *
	public static String callApplication(String path, String[] input, boolean errorStream) {

		StringBuffer outputApplication = new StringBuffer();
		BufferedReader reader;
		Process p;

		String line = "", output;
		long tStart, tEnd, tDelta;

		tStart = System.currentTimeMillis();

		try {

			String[] cmd = { "/bin/sh", "-c", path };

			// Launches a new process
			p = Runtime.getRuntime().exec(cmd); // TODO Kill the process

			if (errorStream)
				reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			else
				reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

			// Sends input
			if (input != null) {

				OutputStream printW = p.getOutputStream();

				for (int i = 0; i < input.length; i++) {

					printW.write((input[i]).getBytes());
					printW.flush();
				}

				printW.close();
			}

			// Takes output process
			while ((line = reader.readLine()) != null) {

				outputApplication.append(line + "\n");
			}
			reader.close();

			// Verifies process state
			tEnd = System.currentTimeMillis();
			tDelta = tEnd - tStart;

			if ((tDelta / 1000.0) < 5.0)
				output = outputApplication.toString();
			else 
				output = "";
			// TODO: Time management

		}
		catch (Exception e) {

			output = "Exception in the process " + path;
		}

		return output;
	}*/
	
	public static AppResponse callApplication(String path, String input[]) {
	
		//StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		
		//if (Log.isInitialized())
			//Log.message().warning("Caller: " + ste[2].getMethodName() + " | Path: " + path);
		
		String line;
	    OutputStream stdin = null;
	    InputStream stderr = null;
	    InputStream stdout = null;
	    
	    String[] response = new String[2];
	    response[0] = "";
	    response[1] = "";

	      // launch EXE and grab stdin/stdout and stderr
	    Process process = null;
		
	    try {
			process = Runtime.getRuntime ().exec (path);

	    
		    stdin = process.getOutputStream ();
		    stderr = process.getErrorStream ();
		    stdout = process.getInputStream ();
	
		    // "write" the parms into stdin
		    if (input != null) {
		    	
			    for (int i=0; i<input.length; i++) {
			    	
				    line = input[i] + "\n";
				    stdin.write(line.getBytes() );
				    stdin.flush();
			    }
		    }
		    stdin.close();
	
		    // clean up if any output in stdout
		    BufferedReader brCleanUp = new BufferedReader (new InputStreamReader (stdout));
		    
		    while ((line = brCleanUp.readLine ()) != null) {
		    	response[0] += line;
		    }
		    brCleanUp.close();
	
		    // clean up if any output in stderr
		    brCleanUp = new BufferedReader (new InputStreamReader (stderr));
		    while ((line = brCleanUp.readLine ()) != null) {
		    	response[1] += line;
		    }
		    brCleanUp.close();
		    
		} catch (IOException e) {
			
			Log.message().severe("Cannot execute the process: " + path);
			Log.message().warning("Exception message: " + e.getMessage());
		}
	    
	    AppResponse ar = new AppResponse(response[0], response[1]);
	    
	    return ar;
	}

	/** Creates a new filename. To avoid collisions, it uses random values (where the seed is an input param) and current
	 * time.
	 * 
	 * @param seedInput Input seed from the user to generate random values
	 * @param path Path where the file will be created
	 * @param create If true, create the file else create only the filename
	 * @return Name of the file required */
	public static String getFile(String seedInput, String path, String extension, boolean create) {
		
		if (rng == null)
			rng = new Random();

		boolean validFileName = false;
		String fileName;
		File f;

		do {
			// Creates filename
			fileName = path + (seedInput.hashCode()) + "_" + rng.nextLong() + extension;
			f = new File(fileName);

			// Checks if the file already exists.
			if ((!f.exists()) && (create)) {
				try {
					f.createNewFile();

				}
				catch (IOException e) {}

				validFileName = true;

			}
			else if (!f.exists()) 
			    validFileName = true;

		}
		while (!validFileName);

		return fileName;
	}

	/** Given a String generates its hash.
	 * 
	 * @param data Input string to hash.
	 * @return Hash generated. */
	public static String hash256(String data) {

		MessageDigest md;
		StringBuffer result = null;

		try {
			md = MessageDigest.getInstance("SHA-256");

			md.update(data.getBytes());
			byte[] bytes = md.digest();

			result = new StringBuffer();

			for (byte byt : bytes)
				result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));

		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return result.toString();
	}

	/** Generate a unique contract hash for a given XML contract.
	 * 
	 * @param contract The XML contract to be hashed.
	 * @param timestamp The timestamp (as Long).
	 * @return Generated hash. */
	public static String hashContract(String contract, Long timestamp) {

		String tmp = contract.replaceAll("\\n", "").replaceAll("\\s", "");

		return hash256(tmp + timestamp);
	}

	/** An alias for hashContract(String, Long).
	 * 
	 * @param contract The XML contract to be hashed.
	 * @param timestamp The timestamp (as String).
	 * @return Generated hash. */
	public static String hashContract(String contract, String timestamp) {

		return hashContract(contract, new Long(timestamp));
	}

	/** Verifies authentication user data.
	 * 
	 * @param db Application database
	 * @param user Client username
	 * @param pass Client password
	 * @return True if user credentials are correct */
	public static boolean authenticate(DatabaseInterface db, String user, String pass) throws SQLException {

		boolean returnValue = false;
		String key = user + "," + pass;
		
		Cache<String, Boolean> cc = MainApplication.getCredentialsCache();
		
		Boolean element = cc.get(key);
		
		if (element != null) {
			
			return element;
		}

		// Checks if exists one and only one row in User table that matches passed values
		ResultSet rs = db.select("SELECT COUNT(*) FROM user WHERE email = '" + user + "' AND password = '"
		        + Tools.hash256(pass) + "';");

		rs.next();

		if (rs.getInt(1) == 1) 
		    returnValue = true;
		
		cc.put(key, returnValue);

		return returnValue;
	}

	/** Verifies permission to use a contract.
	 * 
	 * @param db Application database
	 * @param user Client username
	 * @param contractHash Contract hash
	 * @return True if user is contract's owner */
	public static boolean permissionContract(DatabaseInterface db, String username, String contractHash) {

		ResultSet rs;
		Integer contractOwner, userClient;
		String key = username + "," + contractHash;
		
		Cache<String, Boolean> pc = MainApplication.getPermissionsCache();
		
		Boolean element = pc.get(key);
		
		if (element != null) {
			
			return element;
		}

		try {
			contractOwner = new Contract().loadFromHash(contractHash).getOwnerID();

			rs = db.select("SELECT user_id FROM user WHERE email = '" + username + "';");
			rs.next();
			userClient = rs.getInt(1);

			if (!contractOwner.equals(userClient)) {
				
				pc.put(key, false);
				return false;
			}
			else
				pc.put(key, true);
				return true;

		}
		catch (SQLException e) {

			Log.message().warning(
			        "Cannot determine if USER_EMAIL=" + Log.format(username) + " is the owner of CONTRACT_HASH="
			                + Log.format(contractHash) + ". SQL says: " + e.getMessage());

			return false;
		}
	}

	/** Given a contract, returns its declared context.
	 * 
	 * @param contract Contract to check.
	 * @return A string with the name of the context; the empty string indicates the default context. */
	public static String getDeclaredStringContext(String contract) {

		String result = Messages.ERROR_XML_PARSING;

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(contract));
			Document doc = builder.parse(is);

			// optional, but recommended
			// http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			result = doc.getDocumentElement().getAttribute("context");

			if (result.equals("")) result = DatabaseInterface.CONTEXT_EMPTY_NAME;

		}
		catch (Exception e) {

			Log.message().info(
			        "A context retrieving was aborted because the passed contract (" + contract + ") have an error: " + e.getMessage());
			result = DatabaseInterface.CONTEXT_EMPTY_NAME;
		}
		return result;
	}

	/** Given a context, returns its identifier.
	 * 
	 * @param db Database
	 * @param context Name of the context
	 * @return The identifier of the context */
	public static Integer getIDFromContext(DatabaseInterface db, String context) {

		ResultSet exists;
		Integer contextID;

		try {
			exists = db.select("SELECT context_id FROM context WHERE name = '" + context + "';");
			exists.next();
			contextID = exists.getInt(1);
			
		}
		catch (SQLException e) {
		    
			return 0;
		}

		return contextID;
	}

	/** Given a fused contract, retrieves its state from db in a specific file, created using the input filename.
	 * 
	 * @param db Database application
	 * @param contractHash Hash of the fused contract
	 * @param fileName Name of the file that will be created
	 * @return True if the the network is loaded correctly
	 * @throws SQLException */
	public static boolean loadNetworkFromDB(DatabaseInterface db, String contractHash, String fileName)
	        throws SQLException {

		ResultSet result;
		Integer sessionID;
		String query, sessionHash;

		// 2b) Load session.
		query = "SELECT session_id FROM contract WHERE contract_hash='" + contractHash + "';";
		result = db.select(query);
		result.next();
		sessionID = result.getInt(1);

		query = "SELECT session_hash FROM session WHERE session_id=" + sessionID;
		result = db.select(query);
		result.next();
		sessionHash = result.getString(1);

		// 2d) Load network
		query = "SELECT last_state FROM session WHERE session_hash='" + sessionHash + "' INTO DUMPFILE '" + fileName
		        + "';";
		result = db.select(query);

		return true;
	}

	/** Given an action and a contractHash, it verifies if is possible do the action in the contract.
	 * 
	 * @param db Database application
	 * @param contractHash Hash of the contract where do the action
	 * @param action Name of the action
	 * @return True if action is allowed in this context, false otherwise */
	public static boolean actionAllowed(DatabaseInterface db, String contractHash, String action) {

		String query;
		Integer contextID = -1;
		int count = -1;
		ResultSet result;
		
		// 2) Retrieves contract context
		try {
			contextID = new Contract().loadFromHash(contractHash).getContextID();

			// 3) Checks if action is allowed in context
			query = "SELECT COUNT(*) FROM context_action AS ca JOIN action AS a ON ca.action_id = a.action_id WHERE context_id='" + contextID + "' AND name='" + action + "';";
			result = db.select(query);
			result.next();
			count = result.getInt(1);
		}
		catch (SQLException e) {

			Log.message().warning("The contract with HASH='" + contractHash + "' and CONTEXT_ID="+ contextID + " can't perform the action with NAME='" + action + "'. SQL says: " + e.getMessage());
			
			return false;
		}
		
		// 4) Returns result
		if (count == 1)
			return true;
		else
			return false;
	}

	/** Given an action, it verifies if the action is done.
	 * 
	 * @param db Database application
	 * @param action Name of the action
	 * @return True if the action is done, false otherwise
	 * @throws SQLException */
	public static boolean verifyAction(DatabaseInterface db, String action, String value, Integer contextID) throws SQLException {

		String query, verificationURL, verifierResponse;
		ResultSet rs;
		Integer actionID;

		// Checks if action exists
		query = "SELECT A.action_id, verification_link FROM action AS A LEFT JOIN context_action AS CA ON A.action_id = CA.action_id WHERE name='" + action + "' AND context_id='" + contextID + "';";
		rs = db.select(query);
		rs.next();
		actionID = rs.getInt(1);
		verificationURL = rs.getString(2);
		
		if (verificationURL.equals("true"))
			return true;
		
		verifierResponse = null;

		// 3) Calls verificationLink
		try {
			URL url = new URL(verificationURL + "?value=" + value + "&action=" + action);
			URLConnection connection = url.openConnection();
			connection.connect();
			
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			verifierResponse = in.readLine();
			in.close();
		}
		catch (Exception e) {
			
			Log.message().severe("The verifier cannot check if it is possible to perform ACTION=" + actionID + " with VALUE=" + Log.format("") + ". The returned exception is: " + e.getMessage());
			return false;
		}
		
		if (verifierResponse == null) {
			
			Log.message().severe("The verifier returned a <i>null</i> response when validating the ACTION=" + actionID + " with VALUE=" + Log.format("") + ".");
			return false;
		}
		
		if (verifierResponse.equals("true"))
			return true;
		
		return false;
	}
}
