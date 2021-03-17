//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           I d U t i l                                          //
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

/**
 * Class {@code IdUtil} provides features related to ID string, composed of a prefix
 * string (zero, one or several letters) followed by digits.
 * Examples of such IDs are: G123, I45, O-678, PRF-101, 2345
 *
 * @author Hervé Bitteur
 */
public abstract class IdUtil
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(IdUtil.class);

    /** To compare Entity instances according to their id. */
    public static final Comparator<String> byId = new Comparator<String>()
    {
        @Override
        public int compare (String id1,
                            String id2)
        {
            return IdUtil.compare(id1, id2); // When prefix + integers
        }
    };

    //~ Constructors -------------------------------------------------------------------------------
    private IdUtil ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Compare the integer values of two provided IDs, checking that they share the
     * same prefix.
     *
     * @param id1 first provided ID
     * @param id2 second provided ID
     * @return -1,0,+1
     * @throws IllegalArgumentException if the IDs exhibit different prefixes
     * @throws NumberFormatException    if any integer part is not well formed
     */
    public static int compare (String id1,
                               String id2)
    {
        //        if (!getPrefix(id1).equals(getPrefix(id2))) {
        //            throw new IllegalArgumentException("Different prefixes for IDs " + id1 + " & " + id2);
        //        }
        //
        return Integer.compare(getIntValue(id1), getIntValue(id2));
    }

    /**
     * Forge a new ID string, by incrementing the integer part of the provided ID.
     *
     * @param id the provided ID string
     * @return the forged ID
     * @throws NumberFormatException if integer part is not well formed
     */
    public static String decrement (String id)
    {
        int start = getIntStart(id);

        if (start != -1) {
            return id.substring(0, start) + (Integer.decode(id.substring(start)) - 1);
        }

        return null;
    }

    /**
     * Report the integer value of the ID
     *
     * @param id the provided ID string
     * @return the integer value found or null
     * @throws NumberFormatException if integer part is not well formed
     */
    public static Integer getIntValue (String id)
    {
        int start = getIntStart(id);

        if (start != -1) {
            return Integer.decode(id.substring(start));
        }

        return null;
    }

    /**
     * Report the prefix string used in provided ID.
     *
     * @param id the provided ID string
     * @return the prefix found, perhaps empty
     */
    public static String getPrefix (String id)
    {
        int start = getIntStart(id);

        if (start != -1) {
            return id.substring(0, start);
        }

        return id;
    }

    /**
     * Forge a new ID string, by decrementing the integer part of the provided ID.
     *
     * @param id the provided ID string
     * @return the forged ID
     * @throws NumberFormatException if integer part is not well formed
     */
    public static String increment (String id)
    {
        int start = getIntStart(id);

        if (start != -1) {
            return id.substring(0, start) + (Integer.decode(id.substring(start)) + 1);
        }

        return null;
    }

    /**
     * Return the position of first digit found in ID string.
     *
     * @param id the provided ID string
     * @return the string position of first digit, or -1 if none
     */
    private static int getIntStart (String id)
    {
        if ((id == null) || id.isEmpty()) {
            return -1;
        }

        for (int i = 0; i < id.length(); i++) {
            if (Character.isDigit(id.charAt(i))) {
                return i;
            }
        }

        return -1;
    }
}
