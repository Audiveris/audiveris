//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     N u m b e r S y m b o l                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import java.awt.geom.Rectangle2D;

/**
 * Class <code>NumberSymbol</code> displays an integer number.
 * <p>
 * It is meant for a measure count or for a time number.
 *
 * @author Hervé Bitteur
 */
public class NumberSymbol
        extends ShapeSymbol
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final int value;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>NumberSymbol</code> object.
     *
     * @param shape  the related shape
     * @param family the musicFont family
     * @param value  the number value
     */
    public NumberSymbol (Shape shape,
                         MusicFamily family,
                         int value)
    {
        super(shape, family);
        this.value = value;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        final Params p = new Params();

        p.layout = font.layoutNumberByCode(value);

        final Rectangle2D valRect = p.layout.getBounds();
        p.rect = new Rectangle2D.Double(0, 0, valRect.getWidth(), valRect.getHeight());

        return p;
    }
    //
    //    //-------//
    //    // paint //
    //    //-------//
    //    @Override
    //    protected void paint (Graphics2D g,
    //                          Params params,
    //                          Point2D location,
    //                          Alignment alignment)
    //    {
    //        Point2D center = alignment.translatedPoint(AREA_CENTER, params.rect, location);
    //        OmrFont.paint(g, params.layout, center, AREA_CENTER);
    //    }
}
