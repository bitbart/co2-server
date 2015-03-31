package it.unica.tcs;

import it.unica.tcs.InternalException;
import it.unica.tcs.Log;
import it.unica.tcs.Messages;
import it.unica.tcs.Tools;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;

@Path("/compliance")
public class ComplianceChecker {

	@POST
	@Path("/areCompliant")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ResponsePacket areCompliant(QueryPacket postData) {

		String outputOcaml = "", outputUppaal, outputUppaal_error;
        String[] input = new String[2];
        String path, fileName;
        
        String c1 = postData.getFirstContract();
        String c2 = postData.getSecondContract();

        try {
        	
            // 1) Checks XML input
            if (!Tools.getDeclaredStringContext(c1).equals(Tools.getDeclaredStringContext(c2)))
                return new ResponsePacket(-1, Messages.CONTRACT_SAME_CONTEXT);
            
            if (!Validator.localValidateXML(c1))
                return new ResponsePacket(-1, Messages.CONTRACT_INVALID);

            if (!Validator.localValidateXML(c2))
                return new ResponsePacket(-1, Messages.CONTRACT_INVALID);
        	
        }
        catch (InternalException iie) {

            Log.message().warning("InternalException thrown in localValidateXML: " + iie.getMessage());

            return new ResponsePacket(-1, iie.getMessage());
        }
        catch (FileNotFoundException fnfe) {

            Log.message().severe("File not found exception while validating both contracts:" + fnfe.getMessage());

            return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
        }

        // 2) Creates XML automata with Ocaml CTU
        input[0] = c1 + "\n";
        input[1] = c2 + "\n";
        outputOcaml = Tools.callApplication(Tools.PATH_CTU, input, false);

        try {
            // 2b) Saves XML automata (Uppaal software needs an input file)
            fileName = Tools.getFile(c1.concat(c2), Tools.PATH_CTU_CONS, Tools.EXTENSION_XML, true);
            PrintWriter p = new PrintWriter(fileName);
            p.print(outputOcaml);
            p.close();

        }
        catch (FileNotFoundException e) {

            Log.message().severe("File not found exception while trying to save Uppaal's XML:" + e.getMessage());
            return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
        }

        // 3) Tests automata with Uppaal software
        path = Tools.PATH_UPPAAL + fileName + Tools.UPPAAL_PARAMS;
        outputUppaal = Tools.callApplication(path, null, false);

        // 4) Returns XML response
        if (outputUppaal.contains("is satisfied")) {
            Log.message().info(
                    "Checked compliance for C1=" + Log.format(c1) + " and C2=" + Log.format(c2)
                            + ": they are compliant!");

            return new ResponsePacket(1, Messages.PROPERTY_YES);
        }
        else if (outputUppaal.contains("is NOT satisfied")) {
            Log.message().info(
                    "Checked compliance for C1=" + Log.format(c1) + " and C2=" + Log.format(c2)
                            + ": they aren't compliant!");

            return new ResponsePacket(0, Messages.PROPERTY_NO);
        }
        else {
            Log.message().severe(
                    "Checked compliance for C1=" + Log.format(c1) + " and C2=" + Log.format(c2)
                            + ": unknown response from Uppaal, more details below.");

            if (outputUppaal.isEmpty()) {

                outputUppaal_error = Tools.callApplication(Tools.PATH_CTU, input, true);
                Log.message().info("Command executed: " + path);
                Log.message().info("Uppaal response: " + outputUppaal_error);
            }
            else {
                Log.message().info("Command executed: " + path);
                Log.message().info("Uppaal response: " + outputUppaal);
            }

            return new ResponsePacket(-1, Messages.ERROR_GENERIC_INTERNAL);
        }
	}
}
