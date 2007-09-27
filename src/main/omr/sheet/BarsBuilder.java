//----------------------------------------------------------------------------//
//                                                                            //
//                           B a r s B u i l d e r                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.Main;

import omr.check.CheckBoard;
import omr.check.CheckSuite;
import omr.check.FailureResult;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphModel;
import omr.glyph.GlyphSection;
import omr.glyph.GlyphsBuilder;
import omr.glyph.Shape;
import omr.glyph.ui.GlyphBoard;
import omr.glyph.ui.GlyphLagView;

import omr.lag.JunctionDeltaPolicy;
import omr.lag.RunBoard;
import omr.lag.ScrollLagView;
import omr.lag.SectionBoard;
import omr.lag.SectionsBuilder;
import omr.lag.VerticalOrientation;

import omr.score.Barline;
import omr.score.Measure;
import omr.score.Score;
import omr.score.ScoreConstants;
import omr.score.ScorePart;
import omr.score.Staff;
import omr.score.System;
import omr.score.SystemPart;
import omr.score.UnitDimension;
import omr.score.visitor.ScoreFixer;
import omr.score.visitor.SheetPainter;

import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.selection.SelectionTag;
import static omr.selection.SelectionTag.*;

import omr.step.StepException;

import omr.stick.Stick;
import omr.stick.StickSection;

import omr.ui.BoardsPane;
import omr.ui.GuiActions;
import omr.ui.PixelBoard;
import static omr.ui.field.SpinnerUtilities.*;

