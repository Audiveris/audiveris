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
package omr.sheet.grid;

import omr.constant.ConstantSet;

import omr.glyph.GlyphSection;
import omr.glyph.facets.BasicStick;

import omr.log.Logger;

import omr.math.Barycenter;
import omr.math.NaturalSpline;
import omr.math.PointsCollector;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.sheet.Scale;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Represents a candidate staff line, so it's a long glyph that can be far from
 * being a straight line.
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
     * For comparing Filament instances on coordinate (x) then of position (y)
     * their starting point
     */
    public static final Comparator<Filament> startComparator = new Comparator<Filament>() {
        public int compare (Filament s1,
                            Filament s2)
        {
            if (s1 == s2) {
                return 0;
            }

            // Sort on horizontal coordinate first
            int dStart = s1.getStartPoint().x - s2.getStartPoint().x;

            if (dStart != 0) {
                return dStart;
            }

            // Sort on vertical position second
            int dPos = s1.getStartPoint().y - s2.getStartPoint().y;

            if (dPos != 0) {
                return dPos;
            }

            return -1;
        }
    };

    /**
     * For comparing Filament instances on coordinate (x) then of position (y)
     * their stopping point
     */
    public static final Comparator<Filament> stopComparator = new Comparator<Filament>() {
        public int compare (Filament f1,
                            Filament f2)
        {
            if (f1 == f2) {
                return 0;
            }

            // Sort on horizontal coordinate first
            int dStop = f1.getStopPoint().x - f2.getStopPoint().x;

            if (dStop != 0) {
                return dStop;
            }

            // Sort on vertical position second
            int dPos = f1.getStopPoint().y - f2.getStopPoint().y;

            if (dPos != 0) {
                return dPos;
            }

            return -1;
        }
    };

    /**
     * For comparing Filament instances on top distance then coordinate (x) of
     * their starting point
     */
    public static final Comparator<Filament> distanceComparator = new Comparator<Filament>() {
        public int compare (Filament s1,
                            Filament s2)
        {
            if (s1 == s2) {
                return 0;
            }

            // Sort on distance from top edge first
            int dPos = s1.topDist - s2.topDist;

            if (dPos != 0) {
                return dPos;
            }

            // Sort on horizontal coordinate second
            int dStart = s1.getStopPoint().x - s2.getStopPoint().x;

            if (dStart != 0) {
                return dStart;
            }

            return -1;
        }
    };


    //~ Instance fields --------------------------------------------------------

    /** Related scale */
    private final Scale scale;

    /** Interpolating curve */
    private NaturalSpline curve;

    /** Beginning point (left) */
    private PixelPoint pStart;

    /** Ending point (right) */
    private PixelPoint pStop;

    /** Ref of the filament this one has been merged into */
    private Filament parent;

    /** Patterns where this filament appears. map (column -> pattern) */
    private SortedMap<Integer, FilamentPattern> patterns;

    /** Distance from tilted top egde */
    private Integer topDist;

    /** The line cluster, if any, this filament is part of */
    private LineCluster cluster;

    /** Relative position in cluster */
    private int clusterPos;

    /** Defining points */
    private List<PixelPoint> points;

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

    //------------//
    // setCluster //
    //------------//
    /**
     * Assign this filament to a line cluster
     * @param cluster the containing cluster
     * @param pos the relative line position within the cluster
     */
    public void setCluster (LineCluster cluster,
                            int         pos)
    {
        this.cluster = cluster;
        clusterPos = pos;
    }

    //------------//
    // getCluster //
    //------------//
    /**
     * Report the line cluster, if any, this filament is part of
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
    // getCurve //
    //----------//
    public NaturalSpline getCurve ()
    {
        if (curve == null) {
            computeData();
        }

        return curve;
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

    //-------------//
    // getPatterns //
    //-------------//
    /**
     * @return the patterns
     */
    public SortedMap<Integer, FilamentPattern> getPatterns ()
    {
        if (patterns != null) {
            return patterns;
        } else {
            return new TreeMap<Integer, FilamentPattern>();
        }
    }

    //----------------------//
    // getRectangleCentroid //
    //----------------------//
    public PixelPoint getRectangleCentroid (PixelRectangle roi)
    {
        Barycenter barycenter = new Barycenter();

        for (GlyphSection section : getMembers()) {
            section.cumulate(barycenter, roi);
        }

        if (barycenter.getWeight() != 0) {
            return new PixelPoint(
                (int) Math.rint(barycenter.getX()),
                (int) Math.rint(barycenter.getY()));
        } else {
            return null;
        }
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
     * Report the filament mean thickness at the provided abscissa
     * @param x the desired abscissa
     * @return the mean thickness measured, expressed in number of pixels.
     * Beware, this number will be zero if the probe falls entirely in a hole
     * between two sections.
     */
    public int getThicknessAt (int x)
    {
        PixelRectangle box = getContourBox();

        if ((x < box.x) || (x >= (box.x + box.width))) {
            throw new IllegalArgumentException(
                "Abscissa not within filament range");
        }

        Scale          scale = new Scale(getInterline());
        int            probeWidth = scale.toPixels(constants.probeWidth);
        PixelRectangle roi = new PixelRectangle(x, box.y, 0, box.height);
        roi.grow(probeWidth / 2, 0);

        // Use a large-enough collector
        PointsCollector collector = new PointsCollector(roi);

        for (GlyphSection section : getMembers()) {
            section.cumulate(collector);
        }

        int count = collector.getCount();

        if (count == 0) {
            if (logger.isFineEnabled()) {
                logger.warning("Thickness " + this + " x:" + x + " nopoints");
            }

            return 0;
        } else {
            // Return MEAN thickness on probe width
            int thickness = (int) Math.rint(
                (double) count / (2 * (probeWidth / 2)));

            if (logger.isFineEnabled()) {
                logger.fine(
                    this + " x:" + x + " y:" + (float) getCurve().yAt(x) +
                    " thickness:" + thickness);
            }

            return thickness;
        }
    }

    //----------------//
    // setTopDistance //
    //----------------//
    public void setTopDistance (int topDist)
    {
        this.topDist = topDist;
    }

    //----------------//
    // getTopDistance //
    //----------------//
    public Integer getTopDistance ()
    {
        return topDist;
    }

    //------------//
    // addPattern //
    //------------//
    /**
     * Add a pattern where this filament appears
     * @param column the column index of the pattern
     * @param pattern the pattern which contains this filament
     */
    public void addPattern (int             column,
                            FilamentPattern pattern)
    {
        if (patterns == null) {
            patterns = new TreeMap<Integer, FilamentPattern>();
        }

        patterns.put(column, pattern);
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
        System.out.println("   cluster=" + cluster);
        System.out.println("   clusterPos=" + clusterPos);
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
            include(section);
        }

        that.parent = this;
    }

    //---------//
    // include //
    //---------//
    public void include (GlyphSection section)
    {
        addSection(section, Linking.LINK_BACK);
        invalidateCache();
    }

    //-------//
    // reset //
    //-------//
    @Override
    public void invalidateCache ()
    {
        super.invalidateCache();

        curve = null;
        pStart = pStop = null;
        topDist = null;
        points = null;
    }

    //------------//
    // renderLine //
    //------------//
    @Override
    public void renderLine (Graphics2D g)
    {
        // Draw the defining points
        Color oldColor = g.getColor();
        g.setColor(Color.YELLOW);

        // Radius
        int r = (int) Math.rint(scale.interline() * 0.1);

        if (points != null) {
            for (PixelPoint p : points) {
                g.drawOval(p.x - r, p.y - r, 2 * r, 2 * r);
            }
        }

        g.setColor(oldColor);

        // Then the curve itself
        getCurve(); // Make sure it has been computed

        if (curve != null) {
            g.draw(curve);
        }
    }

    //------------//
    // trueLength //
    //------------//
    public int trueLength ()
    {
        double density = (double) getWeight() / (getLength() * scale.mainFore());

        return (int) Math.rint(getLength() * density);
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

        if (topDist != null) {
            sb.append(" topDist:")
              .append(topDist);
        }

        if (cluster != null) {
            sb.append(" cluster:")
              .append(cluster.getId())
              .append("p")
              .append(clusterPos);
        }

        return sb.toString();
    }

    //-------------//
    // computeData //
    //-------------//
    /**
     * Compute curve, pStart, pStop
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

            PixelRectangle box = getContourBox();
            int            start = (pStart != null) ? pStart.x : box.x;
            int            stop = (pStop != null) ? pStop.x
                                  : (box.x + (box.width - 1));
            int            width = stop - start + 1;

            PixelRectangle probe = new PixelRectangle(box);
            probe.x = start;
            probe.width = probeWidth;

            // Determine the number of segments and their precise length
            int              segCount = (int) Math.rint(width / typicalLength);
            double           segLength = (double) width / segCount;
            List<PixelPoint> points = new ArrayList<PixelPoint>(segCount + 1);

            // First point
            if (pStart == null) {
                pStart = getRectangleCentroid(probe);
                pStart.x = start;
            }

            points.add(pStart);

            // Intermediate points (perhaps none)
            for (int i = 1; i < segCount; i++) {
                probe.x = start + (int) Math.rint(i * segLength);

                PixelPoint pt = getRectangleCentroid(probe);

                // If, unfortunately, we are in a filament hole, just skip it
                if (pt != null) {
                    points.add(pt);
                }
            }

            // Last point
            if (pStop == null) {
                probe.x = stop - probe.width;
                pStop = getRectangleCentroid(probe);
                pStop.x = stop;
            }

            points.add(pStop);

            // Interpolate the best spline through the provided points
            curve = NaturalSpline.interpolate(
                points.toArray(new PixelPoint[points.size()]));
            addAttachment("SPLINE", curve); // Just for fun...

            // Remember points
            this.points = points;
        } catch (Exception ex) {
            logger.warning("Cannot getCurve", ex);
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

        Scale.Fraction probeWidth = new Scale.Fraction(
            0.5, // 0.5
            "Width of probing window to retrieve filament ordinate");
        Scale.Fraction segmentLength = new Scale.Fraction(
            2, // 4
            "Typical length between filament curve intermediate points");
    }
}
