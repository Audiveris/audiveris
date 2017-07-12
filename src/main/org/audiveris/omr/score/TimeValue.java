//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        T i m e V a l u e                                       //
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
package org.audiveris.omr.score;

import org.audiveris.omr.glyph.Shape;

import java.util.Objects;

/**
 * Class {@code TimeValue} represents a class of <b>equivalent</b> time signatures that
 * can be checked in a system column of time-signatures.
 * <ul>
 * <li>"C" (COMMON_TIME) and 4/4 (either whole "4/4" or pair "4","4") are NOT equivalent, because
 * we can't have one in a staff and the other in another staff within the same column.
 * <li>CUT_TIME and 2/2 (either whole "2/2" or pair "2","2") are NOT equivalent (similar as above).
 * <li>But "2/4" (whole specificShape) is equivalent to pair of shapes ("2","4"), because the way
 * shapes
 * were segmented by time builder software does not matter.
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class TimeValue
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Specific specificShape if any: COMMON_TIME or CUT_TIME, otherwise null. */
    public final Shape specificShape;

    /** Time rational value. (6/8, 3/4, etc) */
    public final TimeRational timeRational;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code TimeValue} object.
     *
     * @param specificShape the specific whole specificShape (COMMON_TIME or CUT_TIME) or null!
     * @param timeRational  the exact time rational value (6/8 != 3/4)
     */
    public TimeValue (Shape specificShape,
                      TimeRational timeRational)
    {
        if ((specificShape == null) && (timeRational == null)) {
            throw new IllegalArgumentException(
                    "Expected non-null specific shape or non-null timeRational for TimeValue");
        }

        this.specificShape = specificShape;
        this.timeRational = timeRational;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof TimeValue)) {
            return false;
        }

        TimeValue that = (TimeValue) obj;

        // Check time rational value
        if (!this.timeRational.equals(that.timeRational)) {
            return false;
        }

        // Check specificShape identity
        return (this.specificShape == that.specificShape);
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 7;
        hash = (83 * hash) + Objects.hashCode(this.timeRational);

        return hash;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        if ((specificShape == Shape.COMMON_TIME) || (specificShape == Shape.CUT_TIME)) {
            return specificShape.toString();
        }

        return timeRational.toString();
    }
}
