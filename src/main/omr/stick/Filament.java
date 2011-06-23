//----------------------------------------------------------------------------//
//                                                                            //
//                              F i l a m e n t                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.stick;

import omr.constant.ConstantSet;

import omr.glyph.GlyphSection;
import omr.glyph.facets.BasicStick;

import omr.log.Logger;

import omr.math.Barycenter;
import omr.math.NaturalSpline;
import omr.math.PointsCollector;

import omr.run.Orientation;

import omr.score.common.PixelPoint;

import omr.sheet.Scale;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

/**
 * Class {@code Filament} represents a long glyph that can be far from being a
 * straight line.
 * It is used to handle candidate staff lines and bar lines.
 */
public class Filament
    extends BasicStick
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Filament.class);

    /**
     * For comparing Filament instances on their starting point
     */
    public static final Comparator<Filament> startComparator = new Comparator<Filament>() {
        public int compare (Filament f1,
                            Filament f2)
        {
            // Sort on start
            return Integer.signum(f1.getStartPoint().x - f2.getStartPoint().x);
        }
    };

    /**
     * For comparing Filament instances on their stopping point
     */
    public static final Comparator<Filament> stopComparator = new Comparator<Filament>() {
        public int compare (Filament f1,
                            Filament f2)
        {
            // Sort on stop
            return Integer.signum(f1.getStopPoint().x - f2.getStopPoint().x);
        }
    };

    /**
     * For comparing Filament instances on distance from reference axis
     */
    public static final Comparator<Filament> distanceComparator = new Comparator<Filament>() {
        public int compare (Filament f1,
                            Filament f2)
        {
            // Sort on distance from top edge
            return Integer.signum(f1.refDist - f2.refDist);
        }
    };


    //~ Instance fields --------------------------------------------------------

    /** Related scale */
    private final Scale scale;

    /** Absolute interpolating curve */
    private NaturalSpline curve;

    /** Absolute defining points */
    private List<Point2D> points;

    /** Absolute beginning point */
    private PixelPoint pStart;

    /** Absolute ending point */
    private PixelPoint pStop;

    /** Filament this one has been merged into, if any */
    private Filament parent;

    /** Distance from reference axis */
    private Integer refDist;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // Filament //
    //----------//
    /**
     * Creates a new Filament object.
     *
     * @param scale scaling data
     */
    public Filament (Scale scale)
    {
        super(scale.interline());
        this.scale = scale;
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getAncestor //
    //-------------//
    /**
     * Report the top ancestor of this filament (which is this filament itself,
     * when it has no parent (i.e. not been included into another one))
     * @return the filament ancestor
     */
    public Filament getAncestor ()
    {
        Filament fil = this;

        while (fil.parent != null) {
            fil = fil.parent;
        }

        return fil;
    }

    //-----------//
    // getParent //
    //-----------//
    public Filament getParent ()
    {
        return parent;
    }

    //---------------//
    // getProbeWidth //
    //---------------//
    /**
     * Report the width of the window used to determine filament ordinate
     * @return the scale-independent probe width
     */
    public static Scale.Fraction getProbeWidth ()
    {
        return constants.probeWidth;
    }

    //-----------------//
    // setEndingPoints //
    //-----------------//
    public void setEndingPoints (PixelPoint pStart,
                                 PixelPoint pStop)
    {
        invalidateCache();
        this.pStart = pStart;
        this.pStop = pStop;
        computeData();
    }

    //----------------//
    // setRefDistance //
    //----------------//
    /**
     * Remember the filament distance to reference axis
     * @param refDist the orthogonal distance to reference axis
     */
    public void setRefDistance (int refDist)
    {
        this.refDist = refDist;
    }

    //----------------//
    // getRefDistance //
    //----------------//
    /**
     * Report the orthogonal distance from the filament to the reference axis
     * @return distance from axis that takes global slope into acount
     */
    public Integer getRefDistance ()
    {
        return refDist;
    }

    //-------------------------//
    // getResultingThicknessAt //
    //-------------------------//
    /**
     * Compute the thickness at provided coordinate of the potential merge
     * between this and that filaments
     * @param that the other filament
     * @param coord the provided coordinate
     * @return the resulting thickness
     */
    public double getResultingThicknessAt (Filament that,
                                           int      coord)
    {
        double thisPos = this.positionAt(coord);
        double thatPos = that.positionAt(coord);
        double thisThickness = this.getThicknessAt(coord);
        double thatThickness = that.getThicknessAt(coord);

        return Math.abs(thisPos - thatPos) +
               ((thisThickness + thatThickness) / 2);
    }

    //---------------//
    // getStartPoint //
    //---------------//
    @Override
    public PixelPoint getStartPoint ()
    {
        if (pStart == null) {
            computeData();
        }

        return pStart;
    }

    //--------------//
    // getStopPoint //
    //--------------//
    @Override
    public PixelPoint getStopPoint ()
    {
        if (pStop == null) {
            computeData();
        }

        return pStop;
    }

    //----------------//
    // getThicknessAt //
    //----------------//
    /**
     * Report the filament mean thickness at the provided coordinate
     * @param coord the desired abscissa
     * @return the mean thickness measured, expressed in number of pixels.
     * Beware, this number will be zero if the probe falls entirely in a hole
     * between two sections.
     */
    public double getThicknessAt (int coord)
    {
        final Rectangle bounds = getOrientedBounds();

        if ((coord < bounds.x) || (coord >= (bounds.x + bounds.width))) {
            logger.warning(this + " bounds:" + bounds + " coord:" + coord);
            throw new IllegalArgumentException(
                "Coordinate not within filament range");
        }

        // Use a large-enough collector
        final Rectangle roi = new Rectangle(coord, bounds.y, 0, bounds.height);
        final int       probeHalfWidth = scale.toPixels(constants.probeWidth) / 2;
        roi.grow(probeHalfWidth, 0);

        boolean[] matched = new boolean[roi.width];
        Arrays.fill(matched, false);

        final PointsCollector collector = new PointsCollector(roi);

        for (GlyphSection section : getMembers()) {
            Rectangle inter = roi.intersection(section.getOrientedBounds());

            for (int c = (inter.x + inter.width) - 1; c >= inter.x; c--) {
                matched[c - roi.x] = true;
            }

            section.cumulate(collector);
        }

        int count = collector.getCount();

        if (count == 0) {
            if (logger.isFineEnabled()) {
                logger.warning(
                    "Thickness " + this + " coord:" + coord + " nopoints");
            }

            return 0;
        } else {
            // Return MEAN thickness on MATCHED probe width
            int width = 0;

            for (boolean bool : matched) {
                if (bool) {
                    width++;
                }
            }

            double thickness = (double) count / width;

            if (logger.isFineEnabled()) {
                logger.fine(
                    this + " coord:" + coord + " pos:" +
                    (float) getCurve().yAtX(coord) + " thickness:" + thickness);
            }

            return thickness;
        }
    }

    //------------//
    // addSection //
    //------------//
    public void addSection (GlyphSection section)
    {
        addSection(section, Linking.LINK_BACK);
    }

    //-------------//
    // containsSID //
    //-------------//
    /**
     * Debug function that retruns true if this filament contains the section
     * whose ID is provided
     * @param id the ID of interesting section
     * @return true if such section exists among filament sections
     */
    public boolean containsSID (int id)
    {
        for (GlyphSection section : getMembers()) {
            if (section.getId() == id) {
                return true;
            }
        }

        return false;
    }

    //------//
    // dump //
    //------//
    @Override
    public void dump ()
    {
        super.dump();
        System.out.println("   start=" + getStartPoint());
        System.out.println("   stop=" + getStopPoint());
        System.out.println("   curve=" + getCurve());
        System.out.println("   refDist=" + getRefDistance());
    }

    //---------//
    // include //
    //---------//
    /**
     * Include a whole other filament into this one
     * @param that the filament to swallow
     */
    public void include (Filament that)
    {
        for (GlyphSection section : that.getMembers()) {
            addSection(section);
        }

        that.parent = this;
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    public void invalidateCache ()
    {
        super.invalidateCache();

        curve = null;
        pStart = pStop = null;
        refDist = null;
        points = null;
    }

    //------------//
    // positionAt //
    //------------//
    /**
     * Report the precise filament position for the provided coordinate .
     * @param coord the coord value (x for horizontal fil, y for vertical fil)
     * @return the pos value (y for horizontal fil, x for vertical fil)
     */
    public double positionAt (double coord)
    {
        if (curve == null) {
            computeData();
        }

        Orientation orientation = getLag()
                                      .getOrientation();

        if (orientation.isVertical()) {
            if ((coord < pStart.y) || (coord > pStop.y)) {
                double slope = (double) (pStop.x - pStart.x) / (double) (pStop.y -
                               pStart.y);

                return pStart.x + (slope * (coord - pStart.y));
            } else {
                return curve.xAtY(coord);
            }
        } else {
            if ((coord < pStart.x) || (coord > pStop.x)) {
                double slope = (double) (pStop.y - pStart.y) / (double) (pStop.x -
                               pStart.x);

                return pStart.y + (slope * (coord - pStart.x));
            } else {
                return curve.yAtX(coord);
            }
        }
    }

    //------------//
    // renderLine //
    //------------//
    @Override
    public void renderLine (Graphics2D g)
    {
        // Draw the absolute defining points
        if (points != null) {
            double r = Math.max(1, scale.interline() * 0.1); // Point radius
            Color  oldColor = g.getColor();
            g.setColor(Color.YELLOW);

            for (Point2D p : points) {
                g.drawOval(
                    (int) Math.rint(p.getX() - r),
                    (int) Math.rint(p.getY() - r),
                    (int) Math.rint(2 * r),
                    (int) Math.rint(2 * r));
            }

            g.setColor(oldColor);
        }

        // Then the curve itself
        if (getCurve() != null) {
            g.draw(getCurve());
        }
    }

    //---------//
    // slopeAt //
    //---------//
    public double slopeAt (double coord)
    {
        if (curve == null) {
            computeData();
        }

        Orientation orientation = getLag()
                                      .getOrientation();

        if (orientation.isVertical()) {
            return curve.xDerivativeAtY(coord);
        } else {
            return curve.yDerivativeAtX(coord);
        }
    }

    //------------//
    // trueLength //
    //------------//
    /**
     * Report an evaluation of how this filament is filled by sections
     * @return how solid this filament is
     */
    public int trueLength ()
    {
        return (int) Math.rint((double) getWeight() / scale.mainFore());
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(" start[x=")
          .append(getStartPoint().x)
          .append(",y=")
          .append(getStartPoint().y)
          .append("]");

        sb.append(" stop[x=")
          .append(getStopPoint().x)
          .append(",y=")
          .append(getStopPoint().y)
          .append("]");

        //        sb.append(" curve:")
        //          .append(getCurve());
        if (parent != null) {
            sb.append(" anc:")
              .append(getAncestor());
        }

        if (refDist != null) {
            sb.append(" refDist:")
              .append(refDist);
        }

        return sb.toString();
    }

    //----------//
    // getCurve //
    //----------//
    private NaturalSpline getCurve ()
    {
        if (curve == null) {
            computeData();
        }

        return curve;
    }

    //----------------------//
    // getRectangleCentroid //
    //----------------------//
    /**
     * Report the oriented centroid of all the filament pixels found in the
     * provided oriented ROI
     * @param roi the desired oriented region of interest
     * @return the oriented barycenter of the pixels found
     */
    private Point2D getRectangleCentroid (Rectangle roi)
    {
        Barycenter barycenter = new Barycenter();

        for (GlyphSection section : getMembers()) {
            section.cumulate(barycenter, roi);
        }

        if (barycenter.getWeight() != 0) {
            return new Point2D.Double(barycenter.getX(), barycenter.getY());
        } else {
            return null;
        }
    }

    //-------------//
    // computeData //
    //-------------//
    /**
     * Compute cached data: curve, pStart, pStop
     * Curve goes from pStart to pStop through intermediate points regularly
     * spaced
     */
    private void computeData ()
    {
        try {
            /** Width of window to retrieve pixels */
            int probeWidth = scale.toPixels(constants.probeWidth);

            /** Typical length of curve segments */
            double typicalLength = scale.toPixels(constants.segmentLength);

            // We need lag orientation
            if (getLag() == null) {
                setLag(getMembers().first().getGraph());
            }

            Orientation orientation = getLag()
                                          .getOrientation();

            Point       orientedStart = (pStart == null) ? null
                                        : orientation.switchRef(pStart, null);
            Point       orientedStop = (pStop == null) ? null
                                       : orientation.switchRef(pStop, null);
            Rectangle   bounds = getOrientedBounds();
            int         start = (orientedStart != null) ? orientedStart.x
                                : bounds.x;
            int         stop = (orientedStop != null) ? orientedStop.x
                               : (bounds.x + (bounds.width - 1));
            int         length = stop - start + 1;

            Rectangle   probe = new Rectangle(bounds);
            probe.x = start;
            probe.width = probeWidth;

            // Determine the number of segments and their precise length
            int           segCount = (int) Math.rint(length / typicalLength);
            double        segLength = (double) length / segCount;
            List<Point2D> points = new ArrayList<Point2D>(segCount + 1);

            // First point
            if (pStart == null) {
                Point2D p = getRectangleCentroid(probe);
                pStart = orientation.switchRef(
                    new Point(start, (int) Math.rint(p.getY())),
                    null);
            }

            points.add(pStart);

            // Intermediate points (perhaps none)
            for (int i = 1; i < segCount; i++) {
                probe.x = start + (int) Math.rint(i * segLength);

                Point2D pt = getRectangleCentroid(probe);

                // If, unfortunately, we are in a filament hole, just skip it
                if (pt != null) {
                    points.add(orientation.switchRef(pt));
                }
            }

            // Last point
            if (pStop == null) {
                probe.x = stop - probe.width + 1;

                Point2D p = getRectangleCentroid(probe);
                pStop = orientation.switchRef(
                    new Point(stop, (int) Math.rint(p.getY())),
                    null);
            }

            points.add(pStop);

            // Interpolate the best spline through the provided points
            curve = NaturalSpline.interpolate(
                points.toArray(new Point2D[points.size()]));
            ///addAttachment("SPLINE", curve); // Just for fun...

            // Remember points (atomically)
            this.points = points;
        } catch (Exception ex) {
            logger.warning("Filament cannot computeData", ex);
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        final Scale.Fraction probeWidth = new Scale.Fraction(
            0.5,
            "Width of probing window to retrieve filament ordinate");
        final Scale.Fraction segmentLength = new Scale.Fraction(
            2,
            "Typical length between filament curve intermediate points");
    }
}
