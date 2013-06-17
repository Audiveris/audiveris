//----------------------------------------------------------------------------//
//                                                                            //
//                          R a n d o m F i l t e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code RandomFilter} is a specialization of
 * {@link AdaptiveFilter} which computes mean and standard
 * deviation values based on pre-populated tables of integrals.
 *
 * <p>This implementation is ThreadSafe and provides fast random access to any
 * location in constant time.
 * The drawback is that each of the two underlying tables of integrals needs
 * 8 bytes per image pixel.
 *
 * @author ryo/twitter &#64;xiaot_Tag
 * @author Hervé Bitteur
 */
@ThreadSafe
public class RandomFilter
        extends AdaptiveFilter
        implements PixelFilter
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            RandomFilter.class);

    //~ Constructors -----------------------------------------------------------
    //
    //--------------//
    // RandomFilter //
    //--------------//
    /**
     * Create an adaptive wrapper on a raw pixel source.
     *
     * @param source      the underlying source of raw pixels
     * @param meanCoeff   the coefficient for mean value
     * @param stdDevCoeff the coefficient for standard deviation value
     */
    public RandomFilter (PixelSource source,
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

    //~ Inner Classes ----------------------------------------------------------
    //
    //--------//
    // MyTile //
    //--------//
    /**
     * This is a degenerated tile, since it is as big as the source
     * image and is never shifted.
     */
    private class MyTile
            extends Tile
    {
        //~ Constructors -------------------------------------------------------

        public MyTile (boolean squared)
        {
            // Allocate a tile as big as the source
            super(source.getWidth(), source.getHeight(), squared);

            // Populate the whole tile at once
            for (int x = 0, width = source.getWidth(); x < width; x++) {
                populateColumn(x);
            }
        }
    }
}
