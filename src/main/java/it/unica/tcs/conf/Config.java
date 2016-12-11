package it.unica.tcs.conf;

import java.io.File;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.io.FileUtils;

public class Config {

    private static final String CONF_PROPERTIES_PATH = "conf.properties";

    public static final Configuration configuration;
    public static final File tmpDirFile;

    static {
        Parameters params = new Parameters();
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder = 
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class);
        builder.configure(params.properties().setFileName(CONF_PROPERTIES_PATH));

        try {
            configuration = builder.getConfiguration();
            
            tmpDirFile = new File(configuration.getString("tmp-dir"));
            FileUtils.forceMkdir(tmpDirFile);
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    
}
