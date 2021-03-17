//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   B r a c k e t S y m b o l                                    //
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
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sig.inter.AbstractVerticalInter;
import static org.audiveris.omr.ui.symbol.Alignment.*;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code BracketSymbol} displays a BRACKET symbol: [
 * <p>
 * <img src="doc-files/BracketUpperSerif.png" alt="Bracket upper serif">
 * <p>
 * <img src="doc-files/BracketLowerSerif.png" alt="Bracket lower serif">
 *
 * @author Hervé Bitteur
 */
public class BracketSymbol
        extends ShapeSymbol
{
    //~ Static fields/initializers -----------------------------------------------------------------

    // The upper serif
    private static final BasicSymbol upperSymbol = Symbols.SYMBOL_BRACKET_UPPER_SERIF;

    // The lower serif
    private static final BasicSymbol lowerSymbol = Symbols.SYMBOL_BRACKET_LOWER_SERIF;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a BracketSymbol (which is made of upper and lower parts).
     */
    public BracketSymbol ()
    {
        this(false);
    }

    /**
     * Create a BracketSymbol (which is made of upper and lower parts)
     *
     * @param isIcon true for an icon
     */
    protected BracketSymbol (boolean isIcon)
    {
        super(isIcon, Shape.BRACKET, false);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // getModel //
    //----------//
    @Override
    public AbstractVerticalInter.Model getModel (MusicFont font,
                                                 Point location,
                                                 Alignment alignment)
    {
        MyParams p = getParams(font);

        Point2D loc = alignment.translatedPoint(AREA_CENTER, p.rect, location);

        // Location pointed by the mouse is in fact the center of trunk (not the area center)
        // So, let's retrieve the top left corner manually
        PointUtil.add(loc, -p.model.width / 2, -p.rect.getHeight() / 2);

        p.model.translate(loc.getX(), loc.getY());

        return p.model;
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new BracketSymbol(true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        AffineTransform at = isIcon ? tiny : null;
        p.upperLayout = font.layout(upperSymbol.getString(), at);
        p.layout = font.layout(Shape.THICK_BARLINE, at);
        p.lowerLayout = font.layout(lowerSymbol.getString(), at);

        Rectangle2D upperRect = p.upperLayout.getBounds();
        Rectangle2D trunkRect = p.layout.getBounds();
        Rectangle2D lowerRect = p.lowerLayout.getBounds();
        double width = trunkRect.getWidth();

        p.model = new AbstractVerticalInter.Model(
                new Point2D.Double(width / 2.0, -upperRect.getY()),
                new Point2D.Double(width / 2.0, -upperRect.getY() + trunkRect.getHeight()));
        p.model.width = width;

        p.rect = new Rectangle2D.Double(
                0,
                0,
                upperRect.getWidth(),
                trunkRect.getHeight() - upperRect.getY() + lowerRect.getY() + lowerRect.getHeight());

        p.offset = new Point2D.Double((-p.rect.getWidth() / 2) + (width / 2), 0);

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
        Point2D loc = alignment.translatedPoint(MIDDLE_LEFT, p.rect, location);
        MusicFont.paint(g, p.layout, loc, MIDDLE_LEFT);

        PointUtil.add(loc, 0, -p.rect.getHeight() / 2);
        MusicFont.paint(g, p.upperLayout, loc, TOP_LEFT);

        PointUtil.add(loc, 0, p.rect.getHeight());
        MusicFont.paint(g, p.lowerLayout, loc, BOTTOM_LEFT);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------//
    // MyParams //
    //----------//
    protected static class MyParams
            extends Params
    {
        // offset: pointing to center of trunk
        // layout: trunk
        // rect:   global image

        // model
        AbstractVerticalInter.Model model;

        // Upper serif
        TextLayout upperLayout;

        // Lower serif
        TextLayout lowerLayout;
    }
}
