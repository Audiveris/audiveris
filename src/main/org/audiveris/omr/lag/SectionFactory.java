//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S e c t i o n F a c t o r y                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.lag;

import ij.process.ByteProcessor;

import net.jcip.annotations.NotThreadSafe;

import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.Run;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.run.RunTableFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Class {@code SectionFactory} builds a collection of sections out of provided runs.
 * <p>
 * To do so, this factory needs:<ul>
 * <li>A source of runs, which is generally provided by a {@link RunTable}.
 * Alternatively, a pixel buffer ({@link ByteProcessor}) can be provided, in this case a RunTable
 * instance is then built on the fly from the pixel buffer.</li>
 * <li>The sections coordinates are computed with respect to source origin (its upper left corner).
 * Optionally, a source offset {@link Point} can be specified, so that all sections created from
 * that source get translated accordingly.</li>
 * <li>The chosen {@link Orientation} can be derived from the RunTable input, or from a provided
 * Lag. When the source is only a pixel buffer, the desired orientation must be specified
 * explicitly.</li>
 * <li>A chosen {@link JunctionPolicy} determines if the incoming run can be appended to the
 * current section runs.</li>
 * <li>Optionally, a {@link Lag} instance can be specified, so that the created sections get a
 * lag-wide unique id and are appended to current lag content.</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
