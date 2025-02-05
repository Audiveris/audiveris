//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              N o n D r a g g a b l e S y m b o l                               //
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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * Class <code>NonDraggableSymbol</code> implements a half-size non-draggable symbol: X
 *
 * @author Hervé Bitteur
 */
public class NonDraggableSymbol
        extends ShapeSymbol
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final AffineTransform at = AffineTransform.getScaleInstance(0.5, 0.5);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create an NonDraggableSymbol
     *
     * @param family the musicFont family
     */
    public NonDraggableSymbol (MusicFamily family)
    {
        super(Shape.NON_DRAGGABLE, family);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        Params p = new Params();

        p.layout = font.layoutShapeByCode(shape, at);
        p.rect = p.layout.getBounds();

        return p;
    }

    //-------//
    // paint //
    //-------//
    @Override
    protected void paint (Graphics2D g,
                          Params p,
                          Point2D location,
                          Alignment alignment)
    {
        Color oldColor = g.getColor();
        g.setColor(Color.RED);
        super.paint(g, p, location, alignment);
        g.setColor(oldColor);
    }
}
