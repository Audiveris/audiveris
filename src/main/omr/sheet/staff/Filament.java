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
package omr.sheet.staff;

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

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
     * For comparing Filament instances on position (y) then coordinate (x) of
     * their starting point
     */
    public static final Comparator<Filament> startComparator = new Comparator<Filament>() {
        public int compare (Filament s1,
                            Filament s2)
        {
            if (s1 == s2) {
                return 0;
            }

            // Sort on vertical position first
            int dPos = s1.getStartPoint().y - s2.getStartPoint().y;

            if (dPos != 0) {
                return dPos;
            }

            // Sort on horizontal coordinate second
            int dStart = s1.getStartPoint().x - s2.getStartPoint().x;

            if (dStart != 0) {
                return dStart;
            } else {
                throw new RuntimeException(
                    "Overlapping filaments " + s1 + " & " + s2);
            }
        }
    };

    /**
     * For comparing Filament instances on position (y) then coordinate (x) of
     * their stopping point
     */
    public static final Comparator<Filament> stopComparator = new Comparator<Filament>() {
        public int compare (Filament s1,
                            Filament s2)
        {
            if (s1 == s2) {
                return 0;
            }

            // Sort on vertical position first
            int dPos = s1.getStopPoint().y - s2.getStopPoint().y;

            if (dPos != 0) {
                return dPos;
            }

            // Sort on horizontal coordinate second
            int dStart = s1.getStopPoint().x - s2.getStopPoint().x;

            if (dStart != 0) {
                return dStart;
            } else {
                throw new RuntimeException(
                    "Overlapping filaments " + s1 + " & " + s2);
            }
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
            } else {
                throw new RuntimeException(
                    "Overlapping filaments " + s1 + " & " + s2);
            }
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

    /** This filament has been logically removed */
    private boolean discarded;

    /** Ref of the filament this one has been merged into */
    private Filament parent;

    /** Patterns where this filament appears. map (column -> pattern) */
    private Map<Integer, FilamentPattern> patterns;

    /** Most frequent line position in standard patterns */
    private Integer linePosition;

    /** Distance from tilted top egde */
    private Integer topDist;

    /** The line cluster, if any, this filament is part of */
    private LineCluster cluster;

    /** Relative position in cluster */
    private int clusterPos;

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
     * @param pos  the relative line position within the cluster
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

    //-----------------//
    // setLinePosition //
    //-----------------//
    /**
     * @param linePosition the linePosition to set
     */
    public void setLinePosition (int linePosition)
    {
        this.linePosition = linePosition;
    }

    //-----------------//
    // getLinePosition //
    //-----------------//
    /**
     * @return the linePosition
     */
    public Integer getLinePosition ()
    {
        return linePosition;
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

    //--------------//
    // setDiscarded //
    //--------------//
    /**
     * @param discarded the boolean to set
     */
    public void setDiscarded (boolean discarded)
    {
        this.discarded = discarded;
    }

    //------------//
    // isDiscarded //
    //-------------//
    /**
     * @return the discarded flag
     */
    public boolean isDiscarded ()
    {
        return discarded;
    }

    //-------------//
    // getPatterns //
    //-------------//
    /**
     * @return the patterns
     */
    public Map<Integer, FilamentPattern> getPatterns ()
    {
        if (patterns != null) {
            return patterns;
        } else {
            return Collections.EMPTY_MAP;
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

            //            // Find out precise min and max ordinates
            //            int[] yy = new int[count];
            //            System.arraycopy(collector.getYValues(), 0, yy, 0, count);
            //            Arrays.sort(yy);
            //
            //            int yMin = yy[0];
            //            int yMax = yy[count - 1];
            //
            //            if (logger.isFineEnabled()) {
            //                logger.fine(
            //                    "Thickness " + this + " x:" + x + " yMax:" + yMax +
            //                    " yMin:" + yMin);
            //            }
            //
            //            return yMax - yMin + 1;
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
    }

    //------------//
    // renderLine //
    //------------//
    @Override
    public void renderLine (Graphics2D g)
    {
        NaturalSpline curve = getCurve();

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

        if (linePosition != null) {
            sb.append(" stdPos:")
              .append(linePosition);
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
            Scale          scale = new Scale(getInterline());

            /** Width of window to retrieve pixels */
            int probeWidth = scale.toPixels(constants.probeWidth);

            /** Typical length of curve segments */
            double typicalLength = scale.toPixels(constants.segmentLength);

            PixelRectangle box = getContourBox();
            PixelRectangle probe = new PixelRectangle(box);
            probe.width = probeWidth;

            // Determine the number of segments and their precise length
            int              segCount = (int) Math.rint(
                box.width / typicalLength);
            double           segLength = (double) box.width / segCount;
            List<PixelPoint> points = new ArrayList<PixelPoint>(segCount + 1);

            // First point
            pStart = getRectangleCentroid(probe);
            pStart.x = box.x;
            points.add(pStart);

            // Intermediate points (perhaps none)
            for (int i = 1; i < segCount; i++) {
                probe.x = box.x + (int) Math.rint(i * segLength);

                PixelPoint pt = getRectangleCentroid(probe);

                // If, unfortunately, we are in a filament hole, just skip it
                if (pt != null) {
                    points.add(pt);
                }
            }

            // Last point
            probe.x = ((box.x + box.width) - 1) - probe.width;
            pStop = getRectangleCentroid(probe);
            pStop.x = (box.x + box.width) - 1;
            points.add(pStop);

            // Interpolate the best spline through the provided points
            curve = NaturalSpline.interpolate(
                points.toArray(new PixelPoint[0]));
            addAttachment("SPLINE", curve); // Just for fun...
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
