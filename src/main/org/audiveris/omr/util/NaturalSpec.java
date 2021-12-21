//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      N a t u r a l S p e c                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class <code>NaturalSpec</code> handles a specification of natural numbers,
 * assumed to be non negative and increasing.
 *
 * @author Hervé Bitteur
 */
public abstract class NaturalSpec
{
    //~ Constructors -------------------------------------------------------------------------------

    /** This class is not meant to be instantiated. */
    private NaturalSpec ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // decode //
    //--------//
    /**
     * Decode a specification into a list of naturals.
     * <p>
     * Format example: 3-5, 8, 10-
     * <br>
     * Gives values from 3 to 5, then value 8, then values from 10 to maxValue
     *
     * @param spec            the specification string to decode, perhaps null
     * @param checkIncreasing true to verify if values are strictly increasing
     * @param maxValue        maximum value if any
     * @return the list of naturals, perhaps empty but not null
     * @throws NumberFormatException    if some number is wrongly formatted
     * @throws IllegalArgumentException if the list values are not strictly increasing or
     *                                  if a range ends with '-' while no maxValue was provided
     */
    public static List<Integer> decode (String spec,
                                        boolean checkIncreasing,
                                        Integer maxValue)
    {
        final List<Integer> values = new ArrayList<>();

        if (spec == null) {
            return values;
        }

        final String[] rawTokens = spec.split("\\s*,\\s*");

        for (String rawToken : rawTokens) {
            final String token = rawToken.trim();
            final int minusPos = token.indexOf('-');

            if (minusPos != -1) {
                final String str1 = token.substring(0, minusPos).trim();
                final int i1 = Integer.parseInt(str1);

                final String str2 = token.substring(minusPos + 1).trim();
                final int i2;
                if (str2.isEmpty()) {
                    if (maxValue != null) {
                        i2 = maxValue;
                    } else {
                        throw new IllegalArgumentException("No maximum value provided");
                    }
                } else {
                    i2 = Integer.parseInt(str2);
                    if (i2 < i1) {
                        throw new IllegalArgumentException("Illegal range provided");
                    }
                }

                for (int i = i1; i <= i2; i++) {
                    values.add(i);
                }
            } else {
                for (String p : token.split(" ")) {
                    if (!p.isEmpty()) {
                        values.add(Integer.parseInt(p));
                    }
                }
            }
        }

        if (checkIncreasing && !isIncreasing(values)) {
            throw new IllegalArgumentException("Non increasing values");
        }

        if (maxValue != null) {
            for (Iterator<Integer> it = values.iterator(); it.hasNext();) {
                final Integer v = it.next();

                if (v < 0 || v > maxValue) {
                    it.remove();
                }
            }
        }

        return values;
    }

    //--------//
    // decode //
    //--------//
    /**
     * Decode a specification into a list of naturals, with no maxValue provided.
     *
     * @param spec            the specification string to decode
     * @param checkIncreasing true to verify if values are strictly increasing
     * @return the sequence of naturals
     */
    public static List<Integer> decode (String spec,
                                        boolean checkIncreasing)
    {
        return decode(spec, checkIncreasing, null);
    }

    //--------//
    // encode //
    //--------//
    /**
     * Build the specification from the provided sequence of natural values.
     *
     * @param values provided sequence of values
     * @return the resulting specification, perhaps empty but not null
     */
    public static String encode (List<Integer> values)
    {
        final StringBuilder sb = new StringBuilder();
        boolean holding = false;
        int prev = -1;

        for (int value : values) {
            if (prev == -1) {
                sb.append(value);
            } else if (prev == value - 1) {
                if (!holding) {
                    sb.append("-");
                    holding = true;
                }
            } else {
                if (holding) {
                    sb.append(prev);
                }
                sb.append(',');
                sb.append(value);
                holding = false;
            }

            prev = value;
        }

        if (holding) {
            sb.append(prev);
        }

        return sb.toString();
    }

    //-----------//
    // getCounts //
    //-----------//
    /**
     * Report a string telling the count of sheets selected among the total available.
     *
     * @param spec     the sheets specification
     * @param maxValue the maximum value
     * @return the counts string
     */
    public static String getCounts (String spec,
                                    int maxValue)
    {
        return new StringBuilder()
                .append((spec != null) ? decode(spec, false, maxValue).size() : maxValue)
                .append(" of ")
                .append(maxValue).toString();
    }

    //--------------//
    // isIncreasing //
    //--------------//
    /**
     * Check if the provided sequence of integer values is strictly increasing.
     *
     * @param values the sequence to check
     * @return true if so
     */
    public static boolean isIncreasing (List<Integer> values)
    {
        int prev = Integer.MIN_VALUE;

        for (int value : values) {
            if (value <= prev) {
                return false;
            }
            prev = value;
        }

        return true;
    }

    //------------//
    // normalized //
    //------------//
    /**
     * Report a normalized specification of the provided raw specification,
     * perhaps invalid, null, etc...
     *
     * @param spec     the raw specification
     * @param maxValue the maximum value, greater or equal to 1
     * @return the normalized specification
     */
    public static String normalized (String spec,
                                     int maxValue)
    {
        if (spec == null) {
            spec = "";
        }

        // This may throw an exception...
        final List<Integer> ids = NaturalSpec.decode(spec, true, maxValue);
        final String normalized = NaturalSpec.encode(ids);

        // Here, new specification is valid
        if (normalized.isBlank()) {
            if (maxValue > 1) {
                return "1-" + maxValue;
            } else {
                return "1";
            }
        }

        return normalized;
    }
}
