//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      T e x t S y m b o l                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2022. All rights reserved.
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
import org.audiveris.omr.ui.symbol.MusicFont.Family;
import static org.audiveris.omr.ui.symbol.OmrFont.RATIO_TINY;

import java.awt.Point;
import java.awt.geom.Point2D;

/**
 * Class <code>TextSymbol</code> implements a decorated text symbol.
 *
 * @author Hervé Bitteur
 */
public class TextSymbol
        extends ShapeSymbol
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The text string to use. */
    private final String str;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a <code>TextSymbol</code>.
     *
     * @param shape  the precise shape
     * @param family the MusicFont family (irrelevant in fact, but cannot be null...)
     * @param str    the text to draw
     */
    public TextSymbol (Shape shape,
                       Family family,
                       String str)
    {
        super(shape, family);
        this.str = str;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // getModel //
    //----------//
    @Override
    public WordInter.Model getModel (MusicFont font,
                                     Point location)
    {
        final MyParams p = getParams(font);
        p.model.translate(p.vectorTo(location));

        return p.model;
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        final int fontSize = (int) Math.rint(font.getSize2D() * RATIO_TINY);
        final TextFont textFont = new TextFont(fontSize);
        p.layout = textFont.layout(str);
        p.rect = p.layout.getBounds();

        final Point2D baseLoc = new Point2D.Double(0, 0);
        final FontInfo fontInfo = FontInfo.createDefault(fontSize);
        p.model = new WordInter.Model(str, baseLoc, fontInfo);

        return p;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------//
    // Params //
    //--------//
    protected static class MyParams
            extends ShapeSymbol.Params
    {

        // offset: not used
        // layout: text layout
        // rect:   global image
        // model
        WordInter.Model model;
    }
}
