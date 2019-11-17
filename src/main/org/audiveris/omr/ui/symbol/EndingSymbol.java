//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    E n d i n g S y m b o l                                     //
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
import org.audiveris.omr.sig.inter.EndingInter;
import static org.audiveris.omr.ui.symbol.Alignment.*;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code EndingSymbol} implements an ending symbol
 *
 * @author Hervé Bitteur
 */
public class EndingSymbol
        extends ShapeSymbol
{

    final boolean withRightLeg;

    /**
     * Create an EndingSymbol.
     *
     * @param withRightLeg true to provide the optional right leg
     */
    public EndingSymbol (boolean withRightLeg)
    {
        this(withRightLeg, false);
    }

    /**
     * Create an EndingSymbol.
     *
     * @param withRightLeg true to provide the optional right leg
     * @param isIcon       true for an icon
     */
    protected EndingSymbol (boolean withRightLeg,
                            boolean isIcon)
    {
        super(isIcon, Shape.ENDING, false);
        this.withRightLeg = withRightLeg;
    }

    //----------//
    // getModel //
    //----------//
    @Override
    public EndingInter.Model getModel (MusicFont font,
                                       Point location,
                                       Alignment alignment)
    {
        MyParams p = getParams(font);
        Point2D loc = alignment.translatedPoint(TOP_LEFT, p.rect, location);
        p.model.translate(loc.getX(), loc.getY());

        return p.model;
    }

    //--------//
    // getTip //
    //--------//
    @Override
    public String getTip ()
    {
        return shape + (withRightLeg ? " (w/ right leg)" : "");
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new EndingSymbol(withRightLeg, true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        MyParams p = new MyParams();
        double width = font.getStaffInterline() * 4.0;
        double height = font.getStaffInterline() * 1.0;
        p.rect = new Rectangle2D.Double(0, 0, width, height);

        p.model.topLeft = new Point2D.Double(0, 0);
        p.model.topRight = new Point2D.Double(width - 1, 0);
        p.model.bottomLeft = new Point2D.Double(0, height);

        if (withRightLeg) {
            p.model.bottomRight = new Point2D.Double(width - 1, height);
        }

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

        g.draw(new Line2D.Double(p.model.topLeft, p.model.topRight));
        g.draw(new Line2D.Double(p.model.topLeft, p.model.bottomLeft));

        if (withRightLeg) {
            g.draw(new Line2D.Double(p.model.topRight, p.model.bottomRight));
        }
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
        EndingInter.Model model = new EndingInter.Model();
    }
}
