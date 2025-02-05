//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      T e x t S y m b o l                                       //
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
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.text.FontInfo;
import static org.audiveris.omr.ui.symbol.Alignment.TOP_LEFT;
import static org.audiveris.omr.ui.symbol.OmrFont.RATIO_TINY;
import static org.audiveris.omr.ui.symbol.OmrFont.defaultImageColor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;

/**
 * Class <code>TextSymbol</code> implements a decorated text symbol.
 *
 * @author Hervé Bitteur
 */
public class TextSymbol
        extends ShapeSymbol
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(TextSymbol.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The text font family to use. */
    protected volatile TextFamily textFamily;

    /** The text string to use. */
    protected final String str;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a <code>TextSymbol</code>.
     *
     * @param shape       the precise shape
     * @param musicFamily the MusicFont family (irrelevant in fact, but cannot be null...)
     * @param str         the text to draw
     */
    public TextSymbol (Shape shape,
                       MusicFamily musicFamily,
                       String str)
    {
        super(shape, musicFamily);
        this.str = str;
    }

    /**
     * Create a <code>TextSymbol</code>.
     *
     * @param shape      the precise shape
     * @param textFamily the TextFont family
     * @param str        the text to draw
     */
    public TextSymbol (Shape shape,
                       TextFamily textFamily,
                       String str)
    {
        super(shape, null);
        this.textFamily = textFamily;
        this.str = str;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //------------//
    // buildImage //
    //------------//
    /**
     * Build an image that represents the related shape, using the provided specific text font.
     *
     * @param textFont properly-scaled font (for interline and zoom)
     * @return the image built, or null if failed
     */
    public SymbolImage buildImage (TextFont textFont)
    {
        // Params
        Params p = getParams(textFont);

        // Allocate image of proper size
        Rectangle intRect = p.rect.getBounds();
        SymbolImage img = new SymbolImage(
                intRect.width,
                intRect.height,
                PointUtil.rounded(p.offset));

        // Paint the image
        Graphics2D g = (Graphics2D) img.getGraphics();

        g.setColor(defaultImageColor);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        paint(g, p, new Point(0, 0), TOP_LEFT);

        return img;
    }

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
        TextFamily theTextFamily = textFamily;

        if (theTextFamily == null) {
            theTextFamily = TextFont.getCurrentFamily();
            if (theTextFamily == null)
                theTextFamily = TextFamily.SansSerif;
        }

        final int fontSize = (int) Math.rint(font.getSize2D() * RATIO_TINY);
        final TextFont textFont = new TextFont(
                theTextFamily.getFontName(),
                null,
                Font.PLAIN,
                fontSize);

        final MyParams p = new MyParams();
        p.layout = textFont.layout(str);
        p.rect = p.layout.getBounds();

        final Point2D baseLoc = new Point2D.Double(0, 0);
        final FontInfo fontInfo = FontInfo.createDefault(fontSize);
        p.model = new WordInter.Model(str, baseLoc, fontInfo);

        return p;
    }

    //-----------//
    // getParams //
    //-----------//
    protected MyParams getParams (TextFont textFont)
    {
        final MyParams p = new MyParams();

        p.layout = textFont.layout(str);
        p.rect = p.layout.getBounds();

        final Point2D baseLoc = new Point2D.Double(0, 0);
        final FontInfo fontInfo = new FontInfo(textFont.getSize(), textFont.getFontName());
        p.model = new WordInter.Model(str, baseLoc, fontInfo);

        return p;
    }

    //---------------//
    // getTextFamily //
    //---------------//
    public TextFamily getTextFamily ()
    {
        return textFamily;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //----------//
    // MyParams //
    //----------//
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
