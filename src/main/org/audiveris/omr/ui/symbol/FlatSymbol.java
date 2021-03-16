//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      F l a t S y m b o l                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.ui.symbol;

import org.audiveris.omr.glyph.Shape;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class {@code FlatSymbol} handles a flat or double-flat symbol with a refPoint.
 *
 * @author Hervé Bitteur
 */
public class FlatSymbol
        extends ShapeSymbol
{

    /**
     * Creates a new FlatSymbol object.
     *
     * @param shape the related shape
     * @param codes the codes for MusicFont characters
     */
    public FlatSymbol (Shape shape,
                       int... codes)
    {
        this(false, shape, codes);
    }

    /**
     * Creates a new FlatSymbol object.
     *
     * @param isIcon true for an icon
     * @param shape  the related shape
     * @param codes  the codes for MusicFont characters
     */
    protected FlatSymbol (boolean isIcon,
                          Shape shape,
                          int... codes)
    {
        super(isIcon, shape, false, codes);
    }

    //-------------//
    // getRefPoint //
    //-------------//
    /**
     * {@inheritDoc}
     * <p>
     * For a flat symbol, the reference point is significantly lower than area center.
     */
    @Override
    public Point getRefPoint (Rectangle box)
    {
        return new Point(box.x + (box.width / 2), box.y + (int) Math.rint(box.height * 0.67));
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new FlatSymbol(true, shape, codes);
    }
}
