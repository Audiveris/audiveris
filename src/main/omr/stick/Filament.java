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
import omr.sheet.grid.LineFilament;

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

    //-----------//
    // fillHoles //
    //-----------//
    /**
     * Fill large holes (due to missing intermediate points) in this filament,
     * by interpolating (or extrapolating) from the collection of rather
     * parallel fils, this filament is part of (at provided pos index)
     * @param pos the index of this filament in the provided collection
     * @param fils the provided collection of parallel filaments
     */
    public void fillHoles (int                pos,
                           List<LineFilament> fils)
    {
        int     maxHoleLength = scale.toPixels(constants.maxHoleLength);
        int     virtualLength = scale.toPixels(constants.virtualSegmentLength);

        // Look for long holes
        Double  holeStart = null;
        boolean modified = false;

        for (int ip = 0; ip < points.size(); ip++) {
            Point2D point = points.get(ip);

            if (holeStart == null) {
                holeStart = point.getX();
            } else {
                double holeStop = point.getX();
                double holeLength = holeStop - holeStart;

                if (holeLength > maxHoleLength) {
                    // Try to insert artificial intermediate point(s)
                    int insert = (int) Math.rint(holeLength / virtualLength) -
                                 1;

                    if (insert > 0) {
                        if (logger.isFineEnabled()) {
                            logger.fine(
                                "Hole before ip: " + ip + " insert:" + insert +
                                " for " + this);
                        }

                        double dx = (double) holeLength / (insert + 1);

                        for (int i = 1; i <= insert; i++) {
                            int     x = (int) Math.rint(holeStart + (i * dx));
                            Point2D pt = new Filler(
                                x,
                                pos,
                                fils,
                                virtualLength / 2).findInsertion();

                            if (pt != null) {
                                if (logger.isFineEnabled()) {
                                    logger.info("Inserted " + pt);
                                }

                                points.add(ip++, pt);
                                modified = true;
                            } else {
                                if (logger.isFineEnabled()) {
                                    logger.info("No insertion at x: " + x);
                                }
                            }
                        }
                    }
                }

                holeStart = holeStop;
            }
        }

        if (modified) {
            // Regenerate the underlying curve
            curve = NaturalSpline.interpolate(
                points.toArray(new Point2D[points.size()]));
        }
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

            for (Point2D p : points) {
                g.setColor(
                    (p instanceof VirtualPoint) ? Color.RED : Color.YELLOW);

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

    //-----------//
    // findPoint //
    //-----------//
    protected Point2D findPoint (int x,
                                 int margin)
    {
        Point2D best = null;
        int     bestDx = Integer.MAX_VALUE;

        for (Point2D p : points) {
            int dx = Math.abs((int) Math.rint(p.getX() - x));

            if ((dx <= margin) && (dx < bestDx)) {
                bestDx = dx;
                best = p;
            }
        }

        return best;
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
            List<Point2D> newPoints = new ArrayList<Point2D>(segCount + 1);

            // First point
            if (pStart == null) {
                Point2D p = getRectangleCentroid(probe);
                pStart = orientation.switchRef(
                    new Point(start, (int) Math.rint(p.getY())),
                    null);
            }

            newPoints.add(pStart);

            // Intermediate points (perhaps none)
            for (int i = 1; i < segCount; i++) {
                probe.x = start + (int) Math.rint(i * segLength);

                Point2D pt = getRectangleCentroid(probe);

                // If, unfortunately, we are in a filament hole, just skip it
                if (pt != null) {
                    newPoints.add(orientation.switchRef(pt));
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

            newPoints.add(pStop);

            // Interpolate the best spline through the provided points
            curve = NaturalSpline.interpolate(
                newPoints.toArray(new Point2D[newPoints.size()]));

            // Remember points (atomically)
            this.points = newPoints;
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
        final Scale.Fraction virtualSegmentLength = new Scale.Fraction(
            6,
            "Typical length used for virtual intermediate points");
        final Scale.Fraction maxHoleLength = new Scale.Fraction(
            8,
            "Maximum length for holes without intermediate points");
    }

    //--------//
    // Filler //
    //--------//
    /**
     * A utility class to fill the filament holes with virtual points
     */
    private static class Filler
    {
        //~ Instance fields ----------------------------------------------------

        final int                x; // Preferred abscissa for point insertion
        final int                pos; // Relative position within fils collection
        final List<LineFilament> fils; // Collection of fils this one is part of
        final int                margin; // Margin on abscissa to lookup refs

        //~ Constructors -------------------------------------------------------

        public Filler (int                x,
                       int                pos,
                       List<LineFilament> fils,
                       int                margin)
        {
            this.x = x;
            this.pos = pos;
            this.fils = fils;
            this.margin = margin;
        }

        //~ Methods ------------------------------------------------------------

        //---------------//
        // findInsertion //
        //---------------//
        /**
         * Look for a suitable insertion point. A point is returned only if it
         * can be computed by interpolation, which needs one reference above and
         * one reference below. Extrapolation is not reliable enough, so no
         * insertion point is returned if we lack reference above or below.
         * @return the computed insertion point, or null
         */
        public Point2D findInsertion ()
        {
            // Check for a reference above
            Neighbor one = findNeighbor(fils.subList(0, pos), -1);

            if (one == null) {
                return null;
            }

            // Check for a reference below
            Neighbor two = findNeighbor(fils.subList(pos + 1, fils.size()), 1);

            if (two == null) {
                return null;
            }

            // Interpolate
            double ratio = (double) (pos - one.pos) / (two.pos - one.pos);

            return new VirtualPoint(
                ((1 - ratio) * one.point.getX()) + (ratio * two.point.getX()),
                ((1 - ratio) * one.point.getY()) + (ratio * two.point.getY()));
        }

        /**
         * Browse the provided list in the desired direction to find a suitable
         * point as a reference in a neighboring filament.
         */
        private Neighbor findNeighbor (List<LineFilament> subfils,
                                       int                dir)
        {
            final int firstIdx = (dir > 0) ? 0 : (subfils.size() - 1);
            final int breakIdx = (dir > 0) ? subfils.size() : (-1);

            for (int i = firstIdx; i != breakIdx; i += dir) {
                LineFilament fil = subfils.get(i);
                Point2D      pt = fil.findPoint(x, margin);

                if (pt != null) {
                    return new Neighbor(fil.getClusterPos(), pt);
                }
            }

            return null;
        }

        //~ Inner Classes ------------------------------------------------------

        /** Convey a point together with its relative cluster position */
        private class Neighbor
        {
            //~ Instance fields ------------------------------------------------

            final int     pos;
            final Point2D point;

            //~ Constructors ---------------------------------------------------

            public Neighbor (int     pos,
                             Point2D point)
            {
                this.pos = pos;
                this.point = point;
            }
        }
    }

    //--------------//
    // VirtualPoint //
    //--------------//
    /**
     * Used for artificial intermediate points
     */
    private static class VirtualPoint
        extends Point2D.Double
    {
        //~ Constructors -------------------------------------------------------

        public VirtualPoint (double x,
                             double y)
        {
            super(x, y);
        }
    }
}