@NotThreadSafe
public class SectionFactory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SectionFactory.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The lag to populate, if any. */
    private final Lag lag;

    /** Same orientation for all sections. */
    private final Orientation orientation;

    /** Policy for detection of junctions. */
    private final JunctionPolicy junctionPolicy;

    /** Processed sections. true/false */
    private final Set<DynamicSection> processedSections = new LinkedHashSet<DynamicSection>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create an instance of SectionFactory with a target lag.
     *
     * @param lag            the lag to populate
     * @param junctionPolicy the policy to detect junctions
     */
    public SectionFactory (Lag lag,
                           JunctionPolicy junctionPolicy)
    {
        this.lag = lag;
        this.junctionPolicy = junctionPolicy;

        orientation = lag.getOrientation();
    }

    /**
     * Create an instance of SectionFactory with no lag.
     *
     * @param orientation    desired orientation for sections
     * @param junctionPolicy the policy to detect junctions
     */
    public SectionFactory (Orientation orientation,
                           JunctionPolicy junctionPolicy)
    {
        this.orientation = orientation;
        this.junctionPolicy = junctionPolicy;

        lag = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // createSections //
    //----------------//
    /**
     * Create sections out of all foreground pixels found in a provided buffer.
     *
     * @param buffer the provided buffer
     * @param offset buffer offset, if any, to be applied to sections
     * @return the collection of created sections, with absolute coordinates
     */
    public List<Section> createSections (ByteProcessor buffer,
                                         Point offset)
    {
        // Runs
        RunTable runTable = new RunTableFactory(orientation).createTable(buffer);

        return createSections(runTable, offset, false);
    }

    //----------------//
    // createSections //
    //----------------//
    /**
     * Create sections out of the foreground pixels found in the specified region of
     * interest of a provided buffer.
     *
     * @param buffer the provided buffer
     * @param roi    region of interest (its coordinates are relative to the buffer origin)
     * @return the collection of created sections, with absolute coordinates
     */
    public List<Section> createSections (ByteProcessor buffer,
                                         Rectangle roi)
    {
        // Runs
        RunTable runTable = new RunTableFactory(orientation).createTable(buffer, roi);

        // Offset?
        Point offset = ((roi.x != 0) || (roi.y != 0)) ? roi.getLocation() : null;

        // Create sections within roi/runtable
        return createSections(runTable, offset, false);
    }

    //----------------//
    // createSections //
    //----------------//
    /**
     * Populate a lag by creating sections from the provided table of runs.
     *
     * @param runTable the table of runs
     * @param offset   optional offset for runTable top left corner
     * @param include  if true, include the content of runTable into the lag
     * @return the list of created sections
     */
    public List<Section> createSections (RunTable runTable,
                                         Point offset,
                                         boolean include)
    {
        // Build sections with runTable-based coordinates
        List<DynamicSection> sections = new Build().buildSections(runTable, include);

        // Translate sections to absolute coordinates if an offset was provided
        if (offset != null) {
            for (DynamicSection dynSection : sections) {
                dynSection.translate(offset);
            }
        }

        return getImmutables(sections);
    }

    //---------------//
    // getImmutables //
    //---------------//
    /**
     * Report immutable version of sections.
     *
     * @param dynSections (mutable) sections
     * @return immutable sections
     */
    private List<Section> getImmutables (List<DynamicSection> dynSections)
    {
        final List<Section> sections = new ArrayList<Section>(dynSections.size());

        for (DynamicSection dynSection : dynSections) {
            sections.add(new BasicSection(dynSection));
        }

        return sections;
    }

    //-------------//
    // isProcessed //
    //-------------//
    private boolean isProcessed (DynamicSection dynSection)
    {
        return processedSections.contains(dynSection);
    }

    //--------------//
    // setProcessed //
    //--------------//
    private void setProcessed (DynamicSection dynSection)
    {
        processedSections.add(dynSection);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------//
    // Build //
    //-------//
    /**
     * A instance of this class is dedicated to the one-shot processing of a source.
     * It can be used only once.
     * <p>
     * Comments refer to 'sequences', which are synonymous of columns for vertical runs, and of rows
     * for horizontal runs.
     * We process all sequences one after the other, and sections are created, continued or
     * finished, according to the relative positions of runs found between one sequence ('prev') and
     * the following sequence ('next').
     */
    private class Build
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Counter to set dynamicsection ids when no lag is used. */
        private int localId;

        /** Global list of all sections created. */
        private final List<DynamicSection> created = new ArrayList<DynamicSection>();

        /** All Active sections in the next sequence. */
        private final List<DynamicSection> nextActives = new ArrayList<DynamicSection>();

        /** List of sections in previous sequence that overlap given run in next sequence. */
        private final List<DynamicSection> overlappingSections = new ArrayList<DynamicSection>();

        /**
         * List of all Active sections in the previous sequence, which means only
         * sections that have a run in previous sequence.
         */
        private final List<DynamicSection> prevActives = new ArrayList<DynamicSection>();

        //~ Methods --------------------------------------------------------------------------------
        //--------------//
        // buidSections //
        //--------------//
        /**
         * Build sections from the provided table of runs.
         *
         * @param runTable the table of runs
         * @param include  if true, include the content of runTable into the lag
         * @return the list of created sections
         */
        public List<DynamicSection> buildSections (RunTable runTable,
                                                   boolean include)
        {
            // All runs (if any) in first sequence start each their own dynamicSection
            for (Iterator<Run> it = runTable.iterator(0); it.hasNext();) {
                nextActives.add(createSection(0, it.next()));
            }

            // Now scan each pair of sequences, starting at 2nd sequence
            for (int col = 1, size = runTable.getSize(); col < size; col++) {
                // If we have runs in this sequence
                if (!runTable.isSequenceEmpty(col)) {
                    // Copy the former next actives sections as the new previous active sections
                    prevActives.clear();
                    prevActives.addAll(nextActives);
                    nextActives.clear();

                    // Process all sections of previous sequence, then prevActives
                    // will contain only active sections (that may be continued)
                    logger.debug("Prev sequence");

                    for (DynamicSection dynSection : prevActives) {
                        processPrevSide(dynSection, runTable, col);
                    }

                    // Process all runs of next sequence
                    logger.debug("Next sequence");

                    for (Iterator<Run> it = runTable.iterator(col); it.hasNext();) {
                        processNextSide(col, it.next());
                    }
                } else {
                    nextActives.clear();
                }
            }

            // Store the content of runs table into the lag?
            if (include && (lag != null)) {
                lag.addRunTable(runTable);
            }

            return created;
        }

        //-----------------//
        // continueSection //
        //-----------------//
        private void continueSection (DynamicSection dynSection,
                                      Run run)
        {
            logger.debug("Continuing section {} with {}", dynSection, run);

            dynSection.append(run);
            nextActives.add(dynSection);
        }

        //---------------//
        // createSection //
        //---------------//
        /**
         * Create a section.
         *
         * @param firstPos the starting position of the section
         * @param firstRun the very first run of the section
         * @return the created section
         */
        private DynamicSection createSection (int firstPos,
                                              Run firstRun)
        {
            if (firstRun == null) {
                throw new IllegalArgumentException("null first run");
            }

            final DynamicSection dynSection = new DynamicSection(orientation);

            if (lag != null) {
                lag.register(dynSection); // Section gets an id from lag
            } else {
                dynSection.setId(++localId); // Use a local id
            }

            dynSection.setFirstPos(firstPos);
            dynSection.append(firstRun);

            created.add(dynSection);

            return dynSection;
        }

        //-----------------//
        // processNextSide //
        //-----------------//
        /**
         * ProcessNextSide takes care of the second sequence, at the given run,
         * checking among the prevActives Sections which overlap this run.
         */
        private void processNextSide (int col,
                                      Run run)
        {
            logger.debug("processNextSide for run {}", run);

            int nextStart = run.getStart();
            int nextStop = run.getStop();

            // Check if overlap with a section run in previous sequence
            // All such sections are then stored in overlappingSections
            overlappingSections.clear();

            for (DynamicSection dynSection : prevActives) {
                Run lastRun = dynSection.getLastRun();

                if (lastRun.getStart() > nextStop) {
                    break;
                }

                if (lastRun.getStop() >= nextStart) {
                    logger.debug("Overlap from {} to {}", lastRun, run);
                    overlappingSections.add(dynSection);
                }
            }

            // Processing now depends on nb of overlapping runs
            logger.debug("overlap={}", overlappingSections.size());

            switch (overlappingSections.size()) {
            case 0: // Begin a brand new section
                nextActives.add(createSection(col, run));

                break;

            case 1: // Continuing sections (if not finished)

                DynamicSection prevSection = overlappingSections.get(0);

                if (!isProcessed(prevSection)) {
                    continueSection(prevSection, run);
                } else {
                    // Create a new section, linked by a junction
                    DynamicSection newSection = createSection(col, run);
                    nextActives.add(newSection);
                }

                break;

            default: // Converging sections, end them, start a new one
                logger.debug("Converging at {}", run);

                DynamicSection newSection = createSection(col, run);
                nextActives.add(newSection);
            }
        }

        //-----------------//
        // processPrevSide //
        //-----------------//
        /**
         * Take care of the first sequence, at the given section/run,
         * checking links to the next sequence runs that overlap this run.
         *
         * @param dynSection the section at hand
         * @param runTable   the table of runs
         * @param nextCol    column for the next sequence
         */
        private void processPrevSide (DynamicSection dynSection,
                                      RunTable runTable,
                                      int nextCol)
        {
            Run lastRun = dynSection.getLastRun();
            int prevStart = lastRun.getStart();
            int prevStop = lastRun.getStop();
            logger.debug("processPrevSide for section {}", dynSection);

            // Check if overlap with a run in next sequence
            int overlapNb = 0;
            Run overlapRun = null;

            for (Iterator<Run> it = runTable.iterator(nextCol); it.hasNext();) {
                Run run = it.next();

                if (run.getStart() > prevStop) {
                    break;
                }

                if (run.getStop() >= prevStart) {
                    logger.debug("Overlap from {} to {}", lastRun, run);
                    overlapNb++;
                    overlapRun = new Run(run);
                }
            }

            // Now consider how many overlapping runs we have in next sequence
            logger.debug("overlap={}", overlapNb);

            switch (overlapNb) {
            case 0: // Nothing : end of the section
                logger.debug("Ending section {}", dynSection);

                break;

            case 1: // Continue if consistent

                if (junctionPolicy.consistentRun(overlapRun, dynSection)) {
                    logger.debug(
                            "Perhaps extending section {} with run {}",
                            dynSection,
                            overlapRun);
                } else {
                    logger.debug(
                            "Incompatible height between {} and run {}",
                            dynSection,
                            overlapRun);
                    setProcessed(dynSection);
                }

                break;

            default: // Diverging, so conclude the section here
                setProcessed(dynSection);
            }
        }
    }
}
