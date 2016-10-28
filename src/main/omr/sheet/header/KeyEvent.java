//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         K e y E v e n t                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sheet.header;

/**
 * Formalizes an event while browsing key signature abscissa range.
 *
 * @author Hervé Bitteur
 */
public abstract class KeyEvent
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Left abscissa. */
    protected int start;

    /** Right abscissa. */
    protected int stop;

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
        protected int height;

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
        //~ Constructors ---------------------------------------------------------------------------

        public Space (int start,
                      int stop)
        {
            super(start, stop);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();

            sb.append("Space(").append(start).append("-").append(stop).append(")");

            return sb.toString();
        }
    }
}
