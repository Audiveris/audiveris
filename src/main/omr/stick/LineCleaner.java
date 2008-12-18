//----------------------------------------------------------------------------//
//                                                                            //
//                           L i n e C l e a n e r                            //
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

import omr.log.Logger;

import omr.math.BasicLine;
import omr.math.Line;

import omr.sheet.picture.Picture;

import net.jcip.annotations.ThreadSafe;

import java.util.*;

/**
 * Class <code>LineCleaner</code> is in charge of removing (staff or ledger)
 * line sticks and creating patches to extend crossing sections through the
 * former line.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
@ThreadSafe
public class LineCleaner
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(LineCleaner.class);

    //~ Instance fields --------------------------------------------------------

    /** The related lag */
    private final GlyphLag lag;

    /** The related picture */
    private final Picture picture;

    /**
     * Minimum number of points through a crossing object to compute an
     * extension axis.  Otherwise, the extension is performed orthogonally to
     * the line stick.
     */
    private final int minPointNb;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // LineCleaner //
    //-------------//
    /**
     * Creates a new LineCleaner object, with contextual parameters
     *
     * @param lag the containing lag
     * @param picture the picture which hosts the pixels handled by the stick
     * @param minPointNb Minimum number of points to compute an extension axis
     */
    public LineCleaner (GlyphLag lag,
                        Picture  picture,
                        int      minPointNb)
    {
        this.lag = lag;
        this.picture = picture;
        this.minPointNb = minPointNb;

        if (logger.isFineEnabled()) {
            logger.fine(
                "StickPatcher. lag=" + lag + ", minPointNb=" + minPointNb);
        }
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // cleanupStick //
    //--------------//
    /**
     * When a line stick is logically removed, the crossing objects must be
     * extended through the former line stick until the middle of the staff line
     *
     * @param lineStick the line stick about to be removed and perhaps patched
     */
    public void cleanupStick (Stick lineStick)
    {
        new StickCleaner(lineStick).cleanup();
    }

    //~ Inner Classes ----------------------------------------------------------

    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Ratio maxBorderAdjacency = new Constant.Ratio(
            0.7d,
            "Maximum adjacency for a section to be a border");
        Constant.Angle maxDeltaSlope = new Constant.Angle(
            0.5d,
            "Maximum difference of side tangent slopes when patching TBD");
    }

    //--------------//
    // StickCleaner //
    //--------------//
    /** Cleaner dedicated to one line stick */
    private class StickCleaner
    {
        //~ Instance fields ----------------------------------------------------

        /** The line stick to be cleaned and patched */
        private final Stick lineStick;

        /** Sections that are borders of the line stick */
        private final Set<GlyphSection> borders = new LinkedHashSet<GlyphSection>();

        /** Patch sections created to extend crossing objects through the line */
        private final List<GlyphSection> patches = new ArrayList<GlyphSection>();

        //~ Constructors -------------------------------------------------------

        private StickCleaner (Stick lineStick)
        {
            this.lineStick = lineStick;
        }

        //~ Methods ------------------------------------------------------------

        //---------//
        // cleanup //
        //---------//
        /**
         * When a line stick is logically removed, the crossing objects must be
         * extended through the former line stick until the middle of the staff
         * line
         */
        public void cleanup ()
        {
            if (logger.isFineEnabled()) {
                logger.fine("cleanup stick=" + lineStick);
            }

            // Extend crossing objects
            for (GlyphSection s : lineStick.getMembers()) {
                StickSection section = (StickSection) s;

                // Extend crossing vertices before and after
                for (GlyphSection source : section.getSources()) {
                    new SectionCleaner(section, (StickSection) source, +1).cleanup(
                        true);
                }

                for (GlyphSection target : section.getTargets()) {
                    new SectionCleaner(section, (StickSection) target, -1).cleanup(
                        true);
                }

                // Delete the section itself
                try {
                    section.delete();
                } catch (Exception ex) {
                    // In some cases we try to remove a section several times
                    // So simply ignore this
                    ///logger.warning("Error removing " + section);
                }
            }

            // Include the border sections as line members
            for (GlyphSection s : borders) {
                lineStick.addSection(s, /* link=> */
                                     true);
            }

            // Extend crossing objects for borders
            for (GlyphSection s : borders) {
                StickSection section = (StickSection) s;

                // Extend crossing vertices before and after
                if (logger.isFineEnabled()) {
                    logger.fine("border. before lineSection=" + section);
                }

                for (GlyphSection source : section.getSources()) {
                    new SectionCleaner(section, (StickSection) source, +1).cleanup(
                        false);
                }

                if (logger.isFineEnabled()) {
                    logger.fine("border. after lineSection=" + section);
                }

                for (GlyphSection target : section.getTargets()) {
                    new SectionCleaner(section, (StickSection) target, -1).cleanup(
                        false);
                }

                // Delete the section itself
                try {
                    section.delete();
                } catch (Exception ex) {
                    // In some cases we try to remove a section several times
                    // So simply ignore this
                    ///logger.warning("Error removing " + section);
                }
            }

            // Erase pixels from line sections
            write(lineStick.getMembers(), Picture.BACKGROUND);

            // But write patches to the picture
            write(patches, picture.getMaxForeground());
        }

        //----------------//
        // middlePosition //
        //----------------//
        /**
         * Return the position (y) of the middle of the line stick between
         * c1 & c2 abscissae
         *
         * @param c1 left abscissa (if horizontal)
         * @param c2 right abscissa
         *
         * @return the y for middle of found vertices
         */
        private int middlePosition (int c1,
                                    int c2)
        {
            int firstPos = Integer.MAX_VALUE;
            int lastPos = Integer.MIN_VALUE;

            for (Section section : lineStick.getMembers()) {
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

        //-------//
        // write //
        //-------//
        private void write (Collection<GlyphSection> sections,
                            int                      pixelValue)
        {
            for (GlyphSection section : sections) {
                section.write(picture, pixelValue);
            }
        }

        //~ Inner Classes ------------------------------------------------------

        //----------------//
        // SectionCleaner //
        //----------------//
        private class SectionCleaner
        {
            //~ Instance fields ------------------------------------------------

            private final StickSection lineSection;
            private final StickSection objectSection;
            private final int          direction;

            //~ Constructors ---------------------------------------------------

            /**
             * Create a section cleaner
             *
             * @param lineSection   the line section, through which the
             *                       objectSection is extended
             * @param objectSection the section of the crossing object
             * @param direction     direction in which extension must be performed,
             *                       +1 means extending downwards,
             *                       -1 extending upwards
             */
            public SectionCleaner (StickSection lineSection,
                                   StickSection objectSection,
                                   int          direction)
            {
                this.lineSection = lineSection;
                this.objectSection = objectSection;
                this.direction = direction;
            }

            //~ Methods --------------------------------------------------------

            private void cleanup (boolean borderEnabled)
            {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "sct=" + objectSection + ", direction=" + direction +
                        ", borderEnabled=" + borderEnabled);
                }

                // We are interested in non-stick vertices, and also in failed
                // sticks
                if (objectSection.isGlyphMember()) {
                    if (!(objectSection.getGlyph()
                                       .getResult() instanceof FailureResult)) {
                        if (logger.isFineEnabled()) {
                            logger.fine("Member of successful stick");
                        }

                        return;
                    }
                }

                // Is this objectSection just a border of the line ?
                if (borderEnabled) {
                    if (objectSection.getRunNb() == 1) { // Too restrictive ??? TBD

                        double adjacency = (direction > 0)
                                           ? objectSection.getFirstAdjacency()
                                           : objectSection.getLastAdjacency();

                        if (adjacency <= constants.maxBorderAdjacency.getValue()) {
                            // No concrete crossing object, let's aggregate this section
                            // to the line border.
                            objectSection.setParams(SectionRole.BORDER, 0, 0);
                            borders.add(objectSection);

                            if (logger.isFineEnabled()) {
                                logger.fine("Is a border");
                            }

                            return;
                        }
                    }
                }

                // This objectSection is actually crossing, we extend it
                createPatch();
            }

            //-------------//
            // createPatch //
            //-------------//
            /**
             * Build a patching section, which extends the crossing section
             */
            private void createPatch ()
            {
                /** y value at beginning of the extension */
                final int yBegin = (direction > 0) ? lineSection.getFirstPos()
                                   : lineSection.getLastPos();

                // Try to compute tangents on the edges of the crossing section
                final Tangents tangents = new Tangents(yBegin);

                /** Run (of the crossing object) to be extended */
                final Run objectRun = (direction > 0)
                                      ? objectSection.getLastRun()
                                      : objectSection.getFirstRun();

                // Left abscissa
                final int xStart = objectRun.getStart();

                // Right abscissa
                final int xStop = objectRun.getStop();

                // Middle abscissa
                double xAxis = (xStart + xStop) / 2d;

                if (logger.isFineEnabled()) {
                    logger.fine(
                        "xStart=" + xStart + " xAxis=" + xAxis + " xStop=" +
                        xStop);
                }

                // Compute the y position where our patch should stop
                final int yMid = middlePosition(xStart, xStop);

                if (yMid == 0) {
                    logger.warning("Cannot find line");

                    return;
                }

                // y value, past the last one
                final int    yPast = yMid + direction;

                // Extend the section, using whatever line(s) we have:
                // With startTg & stopTg : we use both directions
                // With just axis, we use a constant length in the axis direction
                // With nothing, we use a constant length in the y direction
                GlyphSection patchSection = null;

                // Sanity check
                if (((yPast - yBegin) * direction) <= 0) {
                    logger.fine(
                        "Weird relative positions yBegin=" + yBegin +
                        " yPast=" + yPast + " dir=" + direction);
                    logger.fine("patchSection line=" + lineSection);
                    logger.fine("patchSection contact=" + objectSection);
                } else {
                    try {
                        // Length of this contact run
                        int runLength = objectRun.getLength();

                        for (int y = yBegin; y != yPast; y += direction) {
                            int start = -1;

                            if (tangents.startTg != null) {
                                start = tangents.startTg.xAt(y);
                                runLength = tangents.stopTg.xAt(y) - start + 1;

                                if (logger.isFineEnabled()) {
                                    logger.fine(
                                        "y=" + y + " start=" + start +
                                        " length=" + runLength);
                                }

                                if (runLength <= 0) { // We have decreased to nothing

                                    if (logger.isFineEnabled()) {
                                        logger.fine("* length is zero *");
                                    }

                                    break;
                                }
                            } else {
                                if (tangents.axis != null) {
                                    xAxis = tangents.axis.xAt(y);

                                    if (logger.isFineEnabled()) {
                                        logger.fine("x=" + xAxis);
                                    }
                                }

                                start = (int) ((0.5d + xAxis) -
                                        (runLength / 2.0));
                            }

                            if (logger.isFineEnabled()) {
                                logger.fine(
                                    "y=" + y + ", start=" + start +
                                    ", length=" + runLength);
                            }

                            //Run newRun = new Run(start, length, Picture.FOREGROUND); // TBD
                            Run newRun = new Run(start, runLength, 127); // TBD for the 127 value of course

                            if (patchSection == null) {
                                patchSection = lag.createSection(y, newRun);

                                // Make the proper junction
                                if (direction > 0) {
                                    StickSection.addEdge(
                                        objectSection,
                                        patchSection);
                                } else {
                                    StickSection.addEdge(
                                        patchSection,
                                        objectSection);
                                }
                            } else {
                                // Extend patchSection in proper direction
                                if (direction > 0) {
                                    patchSection.append(newRun);
                                } else {
                                    patchSection.prepend(newRun);
                                }
                            }

                            if (logger.isFineEnabled()) {
                                logger.fine("patchSection=" + patchSection);
                            }
                        }
                    } catch (Exception ex) {
                        logger.warning("Error patching " + lineStick, ex);
                    }

                    if (patchSection != null) {
                        patches.add(patchSection);

                        // Update potential lagviews on the lag
                        for (DigraphView graphView : lag.getViews()) {
                            GlyphLagView lagView = (GlyphLagView) graphView;
                            lagView.addSectionView(patchSection);
                        }
                    }
                }
            }

            //~ Inner Classes --------------------------------------------------

            class Tangents
            {
                //~ Instance fields --------------------------------------------

                Line startTg = new BasicLine(); // Vertical tangent at runs starts
                Line stopTg = new BasicLine(); // Vertical tangent at runs stops
                Line axis = new BasicLine(); // Middle axis

                //~ Constructors -----------------------------------------------

                public Tangents (int yBegin)
                {
                    // Try to compute tangents on a total of minPointNb points
                    if ((startTg.getNumberOfPoints() +
                        objectSection.getRunNb()) >= (minPointNb + 1)) {
                        int y = yBegin - direction; // Skip the run stuck to the line

                        while (startTg.getNumberOfPoints() < minPointNb) {
                            y -= direction;

                            Run    objRun = objectSection.getRunAt(y);
                            double xAxis = (objRun.getStart() +
                                           objRun.getStop()) / 2d;

                            if (logger.isFineEnabled()) {
                                logger.fine(
                                    "y=" + y + " xStart=" + objRun.getStart() +
                                    " xAxis=" + xAxis + " xStop=" +
                                    objRun.getStop());
                            }

                            startTg.includePoint(objRun.getStart(), y);
                            stopTg.includePoint(objRun.getStop(), y);
                            axis.includePoint(xAxis, y);
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
                        final double deltaSlope = ((stopTg.getInvertedSlope() -
                                                   startTg.getInvertedSlope()) * direction);

                        if (deltaSlope > constants.maxDeltaSlope.getValue()) {
                            startTg = stopTg = null;

                            if (logger.isFineEnabled()) {
                                logger.fine("Merged. Axis=" + axis);
                            }
                        }
                    } else {
                        // No way to compute an axis, just use straight direction
                        axis = startTg = stopTg = null;
                    }
                }
            }
        }
    }
}
