//----------------------------------------------------------------------------//
//                                                                            //
//                           L i n e B u i l d e r                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.Shape;

import omr.lag.Run;

import omr.math.BasicLine;
import omr.math.Line;

import omr.step.StepException;

import omr.stick.*;

import omr.util.Implement;
import omr.util.Logger;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Class <code>LineBuilder</code> processes the horizontal area that corresponds
 * to one histogram peak, and thus one staff line. The main purpose of this
 * class is precisely to retrieve and clean up the staff line.
 *
 * <p/> The area for the to-be-retrieved staff line is scanned twice : <ol>
 *
 * <li> At line creation, the whole area is scanned to retrieve core sections
 * then peripheral and internal sections. This is done by the inherited
 * SticksBuilder class. </li>
 *
 * <li> Then, after all lines of the containing staff have been processed, we
 * have a better knowledge of what left and right extrema should be. We use this
 * information to scan "holes" in the current line, considering that every
 * suitable section found in such holes is actually part of the line. All these
 * hole sections are gathered in a specific stick, called the holeStick, since
 * we don't actually need to separate the connected sections. </li>
 *
 * </ol>
 *
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class LineBuilder
    extends SticksBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(LineBuilder.class);

    // Allocate comparator once & for all
    private static final StickStartComparator  stickStartComparator = new StickStartComparator();
    private static final StickWeightComparator stickWeightComparator = new StickWeightComparator();
    private static int                         globalId = 0;

    //~ Instance fields --------------------------------------------------------

    /** Best line equation */
    private Line line = null;

    /** Source for sections in this area */
    ///private LineSource source;

    /** Hole areas */
    private List<SticksBuilder> holeAreas = new ArrayList<SticksBuilder>();

    /** (local) scale at staff level */
    private Scale staffScale;

    /** Related sheet */
    private Sheet sheet;

    /** Just a sequential id for debug */
    private int id;

    /** Max Thickness for the various staff line chunks */
    private int maxThickness;

    /** Abscissa of left side */
    private int left = Integer.MAX_VALUE;

    /** Abscissa of right side */
    private int right = Integer.MIN_VALUE;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // LineBuilder //
    //-------------//
    /**
     * Create a line builder with the provided environment.
     *
     * @param hLag       the containing horizontal lag
     * @param yTop       ordinate at the beginning of the peak
     * @param yBottom    ordinate at the end of the peak
     * @param vi         the underlying vertex iterator
     * @param sheet      the sheet on which the analysis is performed
     * @param staffScale the specific scale of the containing staff
     */
    public LineBuilder (GlyphLag                   hLag,
                        int                        yTop,
                        int                        yBottom,
                        ListIterator<GlyphSection> vi,
                        Sheet                      sheet,
                        Scale                      staffScale)
    {
        super(
            sheet,
            hLag,
            new LineSource(
                yTop - staffScale.toPixels(constants.yMargin),
                yBottom + staffScale.toPixels(constants.yMargin),
                vi),
            sheet.getScale().toPixels(constants.coreSectionLength), // minCoreLength
            constants.maxAdjacency.getValue(), // maxAdjacency
            staffScale.toPixels(constants.maxThickness),
            constants.maxSlope.getValue(), // max stick slope
            true); // closeTest

        this.sheet = sheet;
        this.staffScale = staffScale;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Build the stick info found in the provided staff line area
     *
     * @return the info to be kept regarding the single staff line
     * @exception StepException raised when processing must stop
     */
    public LineInfo buildInfo ()
        throws StepException
    {
        id = ++globalId;

        if (logger.isFineEnabled()) {
            logger.fine("Building LineBuilder #" + id + " ...");
        }

        // Initialize the line area
        createSticks(null);

        if (logger.isFineEnabled()) {
            logger.fine(
                "End of scanning LineBuilder #" + id + ", found " +
                sticks.size() + " stick(s)");
        }

        // Sort sticks found according to their weight
        // and make sure their abscissae do not overlap
        Collections.sort(sticks, stickWeightComparator);

        for (Iterator<Stick> it = sticks.iterator(); it.hasNext();) {
            Stick stick = it.next();

            // Check with the previous ones in the list
            for (Iterator<Stick> pit = sticks.iterator(); pit.hasNext();) {
                Stick s = pit.next();

                if (s == stick) {
                    break;
                } else if (s.overlapWith(stick)) {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "Removed stick #" + stick.getId() +
                            " overlapping with #" + s.getId());
                    }

                    it.remove();

                    break;
                }
            }
        }

        // Sort sticks found according to their starting abscissa
        Collections.sort(sticks, stickStartComparator);

        // Sanity check
        if (sticks.size() == 0) {
            logger.warning("No sticks found in line area #" + id);
            throw new StepException();
        }

        // Update left & right extrema
        computeSides();

        // Try to cover holes left over
        scanHoles(0, sheet.getPicture().getWidth());

        // Re-compute line
        computeLine();

        // Assign the proper shape to these sticks
        for (Stick stick : sticks) {
            stick.setShape(Shape.STAFF_LINE);
        }

        if (logger.isFineEnabled()) {
            logger.fine(
                "End of LineBuilder #" + id + ", left=" + left + " right=" +
                right);
        }

        // Return the info just built
        LineInfo info = new LineInfo(id, left, right);
        info.setBuilder(this);
        info.setLine(line);

        return info;
    }

    //-------//
    // reset //
    //-------//
    /**
     * Reset the internal identification counter
     */
    public static void reset ()
    {
        globalId = 0;
    }

    //---------//
    // cleanup //
    //---------//
    /**
     * Cleanup the line
     */
    public void cleanup ()
    {
        final int extensionMinLength = sheet.getScale()
                                            .toPixels(
            constants.extensionMinLength);

        for (Stick stick : sticks) {
            StickUtil.cleanup(
                stick,
                lag,
                extensionMinLength,
                sheet.getPicture());
        }
    }

    //-------------//
    // computeLine //
    //-------------//
    /**
     * Perform the least-square computation of the best fitting line.
     */
    public void computeLine ()
    {
        line = new BasicLine();

        for (Stick stick : sticks) {
            line.includeLine(stick.getLine());
        }
    }

    //-----------//
    // scanHoles //
    //-----------//
    /**
     * This method is meant to be called when significant information on line
     * has already been retrieved. The purpose is to scan every potential hole
     * in this line, and gather sections found within such holes in a virtual
     * "holeStick".
     *
     * @param xMin left abscissa of the region
     * @param xMax right abscissa of the region
     */
    public void scanHoles (int xMin,
                           int xMax)
    {
        // Reset sections iterator at beginning of area
        source.reset();

        // First compute the line equation with the known sticks
        computeLine();

        final int           yMargin = staffScale.toPixels(
            constants.yHoleMargin);

        ListIterator<Stick> si = sticks.listIterator();
        Stick               leftStick = si.next();

        if (leftStick == null) { // Safer

            return;
        }

        scanHole(xMin, leftStick);

        while (si.hasNext()) {
            Stick rightStick = si.next();
            scanHole(leftStick, rightStick);
            leftStick = rightStick;
        }

        // Last part on the right
        scanHole(leftStick, xMax);

        // Here we have hole areas, with aggregated sticks
        // Make sure we don't have large empty regions between sticks
        if (logger.isFineEnabled()) {
            logger.fine("hole areas : " + holeAreas.size());
        }

        // Include sticks from hole areas into sticks list
        for (SticksBuilder area : holeAreas) {
            sticks.addAll(area.getSticks());
        }

        // Sort new collection of sticks, and check for empty regions
        Collections.sort(sticks, stickStartComparator);

        if (logger.isFineEnabled()) {
            si = sticks.listIterator();

            int i = 0;

            while (si.hasNext()) {
                Stick stick = si.next();
                System.out.print(i++ + " ");
                stick.dump(false);
            }
        }

        final int maxGapWidth = staffScale.toPixels(constants.maxGapWidth);

        si = sticks.listIterator();
        leftStick = si.next();

        int firstIdx = -1;
        int lastIdx = -1;

        while (si.hasNext()) {
            Stick     rightStick = si.next();
            int       gapStart = leftStick.getStop() + 1;
            final int gapStop = rightStick.getStart() - 1;

            if ((gapStop - gapStart) > maxGapWidth) {
                // Look for presence of runs in this wide gap
                final int yLeft = leftStick.getStoppingPos();
                final int yRight = rightStick.getStartingPos();
                final int yMin = (int) Math.rint(
                    Math.min(yLeft, yRight) - yMargin);
                final int yMax = (int) Math.rint(
                    Math.max(yLeft, yRight) + yMargin);
                Run       run = null;

                do {
                    run = lag.getFirstRectRun(gapStart, gapStop, yMin, yMax); // HB : check order TBD

                    if (run != null) {
                        if ((run.getStart() - gapStart) > maxGapWidth) {
                            break;
                        }

                        gapStart = run.getStop() + 1;
                    }
                } while (((gapStop - gapStart) > maxGapWidth) && (run != null));

                if ((gapStop - gapStart) > maxGapWidth) {
                    // Which ones to keep ?
                    if (leftStick.getStop() <= left) {
                        // We are on left of the staff, remove everything before
                        firstIdx = si.previousIndex();

                        if (logger.isFineEnabled()) {
                            logger.fine(
                                "Discarding before " + firstIdx + " left=" +
                                left + " stop=" + leftStick.getStop() +
                                " start=" + rightStick.getStart());
                        }
                    } else if (rightStick.getStart() >= right) {
                        // We are on right of the staff, remove everything after
                        lastIdx = si.previousIndex() - 1;

                        if (logger.isFineEnabled()) {
                            logger.fine(
                                "Discarding after " + lastIdx + " right=" +
                                right + " stop=" + leftStick.getStop() +
                                " start=" + rightStick.getStart());
                        }

                        break;
                    } else {
                        // We are in the staff itself ! This means that either
                        // left or right value is wrong.
                        if ((gapStop + gapStart) < (left + right)) {
                            // Move left side to the right
                            firstIdx = si.previousIndex();
                            left = rightStick.getStart();

                            if (logger.isFineEnabled()) {
                                logger.fine(
                                    "Discarding left stick before " + firstIdx +
                                    " now left=" + left);
                            }
                        } else {
                            // Move right side to the left, if not already moved
                            if (lastIdx != -1) {
                                lastIdx = si.previousIndex() - 1;
                                right = leftStick.getStop();

                                if (logger.isFineEnabled()) {
                                    logger.fine(
                                        "Discarding right stick after " +
                                        lastIdx + " now right=" + right);
                                }
                            }
                        }
                    }
                }
            }

            leftStick = rightStick;
        }

        if ((firstIdx != -1) || (lastIdx != -1)) {
            if (firstIdx == -1) {
                firstIdx = 0;
            }

            if (lastIdx == -1) {
                lastIdx = sticks.size() - 1;
            }

            if (logger.isFineEnabled()) {
                logger.fine(
                    "Discarding sticks firstIdx=" + firstIdx + " lastIdx=" +
                    lastIdx + " (out of " + sticks.size() + ")");
            }

            sticks = new ArrayList<Stick>(
                sticks.subList(firstIdx, lastIdx + 1));
        }
    }

    //--------------//
    // computeSides //
    //--------------//
    private void computeSides ()
    {
        Stick firstStick = sticks.get(0);
        left = firstStick.getStart();

        Stick lastStick = sticks.get(sticks.size() - 1);
        right = lastStick.getStop();
    }

    //----------//
    // scanHole //
    //----------//
    private void scanHole (int   left,
                           Stick rightStick)
    {
        int right = rightStick.getStart();

        if ((right - left) > 1) {
            scanRect(
                left,
                right,
                line.yAt((double) left), // Line equation
                rightStick.getLine().yAt((double) right));
        }
    }

    //----------//
    // scanHole //
    //----------//
    private void scanHole (Stick leftStick,
                           int   right)
    {
        int left = leftStick.getStop();

        if ((right - left) > 1) {
            scanRect(
                left,
                right,
                leftStick.getLine().yAt((double) left),
                line.yAt((double) right)); // Line equation
        }
    }

    //----------//
    // scanHole //
    //----------//
    private void scanHole (Stick leftStick,
                           Stick rightStick)
    {
        int left = leftStick.getStop();
        int right = rightStick.getStart();

        if ((right - left) > 1) {
            scanRect(
                left,
                right,
                leftStick.getLine().yAt((double) left),
                rightStick.getLine().yAt((double) right));
        }
    }

    //----------//
    // scanRect //
    //----------//
    private void scanRect (int    xMin,
                           int    xMax,
                           double yLeft,
                           double yRight)
    {
        if (logger.isFineEnabled()) {
            logger.fine("scanRect xMin=" + xMin + " xMax=" + xMax);
        }

        // List of hole candidates
        List<GlyphSection> holeCandidates = null;

        // Determine the abscissa limits
        final int xMargin = staffScale.toPixels(constants.xHoleMargin);
        xMin -= xMargin;
        xMax += xMargin;

        // Determine the ordinate limits
        final int yMargin = staffScale.toPixels(constants.yHoleMargin);
        final int yMin = (int) Math.rint(Math.min(yLeft, yRight) - yMargin);
        final int yMax = (int) Math.rint(Math.max(yLeft, yRight) + yMargin);

        // Beware : x & y are swapped for horizontal section !!!
        Rectangle holeRect = new Rectangle(
            yMin,
            xMin,
            yMax - yMin,
            xMax - xMin);
        int       sectionNb = 0;

        // Browse through our sections
        while (source.hasNext()) {
            StickSection section = (StickSection) source.next();

            // Finished ?
            if (section.getStart() > xMax) {
                source.backup();

                break;
            }

            // Available ?
            if (!section.isGlyphMember() // Not too thick ?
                 &&
                (section.getRunNb() <= maxThickness)) {
                // Within the limits ?
                if (holeRect.contains(section.getBounds())) {
                    section.setParams(SectionRole.HOLE, 0, 0);

                    if (holeCandidates == null) {
                        holeCandidates = new ArrayList<GlyphSection>();
                    }

                    holeCandidates.add(section);
                }
            }
        }

        // Have we found anything ?
        if (holeCandidates != null) {
            SticksBuilder holeArea = new SticksBuilder(
                sheet,
                lag,
                source,
                0,
                constants.maxAdjacency.getValue(), // maxAdjacency
                maxThickness,
                constants.maxSlope.getValue(), // max stick slope
                true);
            holeArea.createSticks(holeCandidates);
            holeAreas.add(holeArea);
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

        Scale.Fraction coreSectionLength = new Scale.Fraction(
            10d,
            "Minimum section length to be considered a staff line core section");
        Scale.Fraction extensionMinLength = new Scale.Fraction(
            0.25d,
            "Minimum length to compute extension of crossing objects during cleanup");
        Constant.Ratio maxAdjacency = new Constant.Ratio(
            0.5d,
            "Maximum adjacency ratio to flag a section as peripheral to a staff line");
        Scale.Fraction maxGapWidth = new Scale.Fraction(
            1.0d,
            "Maximum value for horizontal gap between 2 sticks");
        Constant.Angle maxSlope = new Constant.Angle(
            0.04d,
            "Maximum difference in slope to allow merging of two sticks");
        Scale.Fraction maxThickness = new Scale.Fraction(
            0.3d,
            "Maximum value for staff line thickness ");
        Scale.Fraction xHoleMargin = new Scale.Fraction(
            0.2d,
            "Margin on hole abscissa to define the area where hole sections are searched");
        Scale.Fraction yHoleMargin = new Scale.Fraction(
            0.1d,
            "Margin on hole ordinates to define the area where hole sections are searched");
        Scale.Fraction yMargin = new Scale.Fraction(
            0.2d,
            "Margin on peak ordinates to define the area where line sections are searched ");
    }

    //------------//
    // LineSource //
    //------------//
    private static class LineSource
        extends SticksSource
    {
        //~ Instance fields ----------------------------------------------------

        // My private list of sections in related area
        private final List<GlyphSection> sections = new ArrayList<GlyphSection>();
        private final int                yMax;
        private final int                yMin;

        //~ Constructors -------------------------------------------------------

        //------------//
        // LineSource //
        //------------//
        public LineSource (int                        yMin,
                           int                        yMax,
                           ListIterator<GlyphSection> it)
        {
            super(null, null);

            this.yMin = yMin;
            this.yMax = yMax;

            // Build my private list upfront
            while (it.hasNext()) {
                // Update cached data
                GlyphSection section = it.next();

                if (isInArea(section)) {
                    sections.add(section);
                } else if (section.getFirstPos() > yMax) {
                    it.previous();

                    break;
                }
            }

            if (logger.isFineEnabled()) {
                logger.fine("LineSource size : " + sections.size());
            }

            // Sort my list on starting abscissa
            Collections.sort(
                sections,
                new Comparator<GlyphSection>() {
                        public int compare (GlyphSection s1,
                                            GlyphSection s2)
                        {
                            return s1.getStart() - s2.getStart();
                        }
                    });

            // Define an iterator
            reset();
        }

        //~ Methods ------------------------------------------------------------

        //--------//
        // backup //
        //--------//
        @Override
        public void backup ()
        {
            vi.previous();
        }

        //---------//
        // hasNext //
        //---------//
        @Override
        public boolean hasNext ()
        {
            return vi.hasNext();
        }

        //----------//
        // isInArea //
        //----------//
        @Override
        public boolean isInArea (GlyphSection section)
        {
            return (section.getFirstPos() >= yMin) &&
                   (section.getLastPos() <= yMax);
        }

        //------//
        // next //
        //------//
        @Override
        public GlyphSection next ()
        {
            return vi.next();
        }

        //-------//
        // reset //
        //-------//
        @Override
        public void reset ()
        {
            vi = sections.listIterator();
        }
    }

    //----------------------//
    // StickStartComparator //
    //----------------------//
    private static class StickStartComparator
        implements Comparator<Stick>
    {
        //~ Methods ------------------------------------------------------------

        @Implement(Comparator.class)
        public int compare (Stick s1,
                            Stick s2)
        {
            return s1.getStart() - s2.getStart();
        }
    }

    //-----------------------//
    // StickWeightComparator //
    //-----------------------//
    private static class StickWeightComparator
        implements Comparator<Stick>
    {
        //~ Methods ------------------------------------------------------------

        // Sort by DECREASING weight
        @Implement(Comparator.class)
        public int compare (Stick s1,
                            Stick s2)
        {
            return s2.getWeight() - s1.getWeight();
        }
    }
}
