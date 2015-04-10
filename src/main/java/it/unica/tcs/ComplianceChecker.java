package it.unica.tcs;

import it.unica.tcs.InternalException;
import it.unica.tcs.Log;
import it.unica.tcs.Messages;
import it.unica.tcs.Tools;

import java.util.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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
	
	 /** Given a contract, it look for a compliant contract stored in db.
     * 
     * @param db Database
     * @param contractID Identifier of the input contract
     * @param contractXML The contract
     * @param contextID Context of the contract, to optimize database search
     * @param preCheckType A value used to simplify the compliance calculus.
     * @return A BasicPair with the ID and the XML string of the compliant contract found (an empty pair otherwise)
     * 
     * @throws SQLException
     * @throws FileNotFoundException */
    public static BasicPair<Integer, String> getCompliant(DatabaseInterface db, String contractXML, String mapping, String chanList, Integer contextID, String preCheckType)
                throws SQLException, FileNotFoundException {

        boolean compliant = false;
        String otherContractXML, queryText, otherPreCheckType, otherMapping, otherChanList;
        ResultSet rs, rs2;
        Integer otherID;

        BasicPair<Integer, String> compliantData = new BasicPair<Integer, String>();
        List<Quadruple<Double, String[], Integer, Integer>> preCheckCalculus = new ArrayList<Quadruple<Double, String[], Integer, Integer>>();

        // 1) Takes all identifiers of the contracts to check
        queryText = "SELECT contract_id, contract_xml, type_pre_check, mapping, aux FROM `" + DatabaseInterface.TABLE_CONTRACT
                + "` WHERE context_id = " + contextID + " AND state = 0 ORDER BY rand();";
        rs = db.select(queryText);

        // 2) For every id get probability of compliance
        //    If the probability of compliance is bigger than zero, get the other contract's owner reputation
        while (rs.next()) {
            otherID = rs.getInt("contract_id");
            otherContractXML = rs.getString("contract_xml");
            otherMapping = rs.getString("mapping");
            otherChanList = rs.getString("aux"); // For TSTs, the channel list is stored in the auxiliary column
            otherPreCheckType = rs.getString("type_pre_check");

            double preCheckValue = preCheck(preCheckType, otherPreCheckType);
            
            if (preCheckValue > 0) {
                queryText = "SELECT reputation FROM " + DatabaseInterface.TABLE_CONTRACT + " JOIN " + DatabaseInterface.TABLE_USER + " ON owner_id = user_id WHERE contract_id = " + otherID;
                rs2 = db.select(queryText);
                
                if(rs2.next()){
                    
                	int otherReputation = rs2.getInt("reputation");
                    
                	// TODO: change the Quadruple to BasicPair<Double, Contract>, where Contract stores all data like the following, and String contains the precheck value
                    String[] contractData = new String[3];
                    contractData[0] = otherContractXML;
                    contractData[1] = otherMapping;
                    contractData[2] = otherChanList;
                    
                    preCheckCalculus.add(new Quadruple<Double, String[], Integer, Integer>(preCheckValue, contractData, otherID, otherReputation));
                }
            }
        }
        
        Log.message().fine("Finding a compliant for C1='" + Log.format(contractXML) + "'. The size of the precheck list is " + preCheckCalculus.size() + ".");

        // 3) Sort the contracts captured by the probability value calculated and the other contract's owner reputation.
        Collections.sort(preCheckCalculus, new Comparator<Quadruple<Double, String[], Integer, Integer>>() {

            @Override
            public int compare(Quadruple<Double, String[], Integer, Integer> t1, Quadruple<Double, String[], Integer, Integer> t2) {

                int preCheckComparison = t1.getFirst().compareTo(t2.getFirst());
                if (preCheckComparison == 0) {
                    return t1.getFourth().compareTo(t2.getFourth());
                } else {
                    return preCheckComparison;
                }
            }
        });
        
        Log.message().fine("Precheck list sorted.");

        // 4) For each element, it tries if it is compliant with the given contract.
        //    Get a normally distributed index (x' = 0, sigma = 0.2), check if the corresponding contract is compliant.
        //    If it is not, remove the corresponding contract from the list and iterate       
        NormalGenerator ng = new NormalGenerator(0.2);
        
        while(!compliant && !preCheckCalculus.isEmpty()) {
        	
            int index = ng.next(preCheckCalculus.size());
            
            Quadruple<Double, String[], Integer, Integer> element = preCheckCalculus.get(index);
            
            compliant = localAreCompliant(mapping, chanList, element.getSecond()[1], element.getSecond()[2]);
            
            Log.message().fine("The checked contract has ID=" + element.getThird() + ". The compliance result is " + (compliant ? "yes" : "no") + ".");
            
            if(compliant){
                compliantData.set(element.getThird(), element.getSecond()[0]);
            } else {
                preCheckCalculus.remove(index);
            }
            
            //Log.message().fine("New precheck list size: " + preCheckCalculus.size());
        
        }
        
        Log.message().fine("Compliant search finished, returning the results.");

        // If compliant isn't found, return empty data
        return compliantData;
    }

    /** Checks compliance between two contracts.
     * 
     * @param c1 XML first contract
     * @param c2 XML second contract
     * @return True if contracts are compliant or error message
     * @throws FileNotFoundException */
    public static boolean localAreCompliant(String mapping1, String chanList1, String mapping2, String chanList2) throws FileNotFoundException {

        String fileName, fusedMapping, path, outputUppaal;
        
        fusedMapping = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
        				"<!DOCTYPE nta PUBLIC '-//Uppaal Team//DTD Flat System 1.1//EN' 'http://www.it.uu.se/research/group/darts/uppaal/flat-1_2.dtd'>\n" +
        				"<nta>\n<declaration>" + mergeChannels(chanList1, chanList2) + "</declaration>\n<template>\n<name> AnyName1</name>\n" + mapping1 +
        				"</template>\n\n<template>\n<name> AnyName2</name>\n" + mapping2 + "</template>\n<system>\np =  AnyName1();\nq =  AnyName2();\n" +
        				"system p, q;\n</system>\n</nta>";
          
          /*<queries> --> controllare se UPPAAL accetta la query insieme al sistema. Altrimenti aggiornare UPPAAL 
            <query>
              <formula>A[] not deadlock</formula>
              <comment></comment>
            </query>
           </queries>
        </nta>*/

        // 2) Saves XML automata (Uppaal software needs an input file)
        fileName = Tools.getFile(mapping1.concat(mapping2), Tools.PATH_CTU_CONS, Tools.EXTENSION_XML, true);
        Tools.chmod(fileName);
        
        Log.message().info("Checking compliance for two contracts, the UPPAAL template is stored in " + fileName);

        PrintWriter p = new PrintWriter(fileName);
        p.print(fusedMapping);
        p.close();

        // 3) Tests automata with Uppaal software
        path = Tools.PATH_UPPAAL + fileName + Tools.UPPAAL_PARAMS;

        outputUppaal = Tools.callApplication(path, null, false);

        // Remove the temp file
        //Tools.callApplication("rm " + fileName, null, false);

        // 4) Returns XML response
        if (outputUppaal.contains("is satisfied"))
            return true;
        else
            return false;
    }

    /** Analyzes the type of a contract to simplify the (future) compliance calculus.
     * 
     * @param contractXML The contract
     * @return The type of the contract */
    public static String getContractType(String contractXML) {

        String name, typePreCheck;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(contractXML));
            Document doc = builder.parse(is);

            doc.getDocumentElement().normalize();

            name = doc.getFirstChild().getFirstChild().getNodeName();
            Log.message().info("Type PreCheck: FirstElement: " + doc.getFirstChild().getNodeName() + "; SecondElement: " + name + ";");

            switch (name) {
                case "intchoice":
                case "intaction":
                    typePreCheck = "1";
                    break;
                case "extchoice":
                case "extaction":
                    typePreCheck = "-1";
                    break;
                default:
                    typePreCheck = "0";
            }

            return typePreCheck;
        }
        catch (ParserConfigurationException | IOException | SAXException e) {

            Log.message().warning("Unknow error while analyzing DOM: " + e.getMessage());

            return Messages.ERROR_GENERIC_INTERNAL;
        }
    }

    /** Given two contracts, it returns the probability that they are compliant.
     * 
     * @param c1 the first contract
     * @param c2 the other contract
     * @return probability of compliance */
    public static Double preCheck(String typePreCheckC1, String typePreCheckC2) {

        return 0.5 - ((Double.parseDouble(typePreCheckC1) * Double.parseDouble(typePreCheckC2)) / 2); // P(a, b) = 0.5 -
                                                                                                      // (a * b / 2)
    }
    
    private static String mergeChannels(String channels1, String channels2) {
    	
    	HashSet<String> merged = new HashSet<String>();
    	String result = "";
    	
    	channels1 = channels1.replace("\n", "").replace("\r", "").replace(" ", "");
    	channels2 = channels2.replace("\n", "").replace("\r", "").replace(" ", "");
    	
    	String[] chanList1 = channels1.split(",");
    	String[] chanList2 = channels2.split(",");
    	
    	for (int i=0; i < chanList1.length; i++) {
    		
    		if (!chanList1[i].equals(""))
    			merged.add(chanList1[i]);
    	}
    	
    	for (int j=0; j < chanList2.length; j++) {
    		
    		if (!chanList2[j].equals(""))
    			merged.add(chanList2[j]);
    	}
    	
    	for (String s : merged) {
    	    
    		result += "chan " + s + ";\n";
    	}
    	
    	return result;
    }
}
