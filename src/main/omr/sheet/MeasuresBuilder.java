//----------------------------------------------------------------------------//
//                                                                            //
//                       M e a s u r e s B u i l d e r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.check.FailureResult;

import omr.constant.ConstantSet;

import omr.glyph.facets.Glyph;

import omr.grid.BarAlignment;
import omr.grid.StaffInfo;
import omr.grid.StickIntersection;
import static omr.run.Orientation.*;

import omr.score.entity.Barline;
import omr.score.entity.Measure;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;

import omr.util.Navigable;
import omr.util.TreeNode;

import net.jcip.annotations.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

/**
 * Class {@code MeasuresBuilder} is in charge, at system info level, of
 * building measures from the bar sticks found.
 * At this moment, the only glyphs in the system collection are the barline
 * candidates.
 *
 * <p>Each instance of this class is meant to be called by a single thread,
 * dedicated to the processing of one system. So this class does not need to be
 * thread-safe.</p>
 *
 * @author Hervé Bitteur
 */
@NotThreadSafe
public class MeasuresBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            MeasuresBuilder.class);

    // Failures
    private static final FailureResult NOT_WITHIN_SYSTEM = new FailureResult(
            "Bar-NotWithinSystem");

    private static final FailureResult NOT_STAFF_ALIGNED = new FailureResult(
            "Bar-NotStaffAligned");

    private static final FailureResult NOT_SYSTEM_ALIGNED = new FailureResult(
            "Bar-NotSystemAligned");

    //~ Instance fields --------------------------------------------------------
    /** The dedicated system */
    @Navigable(false)
    private final SystemInfo system;

    /** Its counterpart within Score hierarchy */
    @Navigable(false)
    private ScoreSystem scoreSystem;

    /** The related sheet */
    @Navigable(false)
    private final Sheet sheet;

    /** Sheet scale */
    @Navigable(false)
    private final Scale scale;

    //~ Constructors -----------------------------------------------------------
    //-----------------//
    // MeasuresBuilder //
    //-----------------//
    /**
     * Creates a new MeasuresBuilder object.
     *
     * @param system the dedicated system
     */
    public MeasuresBuilder (SystemInfo system)
    {
        this.system = system;

        sheet = system.getSheet();
        scale = sheet.getScale();
    }

    //~ Methods ----------------------------------------------------------------
    //---------------//
    // buildMeasures //
    //---------------//
    /**
     * Based on barlines found, build, check and cleanup score measures.
     */
    public void buildMeasures ()
    {
        scoreSystem = system.getScoreSystem();

        allocateMeasures();

        checkMeasures();
    }

    //------------------//
    // allocateMeasures //
    //------------------//
    /**
     * Bar lines are first sorted according to their abscissa, then we
     * run additional checks on each bar line, since we now know its
     * enclosing system.
     * If OK, then we add the corresponding measures in their parts.
     */
    private void allocateMeasures ()
    {
        // Clear the collection of Measure instances
        for (TreeNode node : scoreSystem.getParts()) {
            SystemPart part = (SystemPart) node;
            part.getMeasures()
                    .clear();
        }

        // Create measures out of BarAlignment instances
        List<BarAlignment> alignments = system.getBarAlignments();
        int firstId = system.getFirstStaff()
                .getId();

        for (BarAlignment align : alignments) {
            if (logger.isDebugEnabled()) {
                logger.debug(align.toString());
            }

            StickIntersection[] inters = align.getIntersections();
            int ip = 0;

            for (TreeNode node : scoreSystem.getParts()) {
                SystemPart part = (SystemPart) node;
                PartInfo partInfo = system.getParts()
                        .get(ip++);
                Measure measure = new Measure(part);
                Barline barline = new Barline(measure);

                for (StaffInfo staffInfo : partInfo.getStaves()) {
                    int is = staffInfo.getId() - firstId;
                    StickIntersection inter = inters[is];

                    if (inter != null) {
                        Glyph stick = inter.getStickAncestor();
                        barline.addGlyph(stick);
                    } else {
                        // TODO
                        logger.warn(
                                "No intersection at index {} in {}",
                                is,
                                align);
                    }
                }

                logger.debug(
                        "S#{} {}" + " - Created measure " + " with {}",
                        scoreSystem.getId(),
                        part,
                        barline);
            }
        }

        // Degraded case w/ no bar stick at all!
        for (TreeNode node : scoreSystem.getParts()) {
            SystemPart part = (SystemPart) node;

            if (part.getMeasures()
                    .isEmpty()) {
                logger.debug("{} - Creating artificial measure", part);

                Measure measure = new Measure(part);
            }
        }
    }

    //----------------//
    // checkEndingBar //
    //----------------//
    /**
     * Use ending bar line if any, to adjust the right abscissa of the
     * system and its staves.
     */
    private void checkEndingBar ()
    {
        try {
            SystemPart part = scoreSystem.getFirstRealPart();
            Measure measure = part.getLastMeasure();
            Barline barline = measure.getBarline();

            if (barline == null) {
                return;
            }

            int lastX = barline.getRightX();
            int minWidth = scale.toPixels(constants.minMeasureWidth);

            if (((scoreSystem.getTopLeft().x + part.getFirstStaff()
                    .getWidth()) - lastX) < minWidth) {
                logger.debug("Adjusting EndingBar {}", system);

                // Adjust end of system & staff(s) to this one
                scoreSystem.setWidth(lastX - scoreSystem.getTopLeft().x);

                for (TreeNode pnode : scoreSystem.getParts()) {
                    SystemPart prt = (SystemPart) pnode;

                    for (Iterator<TreeNode> sit = prt.getStaves()
                            .iterator();
                            sit.hasNext();) {
                        Staff stv = (Staff) sit.next();
                        stv.setWidth(scoreSystem.getDimension().width);
                    }
                }
            }
        } catch (Exception ex) {
            logger.warn(
                    scoreSystem.getContextString()
                    + " Error in checking ending bar",
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
        // Detect very narrow measures which in fact indicate double bar
        // lines.
        mergeBarlines();

        // First barline may be just the beginning of the staff, so do not
        // count the very first bar line, which in general defines the
        // beginning of the staff rather than the end of a measure, but use
        // it to precisely define the left abscissa of the system and all
        // its contained staves.
        removeStartingMeasure();

        // Similarly, use the very last bar line, which generally ends the
        // system, to define the right abscissa of the system and its
        // staves.
        checkEndingBar();
    }

    //---------------//
    // mergeBarlines //
    //---------------//
    /**
     * Check whether two close bar lines are not in fact double lines
     * (with variants).
     */
    private void mergeBarlines ()
    {
        int maxDoubleDx = scale.toPixels(constants.maxDoubleBarDx);

        for (TreeNode node : scoreSystem.getParts()) {
            SystemPart part = (SystemPart) node;
            Measure prevMeasure = null;

            for (Iterator<TreeNode> mit = part.getMeasures()
                    .iterator(); mit.hasNext();) {
                Measure measure = (Measure) mit.next();

                if (prevMeasure != null) {
                    final int measureWidth = measure.getBarline()
                            .getCenter().x
                                             - prevMeasure.getBarline()
                            .getCenter().x;

                    if (measureWidth <= maxDoubleDx) {
                        // Lines are side by side or one above the other?
                        Glyph stick = (Glyph) measure.getBarline()
                                .getGlyphs()
                                .toArray()[0];
                        Glyph prevStick = (Glyph) prevMeasure.getBarline()
                                .getGlyphs()
                                .toArray()[0];

                        if (yOverlap(stick, prevStick)) {
                            // Overlap => side by side
                            // Merge the two bar lines into the first one
                            prevMeasure.getBarline()
                                    .mergeWith(measure.getBarline());

                            logger.debug(
                                    "Merged two close barlines into {}",
                                    prevMeasure.getBarline());
                        } else {
                            // No overlap => one above the other
                            logger.debug(
                                    "Two barlines segments one above the other in  {}",
                                    measure.getBarline());
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

    //-----------------------//
    // removeStartingMeasure //
    //-----------------------//
    /**
     * We associate measures only with their ending bar line(s),
     * so the starting bar of a staff does not end a measure,
     * we thus have to remove the measure that we first had associated
     * with it.
     */
    private void removeStartingMeasure ()
    {
        int minWidth = scale.toPixels(constants.minMeasureWidth);
        Barline firstBarline = scoreSystem.getFirstRealPart()
                .getFirstMeasure()
                .getBarline();

        if (firstBarline == null) {
            return;
        }

        int dx = firstBarline.getLeftX() - scoreSystem.getTopLeft().x;

        // Check is based on the width of this first measure
        if (dx < minWidth) {
            // Adjust system parameters if needed : topLeft and dimension
            if (dx != 0) {
                logger.debug("Adjusting firstX={} {}", dx, system);

                scoreSystem.getTopLeft()
                        .translate(dx, 0);
                scoreSystem.getDimension().width -= dx;
            }

            // Adjust beginning of all staves to this one
            // Remove this false "measure" in all parts of the system
            for (TreeNode node : scoreSystem.getParts()) {
                SystemPart part = (SystemPart) node;

                if (!part.isDummy()) {
                    // Set the bar as starting bar for the staff
                    Measure measure = part.getFirstMeasure();
                    part.setStartingBarline(measure.getBarline());

                    // Remove this first measure
                    List<TreeNode> measures = part.getMeasures();
                    measures.remove(0);

                    // Update abscissa of top-left corner of every staff
                    for (TreeNode sNode : part.getStaves()) {
                        Staff staff = (Staff) sNode;
                        staff.getTopLeft()
                                .translate(dx, 0);
                    }

                    // Update other bar lines abscissae accordingly
                    for (TreeNode mNode : part.getMeasures()) {
                        Measure meas = (Measure) mNode;
                        meas.resetAbscissae();
                    }
                }
            }
        }
    }

    //----------//
    // yOverlap //
    //----------//
    private boolean yOverlap (Glyph one,
                              Glyph two)
    {
        double start = Math.max(
                one.getStartPoint(VERTICAL).getY(),
                two.getStartPoint(VERTICAL).getY());
        double stop = Math.min(
                one.getStopPoint(VERTICAL).getY(),
                two.getStopPoint(VERTICAL).getY());

        return start < stop;
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction maxAlignShiftDx = new Scale.Fraction(
                0.5,
                "Maximum horizontal shift in bars between staves in a system");

        //
        Scale.Fraction maxDoubleBarDx = new Scale.Fraction(
                2.0,
                "Maximum horizontal distance between the two bars of a double bar");

        //
        Scale.Fraction minMeasureWidth = new Scale.Fraction(
                2.0,
                "Minimum width for a measure");

        //
        Scale.Fraction maxBarOffset = new Scale.Fraction(
                1.0,
                "Vertical offset used to detect that a bar extends past a staff");

    }
}
