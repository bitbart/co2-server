package it.unica.tcs.ctu;

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

    // CTU's (Convert To Uppaal) paths and files
    private static final String CTU_EXEC =         Config.configuration.getString("ctu.exec");
    private static final String CTU_PATH_CONS =     Config.configuration.getString("ctu.path.cons");
    private static final String CTU_PATH_NETS =     Config.configuration.getString("ctu.path.nets");
    private static final String CTU_PATH_AUTOMATA = Config.configuration.getString("ctu.path.automata");
    private static final String CTU_PATH_LABELS =   Config.configuration.getString("ctu.path.labels");
    
    private static final String CTU_PARAM_XML_TO_TST =           Config.configuration.getString("ctu.param.xml-to-tst");
    private static final String CTU_PARAM_TST_TO_XML =           Config.configuration.getString("ctu.param.tst-to-xml");
            
    private static final String CTU_PARAM_BINDING =             Config.configuration.getString("ctu.param.binding");
    private static final String CTU_PARAM_CULPABLE =            Config.configuration.getString("ctu.param.culpable");
    private static final String CTU_PARAM_DUTY =                Config.configuration.getString("ctu.param.duty");
    private static final String CTU_PARAM_START =               Config.configuration.getString("ctu.param.start");
    private static final String CTU_PARAM_STEP =                Config.configuration.getString("ctu.param.step");
    private static final String CTU_PARAM_ADMITS_COMPLIANT =    Config.configuration.getString("ctu.param.admit-compliant");
    private static final String CTU_PARAM_KIND_OF =             Config.configuration.getString("ctu.param.kind-of");
    private static final String CTU_PARAM_DUAL_OF =             Config.configuration.getString("ctu.param.dual-of");
    private static final String CTU_PARAM_BUILD_AUTOMATON =     Config.configuration.getString("ctu.param.build-automaton");
    private static final String CTU_PARAM_GET_LABELS =          Config.configuration.getString("ctu.param.get-labels");
    private static final String CTU_PARAM_TO_STRING =           Config.configuration.getString("ctu.param.to-string");

    
    
    public static String xmlToTst(String xmlContract) {
        
        AppResponse result = Tools.callApplication(CTU_EXEC, CTU_PARAM_XML_TO_TST);
        
        checkForErrors(result);
        
        return result.getOutput()+"\n";
    } 
    
    public static String tstToXml(String tstContract) {
        AppResponse result = Tools.callApplication(CTU_EXEC, CTU_PARAM_TST_TO_XML);
        
        checkForErrors(result);
        
        return result.getOutput()+"\n";
    }
    
    private static void checkForErrors(AppResponse result) {
        if (result.hasErrors()) {
            throw new CTUException("Error: "+result.getErrors());
        }
    }
}
