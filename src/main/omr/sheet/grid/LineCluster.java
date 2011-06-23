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

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.stick.Filament;

import java.awt.Graphics2D;
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
{
    //~ Static fields/initializers ---------------------------------------------

    //    /** Specific application parameters */
    //    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(LineCluster.class);

    //~ Instance fields --------------------------------------------------------

    /** Id for debug */
    private final int id;

    /** Interline for this cluster */
    private final int interline;

    /** Ids for lines */
    private int lineId = 0;

    /** Reference to cluster this one has been included into, if any */
    private LineCluster parent;

    /** Composing lines, ordered by their relative position (ordinate) */
    private SortedMap<Integer, FilamentLine> lines;

    /** (Cached) bounding box of this cluster */
    private PixelRectangle contourBox;

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
        if (logger.isFineEnabled()) {
            logger.fine("Creating cluster with F" + seed.getId());
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
    public PixelPoint getCenter ()
    {
        PixelRectangle box = getContourBox();

        return new PixelPoint(
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
     * @param interline the standard interline value, used for extrapolations
     * @return the sequence of cluster points, from top to bottom
     */
    public List<PixelPoint> getPointsAt (int    x,
                                         int    interline,
                                         double globalSlope)
    {
        SortedMap<Integer, PixelPoint> points = new TreeMap<Integer, PixelPoint>();
        List<Integer>                  holes = new ArrayList<Integer>();

        for (Entry<Integer, FilamentLine> entry : lines.entrySet()) {
            int          pos = entry.getKey();
            FilamentLine line = entry.getValue();

            if (line.isWithinRange(x)) {
                points.put(pos, new PixelPoint(x, line.yAt(x)));
            } else {
                holes.add(pos);
            }
        }

        // Interpolate or extrapolate the missing values if any
        for (int pos : holes) {
            Integer prevPos = null;
            Integer prevVal = null;

            for (int p = pos - 1; p >= lines.firstKey(); p--) {
                PixelPoint pt = points.get(p);

                if (pt != null) {
                    prevPos = p;
                    prevVal = pt.y;
                }
            }

            Integer nextPos = null;
            Integer nextVal = null;

            for (int p = pos + 1; p <= lines.lastKey(); p++) {
                PixelPoint pt = points.get(p);

                if (pt != null) {
                    nextPos = p;
                    nextVal = pt.y;
                }
            }

            int y;

            // Interpolate
            if ((prevPos != null) && (nextPos != null)) {
                y = prevVal +
                    (((pos - prevPos) * (nextVal - prevVal)) / (nextPos -
                                                               prevPos));
            } else {
                // Extrapolate
                if (prevPos != null) {
                    y = prevVal + ((pos - prevPos) * interline);
                } else if (nextPos != null) {
                    y = nextVal + ((pos - nextPos) * interline);
                } else {
                    // Here, we are beyond cluster left or right sides.
                    // We assume the delta abscissa is small enough to allow
                    // rather horizontal extrapolation
                    FilamentLine line = lines.get(pos);
                    PixelPoint   point = (x <= line.getStartPoint().x)
                                         ? line.getStartPoint()
                                         : line.getStopPoint();
                    y = (int) Math.rint(
                        point.y + ((x - point.x) * globalSlope));
                }
            }

            points.put(pos, new PixelPoint(x, y));
        }

        return new ArrayList<PixelPoint>(points.values());
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
    public List<PixelPoint> getStarts ()
    {
        List<PixelPoint> points = new ArrayList<PixelPoint>(getSize());

        for (FilamentLine line : lines.values()) {
            points.add(line.getStartPoint());
        }

        return points;
    }

    //----------//
    // getStops //
    //----------//
    public List<PixelPoint> getStops ()
    {
        List<PixelPoint> points = new ArrayList<PixelPoint>(getSize());

        for (FilamentLine line : lines.values()) {
            points.add(line.getStopPoint());
        }

        return points;
    }

    // containsSID ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public boolean containsSID (int id)
    {
        for (FilamentLine line : lines.values()) {
            if (line.fil.containsSID(id)) {
                return true;
            }
        }

        return false;
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
                FilamentLine line = entry.getValue();
                FilamentLine newLine = new FilamentLine(
                    line.getId(),
                    pos - firstPos);
                newLine.include(line);
                newLines.put(pos - firstPos, newLine);
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
    private FilamentLine getLine (int pos)
    {
        FilamentLine line = lines.get(pos);

        if (line == null) {
            line = new FilamentLine(++lineId, pos);
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
        if (logger.isFineEnabled()) {
            logger.fine(
                this + " include pivot:" + pivot.getId() + " at pos:" +
                pivotPos);
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
                    FilamentLine line = getLine(pos);
                    line.add(fil);
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
            getLine(pos)
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
}
