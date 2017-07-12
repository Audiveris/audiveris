//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 R u n T a b l e F a c t o r y                                  //
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
package org.audiveris.omr.run;

import ij.process.ByteProcessor;

import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

/**
 * Class {@code RunTableFactory} retrieves the runs structure out of a given pixel
 * source and builds the related {@link RunTable} structure.
 *
 * @author Hervé Bitteur
 */
public class RunTableFactory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(RunTableFactory.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** The desired orientation. */
    private final Orientation orientation;

    /** The filter, if any, to be applied on run candidates. */
    private final Filter filter;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create an RunsTableFactory, with its specified orientation and no filtering.
     *
     * @param orientation the desired orientation of runs
     */
    public RunTableFactory (Orientation orientation)
    {
        this(orientation, null);
    }

    // ----------------//
    // RunTableFactory //
    // ----------------//
    /**
     * Create an RunsTableFactory, with its specified orientation and filtering.
     *
     * @param orientation the desired orientation of runs
     * @param filter      filtering on runs candidates
     */
    public RunTableFactory (Orientation orientation,
                            Filter filter)
    {
        this.orientation = orientation;
        this.filter = filter;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //
    // ------------//
    // createTable //
    // ------------//
    /**
     * Report the RunTable created with the runs retrieved from the provided source.
     *
     * @param source the source to read runs from.
     * @return a populated RunTable
     */
    public RunTable createTable (ByteProcessor source)
    {
        // ROI is defined as the whole source
        final Rectangle roi = new Rectangle(0, 0, source.getWidth(), source.getHeight());

        return createTable(source, roi);
    }

    //
    // ------------//
    // createTable //
    // ------------//
    /**
     * Report the RunTable created with the runs retrieved from the provided source.
     *
     * @param source the source to read runs from.
     * @param roi    region of interest (its coordinates are relative to the source)
     * @return a populated RunTable
     */
    public RunTable createTable (ByteProcessor source,
                                 Rectangle roi)
    {
        RunTable table = new RunTable(orientation, roi.width, roi.height);
        RunsRetriever retriever = new RunsRetriever(
                orientation,
                orientation.isVertical() ? new VerticalAdapter(source, table, roi.getLocation())
                        : new HorizontalAdapter(source, table, roi.getLocation()));
        retriever.retrieveRuns(roi);

        return table;
    }

    //~ Inner Interfaces ---------------------------------------------------------------------------
    //--------//
    // Filter //
    //--------//
    /**
     * This class is able to filter a run candidate.
     */
    public static interface Filter
    {
        //~ Methods --------------------------------------------------------------------------------

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

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------------//
    // LengthFilter //
    //--------------//
    /**
     * A convenient run filter, that checks whether the run length is sufficient.
     */
    public static class LengthFilter
            implements Filter
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final int minLength;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a length-based filter.
         *
         * @param minLength the minimum acceptable run length (specified in pixels)
         */
        public LengthFilter (int minLength)
        {
            this.minLength = minLength;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public boolean check (int x,
                              int y,
                              int length)
        {
            return length >= minLength;
        }
    }

    // ----------//
    // MyAdapter //
    // ----------//
    private abstract class MyAdapter
            implements RunsRetriever.Adapter
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** The source to read runs of pixels from. */
        protected final ByteProcessor source;

        /** The created RunTable. */
        protected RunTable table;

        /** Table offset, if any, WRT source. */
        protected Point tableOffset;

        //~ Constructors ---------------------------------------------------------------------------
        public MyAdapter (ByteProcessor source,
                          RunTable table,
                          Point tableOffset)
        {
            this.source = source;
            this.table = table;
            this.tableOffset = tableOffset;
        }

        //~ Methods --------------------------------------------------------------------------------
        // --------//
        // foreRun //
        // --------//
        @Override
        public final boolean foreRun (int coord,
                                      int pos,
                                      int length)
        {
            // We consider only runs for which the provided filter, if any, is OK.
            return (filter == null) || checkFilter(coord, pos, length);
        }

        //--------------//
        // isThreadSafe //
        //--------------//
        /**
         * The concurrency aspects of the adapter depends on the underlying PixelFilter.
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

        //-------------//
        // checkFilter //
        //-------------//
        protected abstract boolean checkFilter (int coord,
                                                int pos,
                                                int length);
    }

    //-------------------//
    // HorizontalAdapter //
    //-------------------//
    /**
     * Adapter for horizontal runs.
     */
    private class HorizontalAdapter
            extends MyAdapter
    {
        //~ Constructors ---------------------------------------------------------------------------

        public HorizontalAdapter (ByteProcessor source,
                                  RunTable table,
                                  Point tableOffset)
        {
            super(source, table, tableOffset);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void endPosition (int pos,
                                 List<Run> runs)
        {
            table.setSequence(pos - tableOffset.y, RunTable.encode(runs));
        }

        @Override
        public final boolean isFore (int coord,
                                     int pos)
        {
            return source.get(coord, pos) == 0;
        }

        @Override
        protected boolean checkFilter (int coord,
                                       int pos,
                                       int length)
        {
            return filter.check(coord - length, pos, length);
        }
    }

    //-----------------//
    // VerticalAdapter //
    //-----------------//
    /**
     * Adapter for vertical runs.
     */
    private class VerticalAdapter
            extends MyAdapter
    {
        //~ Constructors ---------------------------------------------------------------------------

        public VerticalAdapter (ByteProcessor source,
                                RunTable table,
                                Point tableOffset)
        {
            super(source, table, tableOffset);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void endPosition (int pos,
                                 List<Run> runs)
        {
            table.setSequence(pos - tableOffset.x, RunTable.encode(runs));
        }

        @Override
        public final boolean isFore (int coord,
                                     int pos)
        {
            return source.get(pos, coord) == 0;
        }

        @Override
        protected boolean checkFilter (int coord,
                                       int pos,
                                       int length)
        {
            return filter.check(pos, coord - length, length);
        }
    }
}
