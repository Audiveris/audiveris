//----------------------------------------------------------------------------//
//                                                                            //
//                         F r a m e s B u i l d e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.staff;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.GlyphsModel;
import omr.glyph.ui.GlyphBoard;
import omr.glyph.ui.GlyphLagView;
import omr.glyph.ui.GlyphsController;
import omr.glyph.ui.ViewParameters;

import omr.lag.JunctionRatioPolicy;
import omr.lag.SectionsBuilder;
import omr.lag.ui.RunBoard;
import omr.lag.ui.ScrollLagView;
import omr.lag.ui.SectionBoard;
import omr.lag.ui.SectionView;

import omr.log.Logger;

import omr.math.Line;
import omr.math.NaturalSpline;
import omr.math.PointsCollector;

import omr.run.Orientation;
import static omr.run.Orientation.*;
import omr.run.Run;
import omr.run.RunsTable;
import omr.run.RunsTableFactory;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.ui.PagePainter;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.picture.Picture;
import omr.sheet.ui.PixelBoard;

import omr.step.Step;
import omr.step.StepException;
import omr.step.Steps;

import omr.stick.StickSection;

import omr.ui.BoardsPane;
import omr.ui.util.UIUtilities;

import omr.util.Predicate;
import omr.util.StopWatch;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.*;
import java.util.List;

/**
 * Class <code>FramesBuilder</code> computes the systems frames of a sheet
 * picture.
 *
 * @author Hervé Bitteur
 */
