//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   D i s t a n c e F i l t e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import ij.process.ByteProcessor;

/**
 * Class {@code DistanceFilter} implements a {@link PixelFilter} on top of a distance
 * image.
 *
 * @author Hervé Bitteur
 */
public class DistanceFilter
        implements PixelFilter
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The underlying distance image (distances to foreground). */
    private final Table distances;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new DistanceFilter object.
     *
     * @param distances the table of distances to foreground
     */
    public DistanceFilter (Table distances)
    {
        this.distances = distances;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public ByteProcessor filteredImage ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int get (int x,
                    int y)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Context getContext (int x,
                               int y)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getHeight ()
    {
        return distances.getHeight();
    }

    @Override
    public int getWidth ()
    {
        return distances.getWidth();
    }

    @Override
    public boolean isFore (int x,
                           int y)
    {
        return distances.getValue(x, y) == 0;
    }
}
