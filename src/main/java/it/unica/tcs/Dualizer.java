package it.unica.tcs;

import java.io.FileNotFoundException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

@Path(value = "/dualize")
public class Dualizer {

	@POST
	@Path(value = "/admitsCompliant")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ResponsePacket admitsCompliant(QueryPacket postData) {
		
		String contract = postData.getFirstContract();
		
		// 1) Checks XML input
		try {
			if (!Validator.localValidateXML(contract))
			    return new ResponsePacket(-1, Messages.CONTRACT_INVALID);
		}
		catch (InternalException iie) {
			
			Log.message().warning("InternalException thrown in localValidateXML: " + iie.getMessage());

			return new ResponsePacket(iie.getType(), iie.getMessage());			
		}
		catch (FileNotFoundException fnfe) {
			
			Log.message().severe("File not found exception while validating both contracts:" + fnfe.getMessage());
			
			return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
		}

		if (localAdmitsCompliant(contract)){
			
			return new ResponsePacket(1, Messages.CONTRACT_ADMITS_COMPLIANT);
			
		} else {
			
			return new ResponsePacket(0, Messages.CONTRACT_DOESNT_ADMITS_COMPLIANT);
			
		}
	}

	@POST
	@Path(value = "/kindof")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ResponsePacket kindOf(QueryPacket postData) {
		
		String path, standardOutputOcaml;
		String[] input = new String[1];	
		
		try {
			// 1) Checks XML input
			if (!Validator.localValidateXML(postData.getFirstContract()))
			    return new ResponsePacket(-1, Messages.CONTRACT_INVALID);
		}
		catch (InternalException iie) {
			
			Log.message().warning("InternalException thrown in localValidateXML: " + iie.getMessage());
			
			return new ResponsePacket(iie.getType(), iie.getMessage());	
		}
		catch (FileNotFoundException fnfe) {
			
			Log.message().severe("File not found exception while validating both contracts:" + fnfe.getMessage());
			
			return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
		}

		path = Tools.PATH_CTU + Tools.CTU_PARAM_KIND_OF;
		
		input[0] = postData.getFirstContract();
		standardOutputOcaml = Tools.callApplication(path, input, false);
		
		return new ResponsePacket(1, standardOutputOcaml);
	}

	@POST
	@Path(value = "/dualof")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ResponsePacket dualOf(QueryPacket postData) {
		
		String standardOutputOcaml;
		
		try {
			
			// Checks XML input
			if (!Validator.localValidateXML(postData.getFirstContract()))
			    return new ResponsePacket(-1, Messages.CONTRACT_INVALID);
		}
		catch (InternalException iie) {
			
			Log.message().warning("InternalException thrown in localValidateXML: " + iie.getMessage());
			
			return new ResponsePacket(iie.getType(), iie.getMessage());	
		}
		catch (FileNotFoundException fnfe) {
			
			Log.message().severe("File not found exception while validating both contracts:" + fnfe.getMessage());
			return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
		}

		standardOutputOcaml = localDualOf(postData.getFirstContract());
		
		return new ResponsePacket(1, standardOutputOcaml);
	}
	
	public static boolean localAdmitsCompliant(String contract) {
		
		String path, standardOutputOcaml;
		String[] input = new String[1];	

		path = Tools.PATH_CTU + Tools.CTU_PARAM_ADMITS_COMPLIANT;
		input[0] = contract;
		standardOutputOcaml = Tools.callApplication(path, input, false);
		
		// TODO: check this when CTU won't return a result
		if (standardOutputOcaml.equals("")) {
			return true;
		}
		
		if(standardOutputOcaml.contains("yes")) 
			return true;
		else 
			return false;
	}
	
	public String localDualOf(String contract) {
		
		String path, standardOutputOcaml;
		String[] input = new String[1];	

		path = Tools.PATH_CTU + Tools.CTU_PARAM_DUAL_OF;
		input[0] = contract;
		standardOutputOcaml = Tools.callApplication(path, input, false);


        // Returns valid dual
		return standardOutputOcaml;
	}

	/** Given a contract, returns its deual.
	 * 
	 * @param contract Contract to check.
	 * @return The dual contract. */
	public static String getXMLDual(String contract) {
		
		String result = Messages.ERROR_XML_PARSING;

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			InputSource is = new InputSource(new StringReader(contract));
			Document doc;
			
			doc = builder.parse(is);

			// optional, but recommended
			// http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			Element contractNode = (Element) doc.getElementsByTagName("contract").item(0);
			contractNode.setAttribute("dual", "1");

			Transformer tf;
			tf = TransformerFactory.newInstance().newTransformer();
			tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			Writer out = new StringWriter();
			tf.transform(new DOMSource(doc), new StreamResult(out));

			return out.toString();

		} catch (Exception e) {
			Log.message().warning("A dual building was aborted because the passed contract (" + contract + ") have an error: " + e.getMessage());
		}

		return result;
	}
}
