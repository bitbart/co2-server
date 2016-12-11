package it.unica.tcs.conf;

import java.io.File;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.io.FileUtils;

public class Config {

    public static final Configuration configuration;
    private static final String CONF_PROPERTIES_PATH = "conf.properties";

    static {
        Parameters params = new Parameters();
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder = 
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class);
        builder.configure(params.properties().setFileName(CONF_PROPERTIES_PATH));

        try {
            configuration = builder.getConfiguration();
            
            FileUtils.forceMkdir(new File(configuration.getString("tmp-dir")));
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
