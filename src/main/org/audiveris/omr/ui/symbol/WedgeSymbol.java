//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      W e d g e S y m b o l                                     //
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

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sig.inter.WedgeInter;
import static org.audiveris.omr.ui.symbol.Alignment.TOP_LEFT;

/**
 * Class {@code WedgeSymbol} implements a wedge symbol.
 * <p>
 * It looks like {@literal "<"} for CRESCENDO and {@literal ">"} for DIMINUENDO.
 *
 * @author Hervé Bitteur
 */
public class WedgeSymbol
        extends ShapeSymbol
{

    /**
     * Create a WedgeSymbol.
     *
     * @param shape CRESCENDO or DIMINUENDO
     */
    public WedgeSymbol (Shape shape)
    {
        this(shape, false);
    }

    /**
     * Create a WedgeSymbol.
     *
     * @param shape  CRESCENDO or DIMINUENDO
     * @param isIcon true for an icon
     */
    protected WedgeSymbol (Shape shape,
                           boolean isIcon)
    {
        super(isIcon, shape, false);
    }

    //----------//
    // getModel //
    //----------//
    @Override
    public WedgeInter.Model getModel (MusicFont font,
                                      Point location,
                                      Alignment alignment)
    {
        MyParams p = (MyParams) getParams(font);
        Point2D loc = alignment.translatedPoint(TOP_LEFT, p.rect, location);
        p.model.translate(loc.getX(), loc.getY());

        return p.model;
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new WedgeSymbol(shape, true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();

        double interline = font.getStaffInterline();
        final double w = 5 * interline;
        final double h = 1.5 * interline;

        final Line2D l1;
        final Line2D l2;

        if (shape == Shape.CRESCENDO) {
            l1 = new Line2D.Double(0, h / 2, w, 0);
            l2 = new Line2D.Double(0, h / 2, w, h);
        } else {
            l1 = new Line2D.Double(0, 0, w, h / 2);
            l2 = new Line2D.Double(0, h, w, h / 2);
        }

        // Hack to fully display lower leg in symbol image
        p.rect = new Rectangle2D.Double(0,
                                        0,
                                        w,
                                        h + 1);

        p.model = new WedgeInter.Model(l1, l2);

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
        Point2D loc = alignment.translatedPoint(TOP_LEFT, p.rect, location);
        p.model.translate(loc.getX(), loc.getY());
        g.draw(new Line2D.Double(p.model.top1, p.model.top2));
        g.draw(new Line2D.Double(p.model.bot1, p.model.bot2));
    }

    //--------//
    // Params //
    //--------//
    protected static class MyParams
            extends BasicSymbol.Params
    {

        // offset: not used
        // layout: not used
        // rect:   global image
        //
        // model
        WedgeInter.Model model;
    }

}
