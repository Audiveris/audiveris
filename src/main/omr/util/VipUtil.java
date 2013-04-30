//----------------------------------------------------------------------------//
//                                                                            //
//                                 V i p U t i l                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code VipUtil} gathers convenient methods related to Vip
 * handling.
 *
 * @author Hervé Bitteur
 */
public class VipUtil
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(VipUtil.class);

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // decodeIds //
    //-----------//
    /**
     * Parse a string of integer ids, separated by comma.
     *
     * @param str the string to parse
     * @return tha sequence of ids decoded
     */
    public static List<Integer> decodeIds (String str)
    {
        List<Integer> ids = new ArrayList<>();

        // Retrieve the list of ids
        String[] tokens = str.split("\\s*,\\s*");

        for (String token : tokens) {
            try {
                String trimmedToken = token.trim();
                if (!trimmedToken.isEmpty()) {
                    ids.add(Integer.decode(trimmedToken));
                }
            } catch (Exception ex) {
                logger.warn("Illegal glyph id", ex);
            }
        }

        return ids;
    }
}
