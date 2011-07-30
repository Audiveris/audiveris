//----------------------------------------------------------------------------//
//                                                                            //
//                     C l u s t e r s R e t r i e v e r                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.math.Histogram;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Skew;

import omr.util.Wrapper;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

/**
 * Class {@code ClustersRetriever} performs vertical samplings of the
 * horizontal filaments in order to detect regular patterns of a preferred
 * interline value and aggregate the filaments into clusters of lines.
 *
 * @author Hervé Bitteur
 */
public class ClustersRetriever
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        ClustersRetriever.class);

    /** Stroke for drawing patterns */
    private static final Stroke patternStroke = new BasicStroke(0.5f);

    //~ Instance fields --------------------------------------------------------

    /** Comparator on cluster ordinate */
    public Comparator<LineCluster> ordinateComparator = new Comparator<LineCluster>() {
        public int compare (LineCluster c1,
                            LineCluster c2)
        {
            double o1 = ordinateOf(c1);
            double o2 = ordinateOf(c2);

            if (o1 < o2) {
                return -1;
            }

            if (o1 > o2) {
                return +1;
            }

            return 0;
        }
    };

    /** Related sheet */
    private final Sheet sheet;

    /** Related scale */
    private final Scale scale;

    /** Desired interline */
    private final int interline;

    /** Scale-dependent constants */
    private final Parameters params;

    /** Picture width to sample for patterns */
    private final int pictureWidth;

    /** Long filaments to process */
    private final List<LineFilament> filaments;

    /** Filaments discarded */
    private final List<LineFilament> discardedFilaments = new ArrayList<LineFilament>();

    /** Skew of the sheet */
    private final Skew skew;

    /** A map (colIndex -> vertical list of samples), sorted on colIndex */
    private Map<Integer, List<FilamentPattern>> colPatterns;

    /** Color used for pattern display */
    private final Color patternColor;

    /**
     * The popular size of patterns detected for the specified interline
     * (typically: 4, 5 or 6)
     */
    private int popSize;

    /** X values per column index */
    private int[] colX;

    /** Collection of clusters */
    private final List<LineCluster> clusters = new ArrayList<LineCluster>();

    //~ Constructors -----------------------------------------------------------

    //-------------------//
    // ClustersRetriever //
    //-------------------//
    /**
     * Creates a new ClustersRetriever object, for a given staff interline.
     *
     * @param sheet the sheet to process
     * @param filaments the current collection of filaments
     * @param interline the precise interline to be processed
     * @param patternColor color to be used for patterns display
     */
    public ClustersRetriever (Sheet              sheet,
                              List<LineFilament> filaments,
                              int                interline,
                              Color              patternColor)
    {
        this.sheet = sheet;
        this.filaments = filaments;
        this.interline = interline;
        this.patternColor = patternColor;

        skew = sheet.getSkew();
        pictureWidth = sheet.getWidth();
        scale = sheet.getScale();
        colPatterns = new TreeMap<Integer, List<FilamentPattern>>();

        params = new Parameters(scale);
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getClusters //
    //-------------//
    /**
     * Report the sequence of clusters detected by this retriever using its
     * provided interline value.
     * @return the sequence of interline-based clusters
     */
    public List<LineCluster> getClusters ()
    {
        return clusters;
    }

    //--------------//
    // getInterline //
    //--------------//
    /**
     * Report the value of the interline this retriever is based upon
     * @return the interline value
     */
    public int getInterline ()
    {
        return interline;
    }

    //-----------//
    // buildInfo //
    //-----------//
    public List<LineFilament> buildInfo ()
    {
        // Retrieve all vertical patterns of filaments
        retrievePatterns();

        // Remember the most popular length 
        retrievePopularSize();

        // Check relevance
        if ((popSize < 4) || (popSize > 6)) {
            logger.warning("Giving up spurious line pattern size: " + popSize);

            return discardedFilaments;
        }

        // Interconnect filaments via the network of patterns
        followPatternsNetwork();

        // Retrieve clusters
        retrieveClusters();

        logger.info(
            sheet.getLogPrefix() + "Retrieved line clusters: " +
            clusters.size() + " of size: " + popSize + " with interline: " +
            interline);

        return discardedFilaments;
    }

    //-------------//
    // renderItems //
    //-------------//
    /**
     * Render the vertical patterns of filaments
     * @param g graphics context
     */
    void renderItems (Graphics2D g)
    {
        Stroke oldStroke = g.getStroke();
        g.setStroke(patternStroke);
        g.setColor(patternColor);

        for (Entry<Integer, List<FilamentPattern>> entry : colPatterns.entrySet()) {
            int col = entry.getKey();
            int x = colX[col];

            for (FilamentPattern pattern : entry.getValue()) {
                ///if (pattern.getCount() == popLength) {
                g.draw(
                    new Line2D.Double(
                        x,
                        pattern.getY(0),
                        x,
                        pattern.getY(pattern.getCount() - 1)));

                ///}
            }
        }

        g.setStroke(oldStroke);
    }

    //-----------//
    // bestMatch //
    //-----------//
    /**
     * Find the best match between provided sequences (which may contain null
     * values when related data is not available)
     * @param one first sequence
     * @param two second sequence
     * @param bestDelta output: best delta between the two sequences
     * @return the best distance found
     */
    private double bestMatch (Double[]         one,
                              Double[]         two,
                              Wrapper<Integer> bestDelta)
    {
        final int deltaMax = one.length - 1;
        final int deltaMin = -deltaMax;

        double    bestDist = Double.MAX_VALUE;
        bestDelta.value = null;

        for (int delta = deltaMin; delta <= deltaMax; delta++) {
            int distSum = 0;
            int count = 0;

            for (int oneIdx = 0; oneIdx < one.length; oneIdx++) {
                int twoIdx = oneIdx + delta;

                if ((twoIdx >= 0) && (twoIdx < two.length)) {
                    Double oneVal = one[oneIdx];
                    Double twoVal = two[twoIdx];

                    if ((oneVal != null) && (twoVal != null)) {
                        count++;
                        distSum += Math.abs(twoVal - oneVal);
                    }
                }
            }

            if (count > 0) {
                double dist = (double) distSum / count;

                if (dist < bestDist) {
                    bestDist = dist;
                    bestDelta.value = delta;
                }
            }
        }

        return bestDist;
    }

    //----------//
    // canMerge //
    //----------//
    /**
     * Check for merge possibility between two clusters
     * @param one first cluster
     * @param two second cluster
     * @param deltaPos output: the delta in positions between these clusters
     * if the test has succeeded
     * @return true if successful
     */
    private boolean canMerge (LineCluster      one,
                              LineCluster      two,
                              Wrapper<Integer> deltaPos)
    {
        final Rectangle oneBox = one.getContourBox();
        final Rectangle twoBox = two.getContourBox();

        final int       oneLeft = oneBox.x;
        final int       oneRight = (oneBox.x + oneBox.width) - 1;
        final int       twoLeft = twoBox.x;
        final int       twoRight = (twoBox.x + twoBox.width) - 1;

        final int       minRight = Math.min(oneRight, twoRight);
        final int       maxLeft = Math.max(oneLeft, twoLeft);
        final int       gap = maxLeft - minRight;
        double          dist;

        if (logger.isFineEnabled()) {
            logger.fine("gap:" + gap);
        }

        if (gap <= 0) {
            // Overlap: use middle of common part
            final int    xMid = (maxLeft + minRight) / 2;
            final double slope = sheet.getSkew()
                                      .getSlope();
            dist = bestMatch(
                ordinatesOf(
                    one.getPointsAt(xMid, params.maxExpandDx, interline, slope)),
                ordinatesOf(
                    two.getPointsAt(xMid, params.maxExpandDx, interline, slope)),
                deltaPos);
        } else if (gap > params.maxMergeDx) {
            if (logger.isFineEnabled()) {
                logger.fine("Gap too wide between " + one + " & " + two);
            }

            return false;
        } else {
            // True gap: use proper edges
            if (oneLeft < twoLeft) { // Case one --- two
                dist = bestMatch(
                    ordinatesOf(one.getStops()),
                    ordinatesOf(two.getStarts()),
                    deltaPos);
            } else { // Case two --- one
                dist = bestMatch(
                    ordinatesOf(one.getStarts()),
                    ordinatesOf(two.getStops()),
                    deltaPos);
            }
        }

        // Check best distance
        if (logger.isFineEnabled()) {
            logger.fine(
                "canMerge dist: " + dist + " one:" + one + " two:" + two);
        }

        return dist <= params.maxMergeDy;
    }

    //-------------------------//
    // computeAcceptableLength //
    //-------------------------//
    private double computeAcceptableLength ()
    {
        // Determine minimum true length for valid clusters
        List<Integer> lengths = new ArrayList<Integer>();

        for (LineCluster cluster : clusters) {
            lengths.add(cluster.getTrueLength());
        }

        Collections.sort(lengths);

        int    medianLength = lengths.get(lengths.size() / 2);
        double minLength = medianLength * constants.minClusterLengthRatio.getValue();

        if (logger.isFineEnabled()) {
            logger.info(
                "medianLength: " + medianLength + " minLength: " + minLength);
        }

        return minLength;
    }

    //------------------//
    // connectAncestors //
    //------------------//
    private void connectAncestors (LineFilament one,
                                   LineFilament two)
    {
        LineFilament oneAnc = one.getAncestor();
        LineFilament twoAnc = two.getAncestor();

        if (oneAnc != twoAnc) {
            if (oneAnc.getLength() >= twoAnc.getLength()) {
                ///logger.info("Inclusion " + twoAnc + " into " + oneAnc);
                oneAnc.include(twoAnc);
                oneAnc.getPatterns()
                      .putAll(twoAnc.getPatterns());
            } else {
                ///logger.info("Inclusion " + oneAnc + " into " + twoAnc);
                twoAnc.include(oneAnc);
                twoAnc.getPatterns()
                      .putAll(oneAnc.getPatterns());
            }
        }
    }

    //----------------//
    // createClusters //
    //----------------//
    private void createClusters ()
    {
        Collections.sort(filaments, LineFilament.reverseLengthComparator);

        for (LineFilament fil : filaments) {
            fil = fil.getAncestor();

            if ((fil.getCluster() == null) && !fil.getPatterns()
                                                  .isEmpty()) {
                LineCluster cluster = new LineCluster(interline, fil);
                clusters.add(cluster);
            }
        }

        removeMergedClusters();
    }

    //----------------------------//
    // destroyNonStandardClusters //
    //----------------------------//
    private void destroyNonStandardClusters ()
    {
        for (Iterator<LineCluster> it = clusters.iterator(); it.hasNext();) {
            LineCluster cluster = it.next();

            if (cluster.getSize() != popSize) {
                if (logger.isFineEnabled()) {
                    logger.info("Destroying non standard " + cluster);
                }

                cluster.destroy();
                it.remove();
            }
        }
    }

    //------------------------------//
    // discardNonClusteredFilaments //
    //------------------------------//
    private void discardNonClusteredFilaments ()
    {
        for (Iterator<LineFilament> it = filaments.iterator(); it.hasNext();) {
            LineFilament fil = it.next();

            if (fil.getCluster() == null) {
                it.remove();
                discardedFilaments.add(fil);
            }
        }
    }

    //--------------//
    // dumpClusters //
    //--------------//
    private void dumpClusters ()
    {
        for (LineCluster cluster : clusters) {
            logger.info(cluster.getCenter() + " " + cluster.toString());
        }
    }

    //---------------//
    // expandCluster //
    //---------------//
    /**
     * Try to expand the provided cluster with filaments taken out of
     * the provided sorted collection of isolated filaments
     * @param cluster the cluster to work on
     * @param fils the (properly sorted) collection of filaments
     */
    private void expandCluster (LineCluster        cluster,
                                List<LineFilament> fils)
    {
        final double slope = sheet.getSkew()
                                  .getSlope();
        Rectangle    clusterBox = null;

        for (LineFilament fil : fils) {
            fil = fil.getAncestor();

            if (fil.getCluster() != null) {
                continue;
            }

            if (clusterBox == null) {
                clusterBox = cluster.getContourBox();
                clusterBox.grow(params.clusterXMargin, params.clusterYMargin);
            }

            PixelRectangle filBox = fil.getContourBox();
            PixelPoint     middle = new PixelPoint();
            middle.x = filBox.x + (filBox.width / 2);
            middle.y = (int) Math.rint(fil.getPositionAt(middle.x));

            if (clusterBox.contains(middle)) {
                // Check if this filament matches a cluster line
                List<Point2D> points = cluster.getPointsAt(
                    middle.x,
                    params.maxExpandDx,
                    interline,
                    slope);

                for (Point2D point : points) {
                    // Check vertical distance, if point is available
                    if (point == null) {
                        continue;
                    }

                    double dy = middle.y - point.getY();

                    if (Math.abs(dy) <= params.maxExpandDy) {
                        int index = points.indexOf(point);

                        if (cluster.includeFilamentByIndex(fil, index)) {
                            if (logger.isFineEnabled() ||
                                fil.isVip() ||
                                cluster.isVip()) {
                                logger.info(
                                    "Aggregated " + fil + " to " + cluster +
                                    " at index " + index);

                                if (fil.isVip()) {
                                    cluster.setVip();
                                }
                            }

                            clusterBox = null; // Invalidate cluster box

                            break;
                        }
                    }
                }
            }
        }
    }

    //----------------//
    // expandClusters //
    //----------------//
    /**
     * Aggregate non-clustered filaments to close clusters when appropriate
     */
    private void expandClusters ()
    {
        List<LineFilament> startFils = new ArrayList<LineFilament>(filaments);
        Collections.sort(startFils, LineFilament.startComparator);

        List<LineFilament> stopFils = new ArrayList<LineFilament>(startFils);
        Collections.sort(stopFils, LineFilament.stopComparator);

        // Browse clusters, starting with the longest ones
        Collections.sort(clusters, LineCluster.reverseLengthComparator);

        for (LineCluster cluster : clusters) {
            if (logger.isFineEnabled()) {
                logger.fine("Expanding " + cluster);
            }

            // Expanding on left side
            expandCluster(cluster, stopFils);
            // Expanding on right side
            expandCluster(cluster, startFils);
        }
    }

    //-----------------------//
    // followPatternsNetwork //
    //-----------------------//
    /**
     * Use the network of patterns and filaments to interconnect filaments via
     * common patterns
     */
    private void followPatternsNetwork ()
    {
        if (logger.isFineEnabled()) {
            logger.info("Following patterns network");
        }

        for (LineFilament fil : filaments) {
            Map<Integer, FilamentPattern> patterns = fil.getPatterns();

            // Sequence of lines around the filament, indexed by relative pos
            Map<Integer, LineFilament>    lines = new TreeMap<Integer, LineFilament>();

            // Loop on all patterns this filament is involved in
            for (FilamentPattern pattern : patterns.values()) {
                int posPivot = pattern.getIndex(fil);

                for (int pos = 0; pos < pattern.getCount(); pos++) {
                    int line = pos - posPivot;

                    if (line != 0) {
                        LineFilament f = lines.get(line);

                        if (f != null) {
                            connectAncestors(f, pattern.getFilament(pos));
                        } else {
                            lines.put(line, pattern.getFilament(pos));
                        }
                    }
                }
            }
        }

        removeMergedFilaments();
    }

    //-------------------//
    // mergeClusterPairs //
    //-------------------//
    /**
     * Merge clusters horizontally or destroy short clusters
     */
    private void mergeClusterPairs ()
    {
        // Sort clusters according to their ordinate in page
        Collections.sort(clusters, ordinateComparator);

        double minLength = computeAcceptableLength();
        WholeLoop: 
        for (int idx = 0; idx < clusters.size();) {
            LineCluster cluster = clusters.get(idx);
            Point2D     dskCenter = skew.deskewed(cluster.getCenter());
            double      yMax = dskCenter.getY() + params.maxMergeCenterDy;

            for (LineCluster cl : clusters.subList(idx + 1, clusters.size())) {
                // Check dy
                if (skew.deskewed(cl.getCenter())
                        .getY() > yMax) {
                    break;
                }

                // Merge
                logger.info(
                    "Pairing clusters C" + cluster.getId() + " & C" +
                    cl.getId());
                cluster.mergeWith(cl, 0);
                clusters.remove(cl);

                continue WholeLoop; // Recheck at same index
            }

            // Short isolated?
            if (cluster.getTrueLength() < minLength) {
                logger.info("Destroying spurious " + cluster);
                clusters.remove(cluster);
            } else {
                idx++; // Move forward
            }
        }

        removeMergedFilaments();
    }

    //---------------//
    // mergeClusters //
    //---------------//
    /**
     * Merge compatible clusters as much as possible
     */
    private void mergeClusters ()
    {
        // Sort clusters according to their ordinate in page
        Collections.sort(clusters, ordinateComparator);

        for (LineCluster current : clusters) {
            LineCluster candidate = current;

            // Keep on working while we do have a candidate to check for merge
            CandidateLoop: 
            while (true) {
                Wrapper<Integer> deltaPos = new Wrapper<Integer>();
                Rectangle        candidateBox = candidate.getContourBox();
                candidateBox.grow(params.clusterXMargin, params.clusterYMargin);

                // Check the candidate vs all clusters until current excluded
                for (LineCluster head : clusters) {
                    if (head == current) {
                        break CandidateLoop; // Actual end of sub list
                    }

                    if ((head == candidate) || (head.getParent() != null)) {
                        continue;
                    }

                    // Check rough proximity
                    Rectangle headBox = head.getContourBox();

                    if (headBox.intersects(candidateBox)) {
                        // Try a merge
                        if (canMerge(head, candidate, deltaPos)) {
                            if (logger.isFineEnabled()) {
                                logger.fine(
                                    "Merging " + candidate + " with " + head +
                                    " delta:" + deltaPos.value);
                            }

                            // Do the merge
                            candidate.mergeWith(head, deltaPos.value);

                            break;
                        }
                    }
                }
            }
        }

        removeMergedClusters();
        removeMergedFilaments();
    }

    //------------//
    // ordinateOf //
    //------------//
    /**
     * Report the orthogonal distance of the provided point
     * to the sheet top edge tilted with global slope.
     */
    private Double ordinateOf (Point2D point)
    {
        if (point != null) {
            return sheet.getSkew()
                        .deskewed(point)
                        .getY();
        } else {
            return null;
        }
    }

    //------------//
    // ordinateOf //
    //------------//
    /**
     * Report the orthogonal distance of the cluster center
     * to the sheet top edge tilted with global slope.
     */
    private double ordinateOf (LineCluster cluster)
    {
        return ordinateOf(cluster.getCenter());
    }

    //-------------//
    // ordinatesOf //
    //-------------//
    private Double[] ordinatesOf (Collection<Point2D> points)
    {
        Double[] ys = new Double[points.size()];
        int      index = 0;

        for (Point2D p : points) {
            ys[index++] = ordinateOf(p);
        }

        return ys;
    }

    //----------------------//
    // removeMergedClusters //
    //----------------------//
    private void removeMergedClusters ()
    {
        for (Iterator<LineCluster> it = clusters.iterator(); it.hasNext();) {
            LineCluster cluster = it.next();

            if (cluster.getParent() != null) {
                it.remove();
            }
        }
    }

    //-----------------------//
    // removeMergedFilaments //
    //-----------------------//
    private void removeMergedFilaments ()
    {
        for (Iterator<LineFilament> it = filaments.iterator(); it.hasNext();) {
            LineFilament fil = it.next();

            if (fil.getPartOf() != null) {
                it.remove();
            }
        }
    }

    //------------------//
    // retrieveClusters //
    //------------------//
    /**
     * Connect filaments via the patterns they are involved in,
     * and come up with clusters of lines
     */
    private void retrieveClusters ()
    {
        // Create clusters recursively out of filements
        createClusters();

        // Aggregate filaments left over when possible
        expandClusters();

        // Merge clusters
        mergeClusters();

        // Trim clusters with too many lines
        trimClusters();

        // Discard non standard clusters
        destroyNonStandardClusters();

        // Merge clusters horizontally
        mergeClusterPairs();

        // Discard non-clustered filaments
        discardNonClusteredFilaments();

        // Debug
        if (logger.isFineEnabled()) {
            dumpClusters();
        }
    }

    //----------------------//
    // retrieveFilamentsAtX //
    //----------------------//
    /**
     * For a given abscissa, retrieve the filaments that are intersected by
     * vertical x, and sort them according to their ordinate at x
     * @param x the desired abscissa
     * @return the sorted list of structures (Fil + Y), perhaps empty
     */
    private List<FilY> retrieveFilamentsAtX (double x)
    {
        List<FilY> list = new ArrayList<FilY>();

        for (LineFilament fil : filaments) {
            if ((x >= fil.getStartPoint()
                         .getX()) &&
                (x <= fil.getStopPoint()
                         .getX())) {
                list.add(new FilY(fil, fil.getPositionAt(x)));
            }
        }

        Collections.sort(list);

        return list;
    }

    //------------------//
    // retrievePatterns //
    //------------------//
    /**
     * Detect patterns of (staff) lines.
     * Use vertical sampling on regularly-spaced abscissae
     */
    private void retrievePatterns ()
    {
        /** Minimum acceptable delta y */
        int dMin = (int) Math.floor(
            interline * (1 - constants.maxJitter.getValue()));

        /** Maximum acceptable delta y */
        int dMax = (int) Math.ceil(
            interline * (1 + constants.maxJitter.getValue()));

        /** Number of vertical samples to collect */
        int sampleCount = -1 +
                          (int) Math.rint(
            (double) pictureWidth / params.samplingDx);

        /** Exact columns abscissae */
        colX = new int[sampleCount + 1];

        /** Precise x interval */
        double samplingDx = (double) pictureWidth / (sampleCount + 1);

        for (int col = 1; col <= sampleCount; col++) {
            final List<FilamentPattern> colList = new ArrayList<FilamentPattern>();
            colPatterns.put(col, colList);

            final int x = (int) Math.rint(samplingDx * col);
            colX[col] = x;

            // Retrieve Filaments with ordinate at x, sorted by increasing y
            List<FilY>      filys = retrieveFilamentsAtX(x);

            // Second, check y deltas to detect patterns
            FilamentPattern pattern = null;
            FilY            prevFily = null;

            for (FilY fily : filys) {
                if (prevFily != null) {
                    int dy = (int) Math.rint(fily.y - prevFily.y);

                    if ((dy >= dMin) && (dy <= dMax)) {
                        if (pattern == null) {
                            // Start of a new pattern
                            pattern = new FilamentPattern(col);
                            colList.add(pattern);
                            pattern.append(prevFily.filament, prevFily.y);

                            if (prevFily.filament.isVip()) {
                                logger.info(
                                    "Created " + pattern + " with " +
                                    prevFily.filament);
                            }
                        }

                        // Extend pattern
                        pattern.append(fily.filament, fily.y);

                        if (fily.filament.isVip()) {
                            logger.info(
                                "Appended " + fily.filament + " to " + pattern);
                        }
                    } else {
                        // No pattern active
                        pattern = null;
                    }
                }

                prevFily = fily;
            }
        }
    }

    //---------------------//
    // retrievePopularSize //
    //---------------------//
    /**
     * Retrieve the most popular size (line count) among all patterns
     */
    private void retrievePopularSize ()
    {
        // Build histogram of patterns lengths
        Histogram<Integer> histo = new Histogram<Integer>();

        for (List<FilamentPattern> list : colPatterns.values()) {
            for (FilamentPattern pattern : list) {
                histo.increaseCount(pattern.getCount(), pattern.getCount());
            }
        }

        // Use the most popular length
        // Should be 4 for bass tab, 5 for standard notation, 6 for guitar tab
        popSize = histo.getMaxBucket();

        if (logger.isFineEnabled()) {
            logger.fine(
                sheet.getLogPrefix() + "Popular line pattern: " + popSize +
                " histo:" + histo.dataString());
        }
    }

    //--------------//
    // trimClusters //
    //--------------//
    private void trimClusters ()
    {
        Collections.sort(clusters, ordinateComparator);

        // Trim clusters with too many lines
        for (Iterator<LineCluster> it = clusters.iterator(); it.hasNext();) {
            LineCluster cluster = it.next();
            cluster.trim(popSize);
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

        Scale.Fraction samplingDx = new Scale.Fraction(
            1,
            "Typical delta X between two vertical samplings");

        //
        Scale.Fraction maxExpandDx = new Scale.Fraction(
            2,
            "Maximum dx to aggregate a filament to a cluster");

        //
        Scale.Fraction maxExpandDy = new Scale.Fraction(
            0.15,
            "Maximum dy to aggregate a filament to a cluster");

        //
        Scale.Fraction maxMergeDx = new Scale.Fraction(
            10,
            "Maximum dx to merge two clusters");

        //
        Scale.Fraction maxMergeDy = new Scale.Fraction(
            0.4,
            "Maximum dy to merge two clusters");

        //
        Scale.Fraction maxMergeCenterDy = new Scale.Fraction(
            1.0,
            "Maximum center dy to merge two clusters");

        //
        Scale.Fraction clusterXMargin = new Scale.Fraction(
            4,
            "Rough margin around cluster abscissa");

        //
        Scale.Fraction clusterYMargin = new Scale.Fraction(
            2,
            "Rough margin around cluster ordinate");

        //
        Constant.Ratio maxJitter = new Constant.Ratio(
            0.1,
            "Maximum gap from standard pattern dy");

        //
        Constant.Ratio minClusterLengthRatio = new Constant.Ratio(
            0.3,
            "Minimum cluster length (as ratio of median length)");
    }

    //------//
    // FilY //
    //------//
    /**
     * Class meant to define an ordering relationship between filaments, knowing
     * their ordinate at a common abscissa value.
     */
    private static class FilY
        implements Comparable<FilY>
    {
        //~ Instance fields ----------------------------------------------------

        final LineFilament filament;
        final double       y;

        //~ Constructors -------------------------------------------------------

        public FilY (LineFilament filament,
                     double       y)
        {
            this.filament = filament;
            this.y = y;
        }

        //~ Methods ------------------------------------------------------------

        public int compareTo (FilY that)
        {
            return Double.compare(this.y, that.y);
        }

        @Override
        public String toString ()
        {
            return "{F" + filament.getId() + " y:" + y + "}";
        }
    }

    //------------//
    // Parameters //
    //------------//
    /**
     * Class {@code Parameters} gathers all constants related to horizontal frames
     */
    private static class Parameters
    {
        //~ Instance fields ----------------------------------------------------

        final int samplingDx;
        final int maxExpandDx;
        final int maxExpandDy;
        final int maxMergeDx;
        final int maxMergeDy;
        final int maxMergeCenterDy;
        final int clusterXMargin;
        final int clusterYMargin;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (Scale scale)
        {
            samplingDx = scale.toPixels(constants.samplingDx);
            maxExpandDx = scale.toPixels(constants.maxExpandDx);
            maxExpandDy = scale.toPixels(constants.maxExpandDy);
            maxMergeDx = scale.toPixels(constants.maxMergeDx);
            maxMergeDy = scale.toPixels(constants.maxMergeDy);
            maxMergeCenterDy = scale.toPixels(constants.maxMergeCenterDy);
            clusterXMargin = scale.toPixels(constants.clusterXMargin);
            clusterYMargin = scale.toPixels(constants.clusterYMargin);

            if (logger.isFineEnabled()) {
                Main.dumping.dump(this);
            }
        }
    }
}
