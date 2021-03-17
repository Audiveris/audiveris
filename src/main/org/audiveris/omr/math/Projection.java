//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       P r o j e c t i o n                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.math;

/**
 * Class {@code Projection} handles cumulated values projected on an axis.
 *
 * @author Hervé Bitteur
 */
public interface Projection
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report a simplistic derivative value at 'pos' point
     *
     * @param pos absolute point (between start and stop)
     * @return value at (pos) minus value at (pos - 1)
     */
    int getDerivative (int pos);

    /**
     * Report the domain length.
     *
     * @return stop - start + 1
     */
    int getLength ();

    /**
     * Report the first point on projection axis
     *
     * @return coordinate of first point (perhaps not zero)
     */
    int getStart ();

    /**
     * Report the last point on projection axis
     *
     * @return coordinate of last point (larger than or equal to start)
     */
    int getStop ();

    /**
     * Report the cumulated value at 'pos' point
     *
     * @param pos absolute point (between start and stop)
     * @return the value read at this point
     */
    int getValue (int pos);

    /**
     * Increment by one the value at 'pos' point.
     *
     * @param pos absolute point (between start and stop)
     */
    void increment (int pos);

    /**
     * Increment by 'inc' the value at 'pos' point.
     *
     * @param pos absolute point (between start and stop)
     * @param inc the increment value
     */
    void increment (int pos,
                    int inc);

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------//
    // Abstract //
    //----------//
    /**
     * Abstract implementation.
     */
    public abstract class Abstract
            implements Projection
    {

        /** First point on projection axis. */
        protected final int start;

        /** Last point on projection axis. */
        protected final int stop;

        /**
         * Create an instance of Abstract class.
         *
         * @param start first value in range
         * @param stop  last value in range
         */
        protected Abstract (int start,
                            int stop)
        {
            this.start = start;
            this.stop = stop;
        }

        @Override
        public final int getDerivative (int pos)
        {
            if (pos <= start) {
                return 0;
            }

            return getValue(pos) - getValue(pos - 1);
        }

        @Override
        public int getLength ()
        {
            return stop - start + 1;
        }

        @Override
        public int getStart ()
        {
            return start;
        }

        @Override
        public int getStop ()
        {
            return stop;
        }
    }

    //---------//
    // Integer //
    //---------//
    /**
     * A Projection using a table of int values.
     */
    public static class Integer
            extends Abstract
    {

        private final int[] data;

        /**
         * Creates a new Projection.Integer object.
         *
         * @param start beginning of axis
         * @param stop  end of axis
         */
        public Integer (int start,
                        int stop)
        {
            super(start, stop);
            data = new int[getLength()];
        }

        @Override
        public final int getValue (int pos)
        {
            return data[pos - start];
        }

        @Override
        public final void increment (int pos)
        {
            data[pos - start]++;
        }

        @Override
        public final void increment (int pos,
                                     int inc)
        {
            data[pos - start] += inc;
        }
    }

    //-------//
    // Short //
    //-------//
    /**
     * A Projection using a table of short values.
     */
    public static class Short
            extends Abstract
    {

        private final short[] data;

        /**
         * Creates a new Projection.Short object.
         *
         * @param start beginning of axis
         * @param stop  end of axis
         */
        public Short (int start,
                      int stop)
        {
            super(start, stop);
            data = new short[getLength()];
        }

        @Override
        public final int getValue (int pos)
        {
            return data[pos - start];
        }

        @Override
        public final void increment (int pos)
        {
            data[pos - start]++;
        }

        @Override
        public final void increment (int pos,
                                     int inc)
        {
            data[pos - start] = (short) (data[pos - start] + inc);
        }
    }
}
