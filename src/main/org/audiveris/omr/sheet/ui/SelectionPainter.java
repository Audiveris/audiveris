//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S e l e c t i o n P a i n t e r                                //
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
import org.audiveris.omr.ui.util.UIUtil;
import static org.audiveris.omr.ui.symbol.Alignment.AREA_CENTER;

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
import org.audiveris.omr.sig.inter.WordInter;

/**
 * Class {@code SelectionPainter} is meant to paint just selected items.
 *
 * @author Hervé Bitteur
 */
public class SelectionPainter
        extends SheetPainter
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SelectionPainter.class);

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

    //-------------//
    // drawSupport //
    //-------------//
    /**
     * Draw the support relation between two inters.
     *
     * @param one          first inter
     * @param two          second inter
     * @param supportClass provided support class
     */
    public void drawSupport (Inter one,
                             Inter two,
                             Class<? extends Relation> supportClass)
    {
        final double zoom = g.getTransform().getScaleX();

        // Draw support line
        final Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 2f);
        g.setColor(NoExclusion.class.isAssignableFrom(supportClass) ? Color.GRAY : Color.GREEN);

        final double r = 2 / zoom; // Radius
        final Point2D oneCenter = one.getRelationCenter();
        g.fill(new Ellipse2D.Double(oneCenter.getX() - r,
                                    oneCenter.getY() - r, 2 * r, 2 * r));

        final Point2D twoCenter = two.getRelationCenter();
        g.fill(new Ellipse2D.Double(twoCenter.getX() - r,
                                    twoCenter.getY() - r, 2 * r, 2 * r));

        final Line2D line = new Line2D.Double(oneCenter.getX(), oneCenter.getY(),
                                              twoCenter.getX(), twoCenter.getY());
        g.draw(line);

        // Print support name at center of line?
        if (zoom >= constants.minZoomForSupportNames.getValue()) {
            final double std = 0.5 * UIUtil.GLOBAL_FONT_RATIO;
            final double z = Math.max(std, zoom);
            final AffineTransform at = AffineTransform.getScaleInstance(std / z, std / z);
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

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio minZoomForSupportNames = new Constant.Ratio(
                2.0,
                "Minimum zoom value to display support names");
    }

    //---------------------//
    // SelectionSigPainter //
    //---------------------//
    private static class SelectionSigPainter
            extends SigPainter
    {

        SelectionSigPainter (Graphics g,
                             Scale scale)
        {
            super(g, scale);
        }

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
