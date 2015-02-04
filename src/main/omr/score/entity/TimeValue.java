//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        T i m e V a l u e                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.Shape;

import omr.sig.inter.TimeInter;

import java.util.Objects;

/**
 * Class {@code TimeValue} represents a class of time signatures.
 *
 * @author Hervé Bitteur
 */
public class TimeValue
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Assigned shape if any: COMMON_TIME or CUT_TIME or null. */
    public final Shape shape;

    /** Time rational value. (6/8, 3/4, etc) */
    public final TimeRational timeRational;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code TimeValue} object.
     *
     * @param shape the specific shape (COMMON_TIME or CUT_TIME)
     */
    public TimeValue (Shape shape)
    {
        this(shape, TimeInter.rationalOf(shape));
    }

    /**
     * Creates a new {@code TimeValue} object.
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
     * @param shape        the specific shape if any (COMMON_TIME or CUT_TIME or null)
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

        if (obj instanceof TimeValue) {
            TimeValue that = (TimeValue) obj;

            return (this.shape == that.shape) && this.timeRational.equals(that.timeRational);
        }

        return false;
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 7;
        hash = (83 * hash) + Objects.hashCode(this.shape);
        hash = (83 * hash) + Objects.hashCode(this.timeRational);

        return hash;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        if (shape != null) {
            return shape.toString();
        }

        return timeRational.toString();
    }
}
