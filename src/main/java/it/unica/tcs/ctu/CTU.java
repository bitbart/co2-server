package it.unica.tcs.ctu;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Random;

import org.apache.commons.io.FileUtils;

import it.unica.tcs.AppResponse;
import it.unica.tcs.Tools;
import it.unica.tcs.conf.Config;

/**
 * CTU wrapper interface
 * 
 * @author nicola
 *
 */
public class CTU {
    
    private static final Random rand = new Random();

    // CTU's (Convert To Uppaal) paths and files
    private static final String CTU_EXEC =          Config.configuration.getString("ctu.exec");
    private static final String CTU_PATH_CONS =     Config.configuration.getString("ctu.path.cons");
    private static final String CTU_PATH_NETS =     Config.configuration.getString("ctu.path.nets");
    private static final String CTU_PATH_AUTOMATA = Config.configuration.getString("ctu.path.automata");
    private static final String CTU_PATH_LABELS =   Config.configuration.getString("ctu.path.labels");
    
    private static final String CTU_PARAM_XML_TO_TST =          Config.configuration.getString("ctu.param.xml-to-tst");
    private static final String CTU_PARAM_TST_TO_XML =          Config.configuration.getString("ctu.param.tst-to-xml");
    private static final String CTU_PARAM_VALIDATE =            Config.configuration.getString("ctu.param.validate");
    
    private static final String CTU_PARAM_CULPABLE =            Config.configuration.getString("ctu.param.culpable");
    private static final String CTU_PARAM_DUTY =                Config.configuration.getString("ctu.param.duty");
    private static final String CTU_PARAM_START =               Config.configuration.getString("ctu.param.start");
    private static final String CTU_PARAM_STEP =                Config.configuration.getString("ctu.param.step");
    private static final String CTU_PARAM_ADMITS_COMPLIANT =    Config.configuration.getString("ctu.param.admit-compliant");
    private static final String CTU_PARAM_KIND_OF =             Config.configuration.getString("ctu.param.kind-of");
    private static final String CTU_PARAM_DUAL_OF =             Config.configuration.getString("ctu.param.dual-of");
    private static final String CTU_PARAM_BUILD_AUTOMATON =     Config.configuration.getString("ctu.param.build-automaton");
    private static final String CTU_PARAM_GET_LABELS =          Config.configuration.getString("ctu.param.get-labels");

    
    private static String getCtuPath() {     
        return CTU_EXEC + (rand.nextInt(4) + 1) + " ";
    }
    
    public static String xmlToTst(String xmlContract) {
        
        AppResponse result = Tools.callApplication(getCtuPath()+CTU_PARAM_XML_TO_TST, xmlContract);
        
        checkForErrors(result);
        
        return result.getOutput()+"\n";
    } 
    
    public static String tstToXml(String tstContract) {
        AppResponse result = Tools.callApplication(getCtuPath()+CTU_PARAM_TST_TO_XML, tstContract);
        
        checkForErrors(result);
        
        return result.getOutput()+"\n";
    }
    
    public static boolean validate(String xmlContract) {
        AppResponse result = Tools.callApplication(getCtuPath()+CTU_PARAM_VALIDATE, xmlContract);
        
        checkForErrors(result);
        
        return !result.isEmpty() && result.getOutput().contains("Contract is valid");
    }
    
    public static String contractsToUppaalAutomaton(String... contracts) {
        
        AppResponse result = Tools.callApplication(getCtuPath(), contracts);
        
        checkForErrors(result);
        
        return result.getOutput()+"\n";
    }
    
    public static boolean checkCompliance(String c1, String c2) {
        
        // generate an automaton for Uppaal
        String automaton = CTU.contractsToUppaalAutomaton(c1, c2);
        
        // get a new temporary file to store the automaton
        String fileName = Tools.getTempFile(Tools.CTU_CONTRACTS_PREFIX, Tools.EXTENSION_XML);
        
        try {
            //store the automaton
            FileUtils.writeStringToFile(new File(fileName), automaton, Charset.forName("UTF-8"));
        }
        catch (IOException e) {
            throw new CTUException("IOException while trying to save Uppaal's XML into '"+fileName+"'", e);
        }
        
        // tests automaton with Uppaal software
        String path = Tools.PATH_UPPAAL +" "+ fileName +" "+ Tools.UPPAAL_PARAMS;
        AppResponse outputUppaal = Tools.callApplication(path);

        // remove the temporary file
        Tools.rm(fileName);
        
        // check the result
        if (outputUppaal.getOutput().contains("is satisfied")) {
            return true;
        }
        else if (outputUppaal.getOutput().contains("is NOT satisfied")) {
            return false;
        }
        else {
            
            StringBuilder log = new StringBuilder();
            log.append("Checked compliance for C1=" + c1 + " and C2=" + c2 + ": unknown response from Uppaal, more details below.");

            if (!outputUppaal.hasErrors()) {

                log.append("Command executed: " + path);
                log.append("Uppaal response: " + outputUppaal.getErrors());
            }
            else {
                log.append("Command executed: " + path);
                log.append("Uppaal response: " + outputUppaal.getOutput());
            }
            
            throw new CTUException(log.toString());
        }
    }
    
    private static void checkForErrors(AppResponse result) {
        if (result.hasErrors()) {
            throw new CTUException("Error: "+result.getErrors());
        }
        
        if (result.getOutput().isEmpty()) {
            throw new CTUException("Error: output is empty");
        }
    }
}
