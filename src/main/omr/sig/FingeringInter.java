//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   F i n g e r i n g I n t e r                                  //
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

/**
 * Class {@code FingeringInter}
 *
 * @author Hervé Bitteur
 */
public class FingeringInter
        extends AbstractInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Integer value for the number. */
    private final int value;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new FingeringInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     * @param value number value
     */
    public FingeringInter (Glyph glyph,
                           Shape shape,
                           double grade,
                           int value)
    {
        super(glyph, null, shape, grade);
        this.value = value;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // create //
    //--------//
    /**
     * (Try to) create a Fingering inter.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     * @return the created instance or null if failed
     */
    public static Inter create (Glyph glyph,
                                Shape shape,
                                double grade)
    {
        return new FingeringInter(glyph, shape, grade, valueOf(shape));
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
        case DIGIT_0:
            return 0;

        case DIGIT_1:
            return 1;

        case DIGIT_2:
            return 2;

        case DIGIT_3:
            return 3;

        case DIGIT_4:
            return 4;

        case DIGIT_5:
            return 5;

        // Following shapes may be useless
        case DIGIT_6:
            return 6;

        case DIGIT_7:
            return 7;

        case DIGIT_8:
            return 8;

        case DIGIT_9:
            return 9;
        }

        return -1;
    }
}
