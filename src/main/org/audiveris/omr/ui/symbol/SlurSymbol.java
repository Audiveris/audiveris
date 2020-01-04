//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S l u r S y m b o l                                       //
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
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sig.inter.SlurInter;
import static org.audiveris.omr.ui.symbol.Alignment.*;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code SlurSymbol} implements a slur symbol.
 *
 * @author Hervé Bitteur
 */
public class SlurSymbol
        extends ShapeSymbol
{

    final boolean above;

    /**
     * Create a SlurSymbol.
     *
     * @param above true for above, false for below
     */
    public SlurSymbol (boolean above)
    {
        this(above, false);
    }

    /**
     * Create a SlurSymbol.
     *
     * @param above  true for above, false for below
     * @param isIcon true for an icon
     */
    protected SlurSymbol (boolean above,
                          boolean isIcon)
    {
        super(isIcon, above ? Shape.SLUR_ABOVE : Shape.SLUR_BELOW, false);
        this.above = above;
    }

    //----------//
    // getModel //
    //----------//
    @Override
    public SlurInter.Model getModel (MusicFont font,
                                     Point location,
                                     Alignment alignment)
    {
        MyParams p = getParams(font);
        Point2D loc = alignment.translatedPoint(TOP_LEFT, p.rect, location);
        loc = PointUtil.subtraction(loc, p.offset);
        p.model.translate(loc.getX(), loc.getY());

        return p.model;
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new SlurSymbol(above, true);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont font)
    {
        final MyParams p = new MyParams();
        final int il = font.getStaffInterline();
        final double w = 3 * il;
        final double h = il;
        p.rect = new Rectangle2D.Double(0, 0, w, h);

        p.model = new SlurInter.Model(above //  /--\ vs \--/
                ? new CubicCurve2D.Double(0, h, w / 5, 0, 4 * w / 5, 0, w, h)
                : new CubicCurve2D.Double(0, 0, w / 5, h, 4 * w / 5, h, w, 0));

        p.offset = new Point2D.Double(0, above ? h / 2 : -h / 2);

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
        CubicCurve2D curve = new CubicCurve2D.Double();
        curve.setCurve(p.model.points, 0);
        g.draw(curve);
    }

    //--------//
    // Params //
    //--------//
    protected static class MyParams
            extends BasicSymbol.Params
    {

        // offset: used
        // layout: not used
        // rect:   global image
        //
        // model
        SlurInter.Model model;
    }
}
