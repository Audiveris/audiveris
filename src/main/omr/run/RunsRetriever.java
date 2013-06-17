//----------------------------------------------------------------------------//
//                                                                            //
//                         R u n s R e t r i e v e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import omr.step.ProcessingCancellationException;

import omr.util.Concurrency;
import omr.util.OmrExecutors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Class {@code RunsRetriever} is in charge of reading a source of 
 * pixels and retrieving foreground runs and background runs from it.
 * What is done with the retrieved runs is essentially the purpose of the
 * provided adapter.
 *
 * @author Hervé Bitteur
 */
public class RunsRetriever
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(RunsRetriever.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** The orientation of desired runs */
    private final Orientation orientation;

    /** The adapter for pixel access and call-backs at run level */
    private final Adapter adapter;

    //~ Constructors -----------------------------------------------------------
    //
    //---------------//
    // RunsRetriever //
    //---------------//
    /**
     * Creates a new RunsRetriever object.
     *
     * @param orientation the desired orientation
     * @param adapter     an adapter to provide pixel access as well as specific
     *                    call-back actions when a run (either foreground or
     *                    background) has just been read.
     */
    public RunsRetriever (Orientation orientation,
                          Adapter adapter)
    {
        this.orientation = orientation;
        this.adapter = adapter;
    }

    //~ Methods ----------------------------------------------------------------
    //
    //--------------//
    // retrieveRuns //
    //--------------//
    /**
     * The {@code retrieveRuns} method can be used to build the runs on
     * the fly, by providing a given absolute rectangle.
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
        adapter.terminate();
    }

    //-----------------//
    // processPosition //
    //-----------------//
    /**
     * Process the pixels in position 'p' between coordinates 'cMin'
     * and 'cMax'
     *
     * @param p    the position in the pixels array (x for vertical)
     * @param cMin the starting coordinate (y for vertical)
     * @param cMax the ending coordinate
     */
    private void processPosition (int p,
                                  int cMin,
                                  int cMax)
    {
        // Current run is FOREGROUND or BACKGROUND
        boolean isFore = false;

        // Current length of the run in progress
        int length = 0;

        // Current cumulated gray level for the run in progress
        int cumul = 0;

        // Browse other dimension
        for (int c = cMin; c <= cMax; c++) {
            final int level = adapter.getLevel(c, p);

            ///logger.info("p:" + p + " c:" + c + " level:" + level);
            if (adapter.isFore(c, p)) {
                // We are on a foreground pixel
                if (isFore) {
                    // Append to the foreground run in progress
                    length++;
                    cumul += level;
                } else {
                    // End the previous background run if any
                    if (length > 0) {
                        adapter.backRun(c, p, length);
                    }

                    // Initialize values for the starting foreground run
                    isFore = true;
                    length = 1;
                    cumul = level;
                }
            } else {
                // We are on a background pixel
                if (isFore) {
                    // End the previous foreground run
                    adapter.foreRun(c, p, length, cumul);

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
            adapter.foreRun(cMax + 1, p, length, cumul);
        } else {
            adapter.backRun(cMax + 1, p, length);
        }
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
        if (OmrExecutors.defaultParallelism.getSpecific() == false
            || !adapter.isThreadSafe()) {
            // Sequential
            for (int p = pMin; p <= pMax; p++) {
                processPosition(p, cMin, cMax);
            }
        } else {
            // Parallel (TODO: should use Java 7 fork/join someday...)
            try {
                // Browse one dimension
                List<Callable<Void>> tasks = new ArrayList<>(
                        pMax - pMin + 1);

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
                OmrExecutors.getHighExecutor()
                        .invokeAll(tasks);
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

    //~ Inner Interfaces -------------------------------------------------------
    //
    //---------//
    // Adapter //
    //---------//
    /**
     * Interface {@code Adapter} is used to plug call-backs to a run
     * retrieval process.
     */
    public static interface Adapter
            extends Concurrency
    {
        //---------//
        // backRun //
        //---------//

        /**
         * Called at end of a background run, with the related coordinates
         *
         * @param coord  location of the point past the end of the run
         * @param pos    constant position of the run
         * @param length length of the run just found
         */
        void backRun (int coord,
                      int pos,
                      int length);

        //---------//
        // foreRun //
        //---------//
        /**
         * Same as background, but for a foreground run. We also provide the
         * measure of accumulated gray level in that case.
         *
         * @param coord  location of the point past the end of the run
         * @param pos    constant position of the run
         * @param length length of the run just found
         * @param cumul  cumulated gray levels along the run
         */
        void foreRun (int coord,
                      int pos,
                      int length,
                      int cumul);

        //----------//
        // getLevel //
        //----------//
        /**
         * This method is used to report the gray level of the pixel
         * read at location (coord, pos).
         *
         * @param coord x for horizontal runs, y for vertical runs
         * @param pos   y for horizontal runs, x for vertical runs
         *
         * @return the pixel gray value (from 0 for black up to 255 for white)
         */
        int getLevel (int coord,
                      int pos);

        //--------//
        // isFore //
        //--------//
        /**
         * This method is used to check if the pixel at location
         * (coord, pos) is a foreground pixel.
         *
         * @param coord x for horizontal runs, y for vertical runs
         * @param pos   y for horizontal runs, x for vertical runs
         *
         * @return true if pixel is foreground, false otherwise
         */
        boolean isFore (int coord,
                        int pos);

        //-----------//
        // terminate //
        //-----------//
        /**
         * Called at the very end of run retrieval.
         */
        void terminate ();
    }
}
