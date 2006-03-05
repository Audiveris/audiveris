//-----------------------------------------------------------------------//
//                                                                       //
//                         B a r s B u i l d e r                         //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.sheet;

import omr.Main;
import omr.ProcessingException;
import omr.check.Check;
import omr.check.CheckSuite;
import omr.check.Checkable;
import omr.check.CheckBoard;
import omr.check.FailureResult;
import omr.check.Result;
import omr.check.SuccessResult;
import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.glyph.Glyph;
import omr.glyph.GlyphDirectory;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.Shape;
import omr.glyph.ui.GlyphBoard;
import omr.lag.JunctionDeltaPolicy;
import omr.lag.LagBuilder;
import omr.lag.VerticalOrientation;
import omr.score.Measure;
import omr.score.Score;
import omr.score.ScoreView;
import omr.score.Stave;
import omr.score.System;
import omr.stick.Stick;
import omr.stick.StickSection;
import omr.stick.StickView;
import omr.ui.BoardsPane;
import omr.ui.PixelBoard;
import omr.lag.ScrollLagView;
import omr.lag.SectionBoard;
import omr.ui.view.Zoom;
import omr.util.Dumper;
import omr.util.Logger;
import omr.util.TreeNode;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Class <code>BarsBuilder</code> handles the vertical lines that are
 * recognized as bar lines.
 *
 * <p> Input is provided by the list of vertical sticks retrieved by the
 * preceding step.
 *
 * <p> Output is the collection of detected Bar lines.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class BarsBuilder
    implements GlyphDirectory
{
    //~ Static variables/initializers -------------------------------------

    private static final Constants constants = new Constants();
    private static final Logger logger = Logger.getLogger(BarsBuilder.class);

    // Success
    private static final SuccessResult BAR_SYSTEM_DEFINING       = new SuccessResult("Bar-SystemDefining");
    private static final SuccessResult BAR_NOT_SYSTEM_DEFINING   = new SuccessResult("Bar-NotSystemDefining");

    // Failure
    private static final FailureResult TOO_SHORT_BAR             = new FailureResult("Bar-TooShort");
    private static final FailureResult OUTSIDE_STAVE_WIDTH       = new FailureResult("Bar-OutsideStaveWidth");
    private static final FailureResult NOT_STAVE_ANCHORED        = new FailureResult("Bar-NotStaveAnchored");
    private static final FailureResult NOT_SYSTEM_ALIGNED        = new FailureResult("Bar-NotSystemAligned");
    private static final FailureResult NOT_WITHIN_SYSTEM         = new FailureResult("Bar-NotWithinSystem");
    private static final FailureResult SHORTER_THAN_STAVE_HEIGHT = new FailureResult("Bar-ShorterThanStaveHeight");
    private static final FailureResult THICK_BAR_NOT_ALIGNED     = new FailureResult("Bar-ThickBarNotAligned");
    private static final FailureResult TOO_HIGH_ADJACENCY        = new FailureResult("Bar-TooHighAdjacency");
    private static final FailureResult CHUNK_AT_TOP              = new FailureResult("Bar-ChunkAtTop");
    private static final FailureResult CHUNK_AT_BOTTOM           = new FailureResult("Bar-ChunkAtBottom");
    private static final FailureResult CANCELLED                 = new FailureResult("Bar-Cancelled");

    //~ Instance variables ------------------------------------------------

    // Underlying lag
    private GlyphLag vLag;

    // Bars area, with retrieved vertical sticks
    private VerticalArea barsArea;

    // List of found bars
    private List<BarInfo> bars = new ArrayList<BarInfo>();

    // Retrieved systems
    private List<SystemInfo> systems = new ArrayList<SystemInfo>();

    // Unused vertical sticks
    private List<Stick> clutter;

    // Cached data
    private final Sheet sheet;
    private Score score;
    private Scale scale;
    private int basicCoreLength;

    // Lag view on bars, if so desired
    private MyLagView lagView;

    // Related glyph board
    private GlyphBoard glyphBoard;

    // Suite of checks
    private BarCheckSuite suite;

    // Display of check results
    private CheckBoard<Context> checkBoard;
    //~ Constructors ------------------------------------------------------

    //-------------//
    // BarsBuilder //
    //-------------//
    /**
     * Prepare a bar retriever on the provided sheet
     *
     * @param sheet the sheet to process
     */
    public BarsBuilder (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods -----------------------------------------------------------

    //-----------//
    // getEntity //
    //-----------//
    public Glyph getEntity (Integer id)
    {
        return vLag.getGlyph(id);
    }

    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Retrieve and store the bars information on the provided sheet
     *
     * @return the built Bars info
     * @throws ProcessingException raised when step processing must stop,
     *                             due to encountered error
     */
    public List<BarInfo> buildInfo ()
            throws ProcessingException
    {
        // Stuff to be made available
        scale = sheet.getScale();
        //sheet.getStaves();
        sheet.getHorizontals();

        // Retrieve the vertical lag of runs
        vLag = new GlyphLag(new VerticalOrientation());
        vLag.setId("vLag");
        vLag.setVertexClass(StickSection.class);
        new LagBuilder<GlyphLag,GlyphSection>().rip
                (vLag,
                 sheet.getPicture(),
                 0, // minRunLength
                 new JunctionDeltaPolicy(constants.maxDeltaLength
                                         .getValue())); // maxDeltaLength
        sheet.setVerticalLag(vLag);

        // Retrieve (vertical) sticks
        barsArea = new VerticalArea(sheet, vLag,
                                    scale.fracToPixels(constants.maxBarThickness));
        // Allocate score
        createScore();

        // Retrieve true bar lines and thus SystemInfos
        retrieveBarLines();

        // Build score Systems & Staves from SystemInfos
        buildSystemsAndStaves();

        // Build Measures
        buildMeasures();

        // Check Measures
        checkMeasures();

        // Erase bar pixels from picture
        //////eraseBars();

        // Update score internal data
        score.computeChildren();

        // Assign bar line shape
        for (BarInfo info : bars){
            Stick stick = info.getStick();
            stick.setShape(isThickBar(stick) ?
                           Shape.THICK_BAR_LINE :
                           Shape.THIN_BAR_LINE);
        }

        // Report number of measures retrieved
        logger.info(score.getLastSystem().getLastMeasureId()
                    + " measure(s) found");

        // Split everything, including horizontals, per system
        SystemSplit.computeSystemLimits(sheet);
        SystemSplit.splitHorizontals(sheet);
        SystemSplit.splitBars(sheet, bars);

        // Display the resulting stickarea is so asked for
        if (constants.displayFrame.getValue() &&
            Main.getJui() != null) {
            displayFrame();
        }

        return bars;
    }

    //~ Methods -----------------------------------------------------------

    //---------//
    // getBars //
    //---------//
    /**
     * Report the list of bar lines retrieved
     *
     * @return the bar lines
     */
    public List<BarInfo> getBars ()
    {
        return bars;
    }

    //--------//
    // getLag //
    //--------//
    /**
     * Report the vertical lag used for this step
     *
     * @return the vertical lag used for bar source
     */
    public GlyphLag getLag ()
    {
        return vLag;
    }

    //------------//
    // isThickBar //
    //------------//
    /**
     * Check if the stick/bar is a thick one
     *
     * @param stick the bar stick to check
     *
     * @return true if thick
     */
    private boolean isThickBar (Stick stick)
    {
        // Max width of a thin bar line, otherwise this must be a thick bar
        final int maxThinWidth = scale.fracToPixels(constants.maxThinWidth);

        final int meanWidth = (int) Math.rint(stick.getWeight()
                                              / stick.getLength());

        return meanWidth > maxThinWidth;
    }

    //-------------//
    // getSystemOf //
    //-------------//
    /**
     * Report the SystemInfo that contains the given BarInfo.
     *
     * @param bar the given BarInfo
     * @param sheet the sheet context
     * @return the containing SystemInfo, null if not found
     */
    public SystemInfo getSystemOf (BarInfo bar,
                                   Sheet   sheet)
    {
        int topIdx = bar.getTopIdx();
        int botIdx = bar.getBotIdx();

        if (topIdx == -1) {
            topIdx = botIdx;
        }

        if (botIdx == -1) {
            botIdx = topIdx;
        }

        Score score = sheet.getScore();
        if (score == null) {
            return null;
        }

        for (Iterator it = score.getSystems().iterator(); it.hasNext();) {
            System     system     = (omr.score.System) it.next();
            SystemInfo systemInfo = system.getInfo();

            if ((systemInfo.getStartIdx() <= botIdx)
                && (systemInfo.getStopIdx() >= topIdx)) {
                return systemInfo;
            }
        }

        // Not found
        return null;
    }

    //---------------//
    // buildMeasures //
    //---------------//
    /**
     * Bar lines are first sorted according to their abscissa, then we run
     * additional checks on each bar line, since we now know its enclosing
     * system. If OK, then we add a corresponding measure in each stave.
     */
    private void buildMeasures ()
    {
        final int maxDy = scale.fracToPixels(constants.maxBarOffset);

        // Sort bar lines by increasing abscissa
        Collections.sort(bars,
                         new Comparator<BarInfo>()
                         {
                             public int compare (BarInfo b1,
                                                 BarInfo b2)
                             {
                                 return b1.getStick().getMidPos()
                                        - b2.getStick().getMidPos();
                             }
                         });

        // Measures building (Bars are already sorted by increasing
        // abscissa)
        for (Iterator<BarInfo> bit = bars.iterator(); bit.hasNext();) {
            BarInfo barInfo = bit.next();

            // Determine the system this bar line belongs to
            SystemInfo systemInfo = getSystemOf(barInfo, sheet);

            if (systemInfo == null) { // Should not occur, but that's safer
                logger.warning("Bar not belonging to any system");
                logger.debug("barInfo = " + barInfo);
                Dumper.dump(barInfo);
                Dumper.dump(barInfo.getStick());

                continue;
            }
            omr.score.System system = systemInfo.getScoreSystem();

            // We don't check that the bar does not start before first
            // stave, this is too restrictive because of alternate endings.
            // We however do check that the bar does not end after last
            // stave of the system.
            int barAbscissa = barInfo.getStick().getMidPos();
            int systemBottom = system.getLastStave().getInfo().getLastLine()
                    .getLine().yAt(barAbscissa);

            if ((barInfo.getStick().getStop() - systemBottom) > maxDy) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Bar stopping too low");
                }

                barInfo.getStick().setResult(NOT_WITHIN_SYSTEM);
                bit.remove();

                continue;
            }

            // We add a measure in each stave of this system, provided that
            // the stave is embraced by the bar line
            for (TreeNode node : system.getStaves()) {
                Stave stave = (Stave) node;
                if (isStaveEmbraced (stave, barInfo)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Creating measure for bar-line " + barInfo.getStick());
                    }
                    new Measure(barInfo, stave, Shape.SINGLE_BARLINE,
                                scale.pixelsToUnits(barAbscissa)
                                - stave.getLeft(),
                                scale.pixelsToUnits(barAbscissa)
                                - stave.getLeft(), false); // invented ?
                }
            }
        }
    }

    //-----------------//
    // isStaveEmbraced //
    //-----------------//
    private boolean isStaveEmbraced (Stave   stave,
                                     BarInfo bar)
    {
        // Extrema of bar, units
        int topUnit = scale.pixelsToUnits(bar.getStick().getStart());
        int botUnit = scale.pixelsToUnits(bar.getStick().getStop());

        // Check that middle of stave is within bar top & bottom
        final int midStave = stave.getTop() + (stave.getSize() / 2);

        return (midStave > topUnit) && (midStave < botUnit);
    }

    //------------------//
    // buildSystemInfos //
    //------------------//
    /**
     * Knowing the starting stave indice of each stave system, we are able
     * to allocate and describe the proper number of systems in the score.
     *
     * @param starts indexed by any stave, to give the stave index of the
     *               containing system. For a system with just one stave,
     *               both indices are equal. For a system of more than 1
     *               stave, the indices differ.
     *
     * @throws omr.ProcessingException raised if processing failed
     */
    private void buildSystemInfos (int[] starts)
            throws omr.ProcessingException
    {
        int id = 0;                     // Id for created SystemInfo's
        int start = -1;

        for (int i = 0; i < starts.length; i++) {
            if (starts[i] != start) {
                if (start != -1) {
                    systems.add(new SystemInfo(++id, sheet, start, starts[i] - 1));
                }

                start = i;
            }
        }

        systems.add(new SystemInfo(++id, sheet, start, starts.length - 1));

        if (logger.isDebugEnabled()) {
            for (SystemInfo info : systems) {
                Dumper.dump(info);
            }
        }

        // Finally, store this list into the sheet instance
        sheet.setSystems(systems);
    }

    //-----------------------//
    // buildSystemsAndStaves //
    //-----------------------//
    /**
     * For each SystemInfo, build the corresponding System entity with all
     * its depending Staves
     */
    private void buildSystemsAndStaves ()
    {
        // Systems
        for (SystemInfo info : systems) {
            // Allocate the system
            omr.score.System system =
                new omr.score.System(info, score,
                                     scale.pixelsToUnits(info.getTop()),
                                     scale.pixelsToUnits(info.getLeft()),
                                     scale.pixelsToUnits(info.getWidth()),
                                     scale.pixelsToUnits(info.getDeltaY()));

            // Set the link SystemInfo -> System
            info.setScoreSystem(system);

            // Allocate the staves in this system
            int staveLink = 0;

            for (StaveInfo set : info.getStaves()) {
                LineInfo line = set.getFirstLine();
                new Stave(set, system,
                          scale.pixelsToUnits(line.getLine().yAt(line.getLeft())),
                          scale.pixelsToUnits(set.getLeft()),
                          scale.pixelsToUnits(set.getRight() - set.getLeft()),
                          64, // Staff vertical size in units
                          staveLink++);
            }
        }
    }

    //--------------------//
    // checkBarAlignments //
    //--------------------//
    /**
     * Check alignment of each measure of each stave with the other stave
     * measures, a test that needs several staves in the system
     *
     * @param system the system to check
     */
    private void checkBarAlignments (omr.score.System system)
    {
        if (system.getStaves().size() > 1) {
            int maxShiftDx = scale.fracToPixels(constants.maxAlignShiftDx);

            for (Iterator sit = system.getStaves().iterator(); sit.hasNext();) {
                Stave stave = (Stave) sit.next();

                for (Iterator mit = stave.getMeasures().iterator();
                     mit.hasNext();) {
                    Measure measure = (Measure) mit.next();

                    // Compare the abscissa with corresponding position in
                    // the other staves
                    int x = measure.getLeftlinex();

                    for (Iterator it = system.getStaves().iterator();
                         it.hasNext();) {
                        Stave stv = (Stave) it.next();

                        if (stv == stave) {
                            continue;
                        }

                        if (null == stv.getMeasureAt(x, maxShiftDx)) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Singular measure removed: "
                                             + Dumper.dumpOf(measure));
                            }

                            // Remove the false bar info
                            for (BarInfo info : measure.getInfos()) {
                                Stick stick = info.getStick();
                                stick.setResult(NOT_SYSTEM_ALIGNED);
                                bars.remove(info);
                            }

                            // Remove the false measure
                            mit.remove();

                            break;
                        }
                    }
                }
            }
        }
    }

    //----------------//
    // checkEndingBar //
    //----------------//
    /**
     * Use ending bar line if any, to adjust the right abscissa of the
     * system and its staves.
     *
     * @param system the system to check
     */
    private void checkEndingBar (omr.score.System system)
    {
        Stave stave = system.getFirstStave();
        Measure measure = stave.getLastMeasure();
        int lastX = measure.getRightlinex();
        int minWidth = scale.fracToPixels(constants.minMeasureWidth);

        if ((stave.getWidth() - lastX) < minWidth) {
            if (logger.isDebugEnabled()) {
                logger.debug("Adjusting EndingBar " + system);
            }

            // Adjust end of system & stave(s) to this one
            system.setWidth(lastX);

            for (Iterator sit = system.getStaves().iterator(); sit.hasNext();) {
                Stave stv = (Stave) sit.next();
                stv.setWidth(system.getWidth());
            }
        }
    }

    //---------------//
    // checkMeasures //
    //---------------//
    /**
     * Check measure reality, using a set of aditional tests.
     */
    private void checkMeasures ()
    {
        // Check are performed on a system basis
        for (Iterator sysit = score.getSystems().iterator();
             sysit.hasNext();) {
            omr.score.System system = (omr.score.System) sysit.next();

            // Check alignment of each measure of each stave with the other
            // stave measures, a test that needs several staves in the
            // system
            checkBarAlignments(system);

            // Detect very narrow measures which in fact indicate double
            // bar lines.
            mergeLines(system);

            // First barline may be just the beginning of the stave, so do
            // not count the very first bar line, which in general defines
            // the beginning of the stave rather than the end of a measure,
            // but use it to precisely define the left abscissa of the
            // system and all its contained staves.
            removeStartingBar(system);

            // Similarly, use the very last bar line, which generally ends
            // the system, to define the right abscissa of the system and
            // its staves.
            checkEndingBar(system);
        }
    }

    //-------------//
    // createScore //
    //-------------//
    private void createScore ()
    {
        if (logger.isDebugEnabled()) {
            logger.debug("Allocating score");
        }

        score = new Score(scale.pixelsToUnits(sheet.getWidth()),
                          scale.pixelsToUnits(sheet.getHeight()),
                          (int) Math.rint(sheet.getSkew().angle()
                                          * ScoreView.BASE),
                          scale.spacing(), sheet.getPath());

        // Mutual referencing
        score.setSheet(sheet);
        sheet.setScore(score);
    }

    //--------------//
    // displayFrame //
    //--------------//
    private void displayFrame ()
    {
        lagView = new MyLagView(vLag);
        lagView.colorize();

        // Ids of recognized glyphs
        List<Integer> knownIds = new ArrayList<Integer>(bars.size() +1);
        knownIds.add(GlyphBoard.NO_VALUE);
        for (BarInfo bar : bars) {
            knownIds.add(new Integer(bar.getStick().getId()));
        }

        glyphBoard = new GlyphBoard(vLag.getLastGlyphId(), knownIds);
        checkBoard = new CheckBoard<Context>(suite);
        lagView.setCheckMonitor(checkBoard);
        BoardsPane boardsPane = new BoardsPane
            (lagView,
             new PixelBoard(),
             new SectionBoard(vLag.getLastVertexId()),
             glyphBoard,
             checkBoard);

        // Create a hosting frame for the view
        ScrollLagView slv = new ScrollLagView(lagView);
        sheet.getAssembly().addViewTab("Bars",  slv, boardsPane);
    }

