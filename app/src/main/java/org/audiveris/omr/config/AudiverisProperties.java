//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               A u d i v e r i s P r o p e r t i e s                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
package org.audiveris.omr.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralized configuration for Audiveris.
 * Loads {@code audiveris.properties} from classpath, overridable by system properties.
 */
public class AudiverisProperties
{
    private static final Properties props = new Properties();

    static {
        try (InputStream in = AudiverisProperties.class.getResourceAsStream(
                "/org/audiveris/config/audiveris.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
            // Use defaults
        }
    }

    // ─── Key constants ────────────────────────────────────

    public static final String KEY_DIRECTORY_STORAGE = "omr.storage.directory";
    public static final String KEY_LOAD_THREAD_POOL_SIZE = "omr.load.threadPoolSize";
    public static final String KEY_MEMORY_UNLOAD_THRESHOLD = "omr.memory.unloadThreshold";

    // ─── Accessors ────────────────────────────────────────

    /** Whether to use directory-based storage (true) or ZIP (.omr) files. */
    public static boolean isDirectoryStorage ()
    {
        return getBoolean(KEY_DIRECTORY_STORAGE, false);
    }

    /** Number of threads used for parallel sheet loading. */
    public static int getLoadThreadPoolSize ()
    {
        return getInt(KEY_LOAD_THREAD_POOL_SIZE, 2);
    }

    /** Memory usage ratio (0.0 - 1.0) that triggers sheet unloading. */
    public static double getMemoryUnloadThreshold ()
    {
        return getDouble(KEY_MEMORY_UNLOAD_THRESHOLD, 0.85);
    }

    // ─── Internal helpers ─────────────────────────────────

    private static boolean getBoolean (String key,
                                        boolean defaultValue)
    {
        String val = System.getProperty(key, props.getProperty(key));
        return val != null ? Boolean.parseBoolean(val) : defaultValue;
    }

    private static int getInt (String key,
                                int defaultValue)
    {
        String val = System.getProperty(key, props.getProperty(key));
        try {
            return val != null ? Integer.parseInt(val) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static double getDouble (String key,
                                      double defaultValue)
    {
        String val = System.getProperty(key, props.getProperty(key));
        try {
            return val != null ? Double.parseDouble(val) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
