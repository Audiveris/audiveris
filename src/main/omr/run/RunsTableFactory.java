//----------------------------------------------------------------------------//
//                                                                            //
//                      R u n s T a b l e F a c t o r y                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import omr.image.PixelFilter;

import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * Class {@code RunsTableFactory} retrieves the runs structure out of
 * a given pixel source and builds the related {@link RunsTable}
 * structure.
 *
 * @author Hervé Bitteur
 */
public class RunsTableFactory
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            RunsTableFactory.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** The source to read runs of pixels from. */
    private final PixelFilter source;

    /** The desired orientation. */
    private final Orientation orientation;

    /** The minimum value for a run length to be considered. */
    private final int minLength;

    /** Remember if we have to swap x and y coordinates. */
    private final boolean swapNeeded;

    /** The created RunsTable. */
    private RunsTable table;

    //~ Constructors -----------------------------------------------------------
    //
    // -----------------//
    // RunsTableFactory //
    // -----------------//
    /**
     * Create an RunsTableFactory, with its key parameters.
     *
     * @param orientation the desired orientation of runs
     * @param source      the source to read runs from.
     *                    Orientation parameter is used to properly access the
     *                    source pixels.
     * @param minLength   the minimum length for each run
     */
    public RunsTableFactory (Orientation orientation,
                             PixelFilter source,
                             int minLength)
    {
        this.orientation = orientation;
        this.source = source;
        this.minLength = minLength;

        swapNeeded = orientation.isVertical();
    }

    //~ Methods ----------------------------------------------------------------
    //
    // ------------//
    // createTable //
    // ------------//
    /**
     * Report the RunsTable created with the runs retrieved from the
     * provided source.
     *
     * @param name the name to be assigned to the table
     * @return a populated RunsTable
     */
    public RunsTable createTable (String name)
    {
        return createTable(name, null);
    }

    // ------------//
    // createTable //
    // ------------//
    /**
     * Report the RunsTable created with the runs retrieved from the
     * provided source.
     *
     * @param name   the name to be assigned to the table
     * @param filter if non-null, use this filter to check any run candidate
     * @return a populated RunsTable
     */
    public RunsTable createTable (String name,
                                  Filter filter)
    {
        table = new RunsTable(
                name,
                orientation,
                new Dimension(source.getWidth(), source.getHeight()));

        RunsRetriever retriever = new RunsRetriever(
                orientation,
                new MyAdapter(filter));

        retriever.retrieveRuns(
                new Rectangle(0, 0, source.getWidth(), source.getHeight()));

        return table;
    }

    //~ Inner Interfaces -------------------------------------------------------
    //--------//
    // Filter //
    //--------//
    /**
     * This class is able to filter a run candidate.
     */
    public static interface Filter
    {
        //~ Methods ------------------------------------------------------------

        /**
         * Perform the filter on the provided run candidate.
         *
         * @param x      abscissa at beginning of run candidate
         * @param y      ordinate at beginning of run candidate
         * @param length the length of the run candidate
         * @return true if candidate is to be kept
         */
        boolean check (int x,
                       int y,
                       int length);
    }

    //~ Inner Classes ----------------------------------------------------------
    //
    // ----------//
    // MyAdapter //
    // ----------//
    private class MyAdapter
            implements RunsRetriever.Adapter
    {
        //~ Instance fields ----------------------------------------------------

        /** Potential filter on candidates. */
        private final Filter filter;

        //~ Constructors -------------------------------------------------------
        public MyAdapter (Filter filter)
        {
            this.filter = filter;
        }

        //~ Methods ------------------------------------------------------------
        // --------//
        // backRun //
        // --------//
        @Override
        public final void backRun (int coord,
                                   int pos,
                                   int length)
        {
            // No interest in background runs
        }

        // --------//
        // foreRun //
        // --------//
        @Override
        public final void foreRun (int coord,
                                   int pos,
                                   int length,
                                   int cumul)
        {
            // We consider only runs that are longer than minLength
            if (length >= minLength) {
                // Run filter if any
                if (filter != null) {
                    final boolean ok = swapNeeded
                            ? filter.check(pos, coord - length, length)
                            : filter.check(coord - length, pos, length);
                    if (!ok) {
                        return;
                    }
                }
                final int level = ((2 * cumul) + length) / (2 * length);
                table.getSequence(pos)
                        .add(new Run(coord - length, length, level));
            }
        }

        // ---------//
        // getLevel //
        // ---------//
        @Override
        public final int getLevel (int coord,
                                   int pos)
        {
            if (swapNeeded) {
                return source.getValue(pos, coord);
            } else {
                return source.getValue(coord, pos);
            }
        }

        // -------//
        // isFore //
        // -------//
        @Override
        public final boolean isFore (int coord,
                                     int pos)
        {
            if (swapNeeded) {
                return source.isFore(pos, coord);
            } else {
                return source.isFore(coord, pos);
            }
        }

        //--------------//
        // isThreadSafe //
        //--------------//
        /**
         * The concurrency aspects of the adapter depends on the
         * underlying PixelFilter.
         *
         * @return true if safe, false otherwise
         */
        @Override
        public boolean isThreadSafe ()
        {
            Class<?> classe = source.getClass();

            // Check for @ThreadSafe annotation
            ThreadSafe safe = classe.getAnnotation(ThreadSafe.class);

            if (safe != null) {
                return true;
            }

            // Check for @NonThreadSafe annotation
            NotThreadSafe notSafe = classe.getAnnotation(NotThreadSafe.class);

            if (notSafe != null) {
                return false;
            }

            // No annotation: it's safer to assume no thread safety
            return false;
        }

        // ----------//
        // terminate //
        // ----------//
        @Override
        public final void terminate ()
        {
            logger.debug("{} Retrieved runs: {}", table, table.getRunCount());
        }
    }
}
