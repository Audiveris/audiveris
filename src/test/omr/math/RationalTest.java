//----------------------------------------------------------------------------//
//                                                                            //
//                          R a t i o n a l T e s t                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Hervé Bitteur
 */
public class RationalTest
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new RationalTest object.
     */
    public RationalTest ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Test of abs method, of class Rational.
     */
    @Test
    public void testAbs ()
    {
        System.out.println("abs");

        Rational instance = new Rational(-2, 3);
        Rational expResult = new Rational(2, 3);
        Rational result = instance.abs();
        assertEquals(expResult, result);
    }

    /**
     * Test of compareTo method, of class Rational.
     */
    @Test
    public void testCompareTo ()
    {
        System.out.println("compareTo");

        Rational instance = new Rational(4, 9);
        Rational that = new Rational(2, 3);
        int      result = instance.compareTo(that);
        assertEquals(-1, result);
        assertEquals(0, instance.compareTo(new Rational(-8, -18)));
        assertEquals(1, instance.compareTo(instance.opposite()));

        instance = new Rational(3, 16);
        assertEquals(-1, instance.compareTo(Rational.MAX_VALUE));
        assertEquals(1, Rational.MAX_VALUE.compareTo(instance));
    }

    /**
     * Test of divides method, of class Rational.
     */
    @Test
    public void testDivides_Rational ()
    {
        System.out.println("divides");

        Rational instance = new Rational(2, 3);
        Rational that = new Rational(4, 5);
        Rational expResult = new Rational(10, 12);
        Rational result = instance.divides(that);
        assertEquals(expResult, result);
    }

    /**
     * Test of divides method, of class Rational.
     */
    @Test
    public void testDivides_int ()
    {
        System.out.println("divides");

        Rational instance = new Rational(2, 3);
        int      that = 2;
        Rational expResult = new Rational(1, 3);
        Rational result = instance.divides(that);
        assertEquals(expResult, result);
    }

    /**
     * Test of equals method, of class Rational.
     */
    @Test
    public void testEquals ()
    {
        System.out.println("equals");

        Rational instance = new Rational(-4, -6);
        assertEquals(true, instance.equals(new Rational(2, 3)));
        assertEquals(false, instance.equals(new Rational(1, 2)));
        assertEquals(false, instance.equals(Rational.ONE));
    }

    /**
     * Test of gcd method, of class Rational.
     */
    @Test
    public void testGcd_RationalArr ()
    {
        System.out.println("gcd");

        Rational[] vals = new Rational[] {
                              new Rational(2, 3), new Rational(1, 4),
                              new Rational(5, 6)
                          };
        Rational   expResult = new Rational(1, 12);
        Rational   result = Rational.gcd(vals);
        assertEquals(expResult, result);
    }

    /**
     * Test of gcd method, of class Rational.
     */
    @Test
    public void testGcd_Rational_Rational ()
    {
        System.out.println("gcd");

        Rational a = new Rational(2, 3);
        Rational b = new Rational(5, 4);
        Rational expResult = new Rational(1, 12);
        Rational result = Rational.gcd(a, b);
        assertEquals(expResult, result);
    }

    /**
     * Test of hashCode method, of class Rational.
     */
    @Test
    public void testHashCode ()
    {
        System.out.println("hashCode");

        Rational instance = new Rational(2, 3);
        int      expResult = 39874;
        int      result = instance.hashCode();
        assertEquals(expResult, result);
    }

    /**
     * Test of minus method, of class Rational.
     */
    @Test
    public void testMinus_Rational ()
    {
        System.out.println("minus");

        Rational instance = new Rational(2, 3);
        Rational that = new Rational(1, 2);
        Rational expResult = new Rational(1, 6);
        Rational result = instance.minus(that);
        assertEquals(expResult, result);
    }

    /**
     * Test of minus method, of class Rational.
     */
    @Test
    public void testMinus_int ()
    {
        System.out.println("minus");

        Rational instance = new Rational(2, 3);
        int      that = 1;
        Rational expResult = new Rational(-1, 3);
        Rational result = instance.minus(that);
        assertEquals(expResult, result);
    }

    /**
     * Test of opposite method, of class Rational.
     */
    @Test
    public void testNegate ()
    {
        System.out.println("opposite");

        Rational instance = new Rational(2, 3);
        Rational expResult = new Rational(-2, 3);
        Rational result = instance.opposite();
        assertEquals(expResult, result);
    }

    /**
     * Test of plus method, of class Rational.
     */
    @Test
    public void testPlus_Rational ()
    {
        System.out.println("plus");

        Rational instance = new Rational(2, 3);
        Rational that = new Rational(1, 2);
        Rational expResult = new Rational(7, 6);
        Rational result = instance.plus(that);
        assertEquals(expResult, result);
    }

    /**
     * Test of plus method, of class Rational.
     */
    @Test
    public void testPlus_int ()
    {
        System.out.println("plus");

        Rational instance = new Rational(2, 3);
        int      that = 5;
        Rational expResult = new Rational(17, 3);
        Rational result = instance.plus(that);
        assertEquals(expResult, result);
    }

    /**
     * Test of inverse method, of class Rational.
     */
    @Test
    public void testReciprocal ()
    {
        System.out.println("inverse");

        Rational instance = new Rational(2, 3);
        Rational expResult = new Rational(3, 2);
        Rational result = instance.inverse();
        assertEquals(expResult, result);
    }

    /**
     * Test of times method, of class Rational.
     */
    @Test
    public void testTimes_Rational ()
    {
        System.out.println("times");

        Rational instance = new Rational(2, 3);
        Rational that = new Rational(4, 5);
        Rational expResult = new Rational(8, 15);
        Rational result = instance.times(that);
        assertEquals(expResult, result);
    }

    /**
     * Test of times method, of class Rational.
     */
    @Test
    public void testTimes_int ()
    {
        System.out.println("times");

        Rational instance = new Rational(2, 3);
        int      that = -10;
        Rational expResult = new Rational(-20, 3);
        Rational result = instance.times(that);
        assertEquals(expResult, result);
    }

    /**
     * Test of toDouble method, of class Rational.
     */
    @Test
    public void testToDouble ()
    {
        System.out.println("toDouble");

        Rational instance = new Rational(2, 4);
        double   expResult = 0.5;
        double   result = instance.toDouble();
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of toString method, of class Rational.
     */
    @Test
    public void testToString ()
    {
        System.out.println("toString");

        Rational instance = new Rational(2, 3);
        String   expResult = "2/3";
        String   result = instance.toString();
        assertEquals(expResult, result);

        instance = new Rational(2, 1);
        expResult = "2";
        result = instance.toString();
        assertEquals(expResult, result);

        instance = new Rational(2, -3);
        expResult = "-2/3";
        result = instance.toString();
        assertEquals(expResult, result);
    }
}
