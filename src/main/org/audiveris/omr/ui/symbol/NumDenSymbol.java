//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    N u m D e n S y m b o l                                     //
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
import static org.audiveris.omr.ui.symbol.Alignment.*;

import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code NumDenSymbol} displays a time sig, with numerator and denominator.
 *
 * @author Hervé Bitteur
 */
public class NumDenSymbol
        extends ShapeSymbol
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final int[] numCodes;

    private final int[] denCodes;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new NumDenSymbol object.
     *
     * @param shape       the related shape
     * @param numerator   the numerator value (not code)
     * @param denominator the denominator value (not code)
     */
    public NumDenSymbol (Shape shape,
                         int numerator,
                         int denominator)
    {
        this(
                shape,
                ShapeSymbol.numberCodes(numerator),
                ShapeSymbol.numberCodes(denominator));
    }

    //--------------//
    // NumDenSymbol //
    //--------------//
    /**
     * Creates a new NumDenSymbol object.
     *
     * @param shape    the related shape
     * @param numCodes the numerator codes
     * @param denCodes the denominator codes
     */
    public NumDenSymbol (Shape shape,
                         int[] numCodes,
                         int[] denCodes)
    {
        this(false, shape, false, numCodes, denCodes);
    }

    //--------------//
    // NumDenSymbol //
    //--------------//
    /**
     * Creates a new NumDenSymbol object.
     *
     * @param isIcon    true for an icon
     * @param shape     the related shape
     * @param decorated true for decoration
     * @param numCodes  the numerator codes
     * @param denCodes  the denominator codes
     */
    public NumDenSymbol (boolean isIcon,
                         Shape shape,
                         boolean decorated,
                         int[] numCodes,
                         int[] denCodes)
    {
        super(isIcon, shape, decorated);
        this.numCodes = numCodes;
        this.denCodes = denCodes;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new NumDenSymbol(true, shape, decorated, numCodes, denCodes);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams(font, numCodes, denCodes);

        Rectangle2D numRect = p.numLayout.getBounds();
        Rectangle2D denRect = p.denLayout.getBounds();
        p.rect = new Rectangle2D.Double(0,
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

        final double dy;

        final TextLayout numLayout;

        final TextLayout denLayout;

        MyParams (MusicFont font,
                  int[] numCodes,
                  int[] denCodes)
        {
            dy = 2 * font.getStaffInterline();
            numLayout = font.layout(numCodes);
            denLayout = font.layout(denCodes);
        }
    }
}
