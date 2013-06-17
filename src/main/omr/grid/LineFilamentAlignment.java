//----------------------------------------------------------------------------//
//                                                                            //
//                 L i n e F i l a m e n t A l i g n m e n t                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.constant.ConstantSet;

import omr.glyph.facets.Glyph;

import omr.math.NaturalSpline;

import omr.run.Orientation;

import omr.sheet.Scale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Point2D;
import java.util.List;

/**
 * Class {@code LineFilamentAlignment} is a GlyphAlignment
 * implementation meant for long staff lines filaments.
 *
 * @author Hervé Bitteur
 */
public class LineFilamentAlignment
        extends FilamentAlignment
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            LineFilamentAlignment.class);

    //~ Constructors -----------------------------------------------------------
    //
    //-----------------------//
    // LineFilamentAlignment //
    //-----------------------//
    /**
     * Creates a new LineFilamentAlignment object.
     *
     * @param glyph the containing filament
     */
    public LineFilamentAlignment (Glyph glyph)
    {
        super(glyph);
    }

    //~ Methods ----------------------------------------------------------------
    //
    //-----------//
    // fillHoles //
    //-----------//
    /**
     * Fill large holes (due to missing intermediate points) in
     * this filament, by interpolating (or extrapolating) from the
     * collection of rather parallel fils, this filament is part of
     * (at provided pos index).
     *
     * @param pos  the index of this filament in the provided collection
     * @param fils the provided collection of parallel filaments
     */
    public void fillHoles (int pos,
                           List<LineFilament> fils)
    {
        Scale scale = new Scale(glyph.getInterline());
        int maxHoleLength = scale.toPixels(constants.maxHoleLength);
        int virtualLength = scale.toPixels(constants.virtualSegmentLength);

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
                    int insert = (int) Math.rint(holeLength / virtualLength)
                                 - 1;

                    if (insert > 0) {
                        logger.debug(
                                "Hole before ip: {} insert:{} for {}",
                                ip,
                                insert,
                                this);

                        double dx = holeLength / (insert + 1);

                        for (int i = 1; i <= insert; i++) {
                            int x = (int) Math.rint(holeStart + (i * dx));
                            Point2D pt = new Filler(
                                    x,
                                    pos,
                                    fils,
                                    virtualLength / 2).findInsertion();

                            if (pt == null) {
                                // Take default line point instead
                                pt = new VirtualPoint(
                                        x,
                                        getPositionAt(x, Orientation.HORIZONTAL));
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
            line = NaturalSpline.interpolate(
                    points.toArray(new Point2D[points.size()]));
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

        final int x; // Preferred abscissa for point insertion

        final int pos; // Relative position within fils collection

        final List<LineFilament> fils; // Collection of fils this one is part of

        final int margin; // Margin on abscissa to lookup refs

        //~ Constructors -------------------------------------------------------
        public Filler (int x,
                       int pos,
                       List<LineFilament> fils,
                       int margin)
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
        private Neighbor findNeighbor (List<LineFilament> subfils,
                                       int dir)
        {
            final int firstIdx = (dir > 0) ? 0 : (subfils.size() - 1);
            final int breakIdx = (dir > 0) ? subfils.size() : (-1);

            for (int i = firstIdx; i != breakIdx; i += dir) {
                LineFilament fil = subfils.get(i);
                Point2D pt = fil.getAlignment()
                        .findPoint(
                        x,
                        Orientation.HORIZONTAL,
                        margin);

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

            final int pos;

            final Point2D point;

            //~ Constructors ---------------------------------------------------
            public Neighbor (int pos,
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
