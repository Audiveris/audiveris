//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S t a f f L i n e                                       //
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.math.NaturalSpline;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code StaffLine} represents a simple final staff line.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "staff-line")
public class StaffLine
        implements LineInfo
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(StaffLine.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Absolute defining points (including start and stop points). */
    @XmlElement(name = "point")
    @XmlJavaTypeAdapter(Jaxb.Point2DAdapter.class)
    protected final List<Point2D> points = new ArrayList<>();

    /** Mean line thickness. */
    @XmlAttribute
    @XmlJavaTypeAdapter(type = double.class, value = Jaxb.Double1Adapter.class)
    protected final double thickness;

    /** Underlying glyph. */
    @XmlIDREF
    @XmlAttribute(name = "glyph")
    protected Glyph glyph;

    // Transient data
    //---------------
    //
    /** Curved line across all defining points. */
    protected NaturalSpline spline;

    /** Bounding box. */
    protected Rectangle bounds;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code StaffLine} object.
     *
     * @param points    all the defining points
     * @param thickness mean line thickness
     */
    public StaffLine (List<Point2D> points,
                      double thickness)
    {
        this.thickness = thickness;

        for (Point2D p : points) {
            // Adjust point ordinates (0.5 lower than int ordinate values)
            this.points.add(new Point2D.Double(p.getX(), p.getY() + 0.5));
        }
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private StaffLine ()
    {
        this.thickness = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // yTranslated //
    //-------------//
    @Override
    public StaffLine yTranslated (double dy)
    {
        StaffLine virtual = new StaffLine(Collections.emptyList(), thickness);

        for (Point2D p : points) {
            virtual.points.add(new Point2D.Double(p.getX(), p.getY() + dy));
        }

        return virtual;
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (bounds == null) {
            Rectangle newBounds = null;

            for (Point2D p : points) {
                if (newBounds == null) {
                    newBounds = new Rectangle(PointUtil.rounded(p));
                } else {
                    newBounds.add(p);
                }
            }

            // Make sure interior area is not empty
            if (newBounds.height == 0) {
                newBounds.height = 1;
            }

            bounds = newBounds;
        }

        return new Rectangle(bounds);
    }

    //-------------//
    // getEndPoint //
    //-------------//
    @Override
    public Point2D getEndPoint (HorizontalSide side)
    {
        final Point2D end = (side == LEFT) ? points.get(0) : points.get(points.size() - 1);

        return new Point2D.Double(end.getX(), end.getY());
    }

    //----------//
    // getGlyph //
    //----------//
    @Override
    public Glyph getGlyph ()
    {
        return glyph;
    }

    //----------//
    // setGlyph //
    //----------//
    /**
     * Assign the underlying glyph for the whole staff line
     *
     * @param glyph the staff line glyph
     */
    public void setGlyph (Glyph glyph)
    {
        this.glyph = glyph;
    }

    //-----------//
    // getSpline //
    //-----------//
    @Override
    public NaturalSpline getSpline ()
    {
        if (spline == null) {
            spline = NaturalSpline.interpolate(points);
        }

        return spline;
    }

    //--------------//
    // getThickness //
    //--------------//
    @Override
    public double getThickness ()
    {
        return thickness;
    }

    //------------//
    // renderLine //
    //------------//
    @Override
    public void renderLine (Graphics2D g,
                            boolean showPoints,
                            double pointWidth)
    {
        getSpline().render(g, showPoints, pointWidth);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        final StringBuilder sb = new StringBuilder("StaffLine{");

        if (points != null) {
            sb.append("points:").append(points.size());
        }

        sb.append('}');

        return sb.toString();
    }

    //-----//
    // yAt //
    //-----//
    @Override
    public int yAt (int x)
    {
        return (int) Math.rint(yAt((double) x));
    }

    //-----//
    // yAt //
    //-----//
    @Override
    public double yAt (double x)
    {
        Point2D start = getEndPoint(LEFT);
        Point2D stop = getEndPoint(RIGHT);

        if ((x < start.getX()) || (x > stop.getX())) {
            // Extrapolate beyond spline abscissa range, using spline global slope
            double slope = (stop.getY() - start.getY()) / (stop.getX() - start.getX());

            return start.getY() + (slope * (x - start.getX()));
        } else {
            return getSpline().yAtX(x);
        }
    }

    /**
     * Reduce the number of intermediate defining points while respecting the provided
     * maximum ordinate shift.
     * <p>
     * Strategy is as follows:
     * <ol>
     * <li>Start with a spline built with just the two left and right defining points
     * <li>Check spline ordinate at every original defining point vs original defining ordinate
     * <li>If all measured ordinates are OK vs maximum shift, return the current list of points
     * <li>If at least one ordinate is not OK, cut all segments in two
     * <li>If segment length is shorter than original segments length, stay with originals
     * <li>Else, compute the new spline and re-check
     * </ol>
     *
     * @param maxDy         maximum acceptable ordinate shift
     * @param segmentLength original segments length
     */
    public void simplify (double maxDy,
                          int segmentLength)
    {
        final Point2D left = points.get(0);
        final Point2D right = points.get(points.size() - 1);
        final List<Point2D> defs = simplify(left, right, maxDy, segmentLength);

        if (!defs.equals(points)) {
            spline = null;
            points.clear();
            points.addAll(defs);
        }
    }

    private List<Point2D> simplify (Point2D left,
                                    Point2D right,
                                    double maxDy,
                                    int segmentLength)
    {
        final double width = right.getX() - left.getX();
        final List<Point2D> defs = new ArrayList<>();
        defs.add(left);
        defs.add(right);

        while (true) {
            final double segLg = width / (defs.size() - 1);

            if (segLg < segmentLength) {
                logger.debug("Initial    pts:{}", points.size());
                return points;
            }

            // Check ordinate on all original points
            final NaturalSpline mySpline = NaturalSpline.interpolate(defs);
            boolean ok = true;

            for (Point2D org : points) {
                final double dy = mySpline.yAtX(org.getX()) - org.getY();

                if (Math.abs(dy) > maxDy) {
                    ok = false;
                    break;
                }
            }

            if (ok) {
                logger.debug("Simplified pts:{}", defs.size());
                return defs;
            }

            // Divide every segment in two
            for (int i = 0; i < defs.size() - 1; i += 2) {
                final Point2D lp = defs.get(i);
                final Point2D rp = defs.get(i + 1);
                final double x = 0.5 * (lp.getX() + rp.getX());
                final double y = yAt(x);
                defs.add(i + 1, new Point2D.Double(x, y));
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    /**
     * Meant for JAXB handling of LineInfo interface.
     */
    public static class Adapter
            extends XmlAdapter<StaffLine, LineInfo>
    {

        @Override
        public StaffLine marshal (LineInfo lineInfo)
                throws Exception
        {
            return (StaffLine) lineInfo;
        }

        @Override
        public LineInfo unmarshal (StaffLine staffLine)
                throws Exception
        {
            return staffLine;
        }
    }
}
