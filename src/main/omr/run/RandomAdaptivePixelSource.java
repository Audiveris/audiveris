//----------------------------------------------------------------------------//
//                                                                            //
//             R a n d o m A d a p t i v e P i x e l S o u r c e              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2012. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import omr.log.Logger;

import net.jcip.annotations.ThreadSafe;

/**
 * Class {@code RandomAdaptivePixelSource} is a specialization of
 * {@link AdaptivePixelSource} which computes mean and standard
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
public class RandomAdaptivePixelSource
        extends AdaptivePixelSource
        implements PixelSource
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(omr.run.AdaptivePixelSource.class);

    //~ Constructors -----------------------------------------------------------
    //
    //---------------------------//
    // RandomAdaptivePixelSource //
    //---------------------------//
    /**
     * Create an adaptive wrapper on a raw pixel source.
     *
     * @param source the underlying source of raw pixels
     */
    public RandomAdaptivePixelSource (RawPixelSource source)
    {
        super(source);

        // Prepare tiles
        tile = new MyTile(/* squared => */false);
        sqrTile = new MyTile(/* squared => */true);
    }

    //~ Methods ----------------------------------------------------------------
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
