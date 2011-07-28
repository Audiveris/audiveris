//----------------------------------------------------------------------------//
//                                                                            //
//                           L i n e C l u s t e r                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.glyph.GlyphSection;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import omr.util.Vip;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.Map.Entry;

/**
 * Class {@code LineCluster} is meant to aggregate instances of
 * {@link Filament} that are linked by {@link FilamentPattern} instances and
 * thus a cluster represents a staff candidate
 *
 * @author Hervé Bitteur
 */
public class LineCluster
    implements Vip
{
    //~ Static fields/initializers ---------------------------------------------

    //    /** Specific application parameters */
    //    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(LineCluster.class);

    /** For comparing LineCluster instances on their true length */
    public static final Comparator<LineCluster> reverseLengthComparator = new Comparator<LineCluster>() {
        public int compare (LineCluster c1,
                            LineCluster c2)
        {
            // Sort on reverse length
            return Double.compare(c2.getTrueLength(), c1.getTrueLength());
        }
    };


    //~ Instance fields --------------------------------------------------------

    /** Id for debug */
    private final int id;

    /** Interline for this cluster */
    private final int interline;

    /** Reference to cluster this one has been included into, if any */
    private LineCluster parent;

    /** Composing lines, ordered by their relative position (ordinate) */
    private SortedMap<Integer, FilamentLine> lines;

    /** (Cached) bounding box of this cluster */
    private PixelRectangle contourBox;

    /** CLuster true length */
    private Integer trueLength;

    /** For debugging */
    private boolean vip = false;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // LineCluster //
    //-------------//
    /**
     * Creates a new LineCluster object.
     *
     * @param seed the first filament of the cluster
     */
    public LineCluster (int          interline,
                        LineFilament seed)
    {
        if (logger.isFineEnabled() || seed.isVip()) {
            logger.info("Creating cluster with F" + seed.getId());

            if (seed.isVip()) {
                setVip();
            }
        }

        this.interline = interline;
        this.id = seed.getId();

        lines = new TreeMap<Integer, FilamentLine>();

        include(seed, 0);
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getAncestor //
    //-------------//
    /**
     * Report the top ancestor of this cluster
     * @return the cluster ancestor
     */
    public LineCluster getAncestor ()
    {
        LineCluster cluster = this;

        while (cluster.parent != null) {
            cluster = cluster.parent;
        }

        return cluster;
    }

    //-----------//
    // getCenter //
    //-----------//
    /**
     * Report the center of cluster
     * @return the center
     */
    public Point2D getCenter ()
    {
        PixelRectangle box = getContourBox();

        return new Point2D.Double(
            box.x + (box.width / 2),
            box.y + (box.height / 2));
    }

    //---------------//
    // getContourBox //
    //---------------//
    public PixelRectangle getContourBox ()
    {
        if (contourBox == null) {
            PixelRectangle box = null;

            for (FilamentLine line : getLines()) {
                if (box == null) {
                    box = new PixelRectangle(line.getContourBox());
                } else {
                    box.add(line.getContourBox());
                }
            }

            contourBox = box;
        }

        if (contourBox != null) {
            return new PixelRectangle(contourBox);
        } else {
            return null;
        }
    }

    //--------------//
    // getFirstLine //
    //--------------//
    public FilamentLine getFirstLine ()
    {
        return lines.get(lines.firstKey());
    }

    //-------//
    // getId //
    //-------//
    /**
     * @return the id
     */
    public int getId ()
    {
        return id;
    }

    //--------------//
    // getInterline //
    //--------------//
    /**
     * @return the interline
     */
    public int getInterline ()
    {
        return interline;
    }

    //-------------//
    // getLastLine //
    //-------------//
    public FilamentLine getLastLine ()
    {
        return lines.get(lines.lastKey());
    }

    //----------//
    // getLines //
    //----------//
    public Collection<FilamentLine> getLines ()
    {
        return lines.values();
    }

    //-----------//
    // getParent //
    //-----------//
    /**
     * @return the parent
     */
    public LineCluster getParent ()
    {
        return parent;
    }

    //-------------//
    // getPointsAt //
    //-------------//
    /**
     * Report the sequence of points that correspond to a provided abscissa
     * @param x the provided abscissa
     * @param xMargin maximum abscissa margin for horizontal extrapolation
     * @param interline the standard interline value, used for vertical 
     * extrapolations
     * @return the sequence of cluster points, from top to bottom, with perhaps
     * some holes indicated by null values
     */
    public List<Point2D> getPointsAt (double x,
                                      int    xMargin,
                                      int    interline,
                                      double globalSlope)
    {
        SortedMap<Integer, Point2D> points = new TreeMap<Integer, Point2D>();
        List<Integer>               holes = new ArrayList<Integer>();

        for (Entry<Integer, FilamentLine> entry : lines.entrySet()) {
            int          pos = entry.getKey();
            FilamentLine line = entry.getValue();

            if (line.isWithinRange(x)) {
                points.put(
                    pos,
                    new Point2D.Double(x, line.yAt(x)));
            } else {
                holes.add(pos);
            }
        }

        // Interpolate or extrapolate the missing values if any
        for (int pos : holes) {
            Integer prevPos = null;
            Double  prevVal = null;

            for (int p = pos - 1; p >= lines.firstKey(); p--) {
                Point2D pt = points.get(p);

                if (pt != null) {
                    prevPos = p;
                    prevVal = pt.getY();

                    break;
                }
            }

            Integer nextPos = null;
            Double  nextVal = null;

            for (int p = pos + 1; p <= lines.lastKey(); p++) {
                Point2D pt = points.get(p);

                if (pt != null) {
                    nextPos = p;
                    nextVal = pt.getY();

                    break;
                }
            }

            Double y = null;

            // Interpolate vertically
            if ((prevPos != null) && (nextPos != null)) {
                y = prevVal +
                    (((pos - prevPos) * (nextVal - prevVal)) / (nextPos -
                                                               prevPos));
            } else {
                // Extrapolate vertically, only for one interline max
                if ((prevPos != null) && ((pos - prevPos) == 1)) {
                    y = prevVal + interline;
                } else if ((nextPos != null) && ((nextPos - pos) == 1)) {
                    y = nextVal - interline;
                } else {
                    // Extrapolate horizontally on a short distance
                    FilamentLine line = lines.get(pos);
                    Point2D      point = (x <= line.getStartPoint()
                                                   .getX())
                                         ? line.getStartPoint()
                                         : line.getStopPoint();
                    double       dx = x - point.getX();

                    if (Math.abs(dx) <= xMargin) {
                        y = point.getY() + (dx * globalSlope);
                    }
                }
            }

            points.put(pos, (y != null) ? new Point2D.Double(x, y) : null);
        }

        return new ArrayList<Point2D>(points.values());
    }

    //---------//
    // getSize //
    //---------//
    public int getSize ()
    {
        return lines.size();
    }

    //-----------//
    // getStarts //
    //-----------//
    public List<Point2D> getStarts ()
    {
        List<Point2D> points = new ArrayList<Point2D>(getSize());

        for (FilamentLine line : lines.values()) {
            points.add(line.getStartPoint());
        }

        return points;
    }

    //----------//
    // getStops //
    //----------//
    public List<Point2D> getStops ()
    {
        List<Point2D> points = new ArrayList<Point2D>(getSize());

        for (FilamentLine line : lines.values()) {
            points.add(line.getStopPoint());
        }

        return points;
    }

    //---------------//
    // getTrueLength //
    //---------------//
    /**
     * Report a measurement of the cluster length
     * @return the mean true length of cluster lines
     */
    public int getTrueLength ()
    {
        if (trueLength == null) {
            // Determine mean true line length in this cluster
            int meanTrueLength = 0;

            for (FilamentLine line : lines.values()) {
                meanTrueLength += line.fil.trueLength();
            }

            meanTrueLength /= lines.size();

            if (logger.isFineEnabled()) {
                logger.info("TrueLength: " + meanTrueLength + " for " + this);
            }

            trueLength = meanTrueLength;
        }

        return trueLength;
    }

    //    //-----------//
    //    // Constants //
    //    //-----------//
    //    private static final class Constants
    //        extends ConstantSet
    //    {
    //        //~ Instance fields ----------------------------------------------------
    //
    //        final Constant.Ratio minTrueLength = new Constant.Ratio(
    //            0.4,
    //            "Minimum true length ratio to keep a line in a cluster");
    //    }

    //--------//
    // setVip //
    //--------//
    public void setVip ()
    {
        vip = true;
    }

    //-------//
    // isVip //
    //-------//
    public boolean isVip ()
    {
        return vip;
    }

    //---------//
    // destroy //
    //---------//
    /**
     * Remove the link back from filaments to this cluster
     */
    public void destroy ()
    {
        for (FilamentLine line : lines.values()) {
            line.fil.setCluster(null, 0);
            line.fil.getPatterns()
                    .clear();
        }
    }

    //------------------------//
    // includeFilamentByIndex //
    //------------------------//
    /**
     * Include a filament to this cluster, using the provided relative line
     * index counted from zero (rather than the line position)
     * Check this room is "free" on the cluster line
     * @param filament the filament to include
     * @param index the zero-based line index
     * @return true if there was room for inclusion
     */
    public boolean includeFilamentByIndex (LineFilament filament,
                                           int          index)
    {
        final PixelRectangle filBox = filament.getContourBox();
        int                  i = 0;

        for (Entry<Integer, FilamentLine> entry : lines.entrySet()) {
            if (i++ == index) {
                FilamentLine line = entry.getValue();

                // Check for horizontal room
                // TODO: Should check the resulting thickness !!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                for (GlyphSection section : line.fil.getMembers()) {
                    if (section.getContourBox()
                               .intersects(filBox)) {
                        if (logger.isFineEnabled()) {
                            logger.fine(
                                "No room for " + filament + " in " + this);
                        }

                        return false;
                    }
                }

                line.add(filament);
                filament.setCluster(this, entry.getKey());
                invalidateCache();

                return true;
            }
        }

        return false; // Should not happen
    }

    //-----------//
    // mergeWith //
    //-----------//
    public void mergeWith (LineCluster that,
                           int         deltaPos)
    {
        include(
            that,
            deltaPos + (this.lines.firstKey() - that.lines.firstKey()));
    }

    //--------//
    // render //
    //--------//
    public void render (Graphics2D g)
    {
        for (FilamentLine line : lines.values()) {
            line.render(g);
        }
    }

    //---------------//
    // renumberLines //
    //---------------//
    /**
     * Renumber the remaining lines counting from zero
     */
    public void renumberLines ()
    {
        // Renumbering
        int firstPos = lines.firstKey();

        if (firstPos != 0) {
            SortedMap<Integer, FilamentLine> newLines = new TreeMap<Integer, FilamentLine>();

            for (Entry<Integer, FilamentLine> entry : lines.entrySet()) {
                int          pos = entry.getKey();
                int          newPos = pos - firstPos;
                FilamentLine line = entry.getValue();
                line.fil.setCluster(this, newPos);
                newLines.put(newPos, new FilamentLine(line.fil));
            }

            lines = newLines;
        }

        invalidateCache();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{Cluster#");
        sb.append(getId());

        sb.append(" interline:")
          .append(getInterline());

        sb.append(" size:")
          .append(getSize());

        for (Entry<Integer, FilamentLine> entry : lines.entrySet()) {
            sb.append(" ")
              .append(entry.getValue());
        }

        sb.append("}");

        return sb.toString();
    }

    //------//
    // trim //
    //------//
    /**
     * Remove lines in excess
     * @param count the target line count
     */
    public void trim (int count)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Trim " + this);
        }

        //        // Determine max true line length in this cluster
        //        int maxTrueLength = 0;
        //
        //        for (FilamentLine line : lines.values()) {
        //            maxTrueLength = Math.max(maxTrueLength, line.fil.trueLength());
        //        }
        //
        //        int minTrueLength = (int) Math.rint(
        //            maxTrueLength * constants.minTrueLength.getValue());
        //
        //        // Pruning
        //        for (Iterator<Integer> it = lines.keySet()
        //                                         .iterator(); it.hasNext();) {
        //            Integer      key = it.next();
        //            FilamentLine line = lines.get(key);
        //
        //            if (line.fil.trueLength() < minTrueLength) {
        //                it.remove();
        //                line.fil.setCluster(null, 0);
        //                line.fil.getPatterns()
        //                        .clear();
        //            }
        //        }

        // Pruning
        while (lines.size() > count) {
            // Remove the top or bottom line
            FilamentLine top = lines.get(lines.firstKey());
            int          topWL = top.fil.trueLength();
            FilamentLine bot = lines.get(lines.lastKey());
            int          botWL = bot.fil.trueLength();
            FilamentLine line = null;

            if (topWL < botWL) {
                line = top;
                lines.remove(lines.firstKey());
            } else {
                line = bot;
                lines.remove(lines.lastKey());
            }

            // House keeping
            line.fil.setCluster(null, 0);
            line.fil.getPatterns()
                    .clear();
        }

        renumberLines();
        invalidateCache();
    }

    //---------//
    // getLine //
    //---------//
    private FilamentLine getLine (int          pos,
                                  LineFilament fil)
    {
        FilamentLine line = lines.get(pos);

        if (line == null) {
            line = new FilamentLine(fil);
            lines.put(pos, line);
        }

        return line;
    }

    //---------//
    // include //
    //---------//
    /**
     * Include a filament, with all its patterns.
     * @param pivot the filament to include
     * @param pivotPos the imposed position within the cluster
     */
    private void include (LineFilament pivot,
                          int          pivotPos)
    {
        if (logger.isFineEnabled() || pivot.isVip()) {
            logger.info(
                this + " include pivot:" + pivot.getId() + " at pos:" +
                pivotPos);

            if (pivot.isVip()) {
                setVip();
            }
        }

        LineFilament ancestor = pivot.getAncestor();

        // Loop on all patterns that involve this filament
        for (FilamentPattern pattern : pivot.getPatterns()
                                            .values()) {
            if (pattern.isProcessed()) {
                continue;
            }

            pattern.setProcessed(true);

            int deltaPos = pivotPos - pattern.getIndex(pivot);

            if (logger.isFineEnabled()) {
                logger.fine(pattern + " deltaPos:" + deltaPos);
            }

            // Dispatch content of pattern to proper lines
            for (int i = 0; i < pattern.getCount(); i++) {
                LineFilament fil = pattern.getFilament(i)
                                          .getAncestor();
                LineCluster  cluster = fil.getCluster();

                if (cluster == null) {
                    int          pos = i + deltaPos;
                    FilamentLine line = getLine(pos, null);
                    line.add(fil);

                    if (fil.isVip()) {
                        logger.info(
                            "Adding " + fil + " to " + this + " at pos " + pos);
                        setVip();
                    }

                    fil.setCluster(this, pos);

                    if (fil != ancestor) {
                        include(fil, pos); // Recursively
                    }
                } else if (cluster.getAncestor() != this.getAncestor()) {
                    // Need to merge the two clusters
                    include(cluster, (i + deltaPos) - fil.getClusterPos());
                }
            }
        }
    }

    //---------//
    // include //
    //---------//
    /**
     * Merge another cluster with this one
     * @param that the other cluster
     * @param deltaPos the delta to apply to that cluster positions
     */
    private void include (LineCluster that,
                          int         deltaPos)
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                "Inclusion of " + that + " into " + this + " deltaPos:" +
                deltaPos);
        }

        for (Entry<Integer, FilamentLine> entry : that.lines.entrySet()) {
            int          pos = entry.getKey() + deltaPos;
            FilamentLine line = entry.getValue();
            getLine(pos, null)
                .include(line);
        }

        that.parent = this;

        if (logger.isFineEnabled()) {
            logger.fine("Merged:" + that);
            logger.fine("Merger:" + this);
        }

        invalidateCache();
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    private void invalidateCache ()
    {
        contourBox = null;
        trueLength = null;
    }
}
