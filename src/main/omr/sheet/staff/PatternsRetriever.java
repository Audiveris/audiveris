//----------------------------------------------------------------------------//
//                                                                            //
//                     P a t t e r n s R e t r i e v e r                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.staff;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.math.Histogram;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.ui.PagePainter;

import omr.sheet.Scale;
import omr.sheet.Sheet;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Class {@code PatternsRetriever} performs vertical samplings of the filaments
 * in order to detect regular patterns of a preferred interline value
 *
 * @author Hervé Bitteur
 */
public class PatternsRetriever
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        PatternsRetriever.class);

    /** Stroke for drawing patterns */
    private static final Stroke patternStroke = new BasicStroke(2f);

    //~ Instance fields --------------------------------------------------------

    /** Related sheet */
    private final Sheet sheet;

    /** Related scale */
    private final Scale scale;

    /** Long filaments found, sorted by Y then x */
    private final Set<Filament> filaments;

    /** Global slope of the sheet */
    private double globalSlope;

    /** A map (colIndex -> vertical list of samples), sorted on colIndex */
    private Map<Integer, List<FilamentPattern>> colPatterns;

    /**
     * The popular length of patterns detected for the specified interline
     * (typically: 4, 5 or 6)
     */
    private int popLength;

    /** X values per column index */
    private int[] colX;

    /** collection of clusters */
    private final Set<LineCluster> clusters = new LinkedHashSet<LineCluster>();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new PatternsRetriever object.
     *
     * @param sheet the sheet to process
     * @param filaments the current collection of filaments
     * @param globalSlope the global scope detected for the sheet
     */
    public PatternsRetriever (Sheet         sheet,
                              Set<Filament> filaments,
                              double        globalSlope)
    {
        this.sheet = sheet;
        this.filaments = filaments;
        this.globalSlope = globalSlope;

        scale = sheet.getScale();
        colPatterns = new TreeMap<Integer, List<FilamentPattern>>();
    }

    //~ Methods ----------------------------------------------------------------

    //------------------//
    // getPopularLength //
    //------------------//
    /**
     * @return the popular Length
     */
    public int getPopularLength ()
    {
        return popLength;
    }

    //------------------//
    // connectFilaments //
    //------------------//
    /**
     * Link the filaments horizontally, despite the global slope
     */
    public void connectFilaments ()
    {
        logger.info("connectFilaments start");

        // Register each pattern to its participating filaments
        linkPatternsToFilaments();

        // Retrieve standard position, if any, for each filament
        retrieveStandardPositions();

        // Connect along filaments with standard positions
        followStandardPositions();
        purgeFilaments();

        // Connect aligned filaments
        followTangents();

        // Aggregate in staves
        retrieveClusters();
        purgeFilaments();

        // Remove non-clustered filaments
        for (Iterator<Filament> it = filaments.iterator(); it.hasNext();) {
            Filament fil = it.next();

            if (fil.getCluster() == null) {
                it.remove();
            }
        }

        sheet.setClusters(clusters);

        logger.info("connectFilaments stop");
    }

    //------------------//
    // retrievePatterns //
    //------------------//
    /**
     * Detect patterns of (staff) lines.
     * Use vertical sampling on regularly-spaced abscissae
     */
    public void retrievePatterns (int pictureWidth)
    {
        /** Minimum acceptable delta y */
        int dMin = (int) Math.floor(scale.interline() * 0.85);

        /** Maximum acceptable delta y */
        int dMax = (int) Math.ceil(scale.interline() * 1.15);

        /** Number of vertical samples to collect */
        int sampleCount = -1 +
                          (int) Math.rint(
            (double) pictureWidth / scale.toPixels(constants.samplingDx));

        /** Exact columns abscissae */
        colX = new int[sampleCount + 1];

        /** Precise x interval */
        double samplingDx = (double) pictureWidth / (sampleCount + 1);

        for (int col = 1; col <= sampleCount; col++) {
            List<FilamentPattern> colList = new ArrayList<FilamentPattern>();
            colPatterns.put(col, colList);

            int x = (int) Math.rint(samplingDx * col);
            colX[col] = x;

            // First, retrieve Filaments and their ordinate at x
            // And sort them by increasing y
            SortedSet<FilYPair> pairs = retrieveFilamentsAtX(x);

            // Second, retrieve deltas
            FilamentPattern pattern = null;
            FilYPair        prevPair = null;

            for (FilYPair pair : pairs) {
                if (prevPair != null) {
                    int dy = (int) Math.rint(pair.y - prevPair.y);

                    if ((dy >= dMin) && (dy <= dMax)) {
                        if (pattern == null) {
                            pattern = new FilamentPattern(col);
                            pattern.append(prevPair.filament, prevPair.y);
                        }

                        pattern.append(pair.filament, pair.y);
                    } else if (pattern != null) {
                        colList.add(pattern);
                        pattern = null;
                    }
                }

                prevPair = pair;
            }

            // Finish on-going pattern, if any
            if (pattern != null) {
                colList.add(pattern);
                pattern = null;
            }
        }

        analyzePatternsPopulation();
    }

    //-------------//
    // renderItems //
    //-------------//
    void renderItems (Graphics2D g)
    {
        Stroke oldStroke = g.getStroke();
        g.setStroke(patternStroke);
        g.setColor(PagePainter.musicColor);

        for (Entry<Integer, List<FilamentPattern>> entry : colPatterns.entrySet()) {
            int col = entry.getKey();
            int x = colX[col];

            for (FilamentPattern pattern : entry.getValue()) {
                if (pattern.getCount() == popLength) {
                    g.drawLine(
                        x,
                        pattern.getY(0),
                        x,
                        pattern.getY(pattern.getCount() - 1));
                }
            }
        }

        g.setStroke(oldStroke);
    }

    //---------------------//
    // aggregateToClusters //
    //---------------------//
    private void aggregateToClusters ()
    {
        purgeFilaments();

        int maxClusterDy = scale.toPixels(constants.maxClusterDy);

        for (Filament fil : filaments) {
            if (fil.getCluster() == null) {
                PixelRectangle box = fil.getContourBox();
                box.grow(0, maxClusterDy);

                PixelPoint start = fil.getStartPoint();
                PixelPoint stop = fil.getStopPoint();
                PixelPoint middle = new PixelPoint(
                    (start.x + stop.x) / 2,
                    (start.y + stop.y) / 2);

                for (Filament f : filaments) {
                    if ((f.getCluster() != null) &&
                        box.intersects(f.getContourBox())) {
                        // Check precise distance
                        double dy = f.getCurve()
                                     .yAt(middle.x) - middle.y;

                        if (Math.abs(dy) <= maxClusterDy) {
                            if (logger.isFineEnabled()) {
                                logger.fine("Aggregate " + fil + " to " + f);
                            }

                            f.include(fil);

                            break;
                        }
                    }
                }
            }
        }

        // Remove too small clusters
        for (Iterator<LineCluster> it = clusters.iterator(); it.hasNext();) {
            LineCluster cluster = it.next();

            if (cluster.getSize() < popLength) {
                it.remove();
            }
        }
    }

    //---------------------------//
    // analyzePatternsPopulation //
    //---------------------------//
    private void analyzePatternsPopulation ()
    {
        // Dump results
        for (Entry<Integer, List<FilamentPattern>> entry : colPatterns.entrySet()) {
            int col = entry.getKey();

            ///logger.info("col:" + col + " x:" + colX[col]);
            for (FilamentPattern pattern : entry.getValue()) {
                ///logger.info("  " + pattern);
            }
        }

        // Build histogram of patterns lengths
        Histogram<Integer> histo = new Histogram();

        for (List<FilamentPattern> list : colPatterns.values()) {
            for (FilamentPattern pattern : list) {
                histo.increaseCount(pattern.getCount(), pattern.getCount());
            }
        }

        histo.print(System.out);

        // Use the most popular length
        // Should be 4 for bass tab, 5 for standard notation, 6 for guitar tab
        popLength = histo.getMaxBucket();
        logger.info("Most popular pattern: " + popLength);
    }

    //------------------//
    // connectAncestors //
    //------------------//
    private void connectAncestors (Filament one,
                                   Filament two)
    {
        Filament oneAnc = one.getAncestor();
        Filament twoAnc = two.getAncestor();

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

    //-------------------------//
    // followStandardPositions //
    //-------------------------//
    private void followStandardPositions ()
    {
        // Process fil by fil, by decreasing length
        logger.info("Connecting filaments");

        List<Filament> filList = new ArrayList<Filament>(filaments);
        Collections.sort(filList, Filament.reverseLengthComparator);

        for (Filament fil : filList) {
            Integer stdPos = fil.getLinePosition();

            if (stdPos == null) {
                continue;
            }

            ///logger.info("Processing " + fil);
            final int       nbUp = stdPos;
            final int       nbDown = popLength - stdPos - 1;

            // Line -> Fil
            Filament[]      lines = new Filament[popLength];
            FilamentPattern prevPattern = null;

            for (Entry<Integer, FilamentPattern> ent : fil.getPatterns()
                                                          .entrySet()) {
                int             col = ent.getKey();
                FilamentPattern pattern = ent.getValue();

                ///logger.info("col:" + col + " " + pattern);
                int posPivot = pattern.getIndex(fil);

                //                // Check that pop-length patterns are consistent
                //                if ((prevPattern != null) &&
                //                    (prevPattern.getCount() == popLength) &&
                //                    (pattern.getCount() == popLength)) {
                //                    if (posPivot != stdPos) {
                //                        logger.warning(
                //                            "Col:" + col + " x:" + colX[col] + " Suspicious " +
                //                            pattern + " around F#" + fil.getId());
                //                    }
                //                }

                // Going up for nbUp
                for (int i = 1; i <= nbUp; i++) {
                    int line = stdPos - i;
                    int pos = posPivot - i;

                    if (pos < 0) {
                        break;
                    }

                    if (lines[line] != null) {
                        connectAncestors(lines[line], pattern.getFilament(pos));
                    } else {
                        lines[line] = pattern.getFilament(pos);
                    }
                }

                // Going down for nbDown
                for (int i = 1; i <= nbDown; i++) {
                    int line = stdPos + i;
                    int pos = posPivot + i;

                    if (pos >= pattern.getCount()) {
                        break;
                    }

                    if (lines[line] != null) {
                        connectAncestors(lines[line], pattern.getFilament(pos));
                    } else {
                        lines[line] = pattern.getFilament(pos);
                    }
                }

                prevPattern = pattern;
            }
        }
    }

    //----------------//
    // followTangents //
    //----------------//
    /**
     * Focus on solid filaments (for example, those with a standard position).
     * Extrapolate using tangents at both ends to test connection with others
     */
    private void followTangents ()
    {
        List<Filament> startFils = new ArrayList<Filament>(filaments);
        Collections.sort(startFils, Filament.startComparator);

        List<Filament> stopFils = new ArrayList<Filament>(filaments);
        Collections.sort(stopFils, Filament.stopComparator);

        List<Filament> filList = new ArrayList<Filament>(filaments);
        Collections.sort(filList, Filament.reverseLengthComparator);

        int lookupDx = scale.toPixels(constants.lookupDx);
        int lookupDy = scale.toPixels(constants.lookupDy);

        for (Filament fil : filList) {
            if ((fil.getParent() != null) || (fil.getLinePosition() == null)) {
                continue;
            }

            ///logger.info("From " + fil);

            // Connect to the right
            boolean progressing = true;

            while (progressing) {
                progressing = false;

                PixelPoint     end = fil.getStopPoint();
                double         endDer = fil.getCurve()
                                           .derivativeAt(end.x);
                PixelPoint     extra = new PixelPoint(
                    end.x + lookupDx,
                    end.y + (int) Math.rint(lookupDx * endDer));

                PixelRectangle box = new PixelRectangle(end.x, end.y, 0, 0);
                box.add(extra);
                box.grow(0, lookupDy);

                Filament bestCandidate = null;
                double   bestDist = Double.MAX_VALUE;

                for (Filament f : startFils) {
                    PixelPoint other = f.getStartPoint();

                    if (other.y < box.y) {
                        continue;
                    }

                    if (other.y > (box.y + box.height)) {
                        break;
                    }

                    if ((other.x < box.x) || (other.x > (box.x + box.width))) {
                        continue;
                    }

                    // We have a candidate, check vertical distance
                    // Measure dy at start and stop abscissae
                    int    dx = other.x - end.x;
                    double otherDy = other.y - (end.y + (endDer * dx));
                    double tg = otherDy / dx;
                    double dtg = Math.abs(tg - globalSlope);

                    if (dtg <= constants.tangentMargin.getValue()) {
                        ///logger.warning((float) dtg + " until " + f);
                        int dist = Math.abs(dx);

                        if (bestCandidate == null) {
                            bestCandidate = f;
                            bestDist = dist;
                        } else if (dist < bestDist) {
                            bestDist = dist;
                            bestCandidate = f;
                        }
                    } else {
                        ///logger.info((float) dtg + " until " + f);
                    }
                }

                // Take the best suitable candidate, if any
                if (bestCandidate != null) {
                    if (bestCandidate.getLength() > fil.getLength()) {
                        Filament tempo = fil;
                        fil = bestCandidate;
                        bestCandidate = tempo;
                    }

                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "Right extending " + fil + " with " +
                            bestCandidate);
                    }

                    fil.include(bestCandidate);
                    progressing = true;
                }
            }

            // Connect to the left
            progressing = true;

            while (progressing) {
                progressing = false;

                PixelPoint     end = fil.getStartPoint();
                double         endDer = fil.getCurve()
                                           .derivativeAt(end.x);
                PixelPoint     extra = new PixelPoint(
                    end.x - lookupDx,
                    end.y - (int) Math.rint(lookupDx * endDer));

                PixelRectangle box = new PixelRectangle(end.x, end.y, 0, 0);
                box.add(extra);
                box.grow(0, lookupDy);

                Filament bestCandidate = null;
                double   bestDist = Double.MAX_VALUE;

                for (Filament f : startFils) {
                    PixelPoint other = f.getStopPoint();

                    if (other.y < box.y) {
                        continue;
                    }

                    if (other.y > (box.y + box.height)) {
                        break;
                    }

                    if ((other.x < box.x) || (other.x > (box.x + box.width))) {
                        continue;
                    }

                    // We have a candidate, check vertical distance
                    // Measure dy at start and stop abscissae
                    int    dx = other.x - end.x;
                    double otherDy = other.y - (end.y + (endDer * dx));
                    double tg = otherDy / dx;
                    double dtg = Math.abs(tg - globalSlope);

                    if (dtg <= constants.tangentMargin.getValue()) {
                        ///logger.warning((float) dtg + " until " + f);
                        int dist = Math.abs(dx);

                        if (bestCandidate == null) {
                            bestCandidate = f;
                            bestDist = dist;
                        } else {
                            if (dist < bestDist) {
                                bestDist = dist;
                                bestCandidate = f;
                            }
                        }
                    } else {
                        ///logger.info((float) dtg + " until " + f);
                    }
                }

                // Take the best suitable candidate, if any
                if (bestCandidate != null) {
                    if (bestCandidate.getLength() > fil.getLength()) {
                        Filament tempo = fil;
                        fil = bestCandidate;
                        bestCandidate = tempo;
                    }

                    ///logger.warning("Left extending " + fil + " with " + bestCandidate);
                    fil.include(bestCandidate);
                    progressing = true;
                }
            }
        }
    }

    //-------------------------//
    // linkPatternsToFilaments //
    //-------------------------//
    private void linkPatternsToFilaments ()
    {
        // Assign patterns to their filaments
        logger.info("Building fil->patterns");

        // Loop on column index
        for (Entry<Integer, List<FilamentPattern>> entry : colPatterns.entrySet()) {
            int                   colIndex = entry.getKey();
            List<FilamentPattern> patterns = entry.getValue();

            for (FilamentPattern pattern : patterns) {
                for (Filament fil : pattern.getFilaments()) {
                    fil.addPattern(colIndex, pattern);
                }
            }
        }
    }

    //---------------//
    // purgeClusters //
    //---------------//
    private void purgeClusters ()
    {
        for (Iterator<LineCluster> it = clusters.iterator(); it.hasNext();) {
            LineCluster cluster = it.next();

            if ((cluster.getParent() != null) || (cluster.getSize() == 0)) {
                it.remove();
            }
        }
    }

    //----------------//
    // purgeFilaments //
    //----------------//
    private void purgeFilaments ()
    {
        // Remove merged filaments
        for (Iterator<Filament> it = filaments.iterator(); it.hasNext();) {
            Filament fil = it.next();

            if (fil.getParent() != null) {
                it.remove();
            }
        }
    }

    //------------------//
    // retrieveClusters //
    //------------------//
    /**
     * Aggregate filaments into line clusters
     */
    private void retrieveClusters ()
    {
        List<Filament> filList = new ArrayList<Filament>(filaments);
        Collections.sort(filList, Filament.reverseLengthComparator);

        for (Filament fil : filList) {
            if ((fil.getParent() == null) &&
                (fil.getCluster() == null) &&
                !fil.getPatterns()
                    .isEmpty()) {
                LineCluster cluster = new LineCluster(fil);
                clusters.add(cluster);
            }
        }

        purgeClusters();

        // Aggregate line parts
        for (LineCluster cluster : clusters) {
            cluster.aggregateLines();
        }

        // Aggregate filament left over, if possible
        aggregateToClusters();

        logger.info("\nClusters:");

        for (LineCluster cluster : clusters) {
            cluster.cleanup(popLength);
            logger.info(cluster.toString());
        }
    }

    //----------------------//
    // retrieveFilamentsAtX //
    //----------------------//
    /**
     * For a given abscissa, retrieve the filaments that are intersected by
     * vertical x, and sort them according to their ordinate at x
     * @param x the dsired abscissa
     * @return the sorted list of pairs (Fil + Y), perhaps empty
     */
    private SortedSet<FilYPair> retrieveFilamentsAtX (int x)
    {
        SortedSet<FilYPair> set = new TreeSet<FilYPair>();

        for (Filament fil : filaments) {
            if ((x >= fil.getStartPoint().x) && (x <= fil.getStopPoint().x)) {
                int y = (int) Math.rint(fil.getCurve().yAt(x));
                set.add(new FilYPair(fil, y));
            }
        }

        return set;
    }

    //-------------------//
    // retrieveOrdinates //
    //-------------------//
    /**
     * Classify the filaments using the orthogonal distance of their mid-point
     * to the sheet top edge tilted with global slope.
     *
     * Staves should stand out, their lines exhibiting similar distances
     */
    private void retrieveOrdinates ()
    {
        // Build this reference line
        double          angleRad = Math.atan(globalSlope);
        AffineTransform at = AffineTransform.getRotateInstance(angleRad);
        Point2D         right = new Point(sheet.getWidth(), 0);
        right = at.transform(right, right);

        Line2D.Double top = new Line2D.Double(0, 0, right.getX(), right.getY());

        for (Filament fil : filaments) {
            PixelPoint start = fil.getStartPoint();
            PixelPoint stop = fil.getStopPoint();
            Point2D    center = new Point(
                (start.x + stop.x) / 2,
                (start.y + stop.y) / 2);
            double     dist = top.ptLineDist(center);
            fil.setTopDistance((int) Math.rint(dist));
        }

        // Sort filaments according to their distance to top, then abscissa
        List<Filament> fils = new ArrayList<Filament>(filaments);
        Collections.sort(fils, Filament.distanceComparator);

        for (Filament fil : fils) {
            logger.info("trueLength:" + fil.trueLength() + " " + fil);
        }
    }

    //---------------------------//
    // retrieveStandardPositions //
    //---------------------------//
    private void retrieveStandardPositions ()
    {
        // Retrieve standard position, if any, for each filament
        logger.info("Retrieving standard positions");

        for (Filament fil : filaments) {
            Map<Integer, FilamentPattern> patterns = fil.getPatterns();

            ///logger.info("Processing " + fil);

            // Determine its most frequent position in patterns (of popLength)
            // Loop on patterns for this fil
            if (!patterns.isEmpty()) {
                Histogram<Integer> histo = new Histogram<Integer>();

                for (Entry<Integer, FilamentPattern> entry : patterns.entrySet()) {
                    FilamentPattern pattern = entry.getValue();

                    if (pattern.getCount() == popLength) {
                        histo.increaseCount(pattern.getIndex(fil), 1);
                    }
                }

                Integer bucket = histo.getMaxBucket();

                if (bucket != null) {
                    fil.setLinePosition(bucket);
                }

                ///logger.info(fil.toString());
            }
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

        Scale.Fraction  samplingDx = new Scale.Fraction(
            1, // 4
            "Typical delta X between two vertical samplings");
        Scale.Fraction  lookupDx = new Scale.Fraction(
            8,
            "Window width when looking for connections");
        Scale.Fraction  lookupDy = new Scale.Fraction(
            1,
            "Window height when looking for connections");
        Scale.Fraction  maxClusterDy = new Scale.Fraction(
            0.25,
            "Maximum dy to aggregate a filament to a cluster");
        Constant.Double tangentMargin = new Constant.Double(
            "tangent",
            0.08,
            "Maximum slope of gap between filaments");
    }

    //----------//
    // FilYPair //
    //----------//
    /**
     * Class meant to define an ordering relationship between filaments, knowing
     * their ordinate at a common abscissa value.
     */
    private static class FilYPair
        implements Comparable<FilYPair>
    {
        //~ Instance fields ----------------------------------------------------

        final Filament filament;
        final int      y;

        //~ Constructors -------------------------------------------------------

        public FilYPair (Filament filament,
                         int      y)
        {
            this.filament = filament;
            this.y = y;
        }

        //~ Methods ------------------------------------------------------------

        public int compareTo (FilYPair that)
        {
            return Integer.signum(y - that.y);
        }
    }
}
