//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        T i m e I n t e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.score.entity.TimeRational;

import java.awt.Rectangle;
import java.util.List;

/**
 * Class {@code TimeInter} represents a time signature, with either one (full) symbol
 * (COMMON or CUT) or a pair of top and bottom numbers.
 *
 * @author Hervé Bitteur
 */
public abstract class TimeInter
        extends AbstractInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** TimeRational components. */
    protected final TimeRational timeRational;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new TimeInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape (COMMON_TIME or CUT_TIME only)
     * @param grade evaluation grade
     */
    public TimeInter (Glyph glyph,
                      Shape shape,
                      double grade)
    {
        super(glyph, null, shape, grade);
        timeRational = rationalOf(shape);
    }

    /**
     * Creates a new TimeInter object.
     *
     * @param glyph        underlying glyph
     * @param box          bounding box
     * @param timeRational the pair of num & den numbers
     * @param grade        evaluation grade
     */
    public TimeInter (Glyph glyph,
                      Rectangle box,
                      TimeRational timeRational,
                      double grade)
    {
        super(glyph, box, null, grade);
        this.timeRational = timeRational;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //--------//
    // create //
    //--------//
    public static List<Inter> create (Shape shape,
                                      Glyph glyph,
                                      double grade)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    //-----------------//
    // getTimeRational //
    //-----------------//
    /**
     * @return the timeRational
     */
    public TimeRational getTimeRational ()
    {
        return timeRational;
    }

    //------------//
    // rationalOf //
    //------------//
    /**
     * Report the num/den pair of predefined time signature shapes.
     *
     * @param shape the queried shape
     * @return the related num/den or null
     */
    public static TimeRational rationalOf (Shape shape)
    {
        switch (shape) {
        case COMMON_TIME:
        case TIME_FOUR_FOUR:
            return new TimeRational(4, 4);

        case CUT_TIME:
        case TIME_TWO_TWO:
            return new TimeRational(2, 2);

        case TIME_TWO_FOUR:
            return new TimeRational(2, 4);

        case TIME_THREE_FOUR:
            return new TimeRational(3, 4);

        case TIME_FIVE_FOUR:
            return new TimeRational(5, 4);

        case TIME_SIX_EIGHT:
            return new TimeRational(6, 8);

        default:
            return null;
        }
    }

    //-----------//
    // sigString //
    //-----------//
    public abstract String sigString ();
}
