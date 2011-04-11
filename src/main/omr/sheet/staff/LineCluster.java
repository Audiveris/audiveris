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
package omr.sheet.staff;

import omr.log.Logger;

import java.awt.Graphics2D;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

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

    /* Id for debug */
    private final int id;

    /**
     * Composing filaments, ordered by their relative position.
     * Within a cluster, the x-ordered set all filaments with the same relative
     * position will compose a staff line.
     */
    private SortedMap<Integer, Line> lines;

    /** Reference to cluster this one has been included into */
    private LineCluster parent;

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
        lines = new TreeMap<Integer, Line>();
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

    //---------//
    // getSize //
    //---------//
    public int getSize ()
    {
        return lines.size();
    }

    //----------------//
    // aggregateLines //
    //----------------//
    public void aggregateLines ()
    {
        // Aggregate line parts
        for (Line line : lines.values()) {
            line.aggregate();
        }
    }

    //---------//
    // cleanup //
    //---------//
    /**
     * - Remove additional lines
     * - Renumber remaining lines starting from zero
     * @param count the target line count
     */
    public void cleanup (int count)
    {
        ///logger.info("Cleanup " + this);

        // Pruning
        while (lines.size() > count) {
            // Remove the top or bottom line
            Line top = lines.get(lines.firstKey());
            int  topWL = top.fils.first()
                                 .trueLength();
            Line bot = lines.get(lines.lastKey());
            int  botWL = bot.fils.first()
                                 .trueLength();

            if (topWL < botWL) {
                lines.remove(lines.firstKey());
            } else {
                lines.remove(lines.lastKey());
            }
        }

        // Renumbering
        int firstPos = lines.firstKey();

        if (firstPos != 0) {
            SortedMap<Integer, Line> newLines = new TreeMap<Integer, Line>();

            for (Entry<Integer, Line> entry : lines.entrySet()) {
                int  pos = entry.getKey();
                Line line = entry.getValue();
                Line newLine = new Line(pos - firstPos);
                newLine.fils.addAll(line.fils);
                newLines.put(pos - firstPos, newLine);
            }

            lines = newLines;
        }
    }

    //--------//
    // render //
    //--------//
    public void render (Graphics2D g)
    {
        for (Line line : lines.values()) {
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

        for (Entry<Integer, Line> entry : lines.entrySet()) {
            sb.append(" ")
              .append(entry.getValue());
        }

        sb.append("}");

        return sb.toString();
    }

    //---------//
    // getLine //
    //---------//
    private Line getLine (int pos)
    {
        Line line = lines.get(pos);

        if (line == null) {
            line = new Line(pos);
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
                    int  pos = i + deltaPos;
                    Line line = getLine(pos);
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

        for (Entry<Integer, Line> entry : that.lines.entrySet()) {
            int  pos = entry.getKey() + deltaPos;
            Line line = entry.getValue();

            for (Filament fil : line.fils) {
                getLine(pos)
                    .add(fil);
            }
        }

        that.parent = this;

        if (logger.isFineEnabled()) {
            logger.fine("Merged:" + that);
            logger.fine("Merger:" + this);
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //------//
    // Line //
    //------//
    private static class Line
    {
        //~ Instance fields ----------------------------------------------------

        /** Relative position within the cluster */
        final int pos;

        /** Set of filaments, ordered by their starting abscissa */
        final SortedSet<Filament> fils = new TreeSet<Filament>(
            Filament.startComparator);

        //~ Constructors -------------------------------------------------------

        public Line (int pos)
        {
            this.pos = pos;
        }

        //~ Methods ------------------------------------------------------------

        public void add (Filament fil)
        {
            fils.add(fil);
        }

        public void aggregate ()
        {
            if (fils.size() > 1) {
                Filament first = fils.first();

                for (Iterator<Filament> it = fils.iterator(); it.hasNext();) {
                    Filament fil = it.next();

                    if (fil != first) {
                        first.include(fil);
                        it.remove();
                    }
                }
            }
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("Line#");
            sb.append(pos);
            sb.append("[");

            boolean started = false;

            for (Filament fil : fils) {
                if (started) {
                    sb.append("+");
                }

                sb.append("F")
                  .append(fil.getId());
                started = true;
            }

            sb.append("]");

            return sb.toString();
        }

        private void render (Graphics2D g)
        {
            for (Filament fil : fils) {
                fil.renderLine(g);
            }
        }
    }
}
