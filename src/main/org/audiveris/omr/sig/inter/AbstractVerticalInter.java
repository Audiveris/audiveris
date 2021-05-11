//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            A b s t r a c t V e r t i c a l I n t e r                           //
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.AreaUtil;
import org.audiveris.omr.math.PointUtil;
import static org.audiveris.omr.run.Orientation.VERTICAL;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.sig.ui.InterEditor.Handle;
import org.audiveris.omr.sig.ui.InterUIModel;
import org.audiveris.omr.util.Jaxb;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code AbstractVerticalInter} is the basis for rather vertical inter classes.
 * <ul>
 * <li>{@link StemInter} and {@link ArpeggiatoInter} that can be smaller or taller than staff
 * height.
 * <li>{@link BarlineInter} and {@link BracketInter} whose height is exactly the staff height.
 * </ul>
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class AbstractVerticalInter
        extends AbstractInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Item width. */
    @XmlAttribute
    @XmlJavaTypeAdapter(Jaxb.Double1Adapter.class)
    protected Double width;

    /** Median line, from top to bottom. */
    @XmlElement
    @XmlJavaTypeAdapter(Jaxb.Line2DAdapter.class)
    protected Line2D median;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code AbstractVerticalInter} object.
     *
     * @param glyph   the underlying glyph
     * @param shape   the assigned shape
     * @param impacts the assignment details
     * @param median  the median line
     * @param width   the line width
     */
    public AbstractVerticalInter (Glyph glyph,
                                  Shape shape,
                                  GradeImpacts impacts,
                                  Line2D median,
                                  Double width)
    {
        super(glyph, null, shape, impacts);

        this.median = (median == null) ? null
                : new Line2D.Double(
                        median.getX1(),
                        median.getY1(),
                        median.getX2(),
                        median.getY2());
        this.width = width;

        if ((median != null) && (width != null)) {
            computeArea();
        }
    }

    /**
     * Creates a new {@code AbstractVerticalInter} object.
     *
     * @param glyph   the underlying glyph
     * @param shape   the assigned shape
     * @param impacts the assignment details
     */
    public AbstractVerticalInter (Glyph glyph,
                                  Shape shape,
                                  GradeImpacts impacts)
    {
        super(glyph, null, shape, impacts);

        if (glyph != null) {
            width = glyph.getMeanThickness(VERTICAL);
            median = glyph.getCenterLine();
            computeArea();
        }
    }

    /**
     * Creates a new {@code AbstractVerticalInter} object.
     *
     * @param glyph  the underlying glyph
     * @param shape  the assigned shape
     * @param grade  the assignment quality
     * @param median the median line
     * @param width  the line width
     */
    public AbstractVerticalInter (Glyph glyph,
                                  Shape shape,
                                  Double grade,
                                  Line2D median,
                                  Double width)
    {
        super(glyph, null, shape, grade);

        this.median = (median == null) ? null
                : new Line2D.Double(
                        median.getX1(),
                        median.getY1(),
                        median.getX2(),
                        median.getY2());
        this.width = width;

        if ((median != null) && (width != null)) {
            computeArea();
        }
    }

    /**
     * Creates a new {@code AbstractVerticalInter} object.
     *
     * @param glyph the underlying glyph
     * @param shape the assigned shape
     * @param grade the assignment quality
     */
    public AbstractVerticalInter (Glyph glyph,
                                  Shape shape,
                                  Double grade)
    {
        super(glyph, null, shape, grade);

        if (glyph != null) {
            width = glyph.getMeanThickness(VERTICAL);
            ///setMedian(glyph.getStartPoint(VERTICAL), glyph.getStopPoint(VERTICAL));
            median = glyph.getCenterLine();
            computeArea();
        }
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

    //-----------//
    // getBottom //
    //-----------//
    /**
     * @return the bottom
     */
    public Point2D getBottom ()
    {
        if (median == null) {
            return null;
        }

        return new Point2D.Double(median.getX2(), median.getY2());
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (bounds != null) {
            return new Rectangle(bounds);
        }

        if ((area == null) && (median != null) && (getWidth() != null)) {
            computeArea();
        }

        if (area != null) {
            return new Rectangle(bounds = area.getBounds());
        }

        if (glyph != null) {
            return new Rectangle(bounds = glyph.getBounds());
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
                    bounds.x + (bounds.width / 2.0),
                    bounds.y,
                    bounds.x + (bounds.width / 2.0),
                    bounds.y + bounds.height);
            width = Double.valueOf(bounds.width);

            computeArea();
        }
    }

    //-----------//
    // getEditor //
    //-----------//
    @Override
    public InterEditor getEditor ()
    {
        return new Editor(this, true);
    }

    //-----------//
    // getMedian //
    //-----------//
    /**
     * Report item median line.
     *
     * @return copy of the median line
     */
    public Line2D getMedian ()
    {
        return new Line2D.Double(median.getX1(), median.getY1(), median.getX2(), median.getY2());
    }

    //-----------//
    // setMedian //
    //-----------//
    /**
     * Assign median via top and bottom points.
     *
     * @param top    upper point
     * @param bottom lower point
     */
    public final void setMedian (Point2D top,
                                 Point2D bottom)
    {
        if (median == null) {
            median = new Line2D.Double(top, bottom);
        } else {
            median.setLine(top, bottom);
        }

        if (getWidth() != null) {
            computeArea();
        }
    }

    //--------//
    // getTop //
    //--------//
    /**
     * @return the top
     */
    public Point2D getTop ()
    {
        if (median == null) {
            return null;
        }

        return median.getP1();
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report item width.
     *
     * @return the width
     */
    public Double getWidth ()
    {
        return width;
    }

    //----------//
    // setWidth //
    //----------//
    /**
     * Set item width.
     *
     * @param width item width
     */
    public final void setWidth (double width)
    {
        this.width = width;
    }

    //----------------//
    // afterUnmarshal //
    //----------------//
    /**
     * Called after all the properties (except IDREF) are unmarshalled for this object,
     * but before this object is set to the parent object.
     */
    @SuppressWarnings("unused")
    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        if ((median != null) && (getWidth() != null)) {
            computeArea();
        }
    }

    //-------------//
    // computeArea //
    //-------------//
    protected void computeArea ()
    {
        setArea(AreaUtil.verticalRibbon(new Path2D.Double(median), getWidth()));

        // Define precise bounds based on this path
        bounds = getArea().getBounds();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------//
    // Model //
    //-------//
    public static class Model
            implements InterUIModel
    {

        // Upper point of median line
        public Point2D p1;

        // Lower point of median line
        public Point2D p2;

        // Width
        public Double width;

        public Model (Point2D p1,
                      Point2D p2)
        {
            this.p1 = p1;
            this.p2 = p2;
        }

        @Override
        public void translate (double dx,
                               double dy)
        {
            PointUtil.add(p1, dx, dy);
            PointUtil.add(p2, dx, dy);
        }

        @Override
        public String toString ()
        {
            return new StringBuilder("avModel{")
                    .append("p1:").append(p1)
                    .append(" p2:").append(p2)
                    .append(" width:").append(width)
                    .append('}').toString();
        }
    }

    //--------//
    // Editor //
    //--------//
    /**
     * User editor for an AbstractVerticalInter (stem, arpeggiato, bar/bracket connector).
     * <p>
     * For a vertical inter, there are 3 handles:
     * <ul>
     * <li>top handle, moving vertically
     * <li>middle handle, moving the whole item horizontally
     * <li>bottom handle, moving vertically
     * </ul>
     */
    protected static class Editor
            extends InterEditor
    {

        private final Model originalModel;

        private final Model model;

        /**
         * Create an editor adapted to actual item.
         *
         * @param vert the inter to edit
         * @param full true for 3 handles (top, middle, bottom), false for just the middle one
         */
        public Editor (AbstractVerticalInter vert,
                       boolean full)
        {
            super(vert);

            originalModel = new Model(vert.getTop(), vert.getBottom());
            model = new Model(vert.getTop(), vert.getBottom());

            final Point2D middle = PointUtil.middle(model.p1, model.p2);

            // Move top, only vertically
            if (full) {
                handles.add(
                        new Handle(model.p1)
                {
                    @Override
                    public boolean move (Point vector)
                    {
                        final int dy = vector.y;

                        if (dy == 0) {
                            return false;
                        }

                        PointUtil.add(model.p1, 0, dy); // Data & handle
                        PointUtil.add(middle, 0, dy / 2.0); // Handle

                        return true;
                    }
                });
            }

            // Global move, only horizontally
            handles.add(
                    selectedHandle = new Handle(middle)
            {
                @Override
                public boolean move (Point vector)
                {
                    final int dx = vector.x;

                    if (dx == 0) {
                        return false;
                    }

                    // Data (and shared handles if any)
                    PointUtil.add(model.p1, dx, 0);
                    PointUtil.add(model.p2, dx, 0);

                    // Handle (middle)
                    PointUtil.add(middle, dx, 0);

                    return true;
                }
            });

            // Bottom move, only vertically
            if (full) {
                handles.add(
                        new Handle(model.p2)
                {
                    @Override
                    public boolean move (Point vector)
                    {
                        final int dy = vector.y;

                        if (dy == 0) {
                            return false;
                        }

                        PointUtil.add(model.p2, 0, dy); // Data & handle
                        PointUtil.add(middle, 0, dy / 2.0); // Handle

                        return true;
                    }
                });
            }
        }

        @Override
        protected void doit ()
        {
            final AbstractVerticalInter vert = (AbstractVerticalInter) inter;
            vert.setMedian(model.p1, model.p2); // Set bounds also

            super.doit(); // No more glyph

            updateChords();
        }

        @Override
        public void undo ()
        {
            final AbstractVerticalInter vert = (AbstractVerticalInter) inter;
            vert.setMedian(originalModel.p1, originalModel.p2); // Set bounds also

            super.undo();

            updateChords();
        }

        protected void updateChords ()
        {
            //void by default
        }
    }
}
