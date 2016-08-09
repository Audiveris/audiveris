//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        T i m e V a l u e                                       //
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
package omr.score;

import omr.glyph.Shape;
import omr.glyph.ShapeSet;

import omr.sig.inter.AbstractTimeInter;

import java.util.Objects;

/**
 * Class {@code TimeValue} represents a class of time signatures.
 *
 * @author Hervé Bitteur
 */
public class TimeValue
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Assigned shape if any: COMMON_TIME, CUT_TIME, predefined combo or null. */
    public final Shape shape;

    /** Time rational value. (6/8, 3/4, etc) */
    public final TimeRational timeRational;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code TimeValue} object from a whole time shape
     *
     * @param shape the specific whole shape (COMMON_TIME or CUT_TIME or predefined combo)
     */
    public TimeValue (Shape shape)
    {
        this(shape, AbstractTimeInter.rationalOf(shape));
    }

    /**
     * Creates a new {@code TimeValue} object from a time rational value
     *
     * @param timeRational the exact time rational value (6/8 != 3/4)
     */
    public TimeValue (TimeRational timeRational)
    {
        this(null, timeRational);
    }

    /**
     * Creates a new {@code TimeValue} object.
     *
     * @param shape        the specific whole shape if any
     * @param timeRational the exact time rational value (6/8 != 3/4)
     */
    private TimeValue (Shape shape,
                       TimeRational timeRational)
    {
        if ((shape == null) && (timeRational == null)) {
            throw new IllegalArgumentException(
                    "Expected non-null shape or non-null timeRational for TimeValue");
        }

        this.shape = shape;
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

        // Check shape identity
        if (this.shape == that.shape) {
            return true;
        }

        // Example of 4/4 time rational value. We can have 3 shape values:
        // - null (provided by pair num & den)
        // - COMMON_TIME
        // - TIME_FOUR_FOUR
        // COMMON_TIME and TIME_FOUR_FOUR are different
        // but null shape and TIME_FOUR_FOUR are OK
        return !ShapeSet.SingleWholeTimes.contains(this.shape)
               && !ShapeSet.SingleWholeTimes.contains(that.shape);
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
        if (shape == Shape.COMMON_TIME || shape == Shape.CUT_TIME) {
            return shape.toString();
        }

        return timeRational.toString();
    }
}
