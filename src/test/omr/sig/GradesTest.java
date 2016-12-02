/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package omr.sig;

import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author herve
 */
public class GradesTest
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new GradesTest object.
     */
    public GradesTest ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Before
    public void setUp ()
    {
    }

    @BeforeClass
    public static void setUpClass ()
    {
    }

    @After
    public void tearDown ()
    {
    }

    @AfterClass
    public static void tearDownClass ()
    {
    }

    //    /**
    //     * Test of contextual method, of class GradeUtil.
    //     */
    //    @Test
    //    public void testContextual_3args_1 ()
    //    {
    //        System.out.println("contextual");
    //        double target = 0.0;
    //        double ratio = 0.0;
    //        double source = 0.0;
    //        double expResult = 0.0;
    //        double result = GradeUtil.contextual(target, ratio, source);
    //        assertEquals(expResult, result, 0.0);
    //    }
    //    /**
    //     * Test of contextual method, of class GradeUtil.
    //     */
    //    @Test
    //    public void testContextual_5args ()
    //    {
    //        System.out.println("contextual");
    //        double target = 0.0;
    //        double ratio1 = 0.0;
    //        double source1 = 0.0;
    //        double ratio2 = 0.0;
    //        double source2 = 0.0;
    //        double expResult = 0.0;
    //        double result = GradeUtil.contextual(target, ratio1, source1, ratio2, source2);
    //        assertEquals(expResult, result, 0.0);
    //    }
    /**
     * Test of contextual method, of class GradeUtil.
     */
    @Test
    public void testContextual_3args_2 ()
    {
        System.out.println("contextual");

        double target = 0.2;
        double[] ratios = new double[]{5.0, 2.0};
        double[] sources = new double[]{0.5, 0.8};
        double expResult = 0.49;
        double result = GradeUtil.contextual(target, sources, ratios);
        assertEquals(expResult, result, 0.01);
    }

    //    /**
    //     * Test of support method, of class GradeUtil.
    //     */
    //    @Test
    //    public void testSupport ()
    //    {
    //        System.out.println("support");
    //        double target = 0.0;
    //        double ratio = 0.0;
    //        double expResult = 0.0;
    //        double result = GradeUtil.support(target, ratio);
    //        assertEquals(expResult, result, 0.0);
    //    }
}
