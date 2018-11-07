//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    N u m D e n S y m b o l                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code NumDenSymbol} displays a time sig, with numerator and denominator.
 */
public class NumDenSymbol
        extends ShapeSymbol
{

    private final int[] numCodes;

    private final int[] denCodes;

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
        this(shape, ShapeSymbol.numberCodes(numerator), ShapeSymbol.numberCodes(denominator));
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
        MyParams p = new MyParams(font);

        Rectangle2D numRect = p.numLayout.getBounds();
        Rectangle2D denRect = p.denLayout.getBounds();
        p.rect = new Rectangle((int) Math.rint(Math.max(numRect.getWidth(), denRect.getWidth())),
                               p.dy + (int) Math.rint(Math.max(numRect.getHeight(), denRect
                                                               .getHeight())));

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
        Point center = alignment.translatedPoint(AREA_CENTER, p.rect, location);

        Point top = new Point(center.x, center.y - (p.dy / 2));
        OmrFont.paint(g, p.numLayout, top, AREA_CENTER);

        Point bot = new Point(center.x, center.y + (p.dy / 2));
        OmrFont.paint(g, p.denLayout, bot, AREA_CENTER);
    }

    //----------//
    // MyParams //
    //----------//
    protected class MyParams
            extends Params
    {

        final int dy;

        final TextLayout numLayout;

        final TextLayout denLayout;

        public MyParams (MusicFont font)
        {
            dy = (int) Math.rint(2 * font.getStaffInterline());
            numLayout = font.layout(numCodes);
            denLayout = font.layout(denCodes);
        }
    }
}
