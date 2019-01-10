/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.audiveris.omr.moment;

import org.audiveris.omr.moments.ARTMoments;
import org.audiveris.omr.moments.BasicARTExtractor;
import org.audiveris.omr.moments.BasicARTMoments;

import org.junit.*;

/**
 * Unit test for (Basic) ARTExtractor.
 *
 * @author Hervé Bitteur
 */
public class ARTExtractorTest
        extends MomentsExtractorTest<ARTMoments>
{

    /**
     * Creates a new ARTExtractorTest object.
     */
    public ARTExtractorTest ()
    {
    }

    /**
     * Test of generate method, of class ARTMoments.
     */
    @Test
    public void testAllShapes ()
            throws Exception
    {
        super.testAllShapes(new BasicARTExtractor(), BasicARTMoments.class);
    }
}
