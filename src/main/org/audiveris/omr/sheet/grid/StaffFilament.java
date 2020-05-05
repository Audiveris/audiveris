//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S t a f f F i l a m e n t                                    //
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
package org.audiveris.omr.sheet.grid;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.glyph.dynamic.CurvedFilament;
import org.audiveris.omr.math.NaturalSpline;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Scale.InterlineScale;
import org.audiveris.omr.sheet.StaffLine;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Class {@code StaffFilament} is a {@link CurvedFilament}, used as (part of) a
 * candidate staff line, thus a filament within a cluster.
 * <p>
 * It is a CurvedFilament augmented by combs and cluster information.
 *
 * @author Hervé Bitteur
 */
public class StaffFilament
        extends CurvedFilament
        implements LineInfo
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(StaffFilament.class);

    /** Combs where this filament appears. map (column index -> comb) */
    private SortedMap<Integer, FilamentComb> combs;

    /** The line cluster this filament is part of, if any. */
    private LineCluster cluster;

    /** Relative position in cluster. (relevant only if cluster is not null) */
    private int clusterPos;

    /**
     * Creates a new LineFilament object.
     * Nota: this constructor is needed for FilamentFactory which calls this
     * kind of constructor via a newInstance() method.
     *
     * @param interline scaling data
     */
    public StaffFilament (int interline)
    {
        super(interline, InterlineScale.toPixels(interline, constants.segmentLength));
    }

    //-------------//
    // yTranslated //
    //-------------//
    @Override
    public LineInfo yTranslated (double dy)
    {
        final List<Point2D> virtualPoints = new ArrayList<>(points.size());

        for (Point2D p : points) {
            virtualPoints.add(new Point2D.Double(p.getX(), p.getY() + dy));
        }

        return new StaffLine(virtualPoints, getThickness());
    }

    //---------//
    // addComb //
    //---------//
    /**
     * Add a comb where this filament appears
     *
     * @param column the sheet column index of the comb
     * @param comb   the comb which contains this filament
     */
    public void addComb (int column,
                         FilamentComb comb)
    {
        if (combs == null) {
            combs = new TreeMap<>();
        }

        combs.put(column, comb);
    }

    //--------//
    // dumpOf //
    //--------//
    @Override
    public String dumpOf ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(super.dumpOf());
        sb.append(String.format("   cluster=%s%n", cluster));
        sb.append(String.format("   clusterPos=%s%n", clusterPos));

        ///sb.append(String.format("   combs=%s%n", combs));
        return sb.toString();
    }

    //-----------//
    // fillHoles //
    //-----------//
    /**
     * Fill large holes (due to missing intermediate points) in this filament, by
     * interpolating (or extrapolating) from the collection of rather parallel fils,
     * this filament is part of (at provided pos index).
     *
     * @param pos  the index of this filament in the provided collection
     * @param fils the provided collection of parallel filaments
     */
    public void fillHoles (int pos,
                           List<StaffFilament> fils)
    {
        int maxHoleLength = InterlineScale.toPixels(interline, constants.maxHoleLength);
        int virtualLength = InterlineScale.toPixels(interline, constants.virtualSegmentLength);

        // Look for long holes
        Double holeStart = null;
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
                    int insert = (int) Math.rint(holeLength / virtualLength) - 1;

                    if (insert > 0) {
                        logger.debug("Hole before ip: {} insert:{} for {}", ip, insert, this);

                        double dx = holeLength / (insert + 1);

                        for (int i = 1; i <= insert; i++) {
                            int x = (int) Math.rint(holeStart + (i * dx));
                            Point2D pt = new Filler(x, pos, fils, virtualLength / 2)
                                    .findInsertion();

                            if (pt == null) {
                                // Take default line point instead
                                pt = new VirtualPoint(x, getPositionAt(x, Orientation.HORIZONTAL));
                            }

                            logger.debug("Inserted {}", pt);
                            points.add(ip++, pt);
                            modified = true;
                        }
                    }
                }

                holeStart = holeStop;
            }
        }

        if (modified) {
            // Regenerate the underlying curve
            spline = NaturalSpline.interpolate(points);
        }
    }

    //------------//
    // getCluster //
    //------------//
    /**
     * Report the line cluster, if any, this filament is part of
     *
     * @return the containing cluster, or null
     */
    public LineCluster getCluster ()
    {
        return cluster;
    }

    //---------------//
    // getClusterPos //
    //---------------//
    /**
     * @return the clusterPos
     */
    public int getClusterPos ()
    {
        return clusterPos;
    }

    //----------//
    // getCombs //
    //----------//
    /**
     * @return the combs
     */
    public SortedMap<Integer, FilamentComb> getCombs ()
    {
        if (combs != null) {
            return combs;
        } else {
            return new TreeMap<>();
        }
    }

    //-------------//
    // getEndPoint //
    //-------------//
    @Override
    public Point2D getEndPoint (HorizontalSide side)
    {
        if (side == HorizontalSide.LEFT) {
            return getStartPoint();
        } else {
            return getStopPoint();
        }
    }

    //----------//
    // getGlyph //
    //----------//
    @Override
    public Glyph getGlyph ()
    {
        throw new UnsupportedOperationException("Feature not supported by StaffFilament!");
    }

    //--------------//
    // getThickness //
    //--------------//
    @Override
    public double getThickness ()
    {
        return (double) getWeight() / getTrueLength();
    }

    //---------//
    // include //
    //---------//
    /**
     * Include a whole other filament into this one
     *
     * @param that the filament to swallow
     */
    public void include (StaffFilament that)
    {
        super.stealSections(that);
        getCombs().putAll(that.getCombs());

        that.cluster = this.cluster;
        that.clusterPos = this.clusterPos;
    }

    //---------------//
    // isWithinRange //
    //---------------//
    /**
     * Report whether the provided abscissa lies within the line range
     *
     * @param x the provided abscissa
     * @return true if within range
     */
    public boolean isWithinRange (double x)
    {
        return (x >= getStartPoint().getX()) && (x <= getStopPoint().getX());
    }

    //------------//
    // setCluster //
    //------------//
    /**
     * Assign this filament to a line cluster
     *
     * @param cluster the containing cluster
     * @param pos     the relative line position within the cluster
     */
    public void setCluster (LineCluster cluster,
                            int pos)
    {
        this.cluster = cluster;
        clusterPos = pos;
    }

    //-------------//
    // toStaffLine //
    //-------------//
    /**
     * Build a simple StaffLine instance from this detailed StaffFilament instance.
     * <p>
     * We reduce the number of defining points to its minimum.
     *
     * @param glyphIndex if not null, register the original glyph in glyph index
     * @return the equivalent StaffLine instance
     */
    public StaffLine toStaffLine (GlyphIndex glyphIndex)
    {
        Glyph glyph = toGlyph(null);

        if (glyphIndex != null) {
            glyph = glyphIndex.registerOriginal(glyph);
        }

        final StaffLine staffLine = new StaffLine(points, getThickness());
        staffLine.reducePoints(constants.maxSimplificationShift.getValue());
        staffLine.setGlyph(glyph);

        return staffLine;
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
            return spline.yAtX(x);
        }
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        if (combs != null) {
            sb.append(" combs:").append(combs.size());
        }

        if (cluster != null) {
            sb.append(" cluster:").append(cluster.getId()).append("p").append(clusterPos);
        }

        return sb.toString();
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction segmentLength = new Scale.Fraction(
                4.0,
                "Typical length between filament curve intermediate points");

        private final Scale.Fraction virtualSegmentLength = new Scale.Fraction(
                5.0,
                "Typical length used for virtual intermediate points");

        private final Scale.Fraction maxHoleLength = new Scale.Fraction(
                6.0,
                "Maximum length for holes without intermediate points");

        private final Constant.Double maxSimplificationShift = new Constant.Double(
                "pixels",
                0.25,
                "Maximum acceptable vertical shift when simplifying staff line points");
    }

    //--------//
    // Filler //
    //--------//
    /**
     * A utility class to fill the filament holes with virtual points
     */
    private static class Filler
    {

        final int x; // Preferred abscissa for point insertion

        final int pos; // Relative position within fils collection

        final List<StaffFilament> fils; // Collection of fils this one is part of

        final int margin; // Margin on abscissa to lookup refs

        Filler (int x,
                int pos,
                List<StaffFilament> fils,
                int margin)
        {
            this.x = x;
            this.pos = pos;
            this.fils = fils;
            this.margin = margin;
        }

        //---------------//
        // findInsertion //
        //---------------//
        /**
         * Look for a suitable insertion point.
         * A point is returned only if it can be computed by interpolation,
         * which needs one reference above and one reference below.
         * Extrapolation is not reliable enough, so no insertion point is
         * returned if we lack reference above or below.
         *
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
         * Browse the provided list in the desired direction to find a
         * suitable point as a reference in a neighboring filament.
         */
        private Neighbor findNeighbor (List<StaffFilament> subfils,
                                       int dir)
        {
            final int firstIdx = (dir > 0) ? 0 : (subfils.size() - 1);
            final int breakIdx = (dir > 0) ? subfils.size() : (-1);

            for (int i = firstIdx; i != breakIdx; i += dir) {
                StaffFilament fil = subfils.get(i);
                Point2D pt = fil.findPoint(x, Orientation.HORIZONTAL, margin);

                if (pt != null) {
                    return new Neighbor(fil.getClusterPos(), pt);
                }
            }

            return null;
        }

        /** Convey a point together with its relative cluster position.. */
        private static class Neighbor
        {

            final int pos;

            final Point2D point;

            Neighbor (int pos,
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
     * Used for artificial intermediate points.
     */
    private static class VirtualPoint
            extends Point2D.Double
    {

        VirtualPoint (double x,
                      double y)
        {
            super(x, y);
        }

        @Override
        public Object clone ()
        {
            return super.clone(); //To change body of generated methods, choose Tools | Templates.
        }
    }
}
