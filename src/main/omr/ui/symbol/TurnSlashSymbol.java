//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 T u r n S l a s h S y m b o l                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.ui.symbol;

import omr.glyph.Shape;
import static omr.ui.symbol.Alignment.*;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code TurnSlashSymbol} displays a TURN symbol with a vertical slash.
 */
public class TurnSlashSymbol
        extends ShapeSymbol
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The turn symbol */
    private final ShapeSymbol turnSymbol = Symbols.getSymbol(Shape.TURN);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new TurnSlashSymbol object.
     *
     * @param isIcon true for an icon
     */
    public TurnSlashSymbol (boolean isIcon)
    {
        super(isIcon, Shape.TURN_SLASH, false);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new TurnSlashSymbol(true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        p.layout = font.layout(turnSymbol.getString());

        Rectangle2D rect = p.layout.getBounds();
        p.rect = new Rectangle(
                (int) Math.ceil(rect.getWidth()),
                (int) Math.ceil(rect.getHeight() * 1.4));
        p.stroke = new BasicStroke(Math.max(1f, p.rect.width / 20f));

        return p;
    }

    //-------//
    // paint //
    //-------//
    @Override
    protected void paint (Graphics2D g,
                          Params params,
                          Point location,
                          Alignment alignment)
    {
        MyParams p = (MyParams) params;
        Point loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);
        MusicFont.paint(g, p.layout, loc, AREA_CENTER);

        Stroke oldStroke = g.getStroke();
        g.setStroke(p.stroke);
        g.drawLine(loc.x, location.y, loc.x, location.y + p.rect.height);
        g.setStroke(oldStroke);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------//
    // Params //
    //--------//
    private class MyParams
            extends Params
    {
        //~ Instance fields ------------------------------------------------------------------------

        Stroke stroke;
    }
}
