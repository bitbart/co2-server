package it.unica.tcs.conf;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class Props {

    private static final String CONF_PROPERTIES_PATH = "conf.properties";
    private static final Configuration config;

    static {
        Parameters params = new Parameters();
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder = 
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class);
        builder.configure(params.properties().setFileName(CONF_PROPERTIES_PATH));

        try {
            config = builder.getConfiguration();
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }

    }

    public static void main(String[] args) {
        System.out.println(config.getClass());
        System.out.println(config.getString("home"));
        System.out.println(config.getString("base-dir"));
        System.out.println(config.getString("ctu.exec"));
        System.out.println(config.getString("ctu.exec"));
    }
}
