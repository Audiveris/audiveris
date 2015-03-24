//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          I n t U t i l                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
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
