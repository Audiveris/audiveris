/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package omr.moment;

import omr.moments.BasicLegendreExtractor;
import omr.moments.BasicLegendreMoments;
import omr.moments.LegendreMoments;

import org.junit.*;

/**
 *
 * @author Etiolles
 */
public class LegendreMomentsTest
    extends MomentsExtractorTest<LegendreMoments>
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new LegendreMomentsTest object.
     */
    public LegendreMomentsTest ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Test of generate method, of class LegendreMoments.
     */
    @Test
    public void testAllShapes ()
    {
        try {
            BasicLegendreExtractor instance = new BasicLegendreExtractor();
            super.testAllShapes(instance, BasicLegendreMoments.class);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
