//----------------------------------------------------------------------------//
//                                                                            //
//                                   L a g s                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.image.PixelBuffer;

import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * Class {@code Lags} gathers utilities for lags.
 *
 * @author Hervé Bitteur
 */
public class Lags
{
    //~ Static fields/initializers ---------------------------------------------

    /** Horizontal (partial) lag. It complements vLag. */
    public static final String HLAG = "hLag";

    /** Vertical (partial) lag. It complements hLag. */
    public static final String VLAG = "vLag";

    /** Horizontal out-of-staves lag. */
    public static final String FULL_HLAG = "fullHLag";

    /** Spot lag. */
    public static final String SPOT_LAG = "spotLag";

    /** Head Lag. */
    public static final String HEAD_LAG = "headLag";

    /** Split lag. */
    public static final String SPLIT_LAG = "splitLag";

    //~ Methods ----------------------------------------------------------------
    //-------------//
    // buildBuffer //
    //-------------//
    /**
     * Populate a buffer with the content of all provided lags.
     *
     * @param dim  dimension of the target buffer
     * @param lags the contributing lags
     * @return the populated buffer
     */
    public static PixelBuffer buildBuffer (Dimension dim,
                                           Lag... lags)
    {
        final Rectangle box = new Rectangle(0, 0, dim.width, dim.height);
        final PixelBuffer buf = new PixelBuffer(dim);

        for (Lag lag : lags) {
            for (Section section : lag.getSections()) {
                section.fillImage(buf, box);
            }
        }

        return buf;
    }
}
