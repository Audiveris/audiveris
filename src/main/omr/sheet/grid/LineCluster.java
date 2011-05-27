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

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(LineCluster.class);

    //~ Instance fields --------------------------------------------------------

    /** Id for debug */
    private final int id;

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
    public LineCluster (Filament seed)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Creating cluster with F" + seed.getId());
        }

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

    //---------//
    // cleanup //
    //---------//
    /**
     * - Remove lines in excess
     * - Renumber remaining lines counting from zero
     * @param count the target line count
     */
    public void cleanup (int count)
    {
        ///logger.info("Cleanup " + this);

        // Pruning
        while (lines.size() > count) {
            // Remove the top or bottom line
            FilamentLine top = lines.get(lines.firstKey());
            int          topWL = top.fil.trueLength();
            FilamentLine bot = lines.get(lines.lastKey());
            int          botWL = bot.fil.trueLength();

            if (topWL < botWL) {
                lines.remove(lines.firstKey());
            } else {
                lines.remove(lines.lastKey());
            }
        }

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
    public boolean includeFilamentByIndex (Filament filament,
                                           int      index)
    {
        final PixelRectangle filBox = filament.getContourBox();
        int                  i = 0;

        for (Entry<Integer, FilamentLine> entry : lines.entrySet()) {
            if (i++ == index) {
                FilamentLine line = entry.getValue();

                // Check for horizontal room
                for (GlyphSection section : line.fil.getMembers()) {
                    if (section.getContourBox()
                               .intersects(filBox)) {
                        logger.warning(
                            "No room for " + filament + " in " + this);

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

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{Cluster#");
        sb.append(getId());

        sb.append(" size:")
          .append(getSize());

        for (Entry<Integer, FilamentLine> entry : lines.entrySet()) {
            sb.append(" ")
              .append(entry.getValue());
        }

        sb.append("}");

        return sb.toString();
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
     * @param filament the filament to include
     */
    private void include (Filament pivot,
                          int      pivotPos)
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                this + " include pivot:" + pivot.getId() + " pos:" + pivotPos);
        }

        Filament ancestor = pivot.getAncestor();

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
                Filament    fil = pattern.getFilament(i)
                                         .getAncestor();
                LineCluster cluster = fil.getCluster();

                if (cluster == null) {
                    int          pos = i + deltaPos;
                    FilamentLine line = getLine(pos);
                    line.add(fil);
                    fil.setCluster(this, pos);

                    if (fil != ancestor) {
                        include(fil, pos); // Recursively
                    }
                } else if (cluster.getAncestor() != this) {
                    // Need to merge the two clusters
                    include(cluster, i - fil.getClusterPos() + deltaPos);
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
}
