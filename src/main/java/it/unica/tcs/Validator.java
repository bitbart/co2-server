package it.unica.tcs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import it.unica.tcs.InternalException.ErrorTypes;
import it.unica.tcs.ctu.CTU;
import it.unica.tcs.database.DBException;
import it.unica.tcs.database.DatabaseInterface;

/** Verifies the syntax of the XML contracts. */
@Path(value = "/validation")
public class Validator {

    private static final Logger logger = LoggerFactory.getLogger(Validator.class);
    
    @POST
	@Path(value = "/validate")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ResponsePacket validate(QueryPacket postData) {

		String contract = postData.getFirstContract();

		try {
			
			// Verifies the syntax of the XML contract
			
			if (localValidateXML(contract))
				return new ResponsePacket(1, Messages.CONTRACT_VALID);
			else
				return new ResponsePacket(0, Messages.CONTRACT_INVALID);

		}
		catch (InternalException iie) {

			logger.error("Illegal input in validateXML: " + iie.getMessage());

			return new ResponsePacket(iie.getType(), iie.getMessage());

		}
		catch (FileNotFoundException fnfe) {

			logger.error("File not found exception in validateXML: " + fnfe.getMessage());

			return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);

		}
	}

	/** Verifies the syntax of the XML contract Returns a string with the response.
	 * 
	 * @param contract XML to be validated
	 * @return Response string
	 * @throws DBException
	 * @throws FileNotFoundException */
	public static boolean localValidateXML(String contract) throws FileNotFoundException, InternalException {

		String fileName, context, contextMessage;
		
		AppResponse outputXmllint;
		DatabaseInterface db = DatabaseInterface.getInstance();

		// 1) Checks null or empty input
		if (contract == null) 
			throw new InternalException(ErrorTypes.TYPE_NULL_CONTRACT);

		if (contract.equals("")) 
			throw new InternalException(ErrorTypes.TYPE_EMPTY_CONTRACT);
		
		if (!contract.startsWith("<contract")) 
            return false;

		// 2) Checks context of variables
		context = Tools.getDeclaredStringContext(contract);

		if (!context.equals(DatabaseInterface.CONTEXT_EMPTY_NAME)) {

			contextMessage = validateContext(db, contract);

			if (!contextMessage.equals(Messages.TYPE_SUCCESS)) 
				throw new InternalException(ErrorTypes.TYPE_CONTEXT_ERROR); //TODO
		}

		// 3) Checks binding variables
		
		boolean valid = CTU.validate(contract);
		
		if (!valid) {
		    return false;
		}
		
		// 4) Checks syntax with xmllint
		// 4a) Creates a temp file with the contract (xmllint needs an input file)
		fileName = Tools.getTempFile((Tools.VALIDATOR_PATH_FILES + Tools.VALIDATOR_FILE_PREFIX), Tools.EXTENSION_XML);
		PrintWriter p = new PrintWriter(fileName);
		p.print(contract);
		p.close();

		// 4b) Creates xmllint process
		String path = Tools.VALIDATOR_EXEC + " " + Tools.VALIDATOR_PATH_SCHEMA + " " + fileName;
		outputXmllint = Tools.callApplication(path, null); // Pay attention. It uses the errorStream as output
		
		//logger.error("OUTPUT: " + outputXmllint.getOutput() + " | ERRORS: " + outputXmllint.getErrors());

		// Remove the temp file
		Tools.rm(fileName);
		
		logger.trace("xmllint output: {}",outputXmllint.getErrors());
		
		if (outputXmllint.getErrors().contains("validates"))  //TODO check for bugs
			return true;

		return false;
	}

	/** Checks if actions_id are valid respect to the context.
	 * 
	 * @param db Database
	 * @param contract Contract that needs to validate
	 * @return Message success or message error */
	public static String validateContext(DatabaseInterface db, String contract) {
		
		logger.trace("ValidateContext of: " + StringEscapeUtils.escapeXml(contract));

		Set<String> elementsFound = new HashSet<>();
		NodeList intaction, extaction;
		String temp, contextNameDeclared;
		Integer contextID;

		// 1) Gets contract context
		contextNameDeclared = Tools.getDeclaredStringContext(contract);

		try {
		    contextID = db.selectContextId(contextNameDeclared);
		    
		} catch (SQLException e) {
		    logger.error("Error retrieving contextID: " + e.getMessage());

		    return Messages.DB_SELECT_FAILED;
		}
		
		// 2) Gets (from contract) all actions done
		if (!contextID.equals(DatabaseInterface.CONTEXT_EMPTY_ID)) {

			try (
			        StringReader contractReader = new StringReader(contract)
			        ){
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				InputSource is = new InputSource(contractReader);
				Document doc = builder.parse(is);

				// optional, but recommended
				// http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
				doc.getDocumentElement().normalize();
				intaction = doc.getElementsByTagName("intaction");
				extaction = doc.getElementsByTagName("extaction");

			}
			catch (ParserConfigurationException | IOException | SAXException e) {

				logger.error("Unknow error while analyzing DOM: " + e.getMessage());

				return Messages.ERROR_GENERIC_INTERNAL;
			}

			for (int x = 0, size = intaction.getLength(); x < size; x++) {
				temp = intaction.item(x).getAttributes().getNamedItem("id").getNodeValue();
				elementsFound.add(temp);
			}

			for (int x = 0, size = extaction.getLength(); x < size; x++) {
				temp = extaction.item(x).getAttributes().getNamedItem("id").getNodeValue();
				elementsFound.add(temp);
			}

			// 3) Gets (from db) all the actions allowed for this context
			try {

			    Set<String> elementsAllowed = db.selectContextActions(contextID);
			    
				
			    // 4) Compares all actions done with the actions allowed
			    for (String element : elementsFound) {
			        if (!elementsAllowed.contains(element)) 
			            return Messages.CONTRACT_ACTION_CONTEXT;
			    }
			}
			catch (SQLException e) {

				logger.error(
				        "Failed SELECT for checking if an action belong to a context. SQL says: " + e.getMessage());
				return Messages.CONTRACT_ACTION_CONTEXT;
			}

		}

		return Messages.TYPE_SUCCESS;
	}
}
