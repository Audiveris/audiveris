//----------------------------------------------------------------------------//
//                                                                            //
//                        L i n e s R e t r i e v e r                         //
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

import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;

import omr.lag.JunctionRatioPolicy;
import omr.lag.SectionsBuilder;

import omr.log.Logger;

import omr.math.Line;
import omr.math.PointsCollector;

import omr.run.Orientation;
import static omr.run.Orientation.*;
import omr.run.Run;
import omr.run.RunsTable;
import omr.run.RunsTableFactory;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.sheet.Scale;
import omr.sheet.Sheet;

import omr.step.StepException;

import omr.stick.StickSection;

import omr.util.Predicate;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Class <code>LinesRetriever</code> computes the systems frames of a sheet
 * picture.
 *
 * @author Hervé Bitteur
 */
public class LinesRetriever
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(LinesRetriever.class);

    //~ Enumerations -----------------------------------------------------------

    // Results from testMerge()
    private enum MergeResult {
        //~ Enumeration constant initializers ----------------------------------

        GAP_WIDE,OVERLAP_FAR, OVERLAP_CLOSE,
        GAP_FAR,
        GAP_SLOPE,
        TOO_THICK,
        OK;
    }

    // Specific phase for mergeFilaments()
    private enum Phase {
        //~ Enumeration constant initializers ----------------------------------

        AGGREGATION,PURGE, LINES;
    }

    //~ Instance fields --------------------------------------------------------

    /** related sheet */
    private final Sheet sheet;

    /** Related scale */
    private final Scale scale;

    /** Scale-dependent constants for horizontal stuff */
    private final Parameters params;

    /** Lag of horizontal runs */
    private GlyphLag hLag;

    /** Long filaments found, non sorted */
    private final Set<Filament> filaments = new HashSet<Filament>();

    /** Filaments flagged as garbage */
    private Set<Filament> garbage = new HashSet<Filament>();

    /** Sections sorted by their starting coordinate (x) */
    private List<GlyphSection> sectionsStarts;

    /** Global slope of the sheet */
    private double globalSlope;

    /** Companion in charge of clusters of lines */
    private ClustersRetriever clustersRetriever;

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // LinesRetriever //
    //----------------//
    /**
     * Retrieve the frames of all staff lines
     *
     * @param sheet the sheet to process
     */
    public LinesRetriever (Sheet sheet)
    {
        this.sheet = sheet;

        scale = sheet.getScale();
        params = new Parameters(scale);
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // getGarbage //
    //------------//
    public Set<Filament> getGarbage ()
    {
        return garbage;
    }

    //----------------//
    // getGlobalSlope //
    //----------------//
    /**
     * @return the globalSlope
     */
    public double getGlobalSlope ()
    {
        return globalSlope;
    }

    //--------//
    // getLag //
    //--------//
    public GlyphLag getLag ()
    {
        return hLag;
    }

    //---------------------//
    // getMinSectionLength //
    //---------------------//
    public int getMinSectionLength ()
    {
        return params.minSectionLength;
    }

    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Compute and display the system frames of the sheet picture
     * Organize the long and thin horizontal sections into filaments (glyphs)
     * that will be good candidates for staff lines.
     */
    public void buildInfo ()
        throws StepException
    {
        // Create filaments out of long sections
        createFilaments();
        logger.info(filaments.size() + " filaments created.");

        // Merge filaments (phase 1)
        mergeFilaments(Phase.AGGREGATION);
        logger.info(filaments.size() + " filaments after merge.");

        // Merge filaments (phase 2)
        mergeFilaments(Phase.PURGE);
        logger.info(filaments.size() + " filaments after purge.");

        // Compute global slope out of longest filaments
        globalSlope = retrieveGlobalSlope();

        // Now retrieve regular patterns of filaments
        clustersRetriever = new ClustersRetriever(
            sheet,
            filaments,
            globalSlope);
        clustersRetriever.buildInfo();
        logger.info(filaments.size() + " filaments after connections.");
    }

    //----------//
    // buildLag //
    //----------//
    /**
     * Build the underlying lag, out of the provided runs table
     * @param wholeVertTable the provided runs table
     */
    public void buildLag (RunsTable wholeVertTable)
    {
        hLag = new GlyphLag("hLag", StickSection.class, Orientation.HORIZONTAL);

        // To record the purged runs (debug)
        RunsTable purgedVertTable = new RunsTable(
            "purged-vert",
            VERTICAL,
            new Dimension(sheet.getWidth(), sheet.getHeight()));

        // Remove runs whose height is much greater than line thickness
        RunsTable shortVertTable = wholeVertTable.clone("short-vert")
                                                 .purge(
            new Predicate<Run>() {
                    public final boolean check (Run run)
                    {
                        return run.getLength() > params.maxVerticalRunLength;
                    }
                },
            purgedVertTable);

        // Add a view on purged table (debug)
        sheet.getAssembly()
             .addRunsTab(purgedVertTable);

        // Add a view on runs table (debug)
        sheet.getAssembly()
             .addRunsTab(shortVertTable);

        // Build table of long horizontal runs
        RunsTable wholeHoriTable = new RunsTableFactory(
            HORIZONTAL,
            shortVertTable.getBuffer(),
            sheet.getPicture().getMaxForeground(),
            0).createTable("whole-hori");

        RunsTable longHoriTable = wholeHoriTable.clone("long-hori")
                                                .purge(
            new Predicate<Run>() {
                    public final boolean check (Run run)
                    {
                        return run.getLength() < params.minRunLength;
                    }
                });

        // Add a view on runs table (debug)
        sheet.getAssembly()
             .addRunsTab(longHoriTable);

        // Build the horizontal hLag
        SectionsBuilder sectionsBuilder = new SectionsBuilder<GlyphLag, GlyphSection>(
            hLag,
            new JunctionRatioPolicy(params.maxLengthRatio));
        sectionsBuilder.createSections(longHoriTable);

        // Purge hLag of too short sections
        hLag.purgeSections(
            new Predicate<GlyphSection>() {
                    public final boolean check (GlyphSection section)
                    {
                        return section.getLength() < params.minSectionLength;
                    }
                });
    }

    //----------------------//
    // getClustersRetriever //
    //----------------------//
    final ClustersRetriever getClustersRetriever ()
    {
        return clustersRetriever;
    }

    //--------------//
    // getFilaments //
    //--------------//
    final Set<Filament> getFilaments ()
    {
        return filaments;
    }

    //--------------//
    // isSectionFat //
    //--------------//
    /**
     * Detect if the provided section is a thick one
     * @param section the section to check
     * @return true if fat
     */
    boolean isSectionFat (GlyphSection section)
    {
        if (section.isFat() == null) {
            // Measure mean thickness on each half
            PixelRectangle box = section.getContourBox();

            // Determine where to measure thickness
            Line           line = ((StickSection) section).getLine();
            int            xLeft = box.x + (box.width / 4);
            int            yLeft = line.yAt(xLeft);
            int            xRight = box.x + ((3 * box.width) / 4);
            int            yRight = line.yAt(xRight);

            // Left side
            PixelRectangle roi = new PixelRectangle(xLeft, yLeft, 0, 0);
            roi.grow(params.probeWidth / 2, params.maxSectionThickness);

            PointsCollector collector = new PointsCollector(roi);
            section.cumulate(collector);

            int leftThickness = (int) Math.rint(
                (double) collector.getCount() / roi.width);

            // Right side
            roi.x += (xRight - xLeft);
            roi.y += (yRight - yLeft);
            collector = new PointsCollector(roi);
            section.cumulate(collector);

            int rightThickness = (int) Math.rint(
                (double) collector.getCount() / roi.width);

            section.setFat(
                (leftThickness > params.maxSectionThickness) ||
                (rightThickness > params.maxSectionThickness));
        }

        return section.isFat();
    }

    //--------------//
    // isMajorChunk //
    //--------------//
    private boolean isMajorChunk (GlyphSection section)
    {
        // Check section length & thickness
        return (section.getLength() >= params.minSectionLength) &&
               !isSectionFat(section);
    }

    //--------------//
    // getThickness //
    //--------------//
    /**
     * Compute the mean thickness in the provided roi
     * @param roi the region of interest
     * @return the mean thickness
     */
    private int getThickness (Rectangle roi)
    {
        int             xMax = (roi.x + roi.width) - 1;

        // Collect data from intersected sections
        PointsCollector collector = new PointsCollector(roi);

        for (GlyphSection sec : sectionsStarts) {
            PixelRectangle box = sec.getContourBox();

            if (box.x > xMax) {
                break;
            }

            if (box.intersects(roi)) {
                sec.cumulate(collector);
            }
        }

        // Mean total thickness
        return (int) Math.rint((double) collector.getCount() / roi.width);
    }

    //-----------------//
    // createFilaments //
    //-----------------//
    /**
     *   Aggregate long sections into initial filaments
     */
    private void createFilaments ()
    {
        // Build a collection of sections to ease their retrieval by ROI
        sectionsStarts = new ArrayList<GlyphSection>(hLag.getSections());
        Collections.sort(sectionsStarts, GlyphSection.startComparator);

        // Sort sections by decreasing length
        List<GlyphSection> sections = new ArrayList<GlyphSection>(
            hLag.getSections());
        Collections.sort(sections, GlyphSection.reverseLengthComparator);

        for (GlyphSection section : sections) {
            // Limit to main sections for the time being
            if (isMajorChunk(section)) {
                // Create a filament with this section
                Filament filament = new Filament(scale);
                filament.include(section);
                filament = (Filament) hLag.addGlyph(filament);
                filaments.add(filament);

                if (logger.isFineEnabled()) {
                    logger.fine("Created " + filament + " with " + section);
                }
            }
        }
    }

    //----------------//
    // mergeFilaments //
    //----------------//
    private void mergeFilaments (Phase phase)
    {
        // List of filaments, sorted by decreasing length
        List<Filament> list = new ArrayList<Filament>(filaments);
        Collections.sort(list, Filament.reverseLengthComparator);

        // Browse by decreasing filament length
        for (Filament current : list) {
            ///logger.info("Current: " + current + " length:" + current.getLength());
            Filament candidate = current;

            // Keep on working while we do have a candidate to check for merge
            while (candidate != null) {
                ///logger.info("Candidate: " + candidate);
                // Check the candidate vs all filaments until current excluded
                HeadsLoop: 
                for (Filament head : list) {
                    ///logger.info("Head: " + head);
                    if (head == current) { // Actual end of sub list
                        candidate = null;

                        break HeadsLoop;
                    }

                    if ((head == candidate) || (head.getParent() != null)) {
                        continue HeadsLoop;
                    }

                    // Check for a possible merge
                    switch (testMerge(head, candidate, phase)) {
                    case OK : {
                        // We merge the shorter into the longer
                        if (candidate.getLength() > head.getLength()) {
                            Filament tempo = head;
                            head = candidate;
                            candidate = tempo;
                        }

                        if (logger.isFineEnabled()) {
                            logger.info(
                                "Merged " + candidate + " into " + head);
                        }

                        head.include(candidate);
                        candidate = head; // This is a new candidate

                        break HeadsLoop;
                    }

                    //                    case OVERLAP_CLOSE : {
                    //                        // We burn the shorter
                    //                        if (candidate.getLength() > head.getLength()) {
                    //                            Filament tempo = head;
                    //                            head = candidate;
                    //                            candidate = tempo;
                    //                        }
                    //
                    //                        //if (logger.isFineEnabled()) {
                    //                            logger.info("Burnt " + candidate + " near " + head);
                    //                        //}
                    //
                    //                        ///candidate.setDiscarded(true);
                    //                        garbage.add(candidate);
                    //                    }
                    default :
                    }
                }
            }
        }

        //        filaments.removeAll(garbage);

        // Discard the merged filaments
        for (Iterator<Filament> it = filaments.iterator(); it.hasNext();) {
            Filament f = it.next();

            if (f.getParent() != null) {
                it.remove();
            }
        }

        if (logger.isFineEnabled()) {
            for (Filament f : filaments) {
                logger.info("Kept " + f);
            }
        }
    }

    //---------------------//
    // retrieveGlobalSlope //
    //---------------------//
    private double retrieveGlobalSlope ()
    {
        // List of filaments, sorted by decreasing length
        List<Filament> list = new ArrayList<Filament>(filaments);
        Collections.sort(list, Filament.reverseLengthComparator);

        // Use the top filaments to determine slope
        double ratio = params.topRatioForSlope;
        int    topCount = Math.max(1, (int) Math.rint(list.size() * ratio));
        double slopes = 0;

        for (int i = 0; i < topCount; i++) {
            Filament fil = list.get(i);
            Point    start = fil.getStartPoint();
            Point    stop = fil.getStopPoint();
            slopes += ((double) (stop.y - start.y) / (stop.x - start.x));
        }

        return slopes / topCount;
    }

    //-----------//
    // testMerge //
    //-----------//
    /**
     * Check whether the two provided filaments could be merged
     * @param one a filament
     * @param two another filament
     * @return the detailed test result
     */
    private MergeResult testMerge (Filament one,
                                   Filament two,
                                   Phase    phase)
    {
        if (logger.isFineEnabled()) {
            logger.info("testMerge " + one + " & " + two);
        }

        // Left & right points for each filament
        PixelPoint oneLeft = one.getStartPoint();
        PixelPoint oneRight = one.getStopPoint();
        PixelPoint twoLeft = two.getStartPoint();
        PixelPoint twoRight = two.getStopPoint();

        // x gap?
        int overlapLeft = Math.max(oneLeft.x, twoLeft.x);
        int overlapRight = Math.min(oneRight.x, twoRight.x);
        int gapDx = (overlapLeft - overlapRight) - 1;

        if (gapDx > params.maxGapDx) {
            if (logger.isFineEnabled()) {
                logger.info(
                    "Gap too wide: " + gapDx + " vs " + params.maxGapDx);
            }

            return MergeResult.GAP_WIDE;
        }

        // y gap?
        if (gapDx < 0) {
            // Overlap between the two filaments
            // Measure dy at middle of overlap
            int    xMid = (overlapLeft + overlapRight) / 2;
            double oneY = one.getCurve()
                             .yAt(xMid);
            double twoY = two.getCurve()
                             .yAt(xMid);
            int    dy = Math.abs((int) Math.rint(oneY - twoY));

            if (dy > params.maxOverlapDy) {
                if ((phase == Phase.PURGE) && (dy < params.maxCloseDy)) {
                    if (logger.isFineEnabled()) {
                        logger.info("Close parallel: " + dy);
                    }

                    return MergeResult.OVERLAP_CLOSE;
                } else {
                    if (logger.isFineEnabled()) {
                        logger.info(
                            "Dy too high: " + dy + " vs " +
                            params.maxOverlapDy);
                    }

                    return MergeResult.OVERLAP_FAR;
                }
            }

            if (phase == Phase.PURGE) {
                // Here, we are closely parallel but still not merged
                if (logger.isFineEnabled()) {
                    logger.info("Close parallel not merged");
                }

                return MergeResult.OVERLAP_CLOSE;
            } else {
                // Check resulting thickness at middle of overlap
                // Only when at least one short filament is involved
                int shortLength = Math.min(one.getLength(), two.getLength());

                if ((phase != Phase.PURGE) &&
                    (shortLength <= params.maxInvolvingLength)) {
                    int            yMid = (int) Math.rint((oneY + twoY) / 2);
                    PixelRectangle roi = new PixelRectangle(xMid, yMid, 0, 0);
                    roi.grow(
                        params.probeWidth / 2,
                        params.maxFilamentThickness);

                    int thickness = getThickness(roi);

                    if (thickness > params.maxFilamentThickness) {
                        if (logger.isFineEnabled()) {
                            logger.info(
                                "Too thick: " + thickness + " vs " +
                                params.maxFilamentThickness + " " + one + " " +
                                two);
                        }

                        return MergeResult.TOO_THICK;
                    }
                }
            }
        } else {
            // No overlap, it's a true gap
            PixelPoint left;
            PixelPoint right;

            if (oneLeft.x < twoLeft.x) {
                // one - two
                left = oneRight;
                right = twoLeft;
            } else {
                // two - one
                left = twoRight;
                right = oneLeft;
            }

            int gapDy = Math.abs(right.y - left.y);

            if (gapDy > params.maxGapDy) {
                if (logger.isFineEnabled()) {
                    logger.info(
                        "Dy too high for gap: " + gapDy + " vs " +
                        params.maxGapDy);
                }

                return MergeResult.GAP_FAR;
            }

            // Check slope (relevant only for significant dy)
            if (gapDy > params.maxGapDyForSlope) {
                double gapSlope = (double) gapDy / gapDx;

                if (gapSlope > params.maxGapSlope) {
                    if (logger.isFineEnabled()) {
                        logger.info(
                            "Slope too high for gap: " + (float) gapSlope +
                            " vs " + params.maxGapSlope);
                    }

                    return MergeResult.GAP_SLOPE;
                }
            }
        }

        if (logger.isFineEnabled()) {
            logger.info("Compatible!");
        }

        return MergeResult.OK;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Ratio     maxLengthRatio = new Constant.Ratio(
            1.5,
            "Maximum ratio in length for a run to be combined with an existing section");
        Constant.Double    maxGapSlope = new Constant.Double(
            "tangent",
            0.5,
            "Maximum absolute slope for a gap");

        // Constants specified WRT mean line thickness
        // -------------------------------------------
        Scale.LineFraction maxVerticalRunLength = new Scale.LineFraction(
            1.75, // 2.0
            "Maximum vertical run length WRT mean line height");
        Scale.LineFraction maxSectionThickness = new Scale.LineFraction(
            1.5,
            "Maximum horizontal section thickness WRT mean line height");
        Scale.LineFraction maxFilamentThickness = new Scale.LineFraction(
            1.5,
            "Maximum filament thickness WRT mean line height");

        // Constants specified WRT mean interline
        // --------------------------------------
        Scale.Fraction minRunLength = new Scale.Fraction(
            1.0,
            "Minimum length for a horizontal run to be considered");
        Scale.Fraction minSectionLength = new Scale.Fraction(
            1,
            "Minimum length for a horizontal section to be considered in frames computation");
        Scale.Fraction maxOverlapDy = new Scale.Fraction(
            0.2,
            "Maximum delta Y between two overlapping filaments");
        Scale.Fraction maxGapDx = new Scale.Fraction(
            1,
            "Maximum delta X for a gap between filaments");
        Scale.Fraction maxGapDy = new Scale.Fraction(
            0.2,
            "Maximum delta Y for a gap between filaments");
        Scale.Fraction maxGapDyForSlope = new Scale.Fraction(
            0.1,
            "Maximum delta Y to check slope for a gap between filaments");
        Scale.Fraction maxCloseDy = new Scale.Fraction(
            0.5,
            "Maximum delta Y to burn a candidate");
        Scale.Fraction maxInvolvingLength = new Scale.Fraction(
            6,
            "Maximum filament length to apply thickness test");
        Constant.Ratio topRatioForSlope = new Constant.Ratio(
            0.1,
            "Percentage of top filaments used to retrieve global slope");
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

        /** Probe width */
        final int probeWidth;

        /** Minimum run length for horizontal lag */
        final int minRunLength;

        /** Maximum vertical run length (to exclude too long vertical runs) */
        final int maxVerticalRunLength;

        /** Used for section junction policy */
        final double maxLengthRatio;

        /** Maximum acceptable thickness for horizontal sections */
        final int maxSectionThickness;

        /** Maximum acceptable thickness for horizontal filaments */
        final int maxFilamentThickness;

        /** Minimum acceptable length for horizontal sections */
        final int minSectionLength;

        /** Maximum acceptable delta Y */
        final int maxOverlapDy;

        /** Maximum width for real gap */
        final int maxGapDx;

        /** Maximum dy for real gaps */
        final int maxGapDy;

        /** Maximum dy for slope check on real gap */
        final int maxGapDyForSlope;

        /** Maximum slope for real gaps */
        final double maxGapSlope;

        /** Maximum dy to "burn" a candidate */
        final int maxCloseDy;

        /** Maximum length to apply thickness test */
        final int maxInvolvingLength;

        /** Percentage of top filaments used to retrieve global slope */
        final double topRatioForSlope;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (Scale scale)
        {
            minRunLength = scale.toPixels(constants.minRunLength);
            maxVerticalRunLength = scale.toPixels(
                constants.maxVerticalRunLength);
            maxLengthRatio = constants.maxLengthRatio.getValue();
            minSectionLength = scale.toPixels(constants.minSectionLength);
            maxSectionThickness = scale.toPixels(constants.maxSectionThickness);
            maxFilamentThickness = scale.toPixels(
                constants.maxFilamentThickness);
            maxOverlapDy = scale.toPixels(constants.maxOverlapDy);
            maxGapDx = scale.toPixels(constants.maxGapDx);
            maxGapDy = scale.toPixels(constants.maxGapDy);
            maxGapDyForSlope = scale.toPixels(constants.maxGapDyForSlope);
            maxGapSlope = constants.maxGapSlope.getValue();
            maxCloseDy = scale.toPixels(constants.maxCloseDy);
            maxInvolvingLength = scale.toPixels(constants.maxInvolvingLength);
            probeWidth = scale.toPixels(Filament.getProbeWidth());
            topRatioForSlope = constants.topRatioForSlope.getValue();

            if (logger.isFineEnabled()) {
                Main.dumping.dump(this);
            }
        }
    }
}
