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
import java.util.List;
import java.util.Objects;

/**
 * Class {@code NaturalSpec} handles a specification of one or several natural numbers,
 * assumed to be non negative and increasing.
 *
 * @author Hervé Bitteur
 */
public abstract class NaturalSpec
{

    //~ Constructors -------------------------------------------------------------------------------
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
     * Format example: 3-5, 8, 10-15
     * <br>
     * Gives values from 3 to 5, then value 8, then values from 10 to 15.
     *
     * @param spec            the specification string to decode
     * @param checkIncreasing true to verify if values are strictly increasing
     * @return the list of naturals, perhaps empty but not null
     * @throws NullPointerException     if spec is null
     * @throws NumberFormatException    if some number is wrongly formatted
     * @throws IllegalArgumentException if the list values are not strictly increasing
     */
    public static List<Integer> decode (String spec,
                                        boolean checkIncreasing)
    {
        Objects.requireNonNull(spec, "Null natural specification");

        final List<Integer> values = new ArrayList<>();
        final String[] rawTokens = spec.split("\\s*,\\s*");

        for (String rawToken : rawTokens) {
            final String token = rawToken.trim();
            final int minusPos = token.indexOf('-');

            if (minusPos != -1) {
                String str1 = token.substring(0, minusPos).trim();
                String str2 = token.substring(minusPos + 1).trim();
                int i1 = Integer.parseInt(str1);
                int i2 = Integer.parseInt(str2);

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
            throw new IllegalArgumentException();
        }

        return values;
    }

    //--------//
    // encode //
    //--------//
    /**
     * Build the spec that corresponds to the provided sequence of natural values.
     *
     * @param values provided sequence of values
     * @return resulting spec
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
}
