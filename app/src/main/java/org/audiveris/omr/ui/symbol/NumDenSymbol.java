//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    N u m D e n S y m b o l                                     //
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

import static org.audiveris.omr.ui.symbol.Alignment.AREA_CENTER;

import org.audiveris.omr.glyph.Shape;

import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class <code>NumDenSymbol</code> displays a time sig, with numerator and denominator.
 *
 * @author Hervé Bitteur
 */
public class NumDenSymbol
        extends ShapeSymbol
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final int numerator;

    private final int denominator;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new NumDenSymbol object.
     *
     * @param shape       the related shape
     * @param family      the musicFont family
     * @param numerator   the numerator value
     * @param denominator the denominator value
     */
    public NumDenSymbol (Shape shape,
                         MusicFamily family,
                         int numerator,
                         int denominator)
    {
        super(shape, family);
        this.numerator = numerator;
        this.denominator = denominator;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        final MyParams p = new MyParams();

        p.numLayout = font.layoutNumberByCode(numerator);
        p.denLayout = font.layoutNumberByCode(denominator);
        p.dy = 2 * font.getStaffInterline();

        final Rectangle2D numRect = p.numLayout.getBounds();
        final Rectangle2D denRect = p.denLayout.getBounds();
        p.rect = new Rectangle2D.Double(
                0,
                0,
                Math.max(numRect.getWidth(), denRect.getWidth()),
                p.dy + Math.max(numRect.getHeight(), denRect.getHeight()));

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
        Point2D center = alignment.translatedPoint(AREA_CENTER, p.rect, location);

        Point2D top = new Point2D.Double(center.getX(), center.getY() - (p.dy / 2));
        OmrFont.paint(g, p.numLayout, top, AREA_CENTER);

        Point2D bot = new Point2D.Double(center.getX(), center.getY() + (p.dy / 2));
        OmrFont.paint(g, p.denLayout, bot, AREA_CENTER);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //----------//
    // MyParams //
    //----------//
    protected static class MyParams
            extends Params
    {
        double dy;

        TextLayout numLayout;

        TextLayout denLayout;
    }
}
