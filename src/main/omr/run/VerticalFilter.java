//----------------------------------------------------------------------------//
//                                                                            //
//                        V e r t i c a l F i l t e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import net.jcip.annotations.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code VerticalFilter} is a specialization of
 * {@link AdaptiveFilter} which computes mean and standard
 * deviation values based on vertical tiles of integrals.
 *
 * <p>This implementation is meant to be functionally equivalent to
 * {@link RandomFilter} with similar performances but much lower
 * memory requirements.
 *
 * <p>It uses a vertical window which performs the computation in constant time,
 * provided that the vertical window always moves to the right.
 * Instead of a whole table of integrals, this class uses a vertical tile whose
 * width equals the window size, and the height equals the picture height.
 * <pre>
 *                                              +----------------+
 *                                              |   TILE_WIDTH   |
 * 0---------------------------------------------+---------------+
 * |                                             |               |
 * |                                             |               |
 * |                                             |               |
 * |                                            a|              b|
 * +---------------------------------------------+---------------+
 * |                                             |               |
 * |                                             |    WINDOW     |
 * |                                             |               |
 * |                                             |               |
 * |                                             |       +       |
 * |                                             |               |
 * |                                             |               |
 * |                                             |               |
 * |                                            c|              d|
 * +---------------------------------------------+---------------+
 * </pre>
 * Since only the (1 + WINDOW_SIZE) last columns are relevant, a tile
 * uses a circular buffer to handle only those columns.
 * <p>
 * Drawback: the implementation of the tile as a circular buffer makes
 * an instance of this class usable by only one thread at a time.
 *
 * @author ryo/twitter &#64;xiaot_Tag
 * @author Hervé Bitteur
 */
@NotThreadSafe
public class VerticalFilter
        extends AdaptiveFilter
        implements PixelFilter
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            VerticalFilter.class);

    //~ Constructors -----------------------------------------------------------
    //
    //----------------//
    // VerticalFilter //
    //----------------//
    /**
     * Create an adaptive wrapper on a raw pixel source.
     *
     * @param source      the underlying source of raw pixels
     * @param meanCoeff   the coefficient for mean value
     * @param stdDevCoeff the coefficient for standard deviation value
     */
    public VerticalFilter (PixelSource source,
                           double meanCoeff,
                           double stdDevCoeff)
    {
        super(source, meanCoeff, stdDevCoeff);

        // Prepare tiles
        tile = new MyTile( /* squared => */
                false);
        sqrTile = new MyTile( /* squared => */
                true);
    }

    //~ Methods ----------------------------------------------------------------
    //
    //----------------------//
    // getDefaultDescriptor //
    //----------------------//
    public static FilterDescriptor getDefaultDescriptor ()
    {
        return AdaptiveDescriptor.getDefault();
    }

    //~ Inner Classes ----------------------------------------------------------
    //
    //--------//
    // MyTile //
    //--------//
    /**
     * A tile as a circular buffer limited by window width.
     */
    private class MyTile
            extends Tile
    {
        //~ Constructors -------------------------------------------------------

        public MyTile (boolean squared)
        {
            super(2 + (2 * HALF_WINDOW_SIZE), source.getHeight(), squared);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        protected void shiftTile (int x2)
        {
            // Make sure we don't violate the tile principle
            if (x2 < xRight) {
                logger.error("SlidingPixelSource can only move forward");
                throw new IllegalStateException();
            }

            // Shift tile as needed to the right
            while (xRight < x2) {
                xRight++;
                populateColumn(xRight);
            }
        }
    }
}
