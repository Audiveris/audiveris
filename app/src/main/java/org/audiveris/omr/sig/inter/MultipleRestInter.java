//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                M u l t i p l e R e s t I n t e r                               //
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.ui.ObjectUIModel;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.relation.MultipleRestCountRelation;
import org.audiveris.omr.sig.relation.MultipleRestSerifRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>MultipleRestInter</code> represents a multiple measure rest drawn as a
 * thick horizontal line centered on staff middle line with serifs at both ends,
 * completed by a time-like number drawn above the staff.
 * <p>
 * <img alt="MultipleRest" src="https://en.wikipedia.org/wiki/File:15_bars_multirest.png">
 * <p>
 * For Audiveris, this music item is constructed by:
 * <ul>
 * <li>A MultipleRestInter, which is similar to a horizontal beam
 * <li>Two {@link VerticalSerifInter} instances, similar to stems, linked by
 * {@link MultipleRestSerifRelation}
 * <li>A {@link MeasureCountInter}, similar to a time number, linked by a
 * {@link MultipleRestCountRelation}
 * </ul>
 *
 * @see VerticalSerifInter
 * @see MeasureCountInter
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "multiple-rest")
public class MultipleRestInter
        extends AbstractHorizontalInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(MultipleRestInter.class);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Meant for JAXB.
     */
    private MultipleRestInter ()
    {
        super((Shape) null, (GradeImpacts) null, null, 0);
    }

    /**
     * Creates manually a new <code>MultipleRestInter</code> ghost object.
     *
     * @param grade quality grade
     */
    public MultipleRestInter (Double grade)
    {
        super(Shape.MULTIPLE_REST, grade);
        setAbnormal(true);
    }

    /**
     * Creates a new <code>MultipleRestInter</code> object.
     *
     * @param grade  evaluation grade
     * @param median median beam line
     * @param height beam height
     */
    public MultipleRestInter (Double grade,
                              Line2D median,
                              double height)
    {
        super(Shape.MULTIPLE_REST, grade, median, height);
        setAbnormal(true);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //---------------//
    // checkAbnormal //
    //---------------//
    /**
     * Check if this multiple rest is connected to a measure count.
     *
     * @return true if abnormal
     */
    @Override
    public boolean checkAbnormal ()
    {
        setAbnormal(!sig.hasRelation(this, MultipleRestCountRelation.class));

        return isAbnormal();
    }

    //------------//
    // deriveFrom //
    //------------//
    @Override
    public boolean deriveFrom (ShapeSymbol symbol,
                               Sheet sheet,
                               MusicFont font,
                               Point dropLocation)
    {
        // First call needed to get bounds
        super.deriveFrom(symbol, sheet, font, dropLocation);

        // If within staff height, we snap ordinate to target pitch 0 (middle line)
        if (staff != null) {
            if (staff.isTablature()) {
                return false;
            }

            final Point center = getCenter();

            if (staff.contains(center)) {
                final double y = staff.pitchToOrdinate(center.getX(), 0);
                dropLocation.y = (int) Math.rint(y);

                // Final call with refined dropLocation
                super.deriveFrom(symbol, sheet, font, dropLocation);
            }
        }

        return true;
    }

    //-----------//
    // getEditor //
    //-----------//
    @Override
    public InterEditor getEditor ()
    {
        return new Editor(this);
    }

    //-----------------//
    // getMeasureCount //
    //-----------------//
    /**
     * Report the measure count related to this multiple measure rest.
     *
     * @return the related MeasureCountInter or null
     */
    public MeasureCountInter getMeasureCount ()
    {
        for (Relation rel : getSig().getRelations(this, MultipleRestCountRelation.class)) {
            return (MeasureCountInter) getSig().getOppositeInter(this, rel);
        }

        return null;
    }

    //-----------//
    // setBounds //
    //-----------//
    @Override
    public void setBounds (Rectangle bounds)
    {
        super.setBounds(bounds);

        if (bounds != null) {
            median = new Line2D.Double(
                    bounds.x,
                    bounds.y + 0.5 * bounds.height,
                    bounds.x + bounds.width,
                    bounds.y + 0.5 * bounds.height);

            height = 0.3 * bounds.height; // Very approximate, but needed for a non-null area
            computeArea();
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //--------//
    // Editor //
    //--------//
    /**
     * User editor for a multipleRest.
     * <p>
     * There are 3 handles:
     * <ul>
     * <li>Left handle, extending horizontally
     * <li>Middle handle, moving the whole rest horizontally
     * <li>Right handle, extending horizontally
     * </ul>
     */
    private static class Editor
            extends InterEditor
    {
        private final Model originalModel;

        private final Model model;

        public Editor (MultipleRestInter rest)
        {
            super(rest);

            originalModel = new Model(rest.median);
            model = new Model(rest.median);

            final Point2D middle = PointUtil.middle(model.p1, model.p2);

            // Move left, only horizontally
            handles.add(new Handle(model.p1)
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    if (dx == 0) {
                        return false;
                    }

                    PointUtil.add(model.p1, dx, 0);
                    PointUtil.add(middle, dx / 2.0, 0);

                    return true;
                }
            });

            // Global move, only horizontally
            handles.add(selectedHandle = new Handle(middle)
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    if (dx == 0) {
                        return false;
                    }

                    for (Handle handle : handles) {
                        PointUtil.add(handle.getPoint(), dx, 0);
                    }

                    return true;
                }
            });

            // Move right, only horizontally
            handles.add(new Handle(model.p2)
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    if (dx == 0) {
                        return false;
                    }

                    PointUtil.add(middle, dx / 2.0, 0);
                    PointUtil.add(model.p2, dx, 0);

                    return true;
                }
            });
        }

        @Override
        protected void doit ()
        {
            final MultipleRestInter rest = (MultipleRestInter) object;
            rest.median.setLine(model.p1, model.p2);
            rest.computeArea(); // Set bounds also

            super.doit(); // No more glyph
        }

        @Override
        public void undo ()
        {
            final MultipleRestInter rest = (MultipleRestInter) object;
            rest.median.setLine(originalModel.p1, originalModel.p2);
            rest.computeArea(); // Set bounds also

            super.undo();
        }
    }

    //-------//
    // Model //
    //-------//
    public static class Model
            implements ObjectUIModel
    {
        // Left point of median line
        public final Point2D p1;

        // Right point of median line
        public final Point2D p2;

        public Model (double x1,
                      double y1,
                      double x2,
                      double y2)
        {
            p1 = new Point2D.Double(x1, y1);
            p2 = new Point2D.Double(x2, y2);
        }

        public Model (Line2D line)
        {
            p1 = line.getP1();
            p2 = line.getP2();
        }

        @Override
        public void translate (double dx,
                               double dy)
        {
            PointUtil.add(p1, dx, dy);
            PointUtil.add(p2, dx, dy);
        }
    }
}
