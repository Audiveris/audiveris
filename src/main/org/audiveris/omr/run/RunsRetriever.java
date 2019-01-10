//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   R u n s R e t r i e v e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.run;

import org.audiveris.omr.step.ProcessingCancellationException;
import org.audiveris.omr.util.Concurrency;
import org.audiveris.omr.util.OmrExecutors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Class {@code RunsRetriever} is in charge of reading a source of pixels and
 * retrieving foreground runs and background runs from it.
 * <p>
 * What is done with the retrieved runs is essentially the purpose of the provided adapter.
 *
 * @author Hervé Bitteur
 */
public class RunsRetriever
{

    private static final Logger logger = LoggerFactory.getLogger(RunsRetriever.class);

    /** The orientation of desired runs */
    private final Orientation orientation;

    /** The adapter for pixel access and call-backs at run level */
    private final Adapter adapter;

    /**
     * Creates a new RunsRetriever object.
     *
     * @param orientation the desired orientation
     * @param adapter     an adapter to provide pixel access as well as specific
     *                    call-back action when a foreground run has just been read.
     */
    public RunsRetriever (Orientation orientation,
                          Adapter adapter)
    {
        this.orientation = orientation;
        this.adapter = adapter;
    }

    //--------------//
    // retrieveRuns //
    //--------------//
    /**
     * Build the runs on the fly, by providing a given absolute rectangle.
     *
     * @param area the ABSOLUTE rectangular area to explore
     */
    public void retrieveRuns (Rectangle area)
    {
        Rectangle rect = orientation.oriented(area);
        final int cMin = rect.x;
        final int cMax = (rect.x + rect.width) - 1;
        final int pMin = rect.y;
        final int pMax = (rect.y + rect.height) - 1;

        rowBasedRetrieval(pMin, pMax, cMin, cMax);
    }

    //-----------------//
    // processPosition //
    //-----------------//
    /**
     * Process the pixels in position 'p' between coordinates 'cMin' and 'cMax'
     *
     * @param pos  the position in the pixels array (x for vertical)
     * @param cMin the starting coordinate (y for vertical)
     * @param cMax the ending coordinate
     */
    private void processPosition (int pos,
                                  int cMin,
                                  int cMax)
    {
        /** Buffer of runs for current position. */
        final List<Run> posRuns = new ArrayList<>();

        // Current run is FOREGROUND or BACKGROUND
        boolean isFore = false;

        // Current length of the run in progress
        int length = 0;

        // Browse other dimension
        for (int c = cMin; c <= cMax; c++) {
            ///logger.info("p:" + p + " c:" + c + " level:" + level);
            if (adapter.isFore(c, pos)) {
                // We are on a foreground pixel
                if (isFore) {
                    // Append to the foreground run in progress
                    length++;
                } else {
                    // Initialize values for the starting foreground run
                    isFore = true;
                    length = 1;
                }
            } else // We are on a background pixel
            {
                if (isFore) {
                    // End the previous foreground run
                    if (adapter.foreRun(c, pos, length)) {
                        // Bufferize the runs
                        posRuns.add(new Run(c - cMin - length, length));
                    }

                    // Initialize values for the starting background run
                    isFore = false;
                    length = 1;
                } else {
                    // Append to the background run in progress
                    length++;
                }
            }
        }

        // Process end of last run in this position
        if (isFore) {
            if (adapter.foreRun((cMax + 1) - cMin, pos, length)) {
                // Bufferize the runs
                posRuns.add(new Run((cMax + 1) - cMin - length, length));
            }
        }

        // Forward the buffer of runs
        adapter.endPosition(pos, posRuns);
    }

    //-------------------//
    // rowBasedRetrieval //
    //-------------------//
    /**
     * Retrieve runs row by row.
     * This method handles the pixels run either in a parallel or a serial way,
     * according to the possibilities of the high OMR executor.
     */
    private void rowBasedRetrieval (int pMin,
                                    int pMax,
                                    final int cMin,
                                    final int cMax)
    {
        if ((OmrExecutors.defaultParallelism.getValue() == false) || !adapter.isThreadSafe()) {
            // Sequential
            for (int p = pMin; p <= pMax; p++) {
                processPosition(p, cMin, cMax);
            }
        } else {
            // Parallel (TODO: should use Java 7 fork/join someday...)
            try {
                // Browse one dimension
                List<Callable<Void>> tasks = new ArrayList<>(pMax - pMin + 1);

                for (int p = pMin; p <= pMax; p++) {
                    final int pp = p;
                    tasks.add(
                            new Callable<Void>()
                    {
                        @Override
                        public Void call ()
                                throws Exception
                        {
                            processPosition(pp, cMin, cMax);

                            return null;
                        }
                    });
                }

                // Launch the tasks and wait for their completion
                OmrExecutors.getHighExecutor().invokeAll(tasks);
            } catch (InterruptedException ex) {
                logger.warn("ParallelRuns got interrupted");
                throw new ProcessingCancellationException(ex);
            } catch (ProcessingCancellationException pce) {
                throw pce;
            } catch (Throwable ex) {
                logger.warn("Exception raised in ParallelRuns", ex);
                throw new RuntimeException(ex);
            }
        }
    }

    //---------//
    // Adapter //
    //---------//
    /**
     * Interface {@code Adapter} is used to plug call-backs to a run retrieval process.
     */
    public static interface Adapter
            extends Concurrency
    {

        /**
         * Called at end of position.
         *
         * @param pos  position value
         * @param runs sequence of runs for this position
         */
        void endPosition (int pos,
                          List<Run> runs);

        /**
         * Called at end of a foreground run.
         *
         * @param coord  location of the point past the end of the run
         * @param pos    constant position of the run
         * @param length length of the run just found
         * @return true if run is accepted
         */
        boolean foreRun (int coord,
                         int pos,
                         int length);

        /**
         * Check if pixel at location (coord, pos) is foreground.
         *
         * @param coord x for horizontal runs, y for vertical runs
         * @param pos   y for horizontal runs, x for vertical runs
         * @return true if pixel is foreground, false otherwise
         */
        boolean isFore (int coord,
                        int pos);
    }
}
