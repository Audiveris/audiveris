//----------------------------------------------------------------------------//
//                                                                            //
//                       M e a s u r e s B u i l d e r                        //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.check.FailureResult;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.log.Logger;

import omr.score.common.PageRectangle;
import omr.score.common.PixelDimension;
import omr.score.common.PixelPoint;
import omr.score.common.UnitDimension;
import omr.score.entity.Barline;
import omr.score.entity.Measure;
import omr.score.entity.ScorePart;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;
import omr.score.visitor.ScoreFixer;

import omr.step.StepException;

import omr.stick.Stick;

import omr.util.Dumper;
import omr.util.TreeNode;

import java.util.*;

/**
 * Class <code>MeasuresBuilder</code> is in charge of building measures on the
 * Score side, from the bar sticks found on the Sheet side
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class MeasuresBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        MeasuresBuilder.class);

    /** Failure, since bar goes higher or lower than the system area */
    private static final FailureResult NOT_WITHIN_SYSTEM = new FailureResult(
        "Bar-NotWithinSystem");
    private static final FailureResult NOT_STAFF_ALIGNED = new FailureResult(
        "Bar-NotStaffAligned");
    private static final FailureResult NOT_SYSTEM_ALIGNED = new FailureResult(
        "Bar-NotSystemAligned");

    //~ Instance fields --------------------------------------------------------

    /** Companion systems builder */
    private final SystemsBuilder systemsBuilder;

    /** Companion bars checker */
    private final BarsChecker barsChecker;

    /** The related sheet */
    private final Sheet sheet;

    /** Sheet scale */
    private final Scale scale;

    /** List of found bar sticks */
    private final List<Stick> barSticks;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new MeasuresBuilder object.
     *
     * @param sheet the related sheet
     */
    public MeasuresBuilder (Sheet sheet)
    {
        this.sheet = sheet;

        systemsBuilder = sheet.getSystemsBuilder();
        barsChecker = systemsBuilder.getBarsChecker();
        barSticks = systemsBuilder.getBarSticks();
        scale = sheet.getScale();
    }

    //~ Methods ----------------------------------------------------------------

    //------------------------//
    // allocateScoreStructure //
    //------------------------//
    /**
     * For each SystemInfo, build the corresponding System entity with all its
     * depending Parts and Staves
     */
    public void allocateScoreStructure ()
        throws StepException
    {
        // Allocate score
        sheet.createScore();

        // Systems
        for (SystemInfo info : sheet.getSystems()) {
            // Allocate the system
            ScoreSystem system = new ScoreSystem(
                info,
                sheet.getScore(),
                scale.toPagePoint(
                    new PixelPoint(info.getLeft(), info.getTop())),
                scale.toUnits(
                    new PixelDimension(info.getWidth(), info.getDeltaY())));

            // Set the link SystemInfo -> System
            info.setScoreSystem(system);

            // Allocate the parts in the system
            for (PartInfo partInfo : info.getParts()) {
                SystemPart part = new SystemPart(system);

                // Allocate the staves in this part
                for (StaffInfo staffInfo : partInfo.getStaves()) {
                    LineInfo line = staffInfo.getFirstLine();
                    new Staff(
                        staffInfo,
                        part,
                        scale.toPagePoint(
                            new PixelPoint(
                                staffInfo.getLeft(),
                                line.yAt(line.getLeft()))),
                        scale.pixelsToUnits(
                            staffInfo.getRight() - staffInfo.getLeft()),
                        64); // Staff vertical size in units);
                }
            }
        }

        // Define score parts
        defineScoreParts();
    }

    //---------------------//
    // buildSystemMeasures //
    //---------------------//
    public void buildSystemMeasures (SystemInfo systemInfo)
    {
        allocateSystemMeasures(systemInfo);
        checkSystemMeasures(systemInfo);
    }

    //------------------------//
    // completeScoreStructure //
    //------------------------//
    public void completeScoreStructure ()
        throws StepException
    {
        // Update score internal data
        sheet.getScore()
             .accept(new ScoreFixer(true));

        reportResults();
    }

    //----------------//
    // isPartEmbraced //
    //----------------//
    /**
     * Check whether the given part is within the vertical range of the given
     * glyph (bar stick or brace glyph)
     *
     * @param part the given part
     * @param glyph the given glyph
     * @return true if part is embraced by the bar
     */
    private boolean isPartEmbraced (SystemPart part,
                                    Glyph      glyph)
    {
        // Extrema of glyph
        PageRectangle box = scale.toUnits(glyph.getContourBox());
        int           top = box.y;
        int           bot = box.y + box.height;

        // Check that part and glyph overlap vertically
        final int topPart = part.getFirstStaff()
                                .getTopLeft().y;
        final int botPart = part.getLastStaff()
                                .getTopLeft().y +
                            part.getLastStaff()
                                .getHeight();

        return Math.max(topPart, top) < Math.min(botPart, bot);
    }

    //------------------------//
    // allocateSystemMeasures //
    //------------------------//
    /**
     * Bar lines are first sorted according to their abscissa, then we run
     * additional checks on each bar line, since we now know its enclosing
     * system. If OK, then we add the corresponding measures in their parts.
     */
    private void allocateSystemMeasures (SystemInfo systemInfo)
    {
        final int         maxDy = scale.toPixels(constants.maxBarOffset);
        final ScoreSystem system = systemInfo.getScoreSystem();

        // Measures building (Sticks are already sorted by increasing abscissa)
        for (Iterator<Glyph> bit = systemInfo.getGlyphs()
                                             .iterator(); bit.hasNext();) {
            Stick bar = (Stick) bit.next();

            // We don't check that the bar does not start before first staff,
            // this is too restrictive because of alternate endings.  We however
            // do check that the bar does not end after last staff of the
            // last part of the system.
            int barAbscissa = bar.getMidPos();
            int systemBottom = system.getLastPart()
                                     .getLastStaff()
                                     .getInfo()
                                     .getLastLine()
                                     .yAt(barAbscissa);

            if ((bar.getStop() - systemBottom) > maxDy) {
                if (logger.isFineEnabled()) {
                    logger.fine("Bar stopping too low");
                }

                bar.setResult(NOT_WITHIN_SYSTEM);

                ///bit.remove();
                continue;
            }

            // We add a measure in each relevant part of this system, provided
            // that the part is embraced by the bar line
            for (TreeNode node : system.getParts()) {
                SystemPart part = (SystemPart) node;

                if (isPartEmbraced(part, bar)) {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            part + " - Creating measure for bar-line " + bar);
                    }

                    Measure measure = new Measure(part);
                    Barline barline = new Barline(measure);
                    bar.setShape(
                        barsChecker.isThickBar(bar) ? Shape.THICK_BAR_LINE
                                                : Shape.THIN_BAR_LINE);
                    barline.addStick(bar);
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
    private void checkBarAlignments (omr.score.entity.ScoreSystem system)
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
                    // at least one stick of the barline
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
                            barSticks.remove(stick);
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
                                barSticks.remove(stick);
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
    private void checkEndingBar (omr.score.entity.ScoreSystem system)
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

    //---------------------//
    // checkSystemMeasures //
    //---------------------//
    /**
     * Check measure reality, using a set of additional tests.
     */
    private void checkSystemMeasures (SystemInfo systemInfo)
    {
        omr.score.entity.ScoreSystem system = systemInfo.getScoreSystem();

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

    //-----------------//
    // chooseRefSystem //
    //-----------------//
    private SystemInfo chooseRefSystem ()
        throws StepException
    {
        // Look for the first largest system (according to its number of parts)
        int        NbOfParts = 0;
        SystemInfo refSystem = null;

        for (SystemInfo systemInfo : sheet.getSystems()) {
            int nb = systemInfo.getScoreSystem()
                               .getParts()
                               .size();

            if (nb > NbOfParts) {
                NbOfParts = nb;
                refSystem = systemInfo;
            }
        }

        if (refSystem == null) {
            throw new StepException("No system found");
        }

        return refSystem;
    }

    //------------------//
    // defineScoreParts //
    //------------------//
    /**
     * From system part, define the score parts
     * @throws StepException
     */
    private void defineScoreParts ()
        throws StepException
    {
        // Take the best representative system
        ScoreSystem refSystem = chooseRefSystem()
                                    .getScoreSystem();

        // Build the ScorePart list based on the parts of the ref system
        sheet.getScore()
             .createPartListFrom(refSystem);

        // Now examine each system as compared with the ref system
        // We browse through the parts "bottom up"
        List<ScorePart> partList = sheet.getScore()
                                        .getPartList();
        final int       nbScoreParts = partList.size();

        for (SystemInfo systemInfo : sheet.getSystems()) {
            ScoreSystem system = systemInfo.getScoreSystem();
            logger.fine(system.toString());

            List<TreeNode> systemParts = system.getParts();
            final int      nbp = systemParts.size();

            for (int ip = 0; ip < nbp; ip++) {
                ScorePart  global = partList.get(nbScoreParts - 1 - ip);
                SystemPart sp = (SystemPart) systemParts.get(nbp - 1 - ip);
                sp.setScorePart(global);
                sp.setId(global.getId());
            }
        }
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
    private void mergeBarlines (omr.score.entity.ScoreSystem system)
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
    private void removeStartingBar (omr.score.entity.ScoreSystem system)
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

    //---------------//
    // reportResults //
    //---------------//
    private void reportResults ()
    {
        StringBuilder    sb = new StringBuilder();

        List<SystemInfo> systems = sheet.getSystems();
        int              nb = systems.get(systems.size() - 1)
                                     .getScoreSystem()
                                     .getLastPart()
                                     .getLastMeasure()
                                     .getId();

        if (nb > 0) {
            sb.append(nb)
              .append(" measure");

            if (nb > 1) {
                sb.append("s");
            }
        } else {
            sb.append("no measure found");
        }

        logger.info(sb.toString());
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Maximum horizontal shift in bars between staves in a system */
        Scale.Fraction maxAlignShiftDx = new Scale.Fraction(
            0.2,
            "Maximum horizontal shift in bars between staves in a system");

        /** Maximum horizontal distance between the two bars of a double bar */
        Scale.Fraction maxDoubleBarDx = new Scale.Fraction(
            0.75,
            "Maximum horizontal distance between the two bars of a double bar");

        /** Minimum width for a measure */
        Scale.Fraction minMeasureWidth = new Scale.Fraction(
            0.75,
            "Minimum width for a measure");
        Scale.Fraction maxBarOffset = new Scale.Fraction(
            1.0,
            "Vertical offset used to detect that a bar extends past a staff");
    }
}
