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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import it.unica.tcs.InternalException.ErrorTypes;
import it.unica.tcs.database.DatabaseInterface;

public class Tools {
	
    private static final Logger logger = LoggerFactory.getLogger(Tools.class);
    private static final String CONF_PROPERTIES_PATH = "conf.properties";
    private static final Configuration config;
    private static final Random rng = new Random();

    static {
        Parameters params = new Parameters();
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder = 
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class);
        builder.configure(params.properties().setFileName(CONF_PROPERTIES_PATH));

        try {
            config = builder.getConfiguration();
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }

    }

    
	// CTU's (Convert To Uppaal) paths and files
	private static final String CTU_EXEC =         config.getString("ctu.exec");
	public static final String CTU_PATH_CONS =     config.getString("ctu.path.cons");
	public static final String CTU_PATH_NETS =     config.getString("ctu.path.nets");
	public static final String CTU_PATH_AUTOMATA = config.getString("ctu.path.automata");
	public static final String CTU_PATH_LABELS =   config.getString("ctu.path.labels");
	public static final String CTU_PARAM_TRANSLATE =           config.getString("ctu.param.translate");
	public static final String CTU_PARAM_BINDING =             config.getString("ctu.param.binding");
	public static final String CTU_PARAM_CULPABLE =            config.getString("ctu.param.culpable");
	public static final String CTU_PARAM_DUTY =                config.getString("ctu.param.duty");
	public static final String CTU_PARAM_START =               config.getString("ctu.param.start");
	public static final String CTU_PARAM_STEP =                config.getString("ctu.param.step");
	public static final String CTU_PARAM_ADMITS_COMPLIANT =    config.getString("ctu.param.admit-compliant");
	public static final String CTU_PARAM_KIND_OF =             config.getString("ctu.param.kind-of");
	public static final String CTU_PARAM_DUAL_OF =             config.getString("ctu.param.dual-of");
	public static final String CTU_PARAM_BUILD_AUTOMATON =     config.getString("ctu.param.build-automaton");
	public static final String CTU_PARAM_GET_LABELS =          config.getString("ctu.param.get-labels");
	public static final String CTU_PARAM_TO_STRING =           config.getString("ctu.param.to-string");
    
	// Uppaal's paths and files
	public static final String PATH_UPPAAL =   config.getString("uppaal.exec");
	public static final String UPPAAL_PARAMS = config.getString("uppaal.params");

	// Xmllint's path and files
	public static final String VALIDATOR_EXEC =            config.getString("validator.exec");
	public static final String VALIDATOR_FILE_PREFIX =     config.getString("validator.file-prefix");
	public static final String VALIDATOR_PATH_SCHEMA =     config.getString("validator.path.schema");
	public static final String VALIDATOR_PATH_FILES =      config.getString("validator.path.files");

	// File extensions
	public static final String EXTENSION_NETS = ".nets";
	public static final String EXTENSION_XML = ".xml";
	public static final String EXTENSION_TXT = ".txt";

	// Configuration
	public static final boolean CONF_MOVE_AFTER_CONTRACT_END = false;
	
	// Input checking
	public static final Regex USERNAME_REGEX = Regex.USERNAME_REGEX;
	public static final Regex PASSWORD_REGEX = Regex.PASSWORD_REGEX;
	public static final Regex XML_CONTRACT_REGEX = Regex.XML_CONTRACT_REGEX;
	
	public enum Regex {USERNAME_REGEX, PASSWORD_REGEX, XML_CONTRACT_REGEX}
	
	
	public static String getCtuPath() {		
		return CTU_EXEC + (rng.nextInt(4) + 1) + " ";
	}
	
	public static boolean isNotValid(String param, Regex type) {
	    
	    switch (type) {
	        
	        case USERNAME_REGEX: return false;
	        case PASSWORD_REGEX: return false;
	        case XML_CONTRACT_REGEX: return false;
	        default: return false;
	    }
	    
	}
	
	/* Chmods a file/dir */
	public static void chmod(String path) {
	    
	    callApplication("chmod 777 " + path, null);
	}
	
	/* Deletes all files and folders inside a specified directory */
	public static void rm(String dir) {
        
        callApplication("rm " + dir, null);
	}
	
	public static AppResponse callApplication(String path, String input[]) {
	
		//StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		
		//if (Log.isInitialized())
			//logger.warn("Caller: " + ste[2].getMethodName() + " | Path: " + path);
		
		String line;
	    
	    String[] response = new String[2];
	    response[0] = "";
	    response[1] = "";

	      // launch EXE and grab stdin/stdout and stderr
	    Process process = null;
		
	    try {
			process = Runtime.getRuntime ().exec (path);

	    
			try (
	                OutputStream stdin = process.getOutputStream ();
	                InputStream stderr = process.getErrorStream ();
	                InputStream stdout = process.getInputStream ();
			        BufferedReader brStdOut = new BufferedReader (new InputStreamReader (stdout));
			        BufferedReader brStdErr = new BufferedReader (new InputStreamReader (stderr));
	                ) {
			    
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
			    
			    while ((line = brStdOut.readLine ()) != null) {
			        response[0] += line;
			    }
			    brStdOut.close();
			    
			    // clean up if any output in stderr

			    while ((line = brStdErr.readLine ()) != null) {
			        response[1] += line;
			    }
			    brStdErr.close();
			}
	
		    
		} catch (IOException e) {
			
			logger.error("Cannot execute the process: " + path);
			logger.warn("Exception message: " + e.getMessage());
		}
	    
	    AppResponse ar = new AppResponse(response[0], response[1]);
	    
	    return ar;
	}
	
	public static void mysqlChown(String filename) {
		
		try {
			Path path = Paths.get(filename);
		    UserPrincipalLookupService lookupService = FileSystems.getDefault()
		        .getUserPrincipalLookupService();
		    UserPrincipal userPrincipal = lookupService.lookupPrincipalByName("mysql");
	
		    Files.setOwner(path, userPrincipal);
			
		} catch (IOException e) {
			logger.error("Can change the group owner of " + filename + " to mysql: " + e.getMessage());
		}
	}

	/** Creates a new filename. To avoid collisions, it uses random values (where the seed is an input param) and current
	 * time.
	 * 
	 * @param seedInput Input seed from the user to generate random values
	 * @param path Path where the file will be created
	 * @param create If true, create the file else create only the filename
	 * @return Name of the file required */
	public static String getFile(String seedInput, String path, String extension, boolean create) {
		
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
					f.setReadable(true, false);
					f.setWritable(true, false);

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

			return result.toString();
		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		return null;
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

	/** Verifies permission to use a contract.
	 * 
	 * @param db Application database
	 * @param user Client username
	 * @param contractHash Contract hash
	 * @return True if user is contract's owner */
    public static boolean permissionContract(DatabaseInterface db, String username, String contractHash) {

        Integer contractOwner, userClient;
        String key = username + "," + contractHash;

        Cache<String, Boolean> pc = MainApplication.getPermissionsCache();

        Boolean element = pc.get(key);

        if (element != null) {

            return element;
        }

        try {
            contractOwner = new Contract().loadFromHash(contractHash).getOwnerID();
            userClient = db.selectUserId(username);

            if (!contractOwner.equals(userClient)) {

                pc.put(key, false);
                return false;
            } else
                pc.put(key, true);
            return true;

        } catch (SQLException e) {

            logger.warn("Cannot determine if USER_EMAIL=" + username
                    + " is the owner of CONTRACT_HASH=" + contractHash + ". SQL says: " + e.getMessage());

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

			logger.info(
			        "A context retrieving was aborted because the passed contract (" + contract + ") have an error: " + e.getMessage());
			result = DatabaseInterface.CONTEXT_EMPTY_NAME;
		}
		return result;
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

        try (
                Connection connection = db.getDatasource().getConnection();
                Statement stmt = connection.createStatement();
                ) {
            
            ResultSet result;
            Integer sessionID;
            String query, sessionHash;
            
            // 2b) Load session.
            query = "SELECT session_id FROM contract WHERE contract_hash='" + contractHash + "';";
            result = stmt.executeQuery(query);
            result.next();
            sessionID = result.getInt(1);
            result.close();
            
            query = "SELECT session_hash FROM session WHERE session_id=" + sessionID;
            result = stmt.executeQuery(query);
            result.next();
            sessionHash = result.getString(1);
            result.close();
            
            // 2d) Load network
            query = "SELECT last_state FROM session WHERE session_hash='" + sessionHash + "' INTO DUMPFILE '" + fileName
                    + "';";
            result = stmt.executeQuery(query);
            result.close();
            
            result.close();
            
            return true;
        }
        
    }

	/** Given an action and a contractHash, it verifies if is possible do the action in the contract.
	 * 
	 * @param db Database application
	 * @param contractHash Hash of the contract where do the action
	 * @param action Name of the action
	 * @return True if action is allowed in this context, false otherwise */
	public static boolean actionAllowed(DatabaseInterface db, String contractHash, String action) {

		Integer contextID = -1;
		
		// 2) Retrieves contract context
		try {
			contextID = new Contract().loadFromHash(contractHash).getContextID();

			// 3) Get the context's actions
			Set<String> actions = db.selectContextActions(contextID);
			
			// 4) Returns result
			return actions.contains(action);
		}
		catch (SQLException e) {

			logger.warn("The contract with HASH='" + contractHash + "' and CONTEXT_ID="+ contextID + " can't perform the action with NAME='" + action + "'. SQL says: " + e.getMessage());
			
			return false;
		}
		
	}

	/** Given an action, it verifies if the action is done.
	 * 
	 * @param db Database application
	 * @param action Name of the action
	 * @param value Value of the action
	 * @param contextID Id of the context
	 * @param user Username
	 * @param hash Hash of the contract
	 * @param sessionID Hash of the session
	 * @return True if the action is done, false otherwise
	 * @throws SQLException */
	public static boolean verifyAction(DatabaseInterface db, String action, String value, Integer contextID, String user, String hash, Integer sessionID) throws SQLException, InternalException {

		// Checks if action exists
		
		Pair<Integer, String> res = db.selectActionIdAndVerificationLink(action, contextID);
		
		Integer actionID = res.getLeft();
		String verificationURL = res.getRight();
		
		if (verificationURL.equals("true"))
			return true;
		else if(verificationURL.equals("false"))
			throw new InternalException(ErrorTypes.TYPE_ACTION_CULPABLE);
		
		
		String verifierResponse = null;
		
		// 3) Calls verificationLink
		try {
			URL url = new URL(verificationURL + "?user=" + user + "&hash=" + hash + "&action=" + action + "&value=" + value + "&session=" + sessionID);
			URLConnection connection = url.openConnection();
			connection.connect();
			
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))
                    ) {
                verifierResponse = in.readLine();
            }
		}
		catch (Exception e) {
			
			logger.error("The verifier cannot check if it is possible to perform ACTION=" + actionID + " with VALUE=" + "" + ". The returned exception is: " + e.getMessage());
			return false;
		}
		
		if (verifierResponse == null) {
			
			logger.error("The verifier returned a <i>null</i> response when validating the ACTION=" + actionID + " with VALUE=" + "" + ".");
			return false;
		}
		
		logger.info("Response:" + verifierResponse);
		
		if (verifierResponse.equals("true"))
			return true;
		
		return false;
	}
}
