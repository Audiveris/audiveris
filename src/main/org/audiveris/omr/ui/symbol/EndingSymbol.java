//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    E n d i n g S y m b o l                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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

import static org.audiveris.omr.ui.symbol.Alignment.TOP_LEFT;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sig.inter.EndingInter;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class <code>EndingSymbol</code> implements an ending symbol.
 *
 * @author Hervé Bitteur
 */
public class EndingSymbol
        extends ShapeSymbol
{
    //~ Instance fields ----------------------------------------------------------------------------

    final boolean withRightLeg;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create an EndingSymbol.
     *
     * @param withRightLeg true to provide the optional right leg
     * @param family       the musicFont family
     */
    public EndingSymbol (boolean withRightLeg,
                         MusicFamily family)
    {
        super(withRightLeg ? Shape.ENDING_WRL : Shape.ENDING, family);
        this.withRightLeg = withRightLeg;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // getModel //
    //----------//
    @Override
    public EndingInter.Model getModel (MusicFont font,
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
        final MyParams p = new MyParams();
        final double width = font.getStaffInterline() * 4.0;
        final double height = font.getStaffInterline() * 1.0;
        p.rect = new Rectangle2D.Double(0, 0, width, height);

        p.model.topLeft = new Point2D.Double(0, 0);
        p.model.topRight = new Point2D.Double(width - 1, 0);
        p.model.bottomLeft = new Point2D.Double(0, height - 1);

        if (withRightLeg) {
            p.model.bottomRight = new Point2D.Double(width - 1, height - 1);
        }

        /** For an Ending symbol, focus center is middle of upper horizontal segment. */
        p.offset = new Point2D.Double(0, -height / 2);

        return p;
    }

    //--------//
    // getTip //
    //--------//
    @Override
    public String getTip ()
    {
        return shape + (withRightLeg ? " (w/ right leg)" : "");
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

    //~ Inner Classes ------------------------------------------------------------------------------

    //--------//
    // Params //
    //--------//
    protected static class MyParams
            extends ShapeSymbol.Params
    {

        // offset: used
        // layout: not used
        // rect:   global image
        //
        // model
        EndingInter.Model model = new EndingInter.Model();
    }
}
