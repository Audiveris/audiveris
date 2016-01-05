//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        F r e t I n t e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code FretInter} represents a fret number for guitar left-hand.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "fret")
public class FretInter
        extends AbstractInter
        implements StringSymbolInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Fret value. */
    private final int value;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code FretInter} object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     * @param value fret number
     */
    public FretInter (Glyph glyph,
                      Shape shape,
                      double grade,
                      int value)
    {
        super(glyph, null, shape, grade);
        this.value = value;
    }

    //~ Methods ------------------------------------------------------------------------------------
    public static FretInter create (Glyph glyph,
                                    Shape shape,
                                    double grade)
    {
        return new FretInter(glyph, shape, grade, valueOf(shape));
    }

    //-----------------//
    // getSymbolString //
    //-----------------//
    @Override
    public String getSymbolString ()
    {
        switch (value) {
        case 1:
            return "I";

        case 2:
            return "II";

        case 3:
            return "III";

        case 4:
            return "IV";

        case 5:
            return "V";

        case 6:
            return "VI";

        case 7:
            return "VII";

        case 8:
            return "VIII";

        case 9:
            return "IX";

        case 10:
            return "X";

        case 11:
            return "XI";

        case 12:
            return "XII";
        }

        throw new IllegalArgumentException("Invalid roman value " + value);
    }

    //---------//
    // valueOf //
    //---------//
    private static char valueOf (Shape shape)
    {
        switch (shape) {
        case ROMAN_I:
            return 1;

        case ROMAN_II:
            return 2;

        case ROMAN_III:
            return 3;

        case ROMAN_IV:
            return 4;

        case ROMAN_V:
            return 5;

        case ROMAN_VI:
            return 6;

        case ROMAN_VII:
            return 7;

        case ROMAN_VIII:
            return 8;

        case ROMAN_IX:
            return 9;

        case ROMAN_X:
            return 10;

        case ROMAN_XI:
            return 11;

        case ROMAN_XII:
            return 12;
        }

        throw new IllegalArgumentException("Invalid roman shape " + shape);
    }
}
