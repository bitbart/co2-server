package it.unica.tcs;

import java.sql.SQLException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import it.unica.tcs.database.DatabaseInterface;
import it.unica.tcs.logging.Log;

@Path("/debug")
public class Debug {
	
	@POST
	@Path("/trustguard")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public ResponsePacket trustGuard(QueryPacket postData) {
		
		DatabaseInterface db = DatabaseInterface.getInstance();
		
		Double[] ftv = new Double[4];
		Double[] ftv1;
		
		ftv[0] = 1.;
		ftv[1] = 2.;
		ftv[2] = 3.;
		ftv[3] = 4.;
		
		try {
			db.saveFTV(17584, ftv);
		} catch (SQLException e) {
			return new ResponsePacket(-1, "errore mysql saveFTV: " + e.getMessage());
		}
		
		try {
			ftv1 = db.getFTV("17584");
			
			if (ftv1[1] == 2.)
				return new ResponsePacket(1, "Il risultato � corretto");
			else
				return new ResponsePacket(-1, "Il risultato � sbagliato: " + ftv1[1]);
			
		} catch (SQLException e) {
			return new ResponsePacket(-1, "errore mysql getFTV: " + e.getMessage());
		}
	}

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
		
		DatabaseInterface db = DatabaseInterface.getInstance();
    	
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
        path = Tools.getCtuPath() + Tools.CTU_PARAM_TRANSLATE;
        
        input[0] = "?pay{;t}.(?ok & ?dispute{t<10; t}.!refund{t<7} & ?abort)"; // compliant
		
		AppResponse firstXml = Tools.callApplication(path, input);
        
        input[0] = firstXml.getOutput() + "\n";
        AppResponse firstMapping = Tools.callApplication(Tools.getCtuPath() + Tools.CTU_PARAM_BUILD_AUTOMATON, input);
        AppResponse firstLabels = Tools.callApplication(Tools.getCtuPath() + Tools.CTU_PARAM_GET_LABELS, input);
        
        input[0] = "?pay{;t}.(?ok & ?dispute{t<9; t}.!refund{t<7} & ?abort)"; // not compliant
		
		AppResponse secondXml = Tools.callApplication(path, input);
        
        input[0] = secondXml.getOutput() + "\n";
        AppResponse secondMapping = Tools.callApplication(Tools.getCtuPath() + Tools.CTU_PARAM_BUILD_AUTOMATON, input);
        AppResponse secondLabels = Tools.callApplication(Tools.getCtuPath() + Tools.CTU_PARAM_GET_LABELS, input);
        
        
        try {
	        for (int i=0; i<N; i++) {
	        	
	        	if (i % 10000 == 0)
	        		Log.message().info("During DB population, new 10.000 contracts had already been stored.");
	        	
	        	// 27230 is the id of testuser1
	        	if (Math.random() < K/N) 
	        		db.insertContract(Tools.hashContract(firstXml.getOutput(), MainApplication.getRand()), firstXml.getOutput(), 27230, 0, DatabaseInterface.CONTRACT_ROLE_LATENT, DatabaseInterface.CONTRACT_LATENT, new Long(MainApplication.getRand()), ComplianceChecker.getContractType(firstXml.getOutput()), firstMapping.getOutput(), firstLabels.getOutput(), 0); 
	        	else
	        		db.insertContract(Tools.hashContract(secondXml.getOutput(), MainApplication.getRand()), firstXml.getOutput(), 27230, 0, DatabaseInterface.CONTRACT_ROLE_LATENT, DatabaseInterface.CONTRACT_LATENT, new Long(MainApplication.getRand()), ComplianceChecker.getContractType(secondXml.getOutput()), secondMapping.getOutput(), secondLabels.getOutput(), 0);
	        }
        }
        catch (SQLException s) {
        	
        	Log.message().severe("SQL error when populating the DB: " + s.getMessage());
        }
        
		return new ResponsePacket(1, "Done!");
	}
}