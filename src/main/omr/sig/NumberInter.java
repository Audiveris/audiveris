//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      N u m b e r I n t e r                                     //
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

import java.awt.Rectangle;

/**
 * Class {@code NumberInter} represents a number, such as the top or bottom number in
 * a time signature, or an ending number, or a number for multi-measure rest, etc.
 *
 * @author Hervé Bitteur
 */
public class NumberInter
        extends AbstractInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Integer value for the number. */
    private final int value;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new NumberInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     * @param value number value
     */
    public NumberInter (Glyph glyph,
                        Shape shape,
                        double grade,
                        int value)
    {
        super(glyph, null, shape, grade);
        this.value = value;
    }

    /**
     * Creates a new NumberInter object.
     *
     * @param box   bounding box of the number
     * @param shape precise shape
     * @param grade evaluation value
     * @param value number value
     */
    public NumberInter (Rectangle box,
                        Shape shape,
                        double grade,
                        int value)
    {
        super(null, box, shape, grade);
        this.value = value;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // create //
    //--------//
    /**
     * (Try to) create a Number inter.
     *
     * @param shape precise shape
     * @param glyph underlying glyph
     * @param grade evaluation value
     * @return the created instance or null if failed
     */
    public static Inter create (Shape shape,
                                Glyph glyph,
                                double grade)
    {
        return new NumberInter(glyph, shape, grade, valueOf(shape));
    }

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
    private static int valueOf (Shape shape)
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

        return -1;
    }
}
