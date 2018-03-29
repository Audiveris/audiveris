//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S l u r S y m b o l                                       //
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
package org.audiveris.omr.ui.symbol;

import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.ui.symbol.Alignment.*;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.CubicCurve2D;

/**
 * Class {@code SlurSymbol} implements a decorated slur symbol
 *
 * @author Hervé Bitteur
 */
public class SlurSymbol
        extends ShapeSymbol
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a SlurSymbol
     */
    public SlurSymbol ()
    {
        this(false);
    }

    /**
     * Create a SlurSymbol
     *
     * @param isIcon true for an icon
     */
    protected SlurSymbol (boolean isIcon)
    {
        super(isIcon, Shape.SLUR, true); // Decorated
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new SlurSymbol(true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        Params p = new Params();
        int il = font.getStaffInterline();
        p.rect = new Rectangle(2 * il, (4 * il) / 3);

        return p;
    }

    //-------//
    // paint //
    //-------//
    @Override
    protected void paint (Graphics2D g,
                          Params p,
                          Point location,
                          Alignment alignment)
    {
        Point loc = alignment.translatedPoint(TOP_LEFT, p.rect, location);

        CubicCurve2D curve = new CubicCurve2D.Double(
                loc.x,
                loc.y + p.rect.height,
                loc.x + ((3 * p.rect.width) / 10),
                loc.y + (p.rect.height / 5),
                loc.x + (p.rect.width / 2),
                loc.y,
                loc.x + p.rect.width,
                loc.y);

        // Slur
        g.draw(curve);
    }
}
