//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             G C D                                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.math;

import java.util.Collection;

/**
 * Class {@code GCD} gathers several functions to compute Greatest Common Divisor
 * of a ensemble of integer values.
 *
 * @author Hervé Bitteur
 */
public abstract class GCD
{
    //~ Constructors -------------------------------------------------------------------------------

    /** Not meant to be instantiated */
    private GCD ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----//
    // gcd //
    //-----//
    /**
     * Report the gcd of an array of int values.
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
     * Basic gcd computation for 2 int values, assumed to be positive or zero.
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
     * Report the gcd of a collection of integer values.
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
     * Report the gcd of an array of integer values.
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
     * Report the Least Common Multiple of 2 values, assumed to be positive or zero.
     *
     * @param m first integer value
     * @param n second integer value
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
     * Report the Least Common Multiple of n values.
     *
     * @param vals an array of integers
     * @return the lcm of these integers
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
