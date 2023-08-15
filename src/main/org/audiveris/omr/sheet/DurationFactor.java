//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  D u r a t i o n F a c t o r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.math.Rational;

/**
 * Class <code>DurationFactor</code> handles a rational representation of duration
 * modification, such as those triggered by a tuplet.
 * <p>
 * Since in {@link Rational} super class, 'num' and 'den' are reduced by their GCD, in this class
 * we add fields 'actualNum' and 'actualDen' which are not reduced.
 * Therefore, DurationFactor 2/3 and 4/6 are not equal, even though their rational values are.
 *
 * @author Hervé Bitteur
 */
public class DurationFactor
        extends Rational
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Actual numerator value, generally 2 or 4. */
    public final int actualNum;

    /** Actual denominator value, generally 3 or 6. */
    public final int actualDen;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new instance of DurationFactor
     *
     * @param num numerator
     * @param den denominator
     */
    public DurationFactor (int num,
                           int den)
    {
        super(num, den);

        actualNum = num;
        actualDen = den;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (obj instanceof DurationFactor that) {
            if ((this.actualNum != that.actualNum) || (this.actualDen != that.actualDen)) {
                return false;
            }

            return super.equals(obj);
        }

        return false;
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 5;
        hash = (97 * hash) + this.actualNum;
        hash = (97 * hash) + this.actualDen;

        return hash;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        if (actualDen == 1) {
            return actualNum + "";
        } else {
            return actualNum + "/" + actualDen;
        }
    }
}
