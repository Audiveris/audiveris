//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         K e y E v e n t                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

/**
 * Formalizes an event while browsing key signature abscissa range.
 *
 * @author Hervé Bitteur
 */
public abstract class KeyEvent
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Left abscissa. */
    protected final int start;

    /** Right abscissa. */
    protected final int stop;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new KeyEvent object.
     *
     * @param start first abscissa of object
     * @param stop  last abscissa of object
     */
    public KeyEvent (int start,
                     int stop)
    {
        this.start = start;
        this.stop = stop;
    }

    //~ Methods ------------------------------------------------------------------------------------
    public int getWidth ()
    {
        return stop - start + 1;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------//
    // Peak //
    //------//
    public static class Peak
            extends KeyEvent
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Cumulated height. */
        private final int height;

        /** Flag for invalid peak. */
        private boolean invalid;

        //~ Constructors ---------------------------------------------------------------------------
        public Peak (int start,
                     int stop,
                     int height)
        {
            super(start, stop);
            this.height = height;
        }

        //~ Methods --------------------------------------------------------------------------------
        public double getCenter ()
        {
            return 0.5 * (start + stop);
        }

        public void setInvalid ()
        {
            invalid = true;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();

            if (invalid) {
                sb.append("Invalid");
            }

            sb.append("Peak{");
            sb.append(start);
            sb.append("-");
            sb.append(stop);
            sb.append("/");
            sb.append(height);
            sb.append("}");

            return sb.toString();
        }

        /**
         * @return the invalid
         */
        boolean isInvalid ()
        {
            return invalid;
        }
    }

    //-------//
    // Space //
    //-------//
    /**
     * An abscissa range where no chunk is detected on top of staff lines and thus
     * indicates a possible separation.
     */
    public static class Space
            extends KeyEvent
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Weight (area below the threshold) of the space. */
        private final int weight;

        private boolean wide;

        //~ Constructors ---------------------------------------------------------------------------
        public Space (int start,
                      int stop,
                      int weight)
        {
            super(start, stop);
            this.weight = weight;
        }

        //~ Methods --------------------------------------------------------------------------------
        public void setWide ()
        {
            wide = true;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();

            if (wide) {
                sb.append("Wide");
            }

            sb.append("Space(").append(start).append("-").append(stop).append(")/").append(weight);

            return sb.toString();
        }

        /**
         * @return the wide value
         */
        boolean isWide ()
        {
            return wide;
        }
    }
}
