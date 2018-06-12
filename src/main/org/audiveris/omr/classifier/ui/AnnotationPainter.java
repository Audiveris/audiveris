//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A n n o t a t i o n P a i n t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright ©  Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.classifier.ui;

import org.audiveris.omr.classifier.Annotation;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.ui.SimpleSheetPainter;
import org.audiveris.omr.ui.util.UIUtil;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.Collection;

/**
 * Class {@code AnnotationPainter} is a helper class to paint all annotations on top
 * of a sheet image background.
 *
 * @author Hervé Bitteur
 */
public abstract class AnnotationPainter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    //~ Constructors -------------------------------------------------------------------------------
    private AnnotationPainter ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // paint //
    //-------//
    /**
     * Paint all sheet annotations on the provided graphics context.
     *
     * @param sheet the containing sheet
     * @param g     Graphic context
     */
    public static void paint (Sheet sheet,
                              Graphics2D g)
    {
        final Rectangle clip = g.getClipBounds();

        // Display underlying image
        RunTable table = sheet.getPicture().getTable(Picture.TableKey.BINARY);
        table.render(g, new Point(0, 0));

        Collection<Annotation> annotations = sheet.getAnnotationIndex().getEntities();

        // Bounds
        Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);
        g.setColor(Color.GREEN);

        for (Annotation a : annotations) {
            Rectangle b = a.getBounds();

            if ((clip == null) || clip.intersects(b)) {
                g.draw(b);
            }
        }

        // OmrShape is displayed only when zoom is high enough
        // When displayed, it uses the same size, whatever the zoom
        double ratio = g.getTransform().getScaleX();

        if (ratio >= constants.minZoomForNames.getValue()) {
            Font oldFont = g.getFont();
            g.setFont(oldFont.deriveFont((float) (constants.nameFontSize.getValue() / ratio)));
            g.setColor(Color.MAGENTA);

            for (Annotation a : annotations) {
                Rectangle b = a.getBounds();

                if ((clip == null) || clip.intersects(b)) {
                    g.drawString(a.getOmrShape().name(), b.x, b.y);
                }
            }

            g.setFont(oldFont);
        }

        // Restore graphics context
        g.setStroke(oldStroke);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------------------//
    // SimpleAnnotationPainter //
    //-------------------------//
    public static class SimpleAnnotationPainter
            implements SimpleSheetPainter
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void paint (Sheet sheet,
                           Graphics2D g)
        {
            AnnotationPainter.paint(sheet, g);
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Ratio minZoomForNames = new Constant.Ratio(
                0.5,
                "Minimum zoom ratio to display shape names");

        private final Constant.Integer nameFontSize = new Constant.Integer(
                "pointSize",
                10,
                "Font size for shape names");
    }
}
