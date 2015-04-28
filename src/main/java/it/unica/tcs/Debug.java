package it.unica.tcs;

import java.sql.SQLException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/debug")
public class Debug {

	@POST
	@Path("/tellStandard")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public ResponsePacket tellStandard(QueryPacket postData) {
		
    	String password = postData.getPassword();
    	
    	if (!password.equals("5de4d51a172d1db82e818d2be49957ed")) {
    		
    		Log.message().severe("Someone try to access the tellStandard() with a bad password.");
    		return new ResponsePacket(-1, "Your IP has been registered. Your violation will be reported to the Judicial Authority.");
    	}
		
		DatabaseInterface db = MainApplication.getDBConnection();
    	
    	try {
			db.deleteContracts();
			
		} catch (SQLException e) {
			
			Log.message().severe("Database cleaning failed: " + e.getMessage());
			return new ResponsePacket(-1, "Cannot delete anything from the DB.");
		}
		
		Double N = Double.valueOf(postData.getFirstContract());
		Double K = Double.valueOf(postData.getSecondContract());
		
		if (N == 0 && K == 0)
			Log.message().warning("Cleaning the database (except for CoRe sessions and contracts).");
		else
			Log.message().warning("Populating the DB with N=" + N + " and K=" + K + ".");
		
        String[] input = new String[1];
        String path;
        path = Tools.PATH_CTU + Tools.CTU_PARAM_TRANSLATE;
        
        input[0] = "?pay{;t}.(?ok & ?dispute{t<10; t}.!refund{t<7} & ?abort)"; // compliant
		
		String firstXml = Tools.callApplication(path, input, false);
        
        input[0] = firstXml + "\n";
        String firstMapping = Tools.callApplication(Tools.PATH_CTU + Tools.CTU_PARAM_BUILD_AUTOMATON, input, false);
        String firstLabels = Tools.callApplication(Tools.PATH_CTU + Tools.CTU_PARAM_GET_LABELS, input, false);
        
        input[0] = "?pay{;t}.(?ok & ?dispute{t<9; t}.!refund{t<7} & ?abort)"; // not compliant
		
		String secondXml = Tools.callApplication(path, input, false);
        
        input[0] = secondXml + "\n";
        String secondMapping = Tools.callApplication(Tools.PATH_CTU + Tools.CTU_PARAM_BUILD_AUTOMATON, input, false);
        String secondLabels = Tools.callApplication(Tools.PATH_CTU + Tools.CTU_PARAM_GET_LABELS, input, false);
        
        try {
	        for (int i=0; i<N; i++) {
	        	
	        	if (Math.random() < K/N) 
	        		db.insertContract(Tools.hashContract(firstXml, MainApplication.getRand()), firstXml, 1, 0, DatabaseInterface.CONTRACT_ROLE_LATENT, DatabaseInterface.CONTRACT_LATENT, new Long(MainApplication.getRand()), ComplianceChecker.getContractType(firstXml), firstMapping, firstLabels); 
	        	else
	        		db.insertContract(Tools.hashContract(secondXml, MainApplication.getRand()), firstXml, 1, 0, DatabaseInterface.CONTRACT_ROLE_LATENT, DatabaseInterface.CONTRACT_LATENT, new Long(MainApplication.getRand()), ComplianceChecker.getContractType(secondXml), secondMapping, secondLabels);
	        }
        }
        catch (SQLException s) {
        	
        	Log.message().severe("SQL error when populating the DB: " + s.getMessage());
        }
		return new ResponsePacket(1, "Done!");
	}
}