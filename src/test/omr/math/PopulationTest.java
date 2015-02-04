//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  P o p u l a t i o n T e s t                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

//import org.testng.annotations.*;
import omr.util.BaseTestCase;
import static junit.framework.Assert.*;

/**
 * Class <code>PopulationTest</code> performs unit tests on Population class.
 *
 * @author Hervé Bitteur
 */
public class PopulationTest
        extends BaseTestCase
{
    //~ Methods ------------------------------------------------------------------------------------

    //-----------//
    // testEmpty //
    //-----------//
    //@Test
    public void testEmpty ()
    {
        Population p = new Population();

        assertEquals("No values cumulated so far.", 0, p.getCardinality());

        try {
            double mv = p.getMeanValue();
            fail(
                    "Exception should be raised"
                    + " when retrieving mean value of an empty population");
        } catch (Exception expected) {
            checkException(expected);
        }

        try {
            double sd = p.getStandardDeviation();
            fail(
                    "Exception should be raised"
                    + " when retrieving standard deviation of an empty population");
        } catch (Exception expected) {
            checkException(expected);
        }

        try {
            double v = p.getVariance();
            fail("Exception should be raised" + " when retrieving variance of an empty population");
        } catch (Exception expected) {
            checkException(expected);
        }

        try {
            p.excludeValue(123);
            fail("Exception should be raised" + " when excluding a value from an empty population");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    //-------------//
    // testExclude //
    //-------------//
    //@Test
    public void testExclude ()
    {
        Population p = new Population();
        p.includeValue(5);
        p.includeValue(6);
        p.includeValue(8);
        p.includeValue(9);

        p.excludeValue(5);
        p.excludeValue(9);

        assertEquals("Population should contain 2 values.", 2, p.getCardinality());

        assertEquals("Check mean value.", 7d, p.getMeanValue());

        double v = p.getVariance();
        //assertEquals("Check variance of 2 values.", 1d, v); // Biased
        assertEquals("Check variance of 2 values.", 2d, v); // Unbiased

        double sd = p.getStandardDeviation();
        ///assertNears("Check standard deviation of 2 values.", Math.sqrt(1d), sd); // Biased
        assertNears("Check standard deviation of 2 values.", Math.sqrt(2d), sd); // Unbiased
    }

    //-------------//
    // testInclude //
    //-------------//
    //@Test
    public void testInclude ()
    {
        Population p = new Population();
        p.includeValue(5);
        p.includeValue(6);
        p.includeValue(8);
        p.includeValue(9);

        assertEquals("Population should contain 4 values.", 4, p.getCardinality());

        assertEquals("Check mean value.", 7d, p.getMeanValue());

        double v = p.getVariance();
        //assertEquals("Check variance of 4 values.", 2.5, p.getVariance()); // Biased
        assertEquals("Check variance of 4 values.", 3.33, p.getVariance(), 0.01); // Unbiased

        double sd = p.getStandardDeviation();
        //assertEquals("Check standard deviation of 4 values.", Math.sqrt(2.5), sd, 0.01); // Biased
        assertEquals("Check standard deviation of 4 values.", Math.sqrt(3.33), sd, 0.01); // Unbiased
    }

    //---------------//
    // testSingleton //
    //---------------//
    //@Test
    public void testSingleton ()
    {
        Population p = new Population();
        double val = 123d;
        p.includeValue(val);

        assertEquals("Population should contain one value.", 1, p.getCardinality());

        assertEquals("Check mean value.", val, p.getMeanValue());

        try {
            double sd = p.getStandardDeviation();
            fail(
                    "Exception should be raised"
                    + " when retrieving standard deviation of a singleton");
        } catch (Exception expected) {
            checkException(expected);
        }

        try {
            double v = p.getVariance();
            fail("Exception should be raised" + " when retrieving variance of a singleton");
        } catch (Exception expected) {
            checkException(expected);
        }

        p.excludeValue(val);
        assertEquals("Population should contain no value.", 0, p.getCardinality());
    }
}
