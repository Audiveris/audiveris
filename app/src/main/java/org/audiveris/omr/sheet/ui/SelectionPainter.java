//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S e l e c t i o n P a i n t e r                                //
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.sig.relation.NoExclusion;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.Relations;
import org.audiveris.omr.sig.relation.Support;
import static org.audiveris.omr.ui.symbol.Alignment.AREA_CENTER;
import org.audiveris.omr.ui.util.UIUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Stroke;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * Class <code>SelectionPainter</code> is meant to paint just selected items.
 *
 * @author Hervé Bitteur
 */
public class SelectionPainter
        extends SheetPainter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SelectionPainter.class);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>SelectionPainter</code> object.
     *
     * @param sheet the sheet to paint
     * @param g     Graphic context
     */
    public SelectionPainter (Sheet sheet,
                             Graphics g)
    {
        super(sheet, g, false, false);

        sigPainter = new SelectionSigPainter();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // drawLink //
    //----------//
    /**
     * Draw the link between two inters.
     *
     * @param one      first inter
     * @param two      second inter
     * @param relation the relation instance
     */
    public void drawLink (Inter one,
                          Inter two,
                          Relation relation)
    {
        final double zoom = g.getTransform().getScaleX();

        // Draw link line
        final Stroke oldStroke = UIUtil.setAbsoluteDashedStroke(g, 1f);
        final Class<? extends Relation> linkClass = relation.getClass();
        g.setColor(
                NoExclusion.class.isAssignableFrom(linkClass) ? Color.GRAY
                        : Support.class.isAssignableFrom(linkClass) ? Color.GREEN.darker()
                                : Color.ORANGE);

        final double r = 2 / zoom; // Radius
        final Point2D oneCenter = one.getRelationCenter(relation);
        g.fill(new Ellipse2D.Double(oneCenter.getX() - r, oneCenter.getY() - r, 2 * r, 2 * r));

        final Point2D twoCenter = two.getRelationCenter(relation);
        g.fill(new Ellipse2D.Double(twoCenter.getX() - r, twoCenter.getY() - r, 2 * r, 2 * r));

        final Line2D line = new Line2D.Double(oneCenter, twoCenter);
        g.draw(line);
        g.setStroke(oldStroke);

        // Print link name at center of line?
        if (zoom >= constants.minZoomForLinkNames.getValue()) {
            final double std = 0.5 * UIUtil.GLOBAL_FONT_RATIO;
            final double z = Math.max(std, zoom);
            final AffineTransform at = AffineTransform.getScaleInstance(std / z, std / z);
            final TextLayout layout = basicLayout(Relations.nameOf(linkClass), at);
            paint(layout, GeoUtil.center2D(line.getBounds()), AREA_CENTER);
        }
    }

    //---------------//
    // getSigPainter //
    //---------------//
    @Override
    protected SigPainter getSigPainter ()
    {
        return sigPainter;
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

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Ratio minZoomForLinkNames = new Constant.Ratio(
                2.0,
                "Minimum zoom value to display link names");
    }

    //---------------------//
    // SelectionSigPainter //
    //---------------------//
    private class SelectionSigPainter
            extends SigPainter
    {
        @Override
        protected void setColor (Inter inter)
        {
            g.setColor(UIUtil.selectionColor(inter.getColor()));
        }

        @Override
        protected boolean splitMirrors ()
        {
            return true;
        }

        @Override
        public void visit (WordInter word)
        {
            // Usually, words are displayed via their containing sentence
            // But a selected word must be rendered on its own
            paintWord(word, word.getFontInfo());
        }
    }
}