public class FramesBuilder
    extends GlyphsModel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(FramesBuilder.class);

    /** Stroke for drawing filaments curves */
    private static final Stroke splineStroke = new BasicStroke(
        (float) constants.splineThickness.getValue(),
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND);

    //~ Enumerations -----------------------------------------------------------

    // Results from testMerge()
    private enum MergeResult {
        //~ Enumeration constant initializers ----------------------------------

        GAP_WIDE,OVERLAP_FAR, OVERLAP_CLOSE,
        GAP_FAR,
        TOO_THICK,
        OK;
    }

    // Specific phase for mergeFilaments()
    private enum Phase {
        //~ Enumeration constant initializers ----------------------------------

        AGGREGATION,PURGE, LINES;
    }

    //~ Instance fields --------------------------------------------------------

    /** Related picture */
    final Picture picture;

    /** Related scale */
    private final Scale scale;

    /** Lag of vertical runs */
    private GlyphLag vLag;

    /** Probe width */
    private final int probeWidth;

    /** Minimum run length */
    private final int minRunLength;

    /** Minimum run length for vertical lag */
    private final int minVerticalRunLength;

    /** Maximum run length  (for vertical runs) */
    private final int maxRunLength;

    /** Maximum acceptable section thickness */
    private final int maxSectionThickness;

    /** Maximum acceptable filament thickness */
    private final int maxFilamentThickness;

    /** Minimum acceptable section length */
    private final int minSectionLength;

    /** Maximum acceptable delta Y */
    private final int maxOverlapDy;

    /** Maximum width for small gap */
    private int maxGapDx;

    /** Maximum dy for real gaps */
    private final int maxGapDy;

    /** Maximum dy to "burn" a candidate */
    private final int maxCloseDy;

    /** Maximum length to apply thickness test */
    private final int maxInvolvingLength;

    /** Predicate to remove too long runs */
    private final Predicate<Run> longRuns = new Predicate<Run>() {
        public final boolean check (Run run)
        {
            return run.getLength() > maxRunLength;
        }
    };

    /** Predicate to remove short sections */
    private final Predicate<GlyphSection> shortSections = new Predicate<GlyphSection>() {
        public final boolean check (GlyphSection section)
        {
            return section.getLength() < minSectionLength;
        }
    };

    /** Max section length per lag */
    private Map<GlyphLag, Integer> maxLengths = new LinkedHashMap<GlyphLag, Integer>();

    /** Long filaments found, non sorted */
    private final Set<Filament> filaments = new HashSet<Filament>();

    /** Filaments flagged as garbage */
    private Set<Filament> garbage = new HashSet<Filament>();

    /** Sections sorted by their starting coordinate (x) */
    private List<GlyphSection> sectionsStarts;

    /** Global slope of the sheet */
    private double globalSlope;

    /** The display if any */
    MyView view;

    /** Companion in charge of patterns */
    private PatternsRetriever patternsRetriever;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // FramesBuilder //
    //---------------//
    /**
     * Retrieve the frames of all staff lines
     *
     * @param sheet the sheet to process
     */
    public FramesBuilder (Sheet sheet)
    {
        super(
            sheet,
            new GlyphLag("hLag", StickSection.class, HORIZONTAL),
            Steps.valueOf(Steps.FRAMES));

        picture = sheet.getPicture();

        // Set scale-dependent parameters
        scale = sheet.getScale();
        minRunLength = scale.toPixels(constants.minRunLength);
        maxRunLength = (int) Math.rint(
            scale.mainFore() * constants.maxRunLineRatio.getValue());
        minVerticalRunLength = scale.toPixels(constants.minVerticalRunLength);
        minSectionLength = scale.toPixels(constants.minSectionLength);
        maxSectionThickness = (int) Math.rint(
            scale.mainFore() * constants.maxSectionLineRatio.getValue());
        maxFilamentThickness = (int) Math.rint(
            scale.mainFore() * constants.maxFilamentLineRatio.getValue());
        maxOverlapDy = scale.toPixels(constants.maxOverlapDy);
        maxGapDx = scale.toPixels(constants.maxGapDx);
        maxGapDy = scale.toPixels(constants.maxGapDy);
        maxCloseDy = scale.toPixels(constants.maxCloseDy);
        maxInvolvingLength = scale.toPixels(constants.maxInvolvingLength);
        probeWidth = scale.toPixels(Filament.getProbeWidth());

        logger.info("minRunLength:" + minRunLength);
        logger.info("minSectionLength:" + minSectionLength);
        logger.info("maxSectionThickness:" + maxSectionThickness);
        logger.info("maxFilamentThickness:" + maxFilamentThickness);
        logger.info("maxFilamentDy:" + maxOverlapDy);
        logger.info("maxGapDx:" + maxGapDx);
        logger.info("maxGapDy:" + maxGapDy);
        logger.info("maxCloseDy:" + maxCloseDy);
        logger.info("maxInvolvingLength: " + maxInvolvingLength);
        logger.info("probeWidth:" + probeWidth);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Compute and display the system frames of the sheet picture
     */
    public void buildInfo ()
        throws StepException
    {
        StopWatch watch = new StopWatch("Building lags");
        // Retrieve vertical runs
        watch.start("builTable whole-vert");

        RunsTable wholeVertTable = buildTable("whole-vert", VERTICAL);
        watch.start("Clone+Purge of long verticals");

        RunsTable longVertTable = wholeVertTable.clone()
                                                .purge(
            new MyPredicate(minVerticalRunLength));
        watch.start("BuildLag vflag");
        vLag = buildLag(null, "vfLag", VERTICAL, longVertTable);

        // Retrieve horizontal runs
        watch.start("Clone+Purge long verticals");

        RunsTable shortVertTable = wholeVertTable.clone()
                                                 .purge(longRuns);

        ///RunsTable wholeHoriTable = buildTable("whole-hori", HORIZONTAL);
        watch.start("CreateTable whole-hori from whole-vert");

        RunsTableFactory factory = new RunsTableFactory(
            HORIZONTAL,
            shortVertTable.getBuffer(),
            sheet.getPicture().getMaxForeground(),
            0);

        RunsTable        wholeHoriTable = factory.createTable("whole-hori");

        watch.start("Clone+Purge of short horizontals");

        RunsTable longHoriTable = wholeHoriTable.clone();
        longHoriTable.purge(new MyPredicate(minRunLength));
        watch.start("BuildLag hfLag");
        buildLag(lag, "hfLag", HORIZONTAL, longHoriTable);
        watch.start("Purge hfLag of short sections");
        lag.purgeSections(shortSections);

        watch.stop();
        watch.print();

        // Display the window
        if (constants.displayFrame.getValue() && (Main.getGui() != null)) {
            displayFrame();
        }

        // Retrieve the filaments
        buildFilaments();

        // Update the display
        if (view != null) {
            final int viewIndex = lag.viewIndexOf(view);

            for (Filament fil : garbage) {
                view.colorizeGlyph(viewIndex, fil, Color.LIGHT_GRAY);
            }
        }
    }

    //--------------//
    // isMajorChunk //
    //--------------//
    private boolean isMajorChunk (GlyphSection section)
    {
        // Check section length
        // Check /quadratic mean/ section thickness
        return (section.getLength() >= minSectionLength) &&
               !isSectionFat(section);
    }

    //--------------//
    // isSectionFat //
    //--------------//
    /**
     * Detect if the provided section is a thick one
     * @param section the section to check
     * @return true if fat
     */
    private boolean isSectionFat (GlyphSection section)
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
            roi.grow(probeWidth / 2, maxSectionThickness);

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
                (leftThickness > maxSectionThickness) ||
                (rightThickness > maxSectionThickness));

            //            if (logger.isFineEnabled()) {
            //                logger.fine(
            //                    (section.isFat() ? "FAT  " : "Slim ") + section.getId() +
            //                    " left:" + leftThickness + " right:" + rightThickness +
            //                    " vs " + maxSectionThickness);
            //            }
        }

        return section.isFat();
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

    //-------------------//
    // aggregateSections //
    //-------------------//
    /**
     *   Aggregate sections into filaments
     */
    private void aggregateSections ()
    {
        // Build a collection of sections to ease their retrieval by ROI
        sectionsStarts = new ArrayList<GlyphSection>(lag.getSections());
        Collections.sort(sectionsStarts, GlyphSection.startComparator);

        // Sort sections by decreasing length
        List<GlyphSection> sections = new ArrayList<GlyphSection>(
            lag.getSections());
        Collections.sort(sections, GlyphSection.reverseLengthComparator);

        for (GlyphSection section : sections) {
            // Limit to main sections for the time being
            if (isMajorChunk(section)) {
                // Create a filament with this section
                Filament filament = new Filament(scale);
                filament.include(section);
                filament = (Filament) lag.addGlyph(filament);
                filaments.add(filament);

                if (logger.isFineEnabled()) {
                    logger.fine("Created " + filament + " with " + section);
                }
            }
        }
    }

    //----------------//
    // buildFilaments //
    //----------------//
    /**
     * Organize the long and thin horizontal sections into filaments (glyphs)
     * that will be good candidates for staff lines.
     */
    private void buildFilaments ()
    {
        StopWatch watch = new StopWatch("Building filaments");

        watch.start("Sections aggregation size:" + lag.getSections().size());
        aggregateSections();
        logger.info(filaments.size() + " filaments created.");

        watch.start("Filaments merge");
        mergeFilaments(Phase.AGGREGATION);
        logger.info(filaments.size() + " filaments after merge.");

        watch.start("Filaments purge");
        mergeFilaments(Phase.PURGE);
        logger.info(filaments.size() + " filaments after purge.");

        watch.start("Patterns detection");

        globalSlope = retrieveGlobalSlope();

        patternsRetriever = new PatternsRetriever(
            sheet,
            filaments,
            globalSlope);
        patternsRetriever.retrievePatterns(picture.getWidth());

        watch.start("Filaments connection");
        patternsRetriever.connectFilaments();
        logger.info(filaments.size() + " filaments after connections.");

        watch.stop();
        watch.print();
    }

    //----------//
    // buildLag //
    //----------//
    private GlyphLag buildLag (GlyphLag    lag,
                               String      name,
                               Orientation orientation,
                               RunsTable   runsTable)
    {
        //  Allocate lag if not already done
        if (lag == null) {
            lag = new GlyphLag(name, StickSection.class, orientation);
        }

        //  Build sections
        SectionsBuilder sectionsBuilder = new SectionsBuilder<GlyphLag, GlyphSection>(
            lag,
            new JunctionRatioPolicy(constants.maxLengthRatio.getValue()));
        sectionsBuilder.createSections(runsTable);

        // Retrieve max section length in the lag
        int maxLength = 0;

        for (GlyphSection section : lag.getVertices()) {
            maxLength = Math.max(maxLength, section.getLength());
        }

        maxLengths.put(lag, maxLength);
        logger.info("Built: " + lag + " maxLength:" + maxLength);

        return lag;
    }

    //------------//
    // buildTable //
    //------------//
    private RunsTable buildTable (String      name,
                                  Orientation orientation)
    {
        // Build runs
        RunsTableFactory factory = new RunsTableFactory(
            orientation,
            picture,
            picture.getMaxForeground(),
            0);

        return factory.createTable(name);
    }

    //--------------//
    // displayFrame //
    //--------------//
    private void displayFrame ()
    {
        GlyphsController controller = new GlyphsController(this);

        // Create a view
        view = new MyView(lag, null, controller);

        // Boards
        final String  unit = sheet.getId() + ":FramesBuilder";
        BoardsPane    boardsPane = new BoardsPane(
            new PixelBoard(unit, sheet),
            new RunBoard(unit, lag),
            new SectionBoard(unit, lag.getLastVertexId(), lag),
            new GlyphBoard(unit, controller, null));

        // Create a hosting frame for the view
        ScrollLagView slv = new ScrollLagView(view);
        sheet.getAssembly()
             .addViewTab(Step.FRAMES_TAB, slv, boardsPane);
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

                    if ((head == candidate) || head.isDiscarded()) {
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
                        candidate.setDiscarded(true);
                        candidate = head; // This is a new candidate

                        break HeadsLoop;
                    }

                    case OVERLAP_CLOSE : {
                        // We burn the shorter
                        if (candidate.getLength() > head.getLength()) {
                            Filament tempo = head;
                            head = candidate;
                            candidate = tempo;
                        }

                        if (logger.isFineEnabled()) {
                            logger.fine("Burnt " + candidate + " near " + head);
                        }

                        candidate.setDiscarded(true);
                        garbage.add(candidate);
                    }

                    default :
                    }
                }
            }
        }

        // Discard the merged filaments
        for (Iterator<Filament> it = filaments.iterator(); it.hasNext();) {
            Filament f = it.next();

            if (f.isDiscarded()) {
                ///logger.info("Removed " + f);
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
        double ratio = constants.topRatioForSlope.getValue();
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

        if (gapDx > maxGapDx) {
            if (logger.isFineEnabled()) {
                logger.fine("Gap too wide: " + gapDx + " vs " + maxGapDx);
            }

            return MergeResult.GAP_WIDE;
        }

        // y gap?
        if (gapDx < 0) {
            // True overlap between the two filaments
            // Measure dy at middle of overlap
            int    xMid = (overlapLeft + overlapRight) / 2;
            double oneY = one.getCurve()
                             .yAt(xMid);
            double twoY = two.getCurve()
                             .yAt(xMid);
            int    dy = Math.abs((int) Math.rint(oneY - twoY));

            if (dy > maxOverlapDy) {
                if ((phase == Phase.PURGE) && (dy < maxCloseDy)) {
                    if (logger.isFineEnabled()) {
                        logger.fine("Close parallel: " + dy);
                    }

                    return MergeResult.OVERLAP_CLOSE;
                } else {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "Dy too high: " + dy + " vs " + maxOverlapDy);
                    }

                    return MergeResult.OVERLAP_FAR;
                }
            }

            if (phase == Phase.PURGE) {
                // Here, we are closely parallel but still not merged
                if (logger.isFineEnabled()) {
                    logger.fine("Close parallel not merged");
                }

                return MergeResult.OVERLAP_CLOSE;
            } else {
                // Check resulting thickness at middle of overlap
                // Only when at least one short filament is involved
                int shortLength = Math.min(one.getLength(), two.getLength());

                if ((phase != Phase.PURGE) &&
                    (shortLength <= maxInvolvingLength)) {
                    int            yMid = (int) Math.rint((oneY + twoY) / 2);
                    PixelRectangle roi = new PixelRectangle(xMid, yMid, 0, 0);
                    roi.grow(probeWidth / 2, maxFilamentThickness);

                    int thickness = getThickness(roi);

                    if (thickness > maxFilamentThickness) {
                        if (logger.isFineEnabled()) {
                            logger.fine(
                                "Too thick: " + thickness + " vs " +
                                maxFilamentThickness + " " + one + " " + two);
                        }

                        return MergeResult.TOO_THICK;
                    }
                }
            }
        } else {
            // No true overlap, it's a gap
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

            if (gapDy > maxGapDy) {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Dy too high for gap: " + gapDy + " vs " + maxGapDy);
                }

                return MergeResult.GAP_FAR;
            }
        }

        if (logger.isFineEnabled()) {
            logger.fine("Compatible!");
        }

        return MergeResult.OK;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-------------//
    // MyPredicate //
    //-------------//
    public static class MyPredicate
        implements Predicate<Run>
    {
        //~ Instance fields ----------------------------------------------------

        private final int minRunLength;

        //~ Constructors -------------------------------------------------------

        public MyPredicate (int minRunLength)
        {
            this.minRunLength = minRunLength;
        }

        //~ Methods ------------------------------------------------------------

        public final boolean check (Run run)
        {
            return run.getLength() < minRunLength;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean displaySpecifics = new Constant.Boolean(
            false,
            "Dummy stuff");
        Constant.Boolean displayFrame = new Constant.Boolean(
            true,
            "Should we display a frame?");
        Constant.Double  splineThickness = new Constant.Double(
            "thickness",
            2d, // 0.5
            "Stroke thickness to draw filaments curves");
        Scale.Fraction   minRunLength = new Scale.Fraction(
            1.0, // 1.0 1.5
            "Minimum length for a run to be considered");
        Scale.Fraction   minVerticalRunLength = new Scale.Fraction(
            1.5, // 1.5
            "Minimum length for a vertical run to be considered");
        Constant.Ratio   maxLengthRatio = new Constant.Ratio(
            1.5, // 1.5
            "Maximum ratio in length for a run to be combined with an existing section");
        Scale.Fraction   minSectionLength = new Scale.Fraction(
            1, // 1.0 2
            "Minimum length for a section to be considered in frames computation");
        Constant.Ratio   maxSectionLineRatio = new Constant.Ratio(
            1.5, // 1.5
            "Maximum section thickness WRT mean line height");
        Constant.Ratio   maxRunLineRatio = new Constant.Ratio(
            2.0, // 2.0
            "Maximum run length WRT mean line height");
        Constant.Ratio   maxFilamentLineRatio = new Constant.Ratio(
            1.5, // 1.5
            "Maximum filament thickness WRT mean line height");
        Scale.Fraction   maxOverlapDy = new Scale.Fraction(
            0.2, // 0.2
            "Maximum delta Y between two overlapping filaments");
        Scale.Fraction   maxGapDx = new Scale.Fraction(
            1, // 1
            "Maximum delta X for a gap between filaments");
        Scale.Fraction   maxGapDy = new Scale.Fraction(
            0.2, // 0.2
            "Maximum delta Y for a true gap between filaments");
        Scale.Fraction   maxCloseDy = new Scale.Fraction(
            0.5, // 0.5
            "Maximum delta Y to burn a candidate");
        Scale.Fraction   maxInvolvingLength = new Scale.Fraction(
            6, // 6
            "Maximum filament length to apply thickness test");
        Constant.Ratio   topRatioForSlope = new Constant.Ratio(
            0.1,
            "Percentage of top filaments used to retrieve global slope");
        Scale.Fraction   tangentDx = new Scale.Fraction(
            4,
            "Typical length to display tangents");
    }

    //--------//
    // MyView //
    //--------//
    /**
     * We paint on the same display the vertical and horizontal sections.
     * The color depends on the section length, darker for the longest and
     * brighter for the shortest.
     */
    private class MyView
        extends GlyphLagView
    {
        //~ Constructors -------------------------------------------------------

        public MyView (GlyphLag           lag,
                       List<GlyphSection> specifics,
                       GlyphsController   controller)
        {
            super(lag, specifics, constants.displaySpecifics, controller, null);
            setName("Frames-View");

            // Additional stuff for vLag
            vLag.addView(this);

            int viewIndex = vLag.viewIndexOf(this);

            for (GlyphSection section : vLag.getVertices()) {
                addSectionView(section);
                colorizeSection(section, viewIndex);
            }
        }

        //~ Methods ------------------------------------------------------------

        //--------//
        // render //
        //--------//
        /**
         * Render this lag in the provided Graphics context, which may be already
         * scaled
         * @param g the graphics context
         */
        @Override
        public void render (Graphics2D g)
        {
            // Should we draw the section borders?
            boolean      drawBorders = ViewParameters.getInstance()
                                                     .isSectionSelectionEnabled();
            final Stroke oldStroke = UIUtilities.setAbsoluteStroke(g, 1f);

            // First render the vertical lag
            final int vIndex = vLag.viewIndexOf(this);
            renderCollection(g, vLag.getVertices(), vIndex, drawBorders);

            // Then standard rendering for hLag (on top of vLag)
            super.render(g);

            // Restore stroke
            g.setStroke(oldStroke);
        }

        //-----------------//
        // colorizeSection //
        //-----------------//
        @Override
        protected void colorizeSection (GlyphSection section,
                                        int          viewIndex)
        {
            GlyphLag lag = section.getGraph();
            double   maxLength = maxLengths.get(lag);
            int      length = section.getLength();
            int      level = (int) Math.rint(200 * (1 - (length / maxLength)));
            Color    color;

            if (lag.isVertical()) {
                color = new Color(level, level, 255); // Blue gradient
            } else {
                // Discard thick sections (using quadratic mean thickness)
                if (isSectionFat(section)) {
                    color = Color.GRAY;
                } else {
                    if (section.getLength() < minSectionLength) {
                        color = Color.LIGHT_GRAY;
                    } else {
                        color = new Color(255, level, level); // Red Gradient
                    }
                }
            }

            SectionView view = (SectionView) section.getView(viewIndex);
            view.setColor(color);
        }

        //-------------//
        // renderItems //
        //-------------//
        @Override
        protected void renderItems (Graphics2D g)
        {
            // Draw curve for the remaining filaments
            g.setColor(PagePainter.musicColor);

            Stroke oldStroke = g.getStroke();
            g.setStroke(splineStroke);

            for (Filament filament : filaments) {
                NaturalSpline curve = filament.getCurve();

                if (curve != null) {
                    g.draw(curve);
                }
            }

            // Draw tangent at each ending point
            g.setColor(Color.BLACK);

            double dx = scale.toPixels(constants.tangentDx);

            for (Filament filament : filaments) {
                NaturalSpline curve = filament.getCurve();

                if (curve != null) {
                    PixelPoint    p = filament.getStartPoint();
                    double        der = curve.derivativeAt(p.x);
                    Line2D.Double line = new Line2D.Double(
                        p.x,
                        p.y,
                        p.x - dx,
                        p.y - (der * dx));
                    g.draw(line);

                    p = filament.getStopPoint();
                    der = curve.derivativeAt(p.x);
                    line = new Line2D.Double(
                        p.x,
                        p.y,
                        p.x + dx,
                        p.y + (der * dx));
                    g.draw(line);
                }
            }

            g.setStroke(oldStroke);

            // Patterns stuff
            if (patternsRetriever != null) {
                patternsRetriever.renderItems(g);
            }
        }
    }
}
