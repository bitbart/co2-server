package it.unica.tcs;

import java.io.FileNotFoundException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/** Translates a string contract to a xml contract. */
@Path(value = "/translation")
public class Translator {

    @POST
    @Path(value = "translate")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public ResponsePacket translate(QueryPacket postData) {

        String[] input = new String[1];
        String translated = null;
        String path;
        input[0] = postData.getFirstContract(); // TODO: check the input
        
        Log.message().finest("Starting translator module.");

        try {
            // 1) Creates Ocaml process
            path = Tools.PATH_CTU + Tools.CTU_PARAM_TRANSLATE;
            
            Integer attempts = 10;
            
            while (attempts > 0 && (translated == null)) {
                
                translated = Tools.callApplication(path, input, false) + Tools.callApplication(path, input, true); // TODO: split
                
                if (translated.equals(""))
                    translated = null;
                
                attempts--;
            }
            
            if (translated == null) {
                
                Log.message().severe("Unknown response from CTU while translating contract C1=" + Log.format(input[0]) + ": response is empty.");

                return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
            }
            else if (!translated.startsWith("<contract")) {
                
                translated = Tools.callApplication(path, input, true);

                Log.message().severe("Unknown response from CTU while translating contract C1=" + Log.format(input[0]) + ": " + translated);

                return new ResponsePacket(-1, translated);
            }

        }
        catch (Exception e) {

            Log.message().severe("Unknown error while translating contract C1=" + Log.format(input[0]) + ": " + e.getMessage());

            return new ResponsePacket(-1, Messages.ERROR_TRANSLATION);
        }
        
        Log.message().finest("Translation step passed without errors!");

        try {
            // 2) Checks XML input
            if (!Validator.localValidateXML(translated))
                return new ResponsePacket(-1, Messages.ERROR_VALIDATION);

        }
        catch (InternalException iie) {

            Log.message().severe("IllegalInputException thrown when calling localValidateXML: " + iie.getMessage());

            return new ResponsePacket(iie.getType(), iie.getMessage());
        }
        catch (FileNotFoundException fnfe) {

            Log.message().severe("File not found exception while validating a translated contract: " + fnfe.getMessage());

            return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
        }
        
        Log.message().fine("Validating step passed without errors. Returning translated contract!");

        // Returns valid contract
        return new ResponsePacket(1, translated);
    }
}
