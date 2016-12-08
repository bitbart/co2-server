package it.unica.tcs.conf;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class Props {

    private static PropertiesConfiguration properties;
    
    static {
    }
    
    
    public static void main(String[] args) {
        Parameters params = new Parameters();
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
            new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
            .configure(params.properties()
                .setFileName("conf.properties"));
        try
        {
            Configuration config = builder.getConfiguration();
            System.out.println(config.getString("home"));
            System.out.println(config.getString("home-dir"));
            System.out.println(config.getString("ctu.path"));
            System.out.println(config.getString("ctu.path"));
            
        }
        catch(ConfigurationException cex)
        {
            // loading of the configuration file failed
        }
        
        
    }
}
