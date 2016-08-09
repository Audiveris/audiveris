//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          I n t U t i l                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code IntUtil} gathers convenient methods related to Integer handling.
 *
 * @author Hervé Bitteur
 */
public abstract class IntUtil
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(IntUtil.class);

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // parseInts //
    //-----------//
    /**
     * Parse a string of integers, separated by comma.
     *
     * @param str the string to parse
     * @return the sequence of integers decoded
     */
    public static List<Integer> parseInts (String str)
    {
        final List<Integer> intList = new ArrayList<Integer>();
        final String[] tokens = str.split("\\s*,\\s*");

        for (String token : tokens) {
            try {
                String trimmedToken = token.trim();

                if (!trimmedToken.isEmpty()) {
                    intList.add(Integer.decode(trimmedToken));
                }
            } catch (NumberFormatException ex) {
                logger.warn("Illegal integer", ex);
            }
        }

        return intList;
    }
}
