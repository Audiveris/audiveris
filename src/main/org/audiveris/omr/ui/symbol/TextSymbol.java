//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      T e x t S y m b o l                                       //
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
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.text.FontInfo;
import static org.audiveris.omr.ui.symbol.Alignment.TOP_LEFT;

import java.awt.Point;
import java.awt.geom.Point2D;

/**
 * Class {@code TextSymbol} implements a decorated text symbol
 *
 * @author Hervé Bitteur
 */
public class TextSymbol
        extends ShapeSymbol
{

    /** The text string to use. */
    private final String str;

    /**
     * Create an TextSymbol
     *
     * @param shape the precise shape
     * @param str   the text to draw
     */
    public TextSymbol (Shape shape,
                       String str)
    {
        this(false, shape, str);
    }

    /**
     * Create an TextSymbol
     *
     * @param isIcon true for an icon
     * @param shape  the precise shape
     * @param str    the text to draw
     */
    protected TextSymbol (boolean isIcon,
                          Shape shape,
                          String str)
    {
        super(isIcon, shape, true); // Decorated
        this.str = str;
    }

    //----------//
    // getModel //
    //----------//
    @Override
    public WordInter.Model getModel (MusicFont font,
                                     Point location,
                                     Alignment alignment)
    {
        MyParams p = getParams(font);
        Point2D topLeft = alignment.translatedPoint(TOP_LEFT, p.rect, location);
        p.model.translate(topLeft.getX() - p.rect.getX(), topLeft.getY() - p.rect.getY());

        return p.model;
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new TextSymbol(true, shape, str);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        int fontSize = (int) Math.rint(font.getSize2D() * 0.5); // 0.5 for icon size
        TextFont textFont = new TextFont(fontSize);
        p.layout = textFont.layout(str);
        p.rect = p.layout.getBounds();
        Point2D baseLoc = new Point2D.Double(0, 0);
        FontInfo fontInfo = FontInfo.createDefault(fontSize);

        p.model = new WordInter.Model(str, baseLoc, fontInfo);

        return p;
    }

    //--------//
    // Params //
    //--------//
    protected static class MyParams
            extends BasicSymbol.Params
    {

        // offset: not used
        // layout: head decoration
        // rect:   global image
        //
        // ledger thickness
        int thickness;

        // model
        WordInter.Model model;
    }
}
