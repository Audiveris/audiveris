//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 T u r n S l a s h S y m b o l                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code TurnSlashSymbol} displays a TURN symbol with a vertical slash.
 *
 * @author Hervé Bitteur
 */
public class TurnSlashSymbol
        extends ShapeSymbol
{

    /** The turn symbol */
    private final ShapeSymbol turnSymbol = Symbols.getSymbol(Shape.TURN);

    /**
     * Creates a new TurnSlashSymbol object.
     */
    public TurnSlashSymbol ()
    {
        this(false);
    }

    /**
     * Creates a new TurnSlashSymbol object.
     *
     * @param isIcon true for an icon
     */
    protected TurnSlashSymbol (boolean isIcon)
    {
        super(isIcon, Shape.TURN_SLASH, false);
    }

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
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        p.layout = font.layout(turnSymbol.getString());

        Rectangle2D rect = p.layout.getBounds();
        p.rect = new Rectangle2D.Double(0,
                                        0,
                                        rect.getWidth(),
                                        rect.getHeight() * 1.4);
        p.stroke = new BasicStroke(Math.max(1f, (float) p.rect.getWidth() / 20f));

        return p;
    }

    //-------//
    // paint //
    //-------//
    @Override
    protected void paint (Graphics2D g,
                          Params params,
                          Point2D location,
                          Alignment alignment)
    {
        MyParams p = (MyParams) params;
        Point2D loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);
        MusicFont.paint(g, p.layout, loc, AREA_CENTER);

        Stroke oldStroke = g.getStroke();
        g.setStroke(p.stroke);

        Point2D top = alignment.translatedPoint(TOP_CENTER, p.rect, location);
        g.draw(new Line2D.Double(loc.getX(), top.getY(),
                                 loc.getX(), top.getY() + p.rect.getHeight()));
        g.setStroke(oldStroke);
    }

    //--------//
    // Params //
    //--------//
    private static class MyParams
            extends Params
    {

        Stroke stroke;
    }
}
