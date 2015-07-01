//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             L a g s                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import ij.process.ByteProcessor;

import java.awt.Rectangle;

/**
 * Class {@code Lags} gathers utilities for lags.
 *
 * @author Hervé Bitteur
 */
public class Lags
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Horizontal (partial) lag. It complements vLag. */
    public static final String HLAG = "hLag";

    /** Vertical (partial) lag. It complements hLag. */
    public static final String VLAG = "vLag";

    /** Horizontal out-of-staves lag. (for ledgers) */
    public static final String LEDGER_LAG = "ledgerLag";

    /** Spot lag. (for beams) */
    public static final String SPOT_LAG = "spotLag";

    /** Symbol lag. (for symbols) */
    public static final String SYMBOL_LAG = "symLag";

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // buildBuffer //
    //-------------//
    /**
     * Populate a buffer with the content of all provided lags.
     *
     * @param width  width of the target buffer
     * @param height height of the target buffer
     * @param lags   the contributing lags
     * @return the populated buffer
     */
    public static ByteProcessor buildBuffer (int width,
                                             int height,
                                             Lag... lags)
    {
        final Rectangle box = new Rectangle(0, 0, width, height);
        final ByteProcessor buf = new ByteProcessor(width, height);
        buf.invert();

        for (Lag lag : lags) {
            for (Section section : lag.getSections()) {
                section.fillImage(buf, box);
            }
        }

        return buf;
    }
}