import omr.util.Dumper;
import omr.util.Logger;
import omr.util.TreeNode;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Class <code>BarsBuilder</code> handles the vertical lines that are recognized
 * as bar lines. This class uses a dedicated companion named {@link
 * omr.sheet.BarsChecker} which handles physical checks.
 *
 * <p> Input is provided by a list of vertical sticks retrieved from the
 * vertical lag.
 *
 * <p> Output is the collection of detected Bar lines.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class BarsBuilder
    extends GlyphModel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BarsBuilder.class);

    /** Failure */
    private static final FailureResult NOT_SYSTEM_ALIGNED = new FailureResult(
        "Bar-NotSystemAligned");
    private static final FailureResult NOT_STAFF_ALIGNED = new FailureResult(
        "Bar-NotStaffAligned");
    private static final FailureResult SHORTER_THAN_STAFF_HEIGHT = new FailureResult(
        "Bar-ShorterThanStaffHeight");
    private static final FailureResult THICK_BAR_NOT_ALIGNED = new FailureResult(
        "Bar-ThickBarNotAligned");
    private static final FailureResult CANCELLED = new FailureResult(
        "Bar-Cancelled");

    //~ Instance fields --------------------------------------------------------

    /** Companion physical stick checker */
    private BarsChecker checker;

    /** Lag view on bars, if so desired */
    private GlyphLagView lagView;

    /** List of found bar sticks */
    private List<Stick> bars = new ArrayList<Stick>();

    /** Unused vertical sticks */
    private List<Stick> clutter;

    /** Sheet scale */
    private Scale scale;

    /** Related score */
    private Score score;

    /** Bars area, with retrieved vertical sticks */
    private VerticalArea barsArea;

    //~ Constructors -----------------------------------------------------------

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
        super(sheet, new GlyphLag("vLag", new VerticalOrientation()));
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // assignBraces //
    //--------------//
    /**
     * Pass the braces symbols found, so that score parts can be defined
     *
     * @param braceLists the braces, system per system
     */
    public void assignBraces (List<List<Glyph>> braceLists)
    {
        int is = 0;

        // Build the SystemParts for each system
        for (SystemInfo systemInfo : sheet.getSystems()) {
            setSystemBraces(systemInfo.getScoreSystem(), braceLists.get(is++));
        }

        // Repaint the score view, if any (TBI)
        if (sheet.getScore()
                 .getView() != null) {
            sheet.getScore()
                 .getView()
                 .repaint();
        }
    }

    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Retrieve and store the bars information on the provided sheet
     *
     * @throws StepException raised when step processing must stop, due to
     *                             encountered error
     */
    public void buildInfo ()
        throws StepException
    {
        // Stuff to be made available
        scale = sheet.getScale();
        sheet.getHorizontals();

        try {
            // Populate the vertical lag of runs
            lag.setVertexClass(StickSection.class);

            SectionsBuilder<GlyphLag, GlyphSection> lagBuilder;
            lagBuilder = new SectionsBuilder<GlyphLag, GlyphSection>(
                lag,
                new JunctionDeltaPolicy(
                    scale.toPixels(constants.maxDeltaLength)));
            lagBuilder.createSections(sheet.getPicture(), 0); // 0 = minRunLength
            sheet.setVerticalLag(lag);

            // Retrieve (vertical) sticks
            barsArea = new VerticalArea(
                sheet,
                lag,
                scale.toPixels(constants.maxBarThickness));
            clutter = new ArrayList<Stick>(barsArea.getSticks());

            // Allocate score
            createScore();

            // Delegate to BarsChecker companion
            checker = new BarsChecker(sheet);
            checker.retrieveMeasures(clutter, bars);

            // Check Measures using only score parameters
            checkMeasures();

            // Define score parts
            defineScoreParts();

            // Remove clutter glyphs from lag (they will be handled as specific
            // glyphs in the user view).
            for (Stick stick : clutter) {
                stick.destroy( /* cutSections => */
                false);
            }

            // Erase bar pixels from picture (not used for the time being)
            //////eraseBars();

            // Update score internal data
            score.accept(new ScoreFixer());

            if (logger.isFineEnabled()) {
                score.dump();
            }

            // Report number of systems & measures retrieved
            StringBuilder sb = new StringBuilder();
            sb.append(sheet.getSystems().size())
              .append(" systems");

            int nb = score.getLastSystem()
                          .getLastPart()
                          .getLastMeasure()
                          .getId();

            if (nb > 0) {
                sb.append(", ")
                  .append(nb)
                  .append(" measure");

                if (nb > 1) {
                    sb.append("s");
                }
            } else {
                sb.append(", no measure found");
            }

            logger.info(sb.toString());

            // Split everything, including horizontals, per system
            SystemSplit.computeSystemLimits(sheet);
            SystemSplit.splitHorizontals(sheet);
            SystemSplit.splitVerticalSections(sheet);

            // Assign the bar stick to the proper system glyphs collection
            GlyphsBuilder glyphsBuilder = sheet.getGlyphsBuilder();

            for (Stick stick : bars) {
                glyphsBuilder.insertGlyph(
                    stick,
                    sheet.getSystemAtY(stick.getContourBox().y));
            }
        } finally {
            // Display the resulting stickarea if so asked for
            if (constants.displayFrame.getValue() && (Main.getGui() != null)) {
                displayFrame();
            }
        }
    }

    //--------------------//
    // deassignGlyphShape //
    //--------------------//
    /**
     * Remove a bar together with all its related entities. This means removing
     * reference in the bars list of this builder, reference in the containing
     * SystemInfo, reference in the Measure it ends, and removing this Measure
     * itself if this (false) bar was the only ending bar left for the
     * Measure. The related stick must also be assigned a failure result.
     *
     * @param glyph the (false) bar glyph to deassign
     * @param record true if this action is to be recorded in the script
     */
    @Override
    public void deassignGlyphShape (Glyph   glyph,
                                    boolean record)
    {
        if ((glyph.getShape() == Shape.THICK_BAR_LINE) ||
            (glyph.getShape() == Shape.THIN_BAR_LINE)) {
            Stick bar = getBarOf(glyph);

            if (bar == null) {
                return;
            } else {
                logger.info("Deassigning a " + glyph.getShape());
            }

            // Related stick has to be freed
            bar.setShape(null);
            bar.setResult(CANCELLED);

            // Remove from the internal all-bars list
            bars.remove(bar);

            // Remove from the containing SystemInfo
            SystemInfo system = checker.getSystemOf(bar, sheet);

            if (system == null) {
                return;
            }

            // Remove from the containing Measure
            System scoreSystem = system.getScoreSystem();

            for (TreeNode pNode : scoreSystem.getParts()) {
                SystemPart part = (SystemPart) pNode;

                if (checker.isPartEmbraced(part, bar)) {
                    for (Iterator mit = part.getMeasures()
                                            .iterator(); mit.hasNext();) {
                        Measure measure = (Measure) mit.next();

                        for (Iterator sit = measure.getBarline()
                                                   .getSticks()
                                                   .iterator(); sit.hasNext();) {
                            Stick stick = (Stick) sit.next();

                            if (stick == bar) {
                                // Remove the bar stick
                                if (logger.isFineEnabled()) {
                                    logger.fine(
                                        "Removing " + stick + " from " +
                                        measure);
                                }

                                sit.remove();

                                // Remove measure as well ?
                                if (measure.getBarline()
                                           .getSticks()
                                           .size() == 0) {
                                    if (logger.isFineEnabled()) {
                                        logger.fine("Removing " + measure);
                                    }

                                    mit.remove();
                                }

                                break;
                            }
                        }
                    }
                }
            }

            assignGlyphShape(glyph, null, false);

            // Update score internal data
            score.accept(new ScoreFixer());

            // Update the view accordingly
            if (lagView != null) {
                lagView.colorize();
                lagView.repaint();
            }
        } else {
            BarsBuilder.logger.warning(
                "No deassign meant for " + glyph.getShape() + " glyph");
        }
    }

    //------------------//
    // deassignSetShape //
    //------------------//
    /**
     * Remove a set of bars
     *
     * @param glyphs the collection of glyphs to be de-assigned
     * @param record true if this action is to be recorded in the script
     */
    @Override
    public void deassignSetShape (Collection<Glyph> glyphs,
                                  boolean           record)
    {
        for (Glyph glyph : glyphs) {
            deassignGlyphShape(glyph, false);
        }
    }

    //------------------//
    // defineScoreParts //
    //------------------//
    /**
     * From system part, define the score parts
     * @throws StepException
     */
    public void defineScoreParts ()
        throws StepException
    {
        // First, make sure all the system are consistent wrt parts
        Integer NbOfParts = null;

        for (SystemInfo systemInfo : sheet.getSystems()) {
            int nb = systemInfo.getScoreSystem()
                               .getParts()
                               .size();

            if (NbOfParts == null) {
                NbOfParts = nb;
            } else if (NbOfParts != nb) {
                String msg = "Systems with different number of parts: " +
                             NbOfParts + " vs " + nb;
                logger.warning(msg);

                if (Main.getGui() != null) {
                    GuiActions.displayWarning(msg);
                }

                throw new StepException(msg);
            }
        }

        // (Re)set the global ScorePart list accordingly
        List<ScorePart> partList = null;
        boolean         ok = true;

        for (SystemInfo systemInfo : sheet.getSystems()) {
            logger.fine(systemInfo.getScoreSystem().toString());

            if (partList == null) {
                // Build a ScorePart list based on the SystemPart list
                partList = new ArrayList<ScorePart>();

                for (TreeNode node : systemInfo.getScoreSystem()
                                               .getParts()) {
                    SystemPart sp = (SystemPart) node;
                    ScorePart  scorePart = new ScorePart(sp, score);
                    logger.fine("Adding " + scorePart);
                    partList.add(scorePart);
                }
            } else {
                // Check our ScorePart list is still ok
                int i = 0;

                for (TreeNode node : systemInfo.getScoreSystem()
                                               .getParts()) {
                    SystemPart sp = (SystemPart) node;
                    ScorePart  global = partList.get(i++);
                    ScorePart  scorePart = new ScorePart(sp, score);
                    logger.fine(
                        "Comparing global " + global + " with " + scorePart);

                    if (!global.equals(scorePart)) {
                        logger.warning("Different SystemPart in system " + i);
                        ok = false;
                    }
                }
            }
        }

        if (ok) {
            // Assign id and names (TBI)
            int index = 0;

            for (ScorePart part : partList) {
                part.setId(++index);
                part.setName("Part_" + index);

                if (logger.isFineEnabled()) {
                    logger.fine("Global " + part);
                }
            }

            // This is now the global score part list
            score.setPartList(partList);

            // Link the SystemPart instances to their corresponding ScorePart
            for (TreeNode node : score.getSystems()) {
                System system = (System) node;

                for (TreeNode n : system.getParts()) {
                    SystemPart sp = (SystemPart) n;
                    sp.setScorePart(score.getPartList().get(sp.getId() - 1));
                }
            }
        }
    }

    //--------------------//
    // checkBarAlignments //
    //--------------------//
    /**
     * Check alignment of each measure of each part with the other part
     * measures, a test that needs several staves in the system
     *
     * @param system the system to check
     */
    private void checkBarAlignments (omr.score.System system)
    {
        if (system.getInfo()
                  .getStaves()
                  .size() > 1) {
            int maxShiftDx = scale.toPixels(constants.maxAlignShiftDx);

            for (Iterator pit = system.getParts()
                                      .iterator(); pit.hasNext();) {
                SystemPart part = (SystemPart) pit.next();

                for (Iterator mit = part.getMeasures()
                                        .iterator(); mit.hasNext();) {
                    Measure           measure = (Measure) mit.next();

                    // Check that all staves in this part are concerned with
                    // one stick of the barline
                    Collection<Staff> staves = new ArrayList<Staff>();

                    for (TreeNode node : part.getStaves()) {
                        staves.add((Staff) node);
                    }

                    if (!measure.getBarline()
                                .joinsAllStaves(staves)) {
                        // Remove the false bar info
                        for (Stick stick : measure.getBarline()
                                                  .getSticks()) {
                            stick.setResult(NOT_STAFF_ALIGNED);
                            stick.setShape(null);
                            bars.remove(stick);
                        }

                        // Remove the false measure
                        mit.remove();

                        break;
                    }

                    // Compare the abscissa with corresponding position in
                    // the other parts
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            system.getContextString() +
                            " Checking measure alignment at x: " +
                            measure.getLeftX());
                    }

                    int x = measure.getBarline()
                                   .getCenter().x;

                    for (Iterator it = system.getParts()
                                             .iterator(); it.hasNext();) {
                        SystemPart prt = (SystemPart) it.next();

                        if (prt == part) {
                            continue;
                        }

                        if (!prt.barlineExists(x, maxShiftDx)) {
                            if (logger.isFineEnabled()) {
                                logger.fine(
                                    "Singular measure removed: " +
                                    Dumper.dumpOf(measure));
                            }

                            // Remove the false bar info
                            for (Stick stick : measure.getBarline()
                                                      .getSticks()) {
                                stick.setResult(NOT_SYSTEM_ALIGNED);
                                bars.remove(stick);
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
     * Use ending bar line if any, to adjust the right abscissa of the system
     * and its staves.
     *
     * @param system the system to check
     */
    private void checkEndingBar (omr.score.System system)
    {
        try {
            SystemPart part = system.getFirstPart();
            Measure    measure = part.getLastMeasure();
            Barline    barline = measure.getBarline();
            int        lastX = barline.getRightX();
            int        minWidth = scale.toPixels(constants.minMeasureWidth);

            if ((part.getFirstStaff()
                     .getWidth() - lastX) < minWidth) {
                if (logger.isFineEnabled()) {
                    logger.fine("Adjusting EndingBar " + system);
                }

                // Adjust end of system & staff(s) to this one
                UnitDimension dim = system.getDimension();

                if (dim == null) {
                    system.setDimension(new UnitDimension(lastX, 0));
                } else {
                    dim.width = lastX;
                }

                for (Iterator pit = system.getParts()
                                          .iterator(); pit.hasNext();) {
                    SystemPart prt = (SystemPart) pit.next();

                    for (Iterator sit = prt.getStaves()
                                           .iterator(); sit.hasNext();) {
                        Staff stv = (Staff) sit.next();
                        stv.setWidth(system.getDimension().width);
                    }
                }
            }
        } catch (Exception ex) {
            logger.warning(
                system.getContextString() + " Error in checking ending bar",
                ex);
        }
    }

    //---------------//
    // checkMeasures //
    //---------------//
    /**
     * Check measure reality, using a set of additional tests.
     */
    private void checkMeasures ()
    {
        // Check are performed on a system basis
        for (TreeNode node : score.getSystems()) {
            omr.score.System system = (omr.score.System) node;

            // Check alignment of each measure of each staff with the other
            // staff measures, a test that needs several staves in the system
            checkBarAlignments(system);

            // Detect very narrow measures which in fact indicate double bar
            // lines.
            mergeBarlines(system);

            // First barline may be just the beginning of the staff, so do not
            // count the very first bar line, which in general defines the
            // beginning of the staff rather than the end of a measure, but use
            // it to precisely define the left abscissa of the system and all
            // its contained staves.
            removeStartingBar(system);

            // Similarly, use the very last bar line, which generally ends the
            // system, to define the right abscissa of the system and its
            // staves.
            checkEndingBar(system);
        }
    }

    //-------------//
    // createScore //
    //-------------//
    private void createScore ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("Allocating score");
        }

        score = new Score(
            scale.toUnits(
                new PixelDimension(sheet.getWidth(), sheet.getHeight())),
            (int) Math.rint(sheet.getSkew().angle() * ScoreConstants.BASE),
            scale,
            sheet.getPath());

        // Mutual referencing
        score.setSheet(sheet);
        sheet.setScore(score);
    }

    //--------------//
    // displayFrame //
    //--------------//
    private void displayFrame ()
    {
        Selection glyphSelection = sheet.getSelection(VERTICAL_GLYPH);
        lagView = new MyLagView(lag);
        lagView.setGlyphSelection(glyphSelection);
        glyphSelection.addObserver(lagView);
        lagView.colorize();

        final String  unit = sheet.getRadix() + ":BarsBuilder";
        BoardsPane    boardsPane = new BoardsPane(
            sheet,
            lagView,
            new PixelBoard(unit),
            new RunBoard(unit, sheet.getSelection(VERTICAL_RUN)),
            new SectionBoard(
                unit,
                lag.getLastVertexId(),
                sheet.getSelection(VERTICAL_SECTION),
                sheet.getSelection(VERTICAL_SECTION_ID)),
            new GlyphBoard(
                unit,
                this,
                clutter,
                sheet.getSelection(VERTICAL_GLYPH),
                sheet.getSelection(VERTICAL_GLYPH_ID),
                sheet.getSelection(GLYPH_SET)),
            new MyCheckBoard(
                unit,
                checker.getSuite(),
                sheet.getSelection(VERTICAL_GLYPH)));

        // Create a hosting frame for the view
        ScrollLagView slv = new ScrollLagView(lagView);
        sheet.getAssembly()
             .addViewTab("Bars", slv, boardsPane);
    }

    //----------//
    // getBarOf //
    //----------//
    private Stick getBarOf (Glyph glyph)
    {
        for (Stick bar : bars) {
            if (bar == glyph) {
                return bar;
            }
        }

        logger.warning("Cannot find bar for " + glyph);

        return null;
    }

    //---------------//
    // mergeBarlines //
    //---------------//
    /**
     * Check whether two close bar lines are not in fact double lines (with
     * variants)
     *
     * @param system the system to check
     */
    private void mergeBarlines (omr.score.System system)
    {
        int maxDoubleDx = scale.toPixels(constants.maxDoubleBarDx);

        for (TreeNode node : system.getParts()) {
            SystemPart part = (SystemPart) node;
            Measure    prevMeasure = null;

            for (Iterator mit = part.getMeasures()
                                    .iterator(); mit.hasNext();) {
                Measure measure = (Measure) mit.next();

                if (prevMeasure != null) {
                    final int measureWidth = measure.getBarline()
                                                    .getCenter().x -
                                             prevMeasure.getBarline()
                                                        .getCenter().x;

                    if (measureWidth <= maxDoubleDx) {
                        // Lines are side by side or one above the other?
                        Stick stick = (Stick) measure.getBarline()
                                                     .getSticks()
                                                     .toArray()[0];
                        Stick prevStick = (Stick) prevMeasure.getBarline()
                                                             .getSticks()
                                                             .toArray()[0];

                        if (stick.overlapWith(prevStick)) {
                            // Overlap => side by side
                            // Merge the two bar lines into the first one
                            prevMeasure.getBarline()
                                       .mergeWith(measure.getBarline());

                            if (logger.isFineEnabled()) {
                                logger.fine(
                                    "Merged two close barlines into " +
                                    prevMeasure.getBarline());
                            }
                        } else {
                            // No overlap => one above the other
                            //                            prevStick.addGlyphSections(stick, true);
                            //                            stick.destroy(false);
                            //                            bars.remove(stick);
                            if (logger.isFineEnabled()) {
                                logger.fine(
                                    "Two barlines segments one above the other in  " +
                                    measure.getBarline());
                            }
                        }

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
     * We associate measures only with their ending bar line(s), so the starting
     * bar of a staff does not end a measure, we thus have to remove the measure
     * that we first had associated with it.
     *
     * @param system the system whose staves starting measure has to be checked
     */
    private void removeStartingBar (omr.score.System system)
    {
        int     minWidth = scale.toPixels(constants.minMeasureWidth);
        Barline firstBarline = system.getFirstPart()
                                     .getFirstMeasure()
                                     .getBarline();
        int     firstX = firstBarline.getLeftX();

        // Check is based on the width of this first measure
        if (firstX < minWidth) {
            // Adjust system parameters if needed : topLeft and dimension
            if (firstX != 0) {
                if (logger.isFineEnabled()) {
                    logger.fine("Adjusting firstX=" + firstX + " " + system);
                }

                system.getTopLeft()
                      .translate(firstX, 0);
                system.getDimension().width -= firstX;
            }

            // Adjust beginning of all staves to this one
            // Remove this false "measure" in all parts of the system
            for (TreeNode node : system.getParts()) {
                SystemPart part = (SystemPart) node;

                // Set the bar as starting bar for the staff
                Measure measure = part.getFirstMeasure();
                part.setStartingBarline(measure.getBarline());

                // Remove this first measure
                part.getMeasures()
                    .remove(0);

                // Update abscissa of top-left corner of every staff
                for (TreeNode sNode : part.getStaves()) {
                    Staff staff = (Staff) sNode;
                    staff.getTopLeft()
                         .translate(firstX, 0);
                }

                // Update other bar lines abscissae accordingly
                for (TreeNode mNode : part.getMeasures()) {
                    Measure meas = (Measure) mNode;
                    meas.resetAbscissae();
                }
            }
        }
    }

    //-----------------//
    // setSystemBraces //
    //-----------------//
    /**
     * Pass the braces symbols found for one system
     *
     * @param braces list of braces for this system
     */
    private void setSystemBraces (System      system,
                                  List<Glyph> braces)
    {
        //        // Map Staff -> its containing staves ensemble (= ScorePart)
        //        Map<Staff, List<Staff>> ensembles = new HashMap<Staff, List<Staff>>();
        //
        //        // Inspect each brace in turn
        //        for (Glyph brace : braces) {
        //            List<Staff> ensemble = new ArrayList<Staff>();
        //
        //            // Inspect all staves for this brace
        //            for (TreeNode node : system.getStaves()) {
        //                Staff staff = (Staff) node;
        //
        //                if (checker.isStaffEmbraced(staff, brace)) {
        //                    ensemble.add(staff);
        //                    ensembles.put(staff, ensemble);
        //                }
        //            }
        //
        //            if (ensemble.size() == 0) {
        //                logger.warning(
        //                    "Brace with no embraced staves at all: " + brace.getId());
        //            }
        //        }
        //
        //        // Now build the parts by looking back at all staves
        //        List<SystemPart> parts = new ArrayList<SystemPart>();
        //        List<Staff>      currentEnsemble = null;
        //
        //        for (TreeNode node : system.getStaves()) {
        //            Staff       staff = (Staff) node;
        //            List<Staff> ensemble = ensembles.get(staff);
        //
        //            if (ensemble == null) {
        //                // Standalone staff, a part by itself
        //                parts.add(new SystemPart(Arrays.asList(staff)));
        //            } else {
        //                // Staff is in a part
        //                if (ensemble != currentEnsemble) {
        //                    parts.add(new SystemPart(ensemble));
        //                } else {
        //                    // Nothing to do
        //                }
        //            }
        //
        //            currentEnsemble = ensemble;
        //        }
        //
        //        // Dump this system parts
        //        if (logger.isFineEnabled()) {
        //            StringBuilder sb = new StringBuilder();
        //
        //            for (SystemPart part : parts) {
        //                sb.append("[");
        //
        //                for (Staff staff : part.getStaves()) {
        //                    sb.append(" ")
        //                      .append(staff.getStaffIndex());
        //                }
        //
        //                sb.append("] ");
        //            }
        //
        //            logger.fine(system + " Parts: " + sb);
        //        }
        //
        //        // Assign the parts to the system
        //        system.setParts(parts);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        /** Should we display a frame on the vertical sticks */
        Constant.Boolean displayFrame = new Constant.Boolean(
            false,
            "Should we display a frame on the vertical sticks");

        /** Maximum horizontal shift in bars between staves in a system */
        Scale.Fraction maxAlignShiftDx = new Scale.Fraction(
            0.2,
            "Maximum horizontal shift in bars between staves in a system");

        /** Maximum thickness of an interesting vertical stick */
        Scale.Fraction maxBarThickness = new Scale.Fraction(
            0.75,
            "Maximum thickness of an interesting vertical stick");

        /** Maximum difference in run length to be part of the same section */
        Scale.Fraction maxDeltaLength = new Scale.Fraction(
            0.2,
            "Maximum difference in run length to be part of the same section");

        /** Maximum horizontal distance between the two bars of a double bar */
        Scale.Fraction maxDoubleBarDx = new Scale.Fraction(
            0.75,
            "Maximum horizontal distance between the two bars of a double bar");

        /** Minimum width for a measure */
        Scale.Fraction minMeasureWidth = new Scale.Fraction(
            0.75,
            "Minimum width for a measure");
    }

    //--------------//
    // MyCheckBoard //
    //--------------//
    private class MyCheckBoard
        extends CheckBoard<BarsChecker.Context>
    {
        public MyCheckBoard (String                          unit,
                             CheckSuite<BarsChecker.Context> suite,
                             Selection                       inputSelection)
        {
            super(unit, suite, inputSelection);
        }

        public void update (Selection     selection,
                            SelectionHint hint)
        {
            BarsChecker.Context context = null;
            Object              entity = selection.getEntity();

            if (entity instanceof Stick) {
                // To get a fresh suite
                setSuite(checker.new BarCheckSuite());

                Stick stick = (Stick) entity;
                context = new BarsChecker.Context(stick);
            }

            tellObject(context);
        }
    }

    //-----------//
    // MyLagView //
    //-----------//
    private class MyLagView
        extends GlyphLagView
    {
        private MyLagView (GlyphLag lag)
        {
            super(lag, null, null, BarsBuilder.this, clutter);
            setName("BarsBuilder-View");

            // Pixel
            setLocationSelection(
                sheet.getSelection(SelectionTag.SHEET_RECTANGLE));

            // Glyph set
            Selection glyphSetSelection = sheet.getSelection(
                SelectionTag.GLYPH_SET);
            setGlyphSetSelection(glyphSetSelection);
            glyphSetSelection.addObserver(this);

            // Glyph id
            sheet.getSelection(SelectionTag.VERTICAL_GLYPH_ID)
                 .addObserver(this);
        }

        //----------//
        // colorize //
        //----------//
        public void colorize ()
        {
            super.colorize();

            // Determine my view index in the lag views
            final int viewIndex = lag.viewIndexOf(this);

            // All remaining vertical sticks clutter
            for (Stick stick : clutter) {
                stick.colorize(lag, viewIndex, Color.red);
            }

            // Recognized bar lines
            for (Stick stick : bars) {
                stick.colorize(lag, viewIndex, Color.yellow);
            }
        }

        //-------------//
        // renderItems //
        //-------------//
        public void renderItems (Graphics g)
        {
            // Render all physical info known so far, which is just the staff
            // line info, lineset by lineset
            sheet.accept(new SheetPainter(g, getZoom()));

            super.renderItems(g);
        }
    }
}
