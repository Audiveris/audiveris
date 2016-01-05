//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       P r o j e c t i o n                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

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
    //---------//
    // Integer //
    //---------//
    /**
     * A Projection using a table of int values.
     */
    public static class Integer
            extends Abstract
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final int[] data;

        //~ Constructors ---------------------------------------------------------------------------
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

        //~ Methods --------------------------------------------------------------------------------
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
        //~ Instance fields ------------------------------------------------------------------------

        private final short[] data;

        //~ Constructors ---------------------------------------------------------------------------
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

        //~ Methods --------------------------------------------------------------------------------
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

    //----------//
    // Abstract //
    //----------//
    public abstract class Abstract
            implements Projection
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** First point on projection axis. */
        protected final int start;

        /** Last point on projection axis. */
        protected final int stop;

        //~ Constructors ---------------------------------------------------------------------------
        protected Abstract (int start,
                            int stop)
        {
            this.start = start;
            this.stop = stop;
        }

        //~ Methods --------------------------------------------------------------------------------
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
}
