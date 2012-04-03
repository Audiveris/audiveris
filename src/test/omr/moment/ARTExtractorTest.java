/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package omr.moment;

import omr.moments.ARTMoments;
import omr.moments.BasicARTExtractor;
import omr.moments.BasicARTMoments;

import org.junit.*;

/**
 * Unit test for (Basic) ARTExtractor.
 *
 * @author Hervé Bitteur
 */
public class ARTExtractorTest
    extends MomentsExtractorTest<ARTMoments>
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ARTExtractorTest object.
     */
    public ARTExtractorTest ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Test of generate method, of class ARTMoments.
     */
    @Test
    public void testAllShapes ()
        throws Exception
    {
        super.testAllShapes(
            new BasicARTExtractor(),
            BasicARTMoments.class);
    }
}
