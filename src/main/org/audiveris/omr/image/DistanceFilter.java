//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   D i s t a n c e F i l t e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.image;

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
