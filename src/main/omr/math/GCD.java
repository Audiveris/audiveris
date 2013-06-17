//----------------------------------------------------------------------------//
//                                                                            //
//                                   G C D                                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import java.util.Collection;

/**
 * Class {@code GCD} gathers several functions to compute Greatest
 * Common Divisor of a ensemble of integer values.
 *
 * @author Hervé Bitteur
 */
public class GCD
{
    //~ Constructors -----------------------------------------------------------

    /** Not meant to be instantiated */
    private GCD ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //-----//
    // gcd //
    //-----//
    /**
     * Report the gcd of an array of int values
     *
     * @param vals the array of int values
     * @return the gcd over the int values
     */
    public static int gcd (int[] vals)
    {
        int s = 0;

        for (int val : vals) {
            s = gcd(s, val);
        }

        return s;
    }

    //-----//
    // gcd //
    //-----//
    /**
     * Basic gcd computation for 2 int values, assumed to be positive or zero
     *
     * @param m one int value
     * @param n another int value
     * @return the gcd of the two values
     */
    public static int gcd (int m,
                           int n)
    {
        if (n == 0) {
            return m;
        } else {
            return gcd(n, m % n);
        }
    }

    //-----//
    // gcd //
    //-----//
    /**
     * Report the gcd of a collection of integer values
     *
     * @param vals the collection of values
     * @return the gcd over the collection
     */
    public static int gcd (Collection<Integer> vals)
    {
        return gcd(vals.toArray(new Integer[vals.size()]));
    }

    //-----//
    // gcd //
    //-----//
    /**
     * Report the gcd of an array of integer values
     *
     * @param vals the array of integer values
     * @return the gcd over the values
     */
    public static int gcd (Integer[] vals)
    {
        int s = 0;

        for (int val : vals) {
            s = gcd(s, val);
        }

        return s;
    }

    //-----//
    // lcm //
    //-----//
    /**
     * Report the Least Common Multiple of 2 values, assumed to be positive
     * or zero
     *
     * @param m
     * @param n
     * @return lcm(|m|, |n|)
     */
    public static int lcm (int m,
                           int n)
    {
        if (m < 0) {
            m = -m;
        }

        if (n < 0) {
            n = -n;
        }

        return m * (n / gcd(m, n));
    }

    //-----//
    // lcm //
    //-----//
    /**
     * Report the Least Common Multiple of n values
     */
    public static int lcm (int... vals)
    {
        int s = vals[0];

        for (int val : vals) {
            s = lcm(s, val);
        }

        return s;
    }
}
