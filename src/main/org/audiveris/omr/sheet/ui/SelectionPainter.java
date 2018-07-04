//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S e l e c t i o n P a i n t e r                                //
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.relation.NoExclusion;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.Relations;
import org.audiveris.omr.sig.ui.SigPainter;
import static org.audiveris.omr.ui.symbol.Alignment.AREA_CENTER;
import org.audiveris.omr.ui.util.UIUtil;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

/**
 * Class {@code SelectionPainter} is meant to paint just selected items.
 *
 * @author Hervé Bitteur
 */
public class SelectionPainter
        extends SheetPainter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SelectionPainter} object.
     *
     * @param sheet the sheet to paint
     * @param g     Graphic context
     */
    public SelectionPainter (Sheet sheet,
                             Graphics g)
    {
        super(sheet, g);

        sigPainter = new SelectionSigPainter(g, sheet.getScale());
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // drawSupport //
    //-------------//
    /**
     * Draw the support relation between two inters.
     *
     * @param one          first inter
     * @param two          second inter
     * @param supportClass provided support class
     * @param potential    true if support is just potential
     */
    public void drawSupport (Inter one,
                             Inter two,
                             Class<? extends Relation> supportClass,
                             boolean potential)
    {
        // Draw support line, using dash and specific color for potential relation
        final Stroke oldStroke = potential ? UIUtil.setAbsoluteDashedStroke(g, 1f)
                : UIUtil.setAbsoluteStroke(g, 1f);
        g.setColor(
                potential ? Color.PINK
                        : (NoExclusion.class.isAssignableFrom(supportClass) ? Color.GRAY : Color.GREEN));

        final double r = 2; // Radius
        final Point oneCenter = one.getRelationCenter();
        Ellipse2D e1 = new Ellipse2D.Double(oneCenter.x - r, oneCenter.y - r, 2 * r, 2 * r);
        g.fill(e1);

        final Point twoCenter = two.getRelationCenter();
        Ellipse2D e2 = new Ellipse2D.Double(twoCenter.x - r, twoCenter.y - r, 2 * r, 2 * r);
        g.fill(e2);

        final Line2D line = new Line2D.Double(oneCenter, twoCenter);
        g.draw(line);

        // Print support name at center of line?
        final double zoom = g.getTransform().getScaleX();

        if (zoom >= constants.minZoomForSupportNames.getValue()) {
            final double z = Math.max(0.5, zoom);
            final AffineTransform at = AffineTransform.getScaleInstance(0.5 / z, 0.5 / z);
            final TextLayout layout = basicLayout(Relations.nameOf(supportClass), at);
            paint(layout, GeoUtil.centerOf(line.getBounds()), AREA_CENTER);
        }
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the selected inter, using some highlighting
     *
     * @param inter the selected inter to render
     */
    public void render (Inter inter)
    {
        inter.accept(sigPainter);
    }

    //---------------//
    // getSigPainter //
    //---------------//
    @Override
    protected SigPainter getSigPainter ()
    {
        return new SelectionSigPainter(g, sheet.getScale());
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Ratio minZoomForSupportNames = new Constant.Ratio(
                2.0,
                "Minimum zoom value to display support names");
    }

    //---------------------//
    // SelectionSigPainter //
    //---------------------//
    private class SelectionSigPainter
            extends SigPainter
    {
        //~ Constructors ---------------------------------------------------------------------------

        public SelectionSigPainter (Graphics g,
                                    Scale scale)
        {
            super(g, scale);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected void setColor (Inter inter)
        {
            // Use complementary of inter color
            g.setColor(UIUtil.complementaryColor(inter.getColor()));
        }
    }
}
