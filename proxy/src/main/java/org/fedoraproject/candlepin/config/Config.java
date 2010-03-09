/**
 * Copyright (c) 2009 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.fedoraproject.candlepin.config;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Defines the default Candlepin configuration
 */
public class Config {
    public static final File CONFIG_DIR = new File("/etc/candlepin");
    protected File CONFIG_FILE = new File(CONFIG_DIR, "candlepin.conf");
    protected static TreeMap<String, String> configuration = null;

    /**
     * Return configuration entry for the given prefix.
     * @param prefix prefix for the entry sought.
     * @return configuration entry for the given prefix.
     */
    public Map<String, String> configurationWithPrefix(String prefix) {
        if (configuration == null) {
            loadConfiguration();
        }
        return configuration.subMap(prefix, prefix + Character.MAX_VALUE);
    }

    /**
     * Returns all of the entries with the given prefix.
     * @param prefix part of the configuration key being sought.
     * @return all of the entries with the given prefix.
     */
    public Properties getNamespaceProperties(String prefix) {
        return this.getNamespaceProperties(prefix, null);
    }

    /**
     * Returns all of the entries with the given prefix, preloading the values
     * contained in the given defaults. The default list is also filtered by
     * prefix as well. For example, if you pass in a prefix of "a.c" and the
     * defaults map contains a key that beings with "b.d" it WILL not be added
     * to the returned Properties.
     * @param prefix part of the configuration key being sought.
     * @param defaults default values you'd like to see defined.
     * @return all of the entries with the given prefix.
     */
    public Properties getNamespaceProperties(String prefix,
        Map<String, String> defaults) {

        Properties p = new Properties();

        if (defaults != null) {
            for (String key : defaults.keySet()) {
                if (key != null && key.startsWith(prefix)) {
                    p.put(key, defaults.get(key));
                }
            }
        }
        p.putAll(configurationWithPrefix(prefix));
        return p;
    }

    /**
     * Returns the JPA Configuration properties.
     * @return the JPA Configuration properties.
     */
    public Properties jpaConfiguration() {
        if (configuration == null) {
            loadConfiguration();
        }
        return new JPAConfigParser().parseConfig(configuration);
    }

    /**
     * Returns the Database Basic Authentication Configuration properties
     * @return the Database Basic Authentication Configuration properties
     */
    public Properties dbBasicAuthConfiguration() {
        if (configuration == null) {
            loadConfiguration();
        }
        return new DbBasicAuthConfigParser().parseConfig(configuration);
    }

    protected synchronized void loadConfiguration() {
        if (configuration == null) {
            initializeMap();
        }
    }

    protected void initializeMap() {
        // start with the default values
        configuration = new TreeMap<String, String>(ConfigProperties.DEFAULT_PROPERTIES);

        // override with user-specified values
        configuration.putAll(loadProperties());
    }

    protected Map<String, String> loadProperties() {
        try {
            return new ConfigurationFileLoader().loadProperties(CONFIG_FILE);
        }
        catch (IOException e) {
            throw new RuntimeException("Problem loading candlepin configuration file.", e);
        }
    }

    /**
     * @param s configuration key
     * @return value associated with the given configuration key.
     */
    public String getString(String s) {
        return configuration.get(s);
    }

    /**
     * Get the configuration entry for the given string name.  If the value
     * is null, then return the given defValue.  defValue can be null as well.
     * @param name name of property
     * @param defValue default value for property if it is null.
     * @return the value of the property with the given name, or defValue.
     */
    public String getString(String name, String defValue) {
        String ret = getString(name);
        if (ret == null) {
            ret = defValue;
        }
        return ret;
    }
    
    /**
     * get the config entry for string s
     *
     * @param s string to get the value of
     * @return the value as an array
     */
    public String[] getStringArray(String s) {
        if (s == null) {
            return null;
        }
        String value = getString(s);

        if (value == null) {
            return null;
        }

        return value.split(",");
    }
}
