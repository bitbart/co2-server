package it.unica.tcs;

import java.io.FileNotFoundException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Translates a string contract to a xml contract. */
@Path(value = "/translation")
public class Translator {

    private static final Logger logger = LoggerFactory.getLogger(Translator.class);
    
    @POST
    @Path(value = "translate")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket translate(QueryPacket postData) {

        String[] input = new String[1];
        String translated = null;
        String path;
        input[0] = postData.getFirstContract(); // TODO: check the input
        
        AppResponse ar;
        
        //logger.trace("Starting translator module.");

        try {
            // 1) Creates Ocaml process
            path = Tools.getCtuPath()+ Tools.CTU_PARAM_TRANSLATE;
            
            Integer attempts = 10;
            
            while (attempts > 0 && (translated == null)) {
                
                ar = Tools.callApplication(path, input); // TODO: split
                
                translated = ar.getOutput() + ar.getErrors();
                
                if (translated.equals(""))
                    translated = null;
                
                attempts--;
            }
            
            if (translated == null) {
                
                logger.error("Error from CTU while translating contract C1=" + input[0] + ": response is empty.");

                return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
            }
            else if (!translated.startsWith("<contract")) {
                
                ar = Tools.callApplication(path, input);
                
                translated = ar.getErrors();

                logger.trace("Error from CTU while translating contract C1=" + input[0] + ": " + translated);

                return new ResponsePacket(-1, translated);
            }

        }
        catch (Exception e) {

            logger.error("Unknown error while translating contract C1=" + input[0] + ": " + e.getMessage());

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
}
