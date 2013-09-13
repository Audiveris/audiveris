/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package omr.sig;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author herve
 */
public class GradesTest
{
    
    public GradesTest ()
    {
    }
    
    @BeforeClass
    public static void setUpClass ()
    {
    }
    
    @AfterClass
    public static void tearDownClass ()
    {
    }
    
    @Before
    public void setUp ()
    {
    }
    
    @After
    public void tearDown ()
    {
    }

//    /**
//     * Test of contextual method, of class Grades.
//     */
//    @Test
//    public void testContextual_3args_1 ()
//    {
//        System.out.println("contextual");
//        double target = 0.0;
//        double ratio = 0.0;
//        double source = 0.0;
//        double expResult = 0.0;
//        double result = Grades.contextual(target, ratio, source);
//        assertEquals(expResult, result, 0.0);
//    }

//    /**
//     * Test of contextual method, of class Grades.
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
//        double result = Grades.contextual(target, ratio1, source1, ratio2, source2);
//        assertEquals(expResult, result, 0.0);
//    }

    /**
     * Test of contextual method, of class Grades.
     */
    @Test
    public void testContextual_3args_2 ()
    {
        System.out.println("contextual");
        double target = 0.2;
        double[] ratios = new double[] {5.0, 2.0};
        double[] sources = new double[] {0.5, 0.8};
        double expResult = 0.49;
        double result = Grades.contextual(target, ratios, sources);
        assertEquals(expResult, result, 0.01);
    }

//    /**
//     * Test of support method, of class Grades.
//     */
//    @Test
//    public void testSupport ()
//    {
//        System.out.println("support");
//        double target = 0.0;
//        double ratio = 0.0;
//        double expResult = 0.0;
//        double result = Grades.support(target, ratio);
//        assertEquals(expResult, result, 0.0);
//    }
}