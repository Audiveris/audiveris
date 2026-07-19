//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                        T e s t A u d i v e r i s P r o p e r t i e s                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
package org.audiveris.omr.config;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Test;

/**
 * Tests for AudiverisProperties: defaults and system property overrides.
 */
public class TestAudiverisProperties
{
    @After
    public void tearDown ()
    {
        System.clearProperty(AudiverisProperties.KEY_DIRECTORY_STORAGE);
        System.clearProperty(AudiverisProperties.KEY_LOAD_THREAD_POOL_SIZE);
        System.clearProperty(AudiverisProperties.KEY_MEMORY_UNLOAD_THRESHOLD);
    }

    @Test
    public void testDefaultValues ()
    {
        assertFalse("Default directory storage should be false",
                AudiverisProperties.isDirectoryStorage());
        assertEquals("Default thread pool size should be 2",
                2, AudiverisProperties.getLoadThreadPoolSize());
        assertEquals("Default memory threshold should be 0.85",
                0.85, AudiverisProperties.getMemoryUnloadThreshold(), 0.001);
    }

    @Test
    public void testSystemPropertyOverride ()
    {
        System.setProperty(AudiverisProperties.KEY_DIRECTORY_STORAGE, "true");
        assertTrue("Should return true when overridden",
                AudiverisProperties.isDirectoryStorage());

        System.setProperty(AudiverisProperties.KEY_LOAD_THREAD_POOL_SIZE, "4");
        assertEquals("Thread pool size should be overridden to 4",
                4, AudiverisProperties.getLoadThreadPoolSize());

        System.setProperty(AudiverisProperties.KEY_MEMORY_UNLOAD_THRESHOLD, "0.7");
        assertEquals("Threshold should be overridden to 0.7",
                0.7, AudiverisProperties.getMemoryUnloadThreshold(), 0.001);
    }

    @Test
    public void testInvalidNumberFallsBack ()
    {
        System.setProperty(AudiverisProperties.KEY_LOAD_THREAD_POOL_SIZE, "invalid");
        assertEquals("Invalid number should fallback to default",
                2, AudiverisProperties.getLoadThreadPoolSize());
    }
}
