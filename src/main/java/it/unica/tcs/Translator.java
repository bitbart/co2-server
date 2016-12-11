package it.unica.tcs;

import java.io.FileNotFoundException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unica.tcs.ctu.CTU;
import it.unica.tcs.ctu.CTUException;

/** Translates a string contract to a xml contract. */
@Path(value = "/translation")
public class Translator {

    private static final Logger logger = LoggerFactory.getLogger(Translator.class);
    
    @POST
    @Path(value = "translate")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket translate(QueryPacket postData) {

        String translated = null;
        String input = postData.getFirstContract(); // TODO: check the input
        
        logger.trace("Starting translator module.");

        try {
            // 1) Creates Ocaml process
            
            
            Integer attempts = 10;
            
            // TODO: (nicola) check if this loop is useful
            while (attempts > 0 && (translated == null)) {
                
                
                translated = CTU.tstToXml(input);
                
                if (translated.equals(""))
                    translated = null;
                
                attempts--;
            }
            
            if (translated == null) {
                
                logger.error("Error from CTU while translating contract C1=" + input + ": response is empty.");

                return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
            }
            else if (!translated.startsWith("<contract")) {
                
                logger.trace("Error from CTU while translating contract C1=" + input + ": " + translated);

                return new ResponsePacket(-1, translated);
            }

        }
        catch (CTUException e) {

            logger.error("CTU exception: " + e.getMessage());

            return new ResponsePacket(-1, Messages.ERROR_TRANSLATION);
        }
        catch (Exception e) {

            logger.error("Unknown error while translating contract C1=" + input + ": " + e.getMessage());

            return new ResponsePacket(-1, Messages.ERROR_TRANSLATION);
        }
        
        logger.trace("Translation step passed without errors!");

        try {
            // 2) Checks XML input
            if (!Validator.localValidateXML(translated))
                return new ResponsePacket(-1, Messages.ERROR_VALIDATION);

        }
        catch (InternalException iie) {

            logger.error("IllegalInputException thrown when calling localValidateXML: " + iie.getMessage());

            return new ResponsePacket(iie.getType(), iie.getMessage());
        }
        catch (FileNotFoundException fnfe) {

            logger.error("File not found exception while validating a translated contract: " + fnfe.getMessage());

            return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
        }
        
        logger.trace("Validating step passed without errors. Returning translated contract!");

        // Returns valid contract
        return new ResponsePacket(1, translated);
    }
    
    
    @POST
    @Path(value = "/xmlToString")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket xmlToString(QueryPacket postData) {
        
        String contract = postData.getFirstContract();
        
        // 1) Checks XML input
        try {
            if (!Validator.localValidateXML(contract))
                return new ResponsePacket(-1, Messages.CONTRACT_INVALID);
            
            
            String tst = CTU.xmlToTst(contract);
            logger.trace("Contract serialized: "+tst);
            return new ResponsePacket(1, tst);            
        }
        catch (InternalException iie) {
            
            logger.error("InternalException thrown in localValidateXML: " + iie.getMessage());

            return new ResponsePacket(iie.getType(), iie.getMessage());         
        }
        catch (FileNotFoundException fnfe) {
            
            logger.error("File not found exception while validating both contracts:" + fnfe.getMessage());
            
            return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
        }
        catch (CTUException e) {
            logger.error("Error during the serialization of the contract: "+e.getMessage());
            return new ResponsePacket(-1, Messages.ERROR_TO_STRING);
        }
    }
}
