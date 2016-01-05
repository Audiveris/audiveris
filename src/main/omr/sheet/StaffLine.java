//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S t a f f L i n e                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.glyph.Glyph;

import omr.math.NaturalSpline;
import omr.math.PointUtil;

import omr.sheet.grid.LineInfo;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;
import omr.util.Jaxb;

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
    // Persistent
    //-----------
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
    @XmlAttribute(name = "glyph-ref")
    protected Glyph glyph;

    // Transient
    //----------
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
