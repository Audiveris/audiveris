//----------------------------------------------------------------------------//
//                                                                            //
//                       S e c t i o n s B u i l d e r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.log.Logger;

import omr.run.Run;
import omr.run.RunsTable;
import omr.run.RunsTableFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>SectionsBuilder</code> populates a full lag, by building the
 * lag sections and junctions, out of a provided {@link RunsTable} instance.
 *
 * @author Hervé Bitteur
 *
 * @param <L> precise lag type
 * @param <S> the precise subtype of Section to be created
 */
public class SectionsBuilder<L extends Lag<L, S>, S extends Section<L, S>>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        SectionsBuilder.class);

    //~ Instance fields --------------------------------------------------------

    /** Policy for detection of junctions */
    private JunctionPolicy junctionPolicy;

    /** The lag to populate */
    private L lag;

    /** All Active sections in the next column */
    private List<S> nextActives = new ArrayList<S>();

    /**
     * List of sections in previous column that overlap given run in next column
     */
    private List<S> overlappingSections = new ArrayList<S>();

    /**
     * All Active sections in the previous column, i.e. only sections that have
     * a run in previous column
     */
    private List<S> prevActives = new ArrayList<S>();

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // SectionsBuilder //
    //-----------------//
    /**
     * Create an instance of SectionsBuilder.
     *
     * @param lag            the lag to populate
     * @param junctionPolicy the policy to detect junctions
     */
    public SectionsBuilder (L              lag,
                            JunctionPolicy junctionPolicy)
    {
        this.lag = lag;
        this.junctionPolicy = junctionPolicy;
    }

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // createSections //
    //----------------//
    /**
     * Populate a lag by creating sections out of the provided table of runs
     * @param runsTable the table of runs
     */
    public void createSections (RunsTable runsTable)
    {
        // All runs (if any) in first column start each their own section
        for (Run run : runsTable.getSequence(0)) {
            nextActives.add(lag.createSection(0, run));
        }

        // Now scan each pair of columns, starting at 2nd column
        for (int col = 1; col < runsTable.getSize(); col++) {
            List<Run> runList = runsTable.getSequence(col);

            // If we have runs in this column
            if (!runList.isEmpty()) {
                // Copy the former next actives sections
                // as the new previous active sections
                prevActives = nextActives;
                nextActives = new ArrayList<S>();

                // Process all sections of previous column, then prevActives
                // will contain only active sections (i.e. that may be
                // continued)
                if (logger.isFineEnabled()) {
                    logger.fine("Prev column");
                }

                for (S section : prevActives) {
                    processPrevSide(section, runList);
                }

                // Process all runs of next column
                if (logger.isFineEnabled()) {
                    logger.fine("Next column");
                }

                for (Run run : runList) {
                    processNextSide(lag, col, run);
                }
            } else {
                nextActives.clear();
            }
        }

        // Some housekeeping
        prevActives = null;
        nextActives = null;
        overlappingSections = null;

        // Reset proper Ids
        for (S section : lag.getVertices()) {
            int id = section.getId();

            if (id < 0) {
                section.setId(-id);
            }

            // Register the section with its signature
            section.setSignature();
        }

        // Store the runs table into the lag
        lag.setRuns(runsTable);
    }

    //----------------//
    // createSections //
    //----------------//
    /**
     * Populate a lag by creating sections directly out of a pixel source
     *
     * @param name a name assigned to the runs table
     * @param source the source to read pixels from
     * @param minRunLength minimum length to consider a run
     */
    public void createSections (String      name,
                                PixelSource source,
                                int         minRunLength)
    {
        // Create the runs table
        RunsTableFactory factory = new RunsTableFactory(
            lag.getOrientation(),
            source,
            source.getMaxForeground(),
            minRunLength);
        RunsTable        table = factory.createTable(name);
        
        ///table.dump(System.out) ; // BINGO

        // Now proceed to section extraction
        createSections(table);
    }

    //------------//
    // isFinished //
    //------------//
    private boolean isFinished (S section)
    {
        return section.getId() < 0;
    }

    //-----------------//
    // continueSection //
    //-----------------//
    private void continueSection (S   section,
                                  Run run)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Continuing section " + section + " with " + run);
        }

        section.append(run);
        nextActives.add(section);
    }

    //--------//
    // finish //
    //--------//
    private void finish (S section)
    {
        section.setId(-section.getId());
    }

    //-----------------//
    // processNextSide //
    //-----------------//
    /**
     * Process NextSide takes care of the second column, at the given run,
     * checking among the prevActives Sections which overlap this run.
     */
    private void processNextSide (L   lag,
                                  int col,
                                  Run run)
    {
        if (logger.isFineEnabled()) {
            logger.fine("processNextSide for run " + run);
        }

        int nextStart = run.getStart();
        int nextStop = run.getStop();

        // Check if overlap with a section run in previous column
        // All such sections are then stored in overlappingSections
        overlappingSections.clear();

        for (S section : prevActives) {
            Run lastRun = section.getLastRun();

            if (lastRun.getStart() > nextStop) {
                break;
            }

            if (lastRun.getStop() >= nextStart) {
                if (logger.isFineEnabled()) {
                    logger.fine("Overlap from " + lastRun + " to " + run);
                }

                overlappingSections.add(section);
            }
        }

        // Processing now depends on nb of overlapping runs
        if (logger.isFineEnabled()) {
            logger.fine("overlap=" + overlappingSections.size());
        }

        switch (overlappingSections.size()) {
        case 0 : // Begin a brand new section
            nextActives.add(lag.createSection(col, run));

            break;

        case 1 : // Continuing sections (if not finished)

            S prevSection = overlappingSections.get(0);

            if (!isFinished(prevSection)) {
                continueSection(prevSection, run);
            } else {
                // Create a new section, linked by a junction
                S sct = lag.createSection(col, run);
                nextActives.add(sct);
                Section.addEdge(prevSection, sct);
            }

            break;

        default : // Converging sections, end them, start a new one

            if (logger.isFineEnabled()) {
                logger.fine("Converging at " + run);
            }

            S newSection = lag.createSection(col, run);
            nextActives.add(newSection);

            for (S section : overlappingSections) {
                Section.addEdge(section, newSection);
            }
        }
    }

    //-----------------//
    // processPrevSide //
    //-----------------//
    /**
     * processPrevSide take care of the first column, at the given section/run,
     * checking links to the nextColumnRuns that overlap this run.
     *
     * @param section
     * @param nextColumnRuns
     */
    private void processPrevSide (S         section,
                                  List<Run> nextColumnRuns)
    {
        Run lastRun = section.getLastRun();
        int prevStart = lastRun.getStart();
        int prevStop = lastRun.getStop();

        if (logger.isFineEnabled()) {
            logger.fine("processPrevSide for section " + section);
        }

        // Check if overlap with a run in next column
        int overlapNb = 0;
        Run overlapRun = null;

        for (Run run : nextColumnRuns) {
            if (run.getStart() > prevStop) {
                break;
            }

            if (run.getStop() >= prevStart) {
                if (logger.isFineEnabled()) {
                    logger.fine("Overlap from " + lastRun + " to " + run);
                }

                overlapNb++;
                overlapRun = run;
            }
        }

        // Now consider how many overlapping runs we have in next column
        if (logger.isFineEnabled()) {
            logger.fine("overlap=" + overlapNb);
        }

        switch (overlapNb) {
        case 0 : // Nothing : end of the section

            if (logger.isFineEnabled()) {
                logger.fine("Ending section " + section);
            }

            break;

        case 1 : // Continue if consistent

            if (junctionPolicy.consistentRun(overlapRun, section)) {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Perhaps extending section " + section + " with run " +
                        overlapRun);
                }
            } else {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Incompatible height between " + section + " and run " +
                        overlapRun);
                }

                finish(section);
            }

            break;

        default : // Diverging, so conclude the section here
            finish(section);
        }
    }
}
