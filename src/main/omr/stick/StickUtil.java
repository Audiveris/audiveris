//----------------------------------------------------------------------------//
//                                                                            //
//                             S t i c k U t i l                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.stick;

import omr.check.FailureResult;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.ui.GlyphLagView;

import omr.graph.DigraphView;

import omr.lag.Run;
import omr.lag.Section;

import omr.math.BasicLine;
import omr.math.Line;

import omr.sheet.Picture;

import omr.util.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class <code>StickUtil</code> gathers static utilities for sticks.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class StickUtil
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(StickUtil.class);

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // areExtensions //
    //---------------//
    /**
     * Checks whether two sticks can be considered as extensions of the other
     * one.  Due to some missing points, a long stick can be broken into several
     * smaller ones, that we must check for this.  This is checked before
     * actually merging them.
     *
     * @param foo           one stick
     * @param bar           one other stick
     * @param maxDeltaCoord Max gap in coordinate (x for horizontal)
     * @param maxDeltaPos   Max gap in position (y for horizontal)
     * @param maxDeltaSlope Max difference in slope
     *
     * @return The result of the test
     */
    public static boolean areExtensions (Stick  foo,
                                         Stick  bar,
                                         int    maxDeltaCoord,
                                         // X for horizontal
    int                                         maxDeltaPos,
                                         // Y for horizontal
    double                                      maxDeltaSlope)
    {
        // Check that a pair of start/stop is compatible
        if ((Math.abs(foo.getStart() - bar.getStop()) <= maxDeltaCoord) ||
            (Math.abs(foo.getStop() - bar.getStart()) <= maxDeltaCoord)) {
            // Check that a pair of positions is compatible
            if ((Math.abs(
                foo.getLine().yAt(foo.getStart()) -
                bar.getLine().yAt(foo.getStop())) <= maxDeltaPos) ||
                (Math.abs(
                foo.getLine().yAt(foo.getStop()) -
                bar.getLine().yAt(foo.getStart())) <= maxDeltaPos)) {
                // Check that slopes are compatible (a useless test ?)
                if (Math.abs(
                    foo.getLine().getSlope() - bar.getLine().getSlope()) <= maxDeltaSlope) {
                    return true;
                } else if (logger.isFineEnabled()) {
                    logger.fine("isExtensionOf:  Incompatible slopes");
                }
            } else if (logger.isFineEnabled()) {
                logger.fine("isExtensionOf:  Incompatible positions");
            }
        } else if (logger.isFineEnabled()) {
            logger.fine("isExtensionOf:  Incompatible coordinates");
        }

        return false;
    }

    //---------//
    // cleanup //
    //---------//
    /**
     * When a stick is logically removed, the crossing objects must be extended
     * through the former stick.
     *
     * @param minPointNb Minimum number of points, across the stick, to be able
     *                   to compute an extension axis. Otherwise, the extension
     *                   is performed orthogonally to the stick.
     * @param picture the picture which hosts the pixels handled by the stick
     */
    public static void cleanup (Stick    stick,
                                GlyphLag lag,
                                int      minPointNb,
                                Picture  picture)
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                "cleanup stick=" + stick + ", lag=" + lag + ", minPointNb=" +
                minPointNb);
        }

        Collection<GlyphSection> members = stick.getMembers();
        List<GlyphSection>       borders = new ArrayList<GlyphSection>();
        List<GlyphSection>       patches = new ArrayList<GlyphSection>();

        // Extend crossing objects
        for (GlyphSection s : members) {
            StickSection section = (StickSection) s;

            // Extend crossing vertices before and after
            for (GlyphSection source : section.getSources()) {
                cleanupSection(
                    stick,
                    borders,
                    patches,
                    lag,
                    picture,
                    minPointNb,
                    section,
                    (StickSection) source,
                    +1,
                    true);
            }

            for (GlyphSection target : section.getTargets()) {
                cleanupSection(
                    stick,
                    borders,
                    patches,
                    lag,
                    picture,
                    minPointNb,
                    section,
                    (StickSection) target,
                    -1,
                    true);
            }

            // Delete the section itself
            section.delete();
        }

        // Include the border sections as line members
        members.addAll(borders);

        // Extend crossing objects for borders
        for (GlyphSection s : borders) {
            StickSection section = (StickSection) s;

            // Extend crossing vertices before and after
            if (logger.isFineEnabled()) {
                logger.fine("border. before lineSection=" + section);
            }

            for (GlyphSection source : section.getSources()) {
                cleanupSection(
                    stick,
                    borders,
                    patches,
                    lag,
                    picture,
                    minPointNb,
                    section,
                    (StickSection) source,
                    +1,
                    false);
            }

            if (logger.isFineEnabled()) {
                logger.fine("border. after lineSection=" + section);
            }

            for (GlyphSection target : section.getTargets()) {
                cleanupSection(
                    stick,
                    borders,
                    patches,
                    lag,
                    picture,
                    minPointNb,
                    section,
                    (StickSection) target,
                    -1,
                    false);
            }

            // Delete the section itself
            section.delete();
        }

        // Erase pixels from members
        write(members, picture, Picture.BACKGROUND);

        // Write patches to the picture
        write(patches, picture, constants.patchGrayLevel.getValue());

        // Get rid of cached data
        // TBD TBD TBD
    }

    //----------------//
    // cleanupSection //
    //----------------//
    /**
     * Cleanup one line section, by extending potential crossing objects in a
     * certain direction. During this operation, we may consider that tangent
     * vertices are in fact borders that we should include in the line, rather
     * than consider them as real crossing objects.
     *
     * @param picture       the picture whose pixels must be modified
     * @param lineSection   the section to clean up
     * @param sct           a potentially crossing section
     * @param direction     in which direction we extend objects
     * @param borderEnabled do we consider adding borders to the line
     */
    private static void cleanupSection (Stick              stick,
                                        List<GlyphSection> borders,
                                        List<GlyphSection> patches,
                                        GlyphLag           lag,
                                        Picture            picture,
                                        int                minPointNb,
                                        StickSection       lineSection,
                                        StickSection       sct,
                                        int                direction,
                                        boolean            borderEnabled)
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                "sct=" + sct + ", direction=" + direction + ", borderEnabled=" +
                borderEnabled);
        }

        // We are interested in non-stick vertices, and also in failed
        // sticks
        if (sct.isGlyphMember()) {
            if (!(sct.getGlyph()
                     .getResult() instanceof FailureResult)) {
                if (logger.isFineEnabled()) {
                    logger.fine("Member of successful stick");
                }

                return;
            }
        }

        // Is this sct just a border of the line ?
        if (borderEnabled) {
            if (sct.getRunNb() == 1) { // Too restrictive ??? TBD

                double adj = (direction > 0) ? sct.getFirstAdjacency()
                             : sct.getLastAdjacency();

                if (adj <= constants.maxBorderAdjacency.getValue()) {
                    // No concrete crossing object, let's aggregate this section
                    // to the line border.
                    sct.setParams(SectionRole.BORDER, 0, 0);
                    borders.add(sct);

                    if (logger.isFineEnabled()) {
                        logger.fine("Is a border");
                    }

                    return;
                }
            }
        }

        // This sct is actually crossing, we extend it
        patchSection(
            stick,
            patches,
            lag,
            minPointNb,
            lineSection,
            sct,
            direction);
    }

    //--------//
    // middle //
    //--------//
    /**
     * Return the position (y) of the middle of the line between c1 & c2
     * abscissa
     *
     * @param c1 left abscissa (if horizontal)
     * @param c2 right abscissa
     *
     * @return the y for middle of found vertices
     */
    private static int middle (Stick stick,
                               int   c1,
                               int   c2)
    {
        int firstPos = Integer.MAX_VALUE;
        int lastPos = Integer.MIN_VALUE;

        for (Section section : stick.getMembers()) {
            // Check overlap with coordinates at hand
            if (Math.max(c1, section.getStart()) <= Math.min(
                c2,
                section.getStop())) {
                firstPos = Math.min(firstPos, section.getFirstPos());
                lastPos = Math.max(lastPos, section.getLastPos());
            }
        }

        return (int) Math.rint((double) (firstPos + lastPos) / 2);
    }

    //--------------//
    // patchSection //
    //--------------//
    /**
     * Build a patching section, which extends the crossing sct in the given
     * direction.
     *
     * @param section   the line section, through which the sct is extended
     * @param sct       the section of the crossing object
     * @param direction the direction in which extension must be performed
     */
    private static void patchSection (Stick              stick,
                                      List<GlyphSection> patches,
                                      GlyphLag           lag,
                                      int                minPointNb,
                                      StickSection       section,
                                      StickSection       sct,
                                      int                direction)
    {
        Run    lineRun; // Run of staff line in contact
        Run    run; // Run to be extended
        int    yBegin; // y value at beginning of the extension
        Line   startTg = new BasicLine(); // Vertical tangent at runs starts
        Line   stopTg = new BasicLine(); // Vertical tangent at runs stops
        Line   axis = new BasicLine(); // Middle axis
        int    x1; // Left abscissa
        int    x2; // Right abscissa
        double x; // Middle abscissa

        if (direction > 0) { // Going downwards
            run = sct.getLastRun();
            lineRun = section.getFirstRun();
            yBegin = section.getFirstPos();
        } else {
            run = sct.getFirstRun();
            lineRun = section.getLastRun();
            yBegin = section.getLastPos();
        }

        int length = run.getLength(); // Length of this contact run

        // Use line portion instead if shorter
        if (lineRun.getLength() < length) {
            if (logger.isFineEnabled()) {
                logger.fine("line shorter than external contact");
            }

            x1 = lineRun.getStart();
            x2 = lineRun.getStop();
            length = lineRun.getLength();
            startTg.includePoint(x1, yBegin);
            stopTg.includePoint(x2, yBegin);
        } else {
            x1 = run.getStart();
            x2 = run.getStop();
        }

        x = (double) (x1 + x2) / 2;

        if (logger.isFineEnabled()) {
            logger.fine("x1=" + x1 + " x=" + x + " x2=" + x2);
        }

        ////axis.includePoint(x, yBegin);

        // Compute the y position where our patch should stop
        final int yMid = middle(stick, x1, x2);

        if (yMid == 0) {
            logger.warning("Cannot find line");

            return;
        }

        final int yPast = yMid + direction; // y value, past the last one

        // Try to compute tangents on a total of minPointNb points
        if ((startTg.getNumberOfPoints() + sct.getRunNb()) >= (minPointNb + 1)) {
            int y = yBegin - direction; // Skip the run stuck to the line

            while (startTg.getNumberOfPoints() < minPointNb) {
                y -= direction;

                Run r = sct.getRunAt(y);

                if (logger.isFineEnabled()) {
                    logger.fine(
                        "y=" + y + " xl=" + r.getStart() + " x=" +
                        ((double) (r.getStart() + r.getStop()) / 2) + " xr=" +
                        r.getStop());
                }

                startTg.includePoint(r.getStart(), y);
                stopTg.includePoint(r.getStop(), y);
                axis.includePoint((double) (r.getStart() + r.getStop()) / 2, y);
            }
        }

        // Check whether we have enough runs to compute extension axis
        if (startTg.getNumberOfPoints() >= minPointNb) {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "startTg=" + startTg + " invertedSlope=" +
                    startTg.getInvertedSlope());
                logger.fine(
                    "axis=" + axis + " invertedSlope=" +
                    axis.getInvertedSlope());
                logger.fine(
                    "stopTg=" + stopTg + " invertedSlope=" +
                    stopTg.getInvertedSlope());
            }

            // Check that we don't diverge (convergence to the line is OK)
            if (((stopTg.getInvertedSlope() - startTg.getInvertedSlope()) * direction) > constants.maxDeltaSlope.getValue()) {
                /////axis = startTg.includeLine(stopTg); // Merge the two sides
                startTg = stopTg = null;

                if (logger.isFineEnabled()) {
                    logger.fine("Merged. Axis=" + axis);
                }
            }
        } else {
            // No way to compute an axis, just use straight direction
            axis = startTg = stopTg = null;
        }

        // Extend the section, using whatever line(s) we have :
        // With startTg & stopTg : we use both directions
        // With just axis, we use a constant length in the axis direction
        // With nothing, we use a constant length in the y direction
        GlyphSection newSct = null;

        // Sanity check
        if (((yPast - yBegin) * direction) <= 0) {
            logger.fine(
                "Weird relative positions yBegin=" + yBegin + " yPast=" +
                yPast + " dir=" + direction);
            logger.fine("patchSection line=" + section);
            logger.fine("patchSection contact=" + sct);
        } else {
            for (int y = yBegin; y != yPast; y += direction) {
                int start = -1;

                if (startTg != null) {
                    start = startTg.xAt(y);
                    length = stopTg.xAt(y) - start + 1;

                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "y=" + y + " start=" + start + " length=" + length);
                    }

                    if (length <= 0) { // We have decreased to nothing

                        if (logger.isFineEnabled()) {
                            logger.fine("* length is zero *");
                        }

                        break;
                    }
                } else {
                    if (axis != null) {
                        x = axis.xAt(y);

                        if (logger.isFineEnabled()) {
                            logger.fine("x=" + x);
                        }
                    }

                    start = (int) ((0.5 + x) - ((double) length / 2));
                }

                if (logger.isFineEnabled()) {
                    logger.fine(
                        "y=" + y + ", start=" + start + ", length=" + length);
                }

                //Run newRun = new Run(start, length, Picture.FOREGROUND); // TBD
                Run newRun = new Run(start, length, 127); // TBD for the 127 value of course

                if (newSct == null) {
                    newSct = lag.createSection(y, newRun);

                    // Make the proper junction
                    if (direction > 0) {
                        StickSection.addEdge(sct, newSct);
                    } else {
                        StickSection.addEdge(newSct, sct);
                    }

                    patches.add(newSct);
                } else {
                    // Extend newSct in proper direction
                    if (direction > 0) {
                        newSct.append(newRun);
                    } else {
                        newSct.prepend(newRun);
                    }
                }

                if (logger.isFineEnabled()) {
                    logger.fine("newSct=" + newSct);
                }
            }

            // Update potential lagviews on the lag
            if (newSct != null) {
                for (DigraphView graphView : lag.getViews()) {
                    GlyphLagView lagView = (GlyphLagView) graphView;
                    lagView.addSectionView(newSct);
                }
            }
        }
    }

    //-------//
    // write //
    //-------//
    private static void write (Collection<GlyphSection> sections,
                               Picture                  picture,
                               int                      pixel)
    {
        for (GlyphSection section : sections) {
            section.write(picture, pixel);
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    private static final class Constants
        extends ConstantSet
    {
        Constant.Ratio   maxBorderAdjacency = new Constant.Ratio(
            0.7d,
            "Maximum adjacency for a section to be a border");
        Constant.Angle   maxDeltaSlope = new Constant.Angle(
            0.5d,
            "Maximum difference of side tangent slopes when patching TBD");
        Constant.Integer patchGrayLevel = new Constant.Integer(
            "ByteLevel",
            200,
            "Gray level to be used when patching crossing objects");
    }
}
