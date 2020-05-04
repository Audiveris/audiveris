//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     L i n e C l u s t e r                                      //
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

import org.audiveris.omr.glyph.dynamic.Compounds;
import org.audiveris.omr.glyph.dynamic.Filament;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Scale.InterlineScale;
import org.audiveris.omr.util.Vip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Class {@code LineCluster} is meant to aggregate instances of {@link Filament} that
 * are linked by {@link FilamentComb} instances and thus a cluster represents a staff
 * candidate, perhaps augmented above and/or below by "virtual lines" of ledgers.
 *
 * @author Hervé Bitteur
 */
public class LineCluster
        implements Vip
{

    private static final Logger logger = LoggerFactory.getLogger(LineCluster.class);

    /** For comparing LineCluster instances on their true length. */
    public static final Comparator<LineCluster> byReverseLength = new Comparator<LineCluster>()
    {
        @Override
        public int compare (LineCluster c1,
                            LineCluster c2)
        {
            // Sort on reverse length
            return Double.compare(c2.getTrueLength(), c1.getTrueLength());
        }
    };

    /** Id for debug. */
    private final String id;

    /** Typical interline. */
    private final InterlineScale interlineScale;

    /** Scaling information. */
    private final Scale scale;

    /** Reference to cluster this one has been included into, if any. */
    private LineCluster parent;

    /** Composing lines, ordered by their relative position (ordinate). */
    private SortedMap<Integer, StaffFilament> lines;

    /** (Cached) bounding box of this cluster. */
    private Rectangle contourBox;

    /** Cluster true length. */
    private Integer trueLength;

    /** For debugging. */
    private boolean vip = false;

    /**
     * Creates a new LineCluster object.
     *
     * @param scale          the global scaling information
     * @param interlineScale precise interline scale for this cluster
     * @param seed           the first filament of the cluster
     */
    public LineCluster (Scale scale,
                        InterlineScale interlineScale,
                        StaffFilament seed)
    {
        if (logger.isDebugEnabled() || seed.isVip()) {
            logger.info("VIP creating cluster with F{}", seed.getId());

            if (seed.isVip()) {
                setVip(true);
            }
        }

        this.scale = scale;
        this.interlineScale = interlineScale;

        id = "C" + seed.getId();

        lines = new TreeMap<>();

        include(seed, 0);
    }

    //---------//
    // destroy //
    //---------//
    /**
     * Remove the link back from filaments to this cluster.
     */
    public void destroy ()
    {
        for (StaffFilament line : lines.values()) {
            line.setCluster(null, 0);
            line.getCombs().clear();
        }
    }

    //-------------//
    // getAncestor //
    //-------------//
    /**
     * Report the top ancestor of this cluster.
     *
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
    // getBounds //
    //-----------//
    /**
     * Report the bounding box of the cluster
     *
     * @return bounds
     */
    public Rectangle getBounds ()
    {
        if (contourBox == null) {
            Rectangle box = null;

            for (StaffFilament line : getLines()) {
                if (box == null) {
                    box = new Rectangle(line.getBounds());
                } else {
                    box.add(line.getBounds());
                }
            }

            contourBox = box;
        }

        if (contourBox != null) {
            return new Rectangle(contourBox); // Copy
        } else {
            return null;
        }
    }

    //-----------//
    // getCenter //
    //-----------//
    /**
     * Report the center of cluster.
     *
     * @return the center
     */
    public Point getCenter ()
    {
        Rectangle box = getBounds();

        return new Point(box.x + (box.width / 2), box.y + (box.height / 2));
    }

    //--------------//
    // getFirstLine //
    //--------------//
    /**
     * Report the first line in cluster
     *
     * @return the first line
     */
    public StaffFilament getFirstLine ()
    {
        return lines.get(lines.firstKey());
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the cluster ID.
     *
     * @return the id
     */
    public String getId ()
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
        return interlineScale.main;
    }

    //-------------//
    // getLastLine //
    //-------------//
    /**
     * Report the last line in cluster
     *
     * @return last line
     */
    public StaffFilament getLastLine ()
    {
        return lines.get(lines.lastKey());
    }

    //----------//
    // getLines //
    //----------//
    /**
     * Report the cluster lines.
     *
     * @return lines
     */
    public Collection<StaffFilament> getLines ()
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
     * Report the sequence of points that correspond to a provided abscissa.
     *
     * @param x           the provided abscissa
     * @param xMargin     maximum abscissa margin for horizontal extrapolation
     * @param globalSlope global slope of the sheet
     * @return the sequence of cluster points, from top to bottom, with perhaps some holes indicated
     *         by null values
     */
    public List<Point2D> getPointsAt (double x,
                                      int xMargin,
                                      double globalSlope)
    {
        // Populate points (and holes)
        SortedMap<Integer, Point2D> points = new TreeMap<>();
        List<Integer> holes = new ArrayList<>();

        for (Entry<Integer, StaffFilament> entry : lines.entrySet()) {
            int pos = entry.getKey();
            StaffFilament line = entry.getValue();

            if (line.isWithinRange(x)) {
                points.put(pos, new Point2D.Double(x, line.yAt(x)));
            } else {
                holes.add(pos);
            }
        }

        // Try to fill holes by extrapolation
        for (int pos : holes) {
            final StaffFilament line = lines.get(pos);
            final Point2D end = (x <= line.getStartPoint().getX()) ? line.getStartPoint()
                    : line.getStopPoint();
            final double endX = end.getX();
            Double y = null;

            // Try to extrapolate vertically from previous or next line only
            for (int dir : new int[]{-1, 1}) {
                final StaffFilament otherLine = lines.get(pos + dir);

                if ((otherLine != null) && otherLine.isWithinRange(x) && otherLine.isWithinRange(
                        endX)) {
                    y = otherLine.yAt(x) + (line.yAt(endX) - otherLine.yAt(endX));

                    break;
                }
            }

            if (y == null) {
                // Try to extrapolate horizontally on a short distance only
                double dx = x - endX;

                if (Math.abs(dx) <= xMargin) {
                    y = end.getY() + (dx * globalSlope);
                }
            }

            points.put(pos, (y != null) ? new Point2D.Double(x, y) : null);
        }

        return new ArrayList<>(points.values());
    }

    //---------//
    // getSize //
    //---------//
    /**
     * Report the cluster count of lines
     *
     * @return count of lines
     */
    public int getSize ()
    {
        return lines.size();
    }

    //-----------//
    // getStarts //
    //-----------//
    /**
     * Report the sequence of lines starting point
     *
     * @return starts
     */
    public List<Point2D> getStarts ()
    {
        List<Point2D> points = new ArrayList<>(getSize());

        for (StaffFilament line : lines.values()) {
            points.add(line.getStartPoint());
        }

        return points;
    }

    //----------//
    // getStops //
    //----------//
    /**
     * Report the sequence of lines ending point
     *
     * @return stops
     */
    public List<Point2D> getStops ()
    {
        List<Point2D> points = new ArrayList<>(getSize());

        for (StaffFilament line : lines.values()) {
            points.add(line.getStopPoint());
        }

        return points;
    }

    //---------------//
    // getTrueLength //
    //---------------//
    /**
     * Report a measurement of the cluster length.
     *
     * @return the mean true length of cluster lines
     */
    public int getTrueLength ()
    {
        if (trueLength == null) {
            // Determine mean true line length in this cluster
            int meanTrueLength = 0;

            for (StaffFilament line : lines.values()) {
                meanTrueLength += line.getTrueLength();
            }

            meanTrueLength /= lines.size();
            logger.debug("TrueLength: {} for {}", meanTrueLength, this);

            trueLength = meanTrueLength;
        }

        return trueLength;
    }

    //------------------------//
    // includeFilamentByIndex //
    //------------------------//
    /**
     * Include a filament to this cluster, using the provided relative
     * line index counted from zero (rather than the line position).
     * Check this room is "free" on the cluster line
     *
     * @param filament the filament to include
     * @param index    the zero-based line index
     * @return true if there was room for inclusion
     */
    public boolean includeFilamentByIndex (StaffFilament filament,
                                           int index)
    {
        final Rectangle filBox = filament.getBounds();
        int i = 0;

        for (Entry<Integer, StaffFilament> entry : lines.entrySet()) {
            if (i++ == index) {
                StaffFilament line = entry.getValue();

                // Check for horizontal room
                // For filaments one above the other, check resulting thickness
                for (Section section : line.getMembers()) {
                    // Horizontal overlap?
                    Rectangle sctBox = section.getBounds();
                    int overlap = GeoUtil.xOverlap(filBox, sctBox);

                    if (overlap > 0) {
                        // Check resulting thickness
                        double thickness = Compounds.getThicknessAt(
                                Math.max(filBox.x, sctBox.x) + (overlap / 2),
                                Orientation.HORIZONTAL,
                                scale,
                                filament,
                                line);

                        if (thickness > scale.getMaxFore()) {
                            if (filament.isVip() || logger.isDebugEnabled()) {
                                logger.info("VIP no room for {} in {}", filament, this);
                            }

                            return false;
                        }
                    }
                }

                line.include(filament);
                filament.setCluster(this, entry.getKey());
                invalidateCache();

                return true;
            }
        }

        return false; // Should not happen
    }

    //-----------//
    // isOneLine //
    //-----------//
    public boolean isOneLine ()
    {
        return lines.size() == 1;
    }

    //-------//
    // isVip //
    //-------//
    @Override
    public boolean isVip ()
    {
        return vip;
    }

    //--------//
    // setVip //
    //--------//
    @Override
    public void setVip (boolean vip)
    {
        this.vip = vip;
    }

    //-----------//
    // mergeWith //
    //-----------//
    /**
     * Include another cluster into this one.
     *
     * @param that     the other cluster to include
     * @param deltaPos shift between clusters vertical positions
     */
    public void mergeWith (LineCluster that,
                           int deltaPos)
    {
        include(that, deltaPos + (this.lines.firstKey() - that.lines.firstKey()));
    }

    //---------------//
    // renumberLines //
    //---------------//
    /**
     * Renumber the cluster (remaining) lines counting from zero.
     */
    public void renumberLines ()
    {
        // Renumbering
        int firstPos = lines.firstKey();

        if (firstPos != 0) {
            SortedMap<Integer, StaffFilament> newLines = new TreeMap<>();

            for (Entry<Integer, StaffFilament> entry : lines.entrySet()) {
                int pos = entry.getKey();
                int newPos = pos - firstPos;
                StaffFilament line = entry.getValue();
                line.setCluster(this, newPos);
                newLines.put(newPos, line);
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
        StringBuilder sb = new StringBuilder("Cluster#");
        sb.append(getId());

        sb.append("{").append(interlineScale);

        sb.append(" size:").append(getSize());

        for (Entry<Integer, StaffFilament> entry : lines.entrySet()) {
            final StaffFilament fil = entry.getValue();
            sb.append(" ").append("fil#").append(fil.getId()).append("@").append(
                    fil.getClusterPos());
        }

        sb.append("}");

        return sb.toString();
    }

    //------//
    // trim //
    //------//
    /**
     * Remove lines in excess, often due to aligned ledgers.
     * <p>
     * We prune the cluster incrementally, choosing either the top line or the
     * bottom line of the cluster.
     * We discard the line with the lower "true length".
     * <p>
     * TODO: We could also use the presence of long segments (much longer than typical
     * ledger length) to differentiate staff lines from sequences of ledgers.
     *
     * @param combSizes               allowed comb sizes
     * @param minTablatureLengthRatio for tablature, minimum length as ratio of other lines
     * @return the removed cluster lines (filaments)
     */
    public List<StaffFilament> trim (TreeSet<Integer> combSizes,
                                     double minTablatureLengthRatio)
    {
        logger.debug("Trim {}", this);
        final List<StaffFilament> removed = new ArrayList<>();

        // Pruning
        final int maxCount = combSizes.last();

        while (lines.size() > maxCount) {
            // Remove the top or bottom line
            final StaffFilament top = lines.get(lines.firstKey());
            int topWL = top.getTrueLength();

            final StaffFilament bot = lines.get(lines.lastKey());
            int botWL = bot.getTrueLength();

            final StaffFilament line; // Which line to remove?

            // Pick up line with lower true length
            if (topWL < botWL) {
                line = top;
                removed.add(lines.remove(lines.firstKey()));
            } else {
                line = bot;
                removed.add(lines.remove(lines.lastKey()));
            }

            // House keeping
            line.setCluster(null, 0);
            line.getCombs().clear();
        }

        if (lines.size() == 6) {
            // We accept 6-line tablatures, but make sure lines are long enough to avoid
            // 5-line standard staff with ledger line to be mistaken for a 6-line tablature
            final SortedMap<Integer, Integer> trueLengths = new TreeMap<>();
            for (Entry<Integer, StaffFilament> entry : lines.entrySet()) {
                trueLengths.put(entry.getKey(), entry.getValue().getTrueLength());
            }

            // Mean true length for lines, top & bottom excepted
            int sum = 0;
            int nb = 0;

            for (int i = lines.firstKey() + 1, iMax = lines.lastKey() - 1; i <= iMax; i++) {
                nb++;
                sum += trueLengths.get(i);
            }

            final int minLength = (int) Math.rint(minTablatureLengthRatio * sum / nb);
            final int topWL = trueLengths.get(lines.firstKey());
            final int botWL = trueLengths.get(lines.lastKey());
            StaffFilament line = null; // A line to remove?

            // Inspect line with lower true length
            if (topWL < botWL) {
                // Check top line
                if (topWL < minLength) {
                    line = lines.get(lines.firstKey());
                    removed.add(lines.remove(lines.firstKey()));
                }
            } else {
                // Check bottom line
                if (botWL < minLength) {
                    line = lines.get(lines.lastKey());
                    removed.add(lines.remove(lines.lastKey()));
                }
            }

            if (line != null) {
                // House keeping
                line.setCluster(null, 0);
                line.getCombs().clear();
            }
        }

        renumberLines();
        invalidateCache();

        return removed;
    }

    //---------//
    // include //
    //---------//
    /**
     * Include a filament, with all its combs.
     *
     * @param pivot    the filament to include
     * @param pivotPos the imposed position within the cluster
     */
    private void include (StaffFilament pivot,
                          int pivotPos)
    {
        if (logger.isDebugEnabled() || pivot.isVip()) {
            logger.info("VIP {} include pivot:{} at pos:{}", this, pivot.getId(), pivotPos);

            if (pivot.isVip()) {
                setVip(true);
            }
        }

        StaffFilament ancestor = (StaffFilament) pivot.getAncestor();

        // Loop on all combs that involve this filament
        // Use a copy to avoid concurrent modification error
        List<FilamentComb> combs = new ArrayList<>(pivot.getCombs().values());

        if (combs.isEmpty()) {
            // Specific case of one-line clusters, without any comb
            lines.put(0, pivot);
            pivot.setCluster(this, 0);
        } else {
            for (FilamentComb comb : combs) {
                if (comb.isProcessed()) {
                    continue;
                }

                comb.setProcessed(true);

                int deltaPos = pivotPos - comb.getIndex(pivot);
                logger.debug("{} deltaPos:{}", comb, deltaPos);

                // Dispatch content of comb to proper lines
                for (int i = 0; i < comb.getCount(); i++) {
                    StaffFilament fil = (StaffFilament) comb.getFilament(i).getAncestor();
                    LineCluster cluster = fil.getCluster();

                    if (cluster == null) {
                        int pos = i + deltaPos;
                        StaffFilament line = lines.get(pos);

                        if (line == null) {
                            lines.put(pos, fil);
                        } else {
                            line.include(fil);
                        }

                        if (fil.isVip()) {
                            logger.info("VIP adding {} to {} at pos {}", fil, this, pos);
                            setVip(true);
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
    }

    //---------//
    // include //
    //---------//
    /**
     * Merge another cluster with this one.
     *
     * @param that     the other cluster
     * @param deltaPos the delta to apply to that cluster positions
     */
    private void include (LineCluster that,
                          int deltaPos)
    {
        if (logger.isDebugEnabled() || isVip() || that.isVip()) {
            logger.info("VIP inclusion of {} into {} deltaPos:{}", that, this, deltaPos);

            if (that.isVip()) {
                setVip(true);
            }
        }

        for (Entry<Integer, StaffFilament> entry : that.lines.entrySet()) {
            int thisPos = entry.getKey() + deltaPos;
            StaffFilament thatLine = entry.getValue();
            StaffFilament thisLine = lines.get(thisPos);

            if (thisLine == null) {
                thisLine = thatLine;
                thatLine.setCluster(this, thisPos);
                lines.put(thisPos, thisLine);
            } else {
                thisLine.include(thatLine);
            }
        }

        that.parent = this;

        if (logger.isDebugEnabled()) {
            logger.debug("Merged:{}", that);
            logger.debug("Merger:{}", this);
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
