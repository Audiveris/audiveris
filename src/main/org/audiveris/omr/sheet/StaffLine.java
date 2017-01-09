//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S t a f f L i n e                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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

    private static final Logger logger = LoggerFactory.getLogger(
            StaffLine.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Absolute defining points (including start & stop points). */
    @XmlElement(name = "point")
    protected final List<Point2D> points;

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
        this.points = points;
        this.thickness = thickness;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private StaffLine ()
    {
        this.points = null;
        this.thickness = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------
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
    // setGlyph //
    //----------//
    public void setGlyph (Glyph glyph)
    {
        this.glyph = glyph;
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
        //~ Methods --------------------------------------------------------------------------------

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
