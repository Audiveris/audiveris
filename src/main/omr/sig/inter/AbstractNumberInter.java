//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              A b s t r a c t N u m b e r I n t e r                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import java.awt.Rectangle;

/**
 * Class {@code AbstractNumberInter} is an abstract inter with a integer value.
 * <p>
 * Concrete subclasses must be defined for Time upper or lower parts, for an ending number, for
 * the number of measures in a multi-measure rest.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractNumberInter
        extends AbstractInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Integer value for the number. */
    protected final int value;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new AbstractNumberInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     * @param value number value
     */
    public AbstractNumberInter (Glyph glyph,
                                Shape shape,
                                double grade,
                                int value)
    {
        super(glyph, null, shape, grade);
        this.value = value;
    }

    /**
     * Creates a new AbstractNumberInter object.
     *
     * @param box   bounding box of the number
     * @param shape precise shape
     * @param grade evaluation value
     * @param value number value
     */
    public AbstractNumberInter (Rectangle box,
                                Shape shape,
                                double grade,
                                int value)
    {
        super(null, box, shape, grade);
        this.value = value;
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

    /**
     * @return the value
     */
    public int getValue ()
    {
        return value;
    }

    //---------//
    // valueOf //
    //---------//
    protected static int valueOf (Shape shape)
    {
        switch (shape) {
        case TIME_ZERO:
            return 0;

        case TIME_ONE:
            return 1;

        case TIME_TWO:
            return 2;

        case TIME_THREE:
            return 3;

        case TIME_FOUR:
            return 4;

        case TIME_FIVE:
            return 5;

        case TIME_SIX:
            return 6;

        case TIME_SEVEN:
            return 7;

        case TIME_EIGHT:
            return 8;

        case TIME_NINE:
            return 9;

        case TIME_TWELVE:
            return 12;

        case TIME_SIXTEEN:
            return 16;
        }

        // Not a predefined value
        return -1;
    }
}
