//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         K e y E v e n t                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sheet.key;

/**
 * Formalizes an event (peak or space) while browsing key signature abscissa range.
 *
 * @author Hervé Bitteur
 */
@Deprecated
public abstract class KeyEvent
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Left abscissa. */
    protected int min;

    /** Right abscissa. */
    protected int max;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new KeyEvent object.
     *
     * @param min first abscissa of object
     * @param max last abscissa of object
     */
    public KeyEvent (int min,
                     int max)
    {
        this.min = min;
        this.max = max;
    }

    //~ Methods ------------------------------------------------------------------------------------
    public int getWidth ()
    {
        return max - min + 1;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------//
    // Space //
    //-------//
    /**
     * An abscissa range where no chunk is detected on top of staff lines and thus
     * indicates a possible separation.
     */
    @Deprecated
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

            sb.append("Space(").append(min).append("-").append(max).append(")");

            return sb.toString();
        }
    }
}