//     //-----------//
//     // eraseBars //
//     //-----------//
//     private void eraseBars ()
//     {
//         Picture picture = sheet.getPicture();

//         for (BarInfo bar : bars) {
//             Stick stick = bar.getStick();
//             stick.erasePixels(picture);
//         }
//     }

    //------------//
    // mergeLines //
    //------------//
    /**
     * Check whether two close bar lines are not in fact double lines (with
     * variants)
     *
     * @param system the system to check
     */
    private void mergeLines (omr.score.System system)
    {
        int maxDoubleDx = scale.fracToPixels(constants.maxDoubleBarDx);

        for (Iterator sit = system.getStaves().iterator(); sit.hasNext();) {
            Stave stave = (Stave) sit.next();

            Measure prevMeasure = null;

            for (Iterator mit = stave.getMeasures().iterator();
                 mit.hasNext();) {
                Measure measure = (Measure) mit.next();

                if (prevMeasure != null) {
                    final int measureWidth = measure.getLeftlinex()
                                             - prevMeasure.getLeftlinex();

                    if (measureWidth <= maxDoubleDx) {
                        BarInfo bar = measure.getInfos().get(0);

                        if (isThickBar(bar.getStick())) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Merging a thinThick bar");
                            }

                            prevMeasure.setLinetype(Shape.FINAL_BARLINE);
                        } else {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Merging a double bar");
                            }

                            prevMeasure.setLinetype(Shape.DOUBLE_BARLINE);
                        }

                        prevMeasure.setRightlinex(measure.getRightlinex());
                        prevMeasure.addInfos(measure.getInfos());
                        mit.remove();
                    } else {
                        prevMeasure = measure;
                    }
                } else {
                    prevMeasure = measure;
                }
            }
        }
    }

    //-------------------//
    // removeStartingBar //
    //-------------------//
    /**
     * We associate measures only with their ending bar line(s), so the
     * starting bar of a stave (or system) does not end a measure, we thus
     * have to remove the measure that we first had associated with it.
     *
     * @param system the system whose starting measure has to be checked
     *               (the check is based on the width of this false
     *               measure)
     */
    private void removeStartingBar (omr.score.System system)
    {
        Stave stave = system.getFirstStave();
        Measure measure = stave.getFirstMeasure();
        BarInfo bar = measure.getInfos().get(0);
        int firstX = measure.getLeftlinex();
        int minWidth = scale.fracToPixels(constants.minMeasureWidth);

        if (firstX < minWidth) {
            // Adjust beginning of system to this one
            if (logger.isDebugEnabled()) {
                logger.debug("Adjusting firstX=" + firstX + " " + system);
            }

            system.setLeft(system.getLeft() + firstX);

            // Adjust beginning of all stave(s) to this one
            // Remove this false "measure" in all staves of the system
            for (Iterator sit = system.getStaves().iterator(); sit.hasNext();) {
                Stave stv = (Stave) sit.next();
                int staveDx = system.getLeft() - stv.getLeft();
                stv.setLeft(system.getLeft());

                // Remove this first measure
                stv.getMeasures().remove(0);

                // Set the bar as starting bar for the stave
                stv.setStartingBar(bar);

                // Update other bar lines abscissae accordingly
                for (Iterator mit = stv.getMeasures().iterator();
                     mit.hasNext();) {
                    Measure meas = (Measure) mit.next();
                    meas.setLeftlinex(meas.getLeftlinex() - staveDx);
                    meas.setRightlinex(meas.getRightlinex() - staveDx);
                }
            }
        }
    }

    /**
     * Remove a bar together with all its related entities. This means
     * removing reference in the bars list of this builder, reference in
     * the containing SystemInfo, reference in the Measure it ends, and
     * removing this Measure itself if this (false) bar was the only ending
     * bar left for the Measure. The related stick must also be assigned a
     * failure result.
     *
     * @param glyph the (false) bar glyph to deassign
     */
    public void deassignBarGlyph (Glyph glyph)
    {
        BarInfo bar = getBarOf(glyph);
        if (bar == null) {
            return;
        } else {
            logger.info("Removing a " + glyph.getShape());
        }

        // Related stick has to be freed
        bar.getStick().setShape(null);
        bar.getStick().setResult(CANCELLED);

        // Remove from the internal all-bars list
        bars.remove(bar);

        // Remove from the containing SystemInfo
        SystemInfo system = getSystemOf(bar, sheet);
        if (system == null) {
            return;
        } else {
            system.getBars().remove(bar);
        }

        // Remove from the containing Measure
        System scoreSystem = system.getScoreSystem();
        for (Iterator it = scoreSystem.getStaves().iterator(); it.hasNext();) {
            Stave stave = (Stave) it.next();
            if (isStaveEmbraced (stave, bar)) {
                for (Iterator mit = stave.getMeasures().iterator();
                     mit.hasNext();) {
                    Measure measure = (Measure) mit.next();
                    for (Iterator bit = measure.getInfos().iterator();
                         bit.hasNext();) {
                        BarInfo info = (BarInfo) bit.next();
                        if (info == bar) {
                            // Remove the bar info
                            if (logger.isDebugEnabled()) {
                                logger.debug("Removing " + info +
                                             " from " + measure);
                            }
                            bit.remove();

                            // Remove measure as well ?
                            if (measure.getInfos().size() == 0) {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Removing " + measure);
                                }
                                mit.remove();
                            }

                            break;
                        }
                    }
                }
            }
        }

        // Update the glyph board
        if (glyphBoard != null) {
            glyphBoard.update(bar.getStick());
        }

        // Update the view accordingly
        if (lagView != null) {
            lagView.colorize();
            lagView.repaint();
        }
    }

    //----------//
    // getBarOf //
    //----------//
    private BarInfo getBarOf (Glyph glyph)
    {
        for (BarInfo bar : bars) {
            if (bar.getStick() == glyph) {
                return bar;
            }
        }

        logger.warning("Cannot find bar for " + glyph);
        return null;
    }

    //------------------//
    // retrieveBarLines //
    //------------------//
    /**
     * From the list of vertical sticks, this method uses several tests
     * based on stick location, and stick shape (the test is based on
     * adjacency, it should be improved), to detect true bar lines.
     *
     * <p> The output is thus a filled 'bars' list of bar lines, and the
     * list of SystemInfos which describe the parameters of each
     * system.
     *
     * @throws ProcessingException Raised when a sanity check on systems
     *                             found has failed
     */
    private void retrieveBarLines ()
        throws ProcessingException
    {
        // The list of candidate vertical sticks
        clutter = new ArrayList<Stick>(barsArea.getSticks());
        if (logger.isDebugEnabled()) {
            logger.debug(clutter.size() + " sticks to check");
        }

        // A way to tell the System for each stave, by providing the stave
        // index of the starting stave of the containing system.
        int[] starts = new int[sheet.getStaves().size()];

        for (int i = starts.length - 1; i >= 0; i--) {
            starts[i] = -1;
        }

        suite = new BarCheckSuite();

        double minResult = constants.minCheckResult.getValue();

        // Check each candidate stick in turn
        for (Stick stick : clutter) {
            // Allocate the candidate context, and pass the whole check
            // suite
            Context context = new Context(stick);
            double res = suite.pass(context);

            if (logger.isDebugEnabled()) {
                logger.debug("suite => " + res + " for " + stick);
            }

            if (res >= minResult) {
                // OK, we insert this candidate stick as a true bars
                // member.
                bars.add(new BarInfo(stick, context.topIdx, context.botIdx));

                // Bars that define a system (they start AND end with
                // staves limits)
                if ((context.topIdx != -1) && (context.botIdx != -1)) {
                    for (int i = context.topIdx; i <= context.botIdx; i++) {
                        if (starts[i] == -1) {
                            starts[i] = context.topIdx;
                        }
                    }

                    stick.setResult(BAR_SYSTEM_DEFINING);

                    if (logger.isDebugEnabled()) {
                        logger.debug("System-defining Bar line from stave "
                                     + context.topIdx + " to stave "
                                     + context.botIdx + " " + stick);
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Non-System-defining Bar line "
                                     + ((context.topIdx != -1)
                                        ? (" topIdx=" + context.topIdx)
                                        : "")
                                     + ((context.botIdx != -1)
                                        ? (" botIdx=" + context.botIdx)
                                        : ""));
                    }

                    stick.setResult(BAR_NOT_SYSTEM_DEFINING);
                }
            }
        }

        // Sanity check on the systems found
        for (int i = 0; i < starts.length; i++) {
            if (logger.isDebugEnabled()) {
                logger.debug("stave " + i + " system " + starts[i]);
            }

            if (starts[i] == -1) {
                logger.warning("No system found for stave " + i);
                throw new ProcessingException();
            }
        }

        // System retrieval
        buildSystemInfos(starts);
    }

    //~ Classes -----------------------------------------------------------

    //-----------//
    // MyLagView //
    //-----------//
    private class MyLagView
        extends StickView<Context>
    {
        //~ Constructors --------------------------------------------------

        private MyLagView (GlyphLag lag)
        {
            super(lag, null, BarsBuilder.this);
        }

        //~ Methods -------------------------------------------------------

        //----------//
        // colorize //
        //----------//
        public void colorize ()
        {
            super.colorize();

            // Determine my view index in the lag views
            final int viewIndex = vLag.getViews().indexOf(this);

            // All remaining vertical sticks clutter
            for (Stick stick : clutter) {
                stick.colorize(lag, viewIndex, Color.red);
            }

            // Recognized bar lines
            for (BarInfo info : bars) {
                info.getStick().colorize(lag, viewIndex, Color.yellow);
            }
        }

        //-------------//
        // renderItems //
        //-------------//
        public void renderItems (Graphics g)
        {
            Zoom z = getZoom();

            // Render all physical info known so far, which is just the
            // staff line info, lineset by lineset
            sheet.render(g, z);

            // Draw the contour of bar lines
            for (BarInfo info : bars) {
                info.getStick().renderContour(g, z);
            }
        }

        //---------------//
        // glyphSelected //
        //---------------//
        @Override
            protected void glyphSelected (Glyph glyph,
                                          Point pt)
        {
            ///logger.info(getClass() + " glyphSelected " + glyph);
            if (glyph == null) {
                checkMonitor.tellObject(null);
            } else if (glyph instanceof Stick) {
                // To get a fresh suite
                suite = new BarCheckSuite();
                checkBoard.setSuite(suite);

                Stick stick = (Stick) glyph;
                checkMonitor.tellObject(new Context(stick));
            }
        }

        //---------------//
        // deassignGlyph //
        //---------------//
        @Override
            public void deassignGlyph (Glyph glyph)
        {
            if (glyph.getShape() == Shape.THICK_BAR_LINE ||
                glyph.getShape() == Shape.THIN_BAR_LINE) {

                deassignBarGlyph(glyph);
            } else {
                logger.warning("No deassign meant for "
                               + glyph.getShape() + " glyph");
            }
        }
    }

    //----------//
    // TopCheck //
    //----------//
    private class TopCheck
        extends Check<Context>
    {
        //~ Constructors --------------------------------------------------

        protected TopCheck ()
        {
            super("Top",
                  "Check that top of stick is close to top of staff"+
                  " (unit is interline)",
                  constants.maxStaveShiftDyLow.getValue(),
                  constants.maxStaveShiftDyHigh.getValue(),
                  false, null);
        }

        //~ Methods -------------------------------------------------------

        // Retrieve the distance with proper stave border
        protected double getValue (Context context)
        {
            Stick stick = context.stick;
            int start = stick.getStart();

            // Which stave area contains the top of the stick?
            context.topArea = sheet.getStaveIndexAtY(start);

            StaveInfo area = sheet.getStaves().get(context.topArea);

            // How far are we from the start of the stave?
            int staveTop = area.getFirstLine().getLine().yAt(stick.getMidPos());

            double dy = sheet.getScale().pixelsToFrac(Math.abs(staveTop
                                                               - start));

            // Side-effect
            if (dy <= getLow()) {
                context.topIdx = context.topArea;
            }

            return dy;
        }
    }

    //-------------//
    // BottomCheck //
    //-------------//
    private class BottomCheck
        extends Check<Context>
    {
        //~ Constructors --------------------------------------------------

        protected BottomCheck ()
        {
            super("Bottom",
                  "Check that bottom of stick is close to bottom of staff"+
                  " (unit is interline)",
                  constants.maxStaveShiftDyLow.getValue(),
                  constants.maxStaveShiftDyHigh.getValue(),
                  false, null);
        }

        //~ Methods -------------------------------------------------------

        // Retrieve the distance with proper stave border
        protected double getValue (Context context)
        {
            Stick stick = context.stick;
            int stop = stick.getStop();

            // Which stave area contains the bottom of the stick?
            context.bottomArea = sheet.getStaveIndexAtY(stop);

            StaveInfo area = sheet.getStaves().get(context.bottomArea);

            // How far are we from the stop of the stave?
            int staveBottom = area.getLastLine().getLine().yAt(stick
                                                               .getMidPos());

            double dy = sheet.getScale().pixelsToFrac(Math.abs(staveBottom
                                                               - stop));

            // Side-effect
            if (dy <= getLow()) {
                context.botIdx = context.bottomArea;
            }

            return dy;
        }
    }

    //-------------//
    // AnchorCheck //
    //-------------//
    private class AnchorCheck
        extends Check<Context>
    {
        //~ Constructors --------------------------------------------------

        protected AnchorCheck ()
        {
            super("Anchor",
                  "Check that thick bars are top and bottom aligned with staff",
                  0.5, 0.5,
                  true, NOT_STAVE_ANCHORED);
        }

        //~ Methods -------------------------------------------------------

        // Make sure that at least top or bottom are stave anchors, and
        // that both are stave anchors in the case of thick bars.
        protected double getValue (Context context)
        {
            Stick stick = context.stick;
            context.isThick = isThickBar(stick);

            if (context.isThick) {
                if ((context.topIdx != -1) && (context.botIdx != -1)) {
                    return 1;
                }
            } else {
                if ((context.topIdx != -1) || (context.botIdx != -1)) {
                    return 1;
                }
            }

            return 0;
        }
    }

    //----------------//
    // MinLengthCheck //
    //----------------//
    private class MinLengthCheck
        extends Check<Context>
    {
        //~ Constructors --------------------------------------------------

        protected MinLengthCheck ()
        {
            super("MinLength",
                  "Check that stick is as long as staff height"+
                  " (diff is in interline unit)",
                  -constants.maxStaveShiftDyLow.getValue(), 0,
                  true, TOO_SHORT_BAR);
        }

        //~ Methods -------------------------------------------------------

        // Retrieve the length data
        protected double getValue (Context context)
        {
            Stick stick = context.stick;
            int x = stick.getMidPos();
            int height = Integer.MAX_VALUE;

            // Check wrt every stave in the stick range
            for (int i = context.topArea; i <= context.bottomArea; i++) {
                StaveInfo area = sheet.getStaves().get(i);
                height = Math.min(height, area.getHeight());
            }

            return sheet.getScale().pixelsToFrac(stick.getLength() - height);
        }
    }

    //-----------//
    // LeftCheck //
    //-----------//
    private class LeftCheck
        extends Check<Context>
    {
        //~ Constructors --------------------------------------------------

        protected LeftCheck ()
        {
            super("Left",
                  "Check that stick is on the right of staff beginning bar"+
                  " (diff is in interline unit)",
                  0, 0,
                  true, OUTSIDE_STAVE_WIDTH);
        }

        //~ Methods -------------------------------------------------------

        // Retrieve the stick abscissa
        protected double getValue (Context context)
        {
            Stick stick = context.stick;
            int x = stick.getMidPos();
            int dist = Integer.MAX_VALUE;

            // Check wrt every stave in the stick range
            for (int i = context.topArea; i <= context.bottomArea; i++) {
                StaveInfo area = sheet.getStaves().get(i);
                dist = Math.min(dist, x - area.getLeft());
            }

            return sheet.getScale().pixelsToFrac(dist);
        }
    }

    //------------//
    // RightCheck //
    //------------//
    private class RightCheck
        extends Check<Context>
    {
        //~ Constructors --------------------------------------------------

        protected RightCheck ()
        {
            super("Right",
                  "Check that stick is on the left of staff ending bar"+
                  " (diff is in interline unit)",
                  0, 0,
                  true, OUTSIDE_STAVE_WIDTH);
        }

        //~ Methods -------------------------------------------------------

        // Retrieve the stick abscissa
        protected double getValue (Context context)
        {
            Stick stick = context.stick;
            int x = stick.getMidPos();
            int dist = Integer.MAX_VALUE;

            // Check wrt every stave in the stick range
            for (int i = context.topArea; i <= context.bottomArea; i++) {
                StaveInfo area = sheet.getStaves().get(i);
                dist = Math.min(dist, area.getRight() - x);
            }

            return sheet.getScale().pixelsToFrac(dist);
        }
    }

    //---------------//
    // TopChunkCheck //
    //---------------//
    /**
     * Class <code>TopChunkCheck</code> checks for lack of chunk at top
     */
    private class TopChunkCheck
        extends Check<Context>
    {
        //~ Instance variables --------------------------------------------

        // Half-dimensions for window at top, checking for chunks
        private final int nWidth;
        private final int nHeight;

        //~ Constructors --------------------------------------------------

        protected TopChunkCheck ()
        {
            super("TopChunk",
                  "Check there is no big chunck stuck on top of stick"+
                  " (unit is interline squared)",
                  0, 0,
                  false, CHUNK_AT_TOP);

            // Adjust chunk window according to system scale (problem, we
            // have sheet scale and stave scale, not system scale...)
            Scale scale = sheet.getScale();
            nWidth = scale.fracToPixels(constants.chunkWidth);
            nHeight = scale.fracToPixels(constants.chunkHeight);

            int area = 4 * nWidth * nHeight;
            setLowHigh(area * constants.chunkRatioLow.getValue(),
                       area * constants.chunkRatioHigh.getValue());
        }

        //~ Methods -------------------------------------------------------

        protected double getValue (Context context)
        {
            Stick stick = context.stick;

            // Retrieve the stick chunk at top
            return stick.getAliensAtStart(nHeight, nWidth);
        }
    }

    //------------------//
    // BottomChunkCheck //
    //------------------//
    /**
     * Class <code>BottomChunkCheck</code> checks for lack of chunk at
     * bottom
     */
    private class BottomChunkCheck
        extends Check<Context>
    {
        //~ Instance variables --------------------------------------------

        // Half-dimensions for window at bottom, checking for chunks
        private final int nWidth;
        private final int nHeight;

        //~ Constructors --------------------------------------------------

        protected BottomChunkCheck ()
        {
            super("BotChunk",
                  "Check there is no big chunck stuck on bottom of stick"+
                  " (unit is interline squared)",
                  0, 0, false, CHUNK_AT_BOTTOM);

            // Adjust chunk window according to system scale (problem, we
            // have sheet scale and stave scale, not system scale...)
            Scale scale = sheet.getScale();
            nWidth = scale.fracToPixels(constants.chunkWidth);
            nHeight = scale.fracToPixels(constants.chunkHeight);

            int area = 4 * nWidth * nHeight;
            setLowHigh(area * constants.chunkRatioLow.getValue(),
                       area * constants.chunkRatioHigh.getValue());
        }

        //~ Methods -------------------------------------------------------

        protected double getValue (Context context)
        {
            Stick stick = context.stick;

            // Retrieve the stick chunk at bottom
            return stick.getAliensAtStop(nHeight, nWidth);
        }
    }

    //--------------------//
    // LeftAdjacencyCheck //
    //--------------------//
    private static class LeftAdjacencyCheck
        extends Check<Context>
    {
        //~ Constructors --------------------------------------------------

        protected LeftAdjacencyCheck ()
        {
            super("LeftAdj",
                  "Check that left side of the stick is open enough"+
                  " (dimension-less)",
                  constants.maxAdjacencyLow.getValue(),
                  constants.maxAdjacencyHigh.getValue(),
                  false, TOO_HIGH_ADJACENCY);
        }

        //~ Methods -------------------------------------------------------

        // Retrieve the adjacency value
        protected double getValue (Context context)
        {
            Stick stick = context.stick;
            int length = stick.getLength();

            return (double) stick.getFirstStuck() / (double) length;
        }
    }

    //---------------------//
    // RightAdjacencyCheck //
    //---------------------//
    private static class RightAdjacencyCheck
        extends Check<Context>
    {
        //~ Constructors --------------------------------------------------

        protected RightAdjacencyCheck ()
        {
            super("RightAdj",
                  "Check that right side of the stick is open enough"+
                  " (dimension-less)",
                  constants.maxAdjacencyLow.getValue(),
                  constants.maxAdjacencyHigh.getValue(),
                  false, TOO_HIGH_ADJACENCY);
        }

        //~ Methods -------------------------------------------------------

        // Retrieve the adjacency value
        protected double getValue (Context context)
        {
            Stick stick = context.stick;
            int length = stick.getLength();

            return (double) stick.getLastStuck() / (double) length;
        }
    }

    //---------//
    // Context //
    //---------//
    private class Context
        implements Checkable
    {
        //~ Instance variables --------------------------------------------

        Stick   stick;
        int     topArea    = -1;
        int     bottomArea = -1;
        int     topIdx     = -1;
        int     botIdx     = -1;
        boolean isThick;

        //~ Constructors --------------------------------------------------

        public Context (Stick stick)
        {
            this.stick = stick;
        }

        //~ Methods -------------------------------------------------------

        public void setResult (Result result)
        {
            stick.setResult(result);
        }
    }

    //---------------//
    // BarCheckSuite //
    //---------------//
    private class BarCheckSuite
        extends CheckSuite<Context>
    {
        //~ Constructors --------------------------------------------------

        public BarCheckSuite ()
        {
            super("Bar", constants.minCheckResult.getValue());

            // Be very careful with check order, because of side-effects
            add(1, new TopCheck());
            add(1, new BottomCheck());
            add(1, new MinLengthCheck());
            add(1, new AnchorCheck());
            add(1, new LeftCheck());
            add(1, new RightCheck());
            add(1, new TopChunkCheck());
            add(1, new BottomChunkCheck());
            add(1, new LeftAdjacencyCheck());
            add(1, new RightAdjacencyCheck());

            if (logger.isDebugEnabled()) {
                dump();
            }
        }
    }

    private static class Constants
        extends ConstantSet
    {
        //~ Instance variables --------------------------------------------

        Scale.Fraction chunkHeight = new Scale.Fraction
                (0.33,
                 "Height of half area to look for chunks");

        Constant.Double chunkRatioLow = new Constant.Double
                (0.25,
                 "LowMinimum ratio of alien pixels to detect chunks");

        Constant.Double chunkRatioHigh = new Constant.Double
                (0.25,
                 "HighMinimum ratio of alien pixels to detect chunks");

        Scale.Fraction chunkWidth = new Scale.Fraction
                (0.33,
                 "Width of half area to look for chunks");

        Constant.Boolean displayFrame = new Constant.Boolean
                (false,
                 "Should we display a frame on the vertical sticks");

        Scale.Fraction maxAlignShiftDx = new Scale.Fraction
                (0.2,
                 "Maximum horizontal shift in bars between staves in a system");

        Constant.Double maxAdjacencyLow = new Constant.Double
                (0.25d,
                 "LowMaximum adjacency ratio for a bar stick");

        Constant.Double maxAdjacencyHigh = new Constant.Double
                (0.25d,
                 "HighMaximum adjacency ratio for a bar stick");

        Scale.Fraction maxBarOffset = new Scale.Fraction
                (1.0,
                 "Vertical offset used to detect that a bar extends past a stave");

        Constant.Integer maxDeltaLength = new Constant.Integer
                (4,
                 "Maximum difference in run length to be part of the same section");

        Scale.Fraction maxDoubleBarDx = new Scale.Fraction
                (0.75,
                 "Maximum horizontal distance between the two bars of a double bar");

        Scale.Fraction maxStaveShiftDyLow = new Scale.Fraction
                (0.125,
                 "LowMaximum vertical distance between a bar edge and the stave line");

        Scale.Fraction maxStaveShiftDyHigh = new Scale.Fraction
                (10,
                 "HighMaximum vertical distance between a bar edge and the stave line");

        Scale.Fraction maxBarThickness = new Scale.Fraction
                (0.75,
                 "Maximum thickness of an interesting vertical stick");

        Scale.Fraction maxThinWidth = new Scale.Fraction
                (0.3,
                 "Maximum width of a normal bar, versus a thick bar");

        Scale.Fraction minMeasureWidth = new Scale.Fraction
                (0.75,
                 "Minimum width for a measure");

        Scale.Fraction minForeWeight = new Scale.Fraction
                (1.25,
                 "Minimum foreground weight for a section to be kept");

        Constant.Double minCheckResult = new Constant.Double
                (0.50,
                 "Minimum result for suite of check");

        Constants ()
        {
            initialize();
        }
    }
}
