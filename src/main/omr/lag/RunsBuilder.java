//----------------------------------------------------------------------------//
//                                                                            //
//                           R u n s B u i l d e r                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.lag;

import omr.util.Logger;
import omr.util.OmrExecutors;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Class <code>RunsBuilder</code> is in charge of building a collection of runs,
 * that can be used by a lag for example.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class RunsBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(RunsBuilder.class);

    //~ Instance fields --------------------------------------------------------

    /** The adapter that takes care of pixel access */
    private final Reader reader;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // RunsBuilder //
    //-------------//
    /**
     * Creates a new RunsBuilder object.
     *
     * @param reader an adapter to provide pixel access as well as specific
     *               call-back actions when a run (either foreground or
     *               background) has just been read.
     */
    public RunsBuilder (Reader reader)
    {
        this.reader = reader;
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // createRuns //
    //------------//
    /**
     * The <code>createRuns</code> method can be used to build the runs on the
     * fly, by providing a given rectangle. Note that the w and h parameters can
     * be swapped, which allows both vertical and horizontal uses, if the
     * Reader.getPixel() method is defined accordingly.
     *
     * @param rect   the rectangular area (coord x pos) to explore
     */
    public void createRuns (Rectangle rect)
    {
        final int cMin = rect.x;
        final int cMax = (rect.x + rect.width) - 1;
        final int pMin = rect.y;
        final int pMax = (rect.y + rect.height) - 1;

        createParallelRuns(pMin, pMax, cMin, cMax);
        reader.terminate();
    }

    //--------------------//
    // createParallelRuns //
    //--------------------//
    /**
     * This method handles the pixels run either in a parallel or a serial way,
     * according to the possibilities of the high OMR executor.
     */
    private void createParallelRuns (int       pMin,
                                     int       pMax,
                                     final int cMin,
                                     final int cMax)
    {
        try {
            // Browse one dimension
            List<Callable<Void>> tasks = new ArrayList<Callable<Void>>(
                pMax - pMin + 1);

            for (int p = pMin; p <= pMax; p++) {
                final int pp = p;
                tasks.add(
                    new Callable<Void>() {
                            public Void call ()
                                throws Exception
                            {
                                processPosition(pp, cMin, cMax);

                                return null;
                            }
                        });
            }

            OmrExecutors.getHighExecutor()
                        .invokeAll(tasks);
        } catch (InterruptedException ex) {
            logger.warning("ParallelRuns got interrupted", ex);
        }
    }

    //-----------------//
    // processPosition //
    //-----------------//
    /**
     * Process the pixels in position 'p' between coordinates 'cMin' & 'cMax'
     * @param p the position in the pixels array (x for vertical)
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

        // Current cumulated grey level for the run in progress
        int cumul = 0;

        // Browse other dimension
        for (int c = cMin; c <= cMax; c++) {
            final int level = reader.getLevel(c, p);

            if (reader.isFore(level)) {
                // We are on a foreground pixel
                if (isFore) {
                    // Append to the foreground run in progress
                    length++;
                    cumul += level;
                } else {
                    // End the previous background run if any
                    if (length > 0) {
                        reader.backRun(c, p, length);
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
                    reader.foreRun(c, p, length, cumul);

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
            reader.foreRun(cMax + 1, p, length, cumul);
        } else {
            reader.backRun(cMax + 1, p, length);
        }
    }

    //~ Inner Interfaces -------------------------------------------------------

    /**
     * Interface <code<Reader</code> is used to plug call-backs to a run
     * retrieval process.
     */
    public static interface Reader
    {
        //~ Methods ------------------------------------------------------------

        //--------//
        // isFore //
        //--------//
        /**
         * This method is used to check if the grey level corresponds to a
         * foreground pixel.
         *
         * @param level pixel level of grey
         *
         * @return true if pixel is foreground, false otherwise
         */
        boolean isFore (int level);

        //----------//
        // getLevel //
        //----------//
        /**
         * This method is used to report the grey level of the pixel read at
         * location (coord, pos).
         *
         * @param coord x for horizontal runs, y for vertical runs
         * @param pos   y for horizontal runs, x for vertical runs
         *
         * @return the pixel grey value (from 0 for black up to 255 for white)
         */
        int getLevel (int coord,
                      int pos);

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
         * measure of accumulated grey level in that case.
         *
         * @param coord  location of the point past the end of the run
         * @param pos    constant position of the run
         * @param length length of the run just found
         * @param cumul  cumulated grey levels along the run
         */
        void foreRun (int coord,
                      int pos,
                      int length,
                      int cumul);

        //-----------//
        // terminate //
        //-----------//
        /**
         * Called at the very end of run retrieval
         */
        void terminate ();
    }
}
