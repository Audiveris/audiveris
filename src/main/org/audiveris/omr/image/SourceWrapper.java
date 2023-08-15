//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S o u r c e W r a p p e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
 * Class <code>SourceWrapper</code> wraps a PixelSource.
 *
 * @author Hervé Bitteur
 */
public class SourceWrapper
        implements PixelSource
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Underlying pixel source. */
    protected final ByteProcessor source;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SourceWrapper object.
     *
     * @param source the pixel source
     */
    public SourceWrapper (ByteProcessor source)
    {
        this.source = source;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----//
    // get //
    //-----//
    @Override
    public int get (int x,
                    int y)
    {
        return source.get(x, y);
    }

    //-----------//
    // getHeight //
    //-----------//
    @Override
    public int getHeight ()
    {
        return source.getHeight();
    }

    //----------//
    // getWidth //
    //----------//
    @Override
    public int getWidth ()
    {
        return source.getWidth();
    }
}
