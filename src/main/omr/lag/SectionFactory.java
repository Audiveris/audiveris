//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S e c t i o n F a c t o r y                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.run.Orientation;
import omr.run.Run;
import omr.run.RunTable;
import omr.run.RunTableFactory;

import ij.process.ByteProcessor;

import net.jcip.annotations.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
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
    private final Set<Section> processedSections = new HashSet<Section>();

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
     * Create sections out of the foreground pixels found in a provided buffer and
     * contained in the specified region of interest.
     *
     * @param buffer the provided buffer
     * @param offset buffer offset, if any, to be applied to sections
     * @param roi    region of interest (its coordinates are relative to the buffer origin)
     * @return the collection of created sections, with absolute coordinates
     */
    public List<Section> createSections (ByteProcessor buffer,
                                         Point offset,
                                         Rectangle roi)
    {
        // Runs
        RunTable runTable = new RunTableFactory(orientation).createTable(buffer, roi);

        // Create sections within roi/runtable
        List<Section> sections = createSections(runTable, offset, false);

        if ((roi.x != 0) || (roi.y != 0)) {
            Point roiOffset = roi.getLocation();

            for (Section section : sections) {
                section.translate(roiOffset);
            }
        }

        return sections;
    }

    //----------------//
    // createSections //
    //----------------//
    /**
     * Create sections from the provided table of runs, with no source offset and no
     * inclusion of runs into the lag.
     *
     * @param runTable the table of runs
     * @return the list of created sections
     */
    public List<Section> createSections (RunTable runTable)
    {
        return createSections(runTable, null, false);
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
        List<Section> sections = new Build().buildSections(runTable, include);

        // Translate sections to absolute coordinates if an offset was provided
        if (offset != null) {
            for (Section section : sections) {
                section.translate(offset);
            }
        }

        return sections;
    }

    //-------------//
    // isProcessed //
    //-------------//
    private boolean isProcessed (Section section)
    {
        return processedSections.contains(section);
    }

    //--------------//
    // setProcessed //
    //--------------//
    private void setProcessed (Section section)
    {
        processedSections.add(section);
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

        /** Counter to set section ids when no lag is used. */
        private int localId;

        /** Global list of all sections created. */
        private final List<Section> created = new ArrayList<Section>();

        /** All Active sections in the next sequence. */
        private final List<Section> nextActives = new ArrayList<Section>();

        /** List of sections in previous sequence that overlap given run in next sequence. */
        private final List<Section> overlappingSections = new ArrayList<Section>();

        /**
         * List of all Active sections in the previous sequence, which means only
         * sections that have a run in previous sequence.
         */
        private final List<Section> prevActives = new ArrayList<Section>();

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
        public List<Section> buildSections (RunTable runTable,
                                            boolean include)
        {
            // All runs (if any) in first sequence start each their own section
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

                    for (Section section : prevActives) {
                        processPrevSide(section, runTable, col);
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
        private void continueSection (Section section,
                                      Run run)
        {
            logger.debug("Continuing section {} with {}", section, run);

            section.append(run);
            nextActives.add(section);
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
        private Section createSection (int firstPos,
                                       Run firstRun)
        {
            if (firstRun == null) {
                throw new IllegalArgumentException("null first run");
            }

            final Section section = new BasicSection(orientation);

            if (lag != null) {
                lag.register(section); // Section gets an id from lag
            } else {
                section.setId("" + ++localId); // Use a local id
            }

            section.setFirstPos(firstPos);
            section.append(firstRun);

            created.add(section);

            return section;
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

            for (Section section : prevActives) {
                Run lastRun = section.getLastRun();

                if (lastRun.getStart() > nextStop) {
                    break;
                }

                if (lastRun.getStop() >= nextStart) {
                    logger.debug("Overlap from {} to {}", lastRun, run);
                    overlappingSections.add(section);
                }
            }

            // Processing now depends on nb of overlapping runs
            logger.debug("overlap={}", overlappingSections.size());

            switch (overlappingSections.size()) {
            case 0: // Begin a brand new section
                nextActives.add(createSection(col, run));

                break;

            case 1: // Continuing sections (if not finished)

                Section prevSection = overlappingSections.get(0);

                if (!isProcessed(prevSection)) {
                    continueSection(prevSection, run);
                } else {
                    // Create a new section, linked by a junction
                    Section sct = createSection(col, run);
                    nextActives.add(sct);

                    ///prevSection.addTarget(sct);
                }

                break;

            default: // Converging sections, end them, start a new one
                logger.debug("Converging at {}", run);

                Section newSection = createSection(col, run);
                nextActives.add(newSection);

                for (Section section : overlappingSections) {
                    ///section.addTarget(newSection);
                }
            }
        }

        //-----------------//
        // processPrevSide //
        //-----------------//
        /**
         * Take care of the first sequence, at the given section/run,
         * checking links to the next sequence runs that overlap this run.
         *
         * @param section  the section at hand
         * @param runTable the table of runs
         * @param nextCol  column for the next sequence
         */
        private void processPrevSide (Section section,
                                      RunTable runTable,
                                      int nextCol)
        {
            Run lastRun = section.getLastRun();
            int prevStart = lastRun.getStart();
            int prevStop = lastRun.getStop();
            logger.debug("processPrevSide for section {}", section);

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
                logger.debug("Ending section {}", section);

                break;

            case 1: // Continue if consistent

                if (junctionPolicy.consistentRun(overlapRun, section)) {
                    logger.debug("Perhaps extending section {} with run {}", section, overlapRun);
                } else {
                    logger.debug("Incompatible height between {} and run {}", section, overlapRun);
                    setProcessed(section);
                }

                break;

            default: // Diverging, so conclude the section here
                setProcessed(section);
            }
        }
    }
}
