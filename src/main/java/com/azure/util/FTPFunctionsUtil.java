package com.azure.util;

import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * Util class for use useful methods used by multiple functions.
 */
public class FTPFunctionsUtil {

    /**
     * Interprets the input properties file using the current thread. If properties file is not
     * found the function will return null.
      * @param propertiesFile Path to the properties file.
     * @return map representing all the keys and values in the properties file.
     */
    public static Map<String, String> getPropertiesMap(String propertiesFile) {
        try {
            Properties properties = new Properties();
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            properties.load(loader.getResourceAsStream(propertiesFile));
            Map<String, String> mapOfProperties = Maps.fromProperties(properties);
            return mapOfProperties;
        } catch (IOException e) {
            System.out.println("Unable to find properties file: " + propertiesFile);
            return null;
        }
    }
}
