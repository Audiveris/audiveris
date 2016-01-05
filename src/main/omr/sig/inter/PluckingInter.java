//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    P l u c k i n g I n t e r                                   //
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
 * Class {@code PluckingInter} represents the fingering for guitar right-hand.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "plucking")
public class PluckingInter
        extends AbstractInter
        implements StringSymbolInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Letter for the finger. (p, i, m, a) */
    private final char letter;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code PluckingInter} object.
     *
     * @param glyph  underlying glyph
     * @param shape  precise shape
     * @param grade  evaluation value
     * @param letter finger name
     */
    public PluckingInter (Glyph glyph,
                          Shape shape,
                          double grade,
                          char letter)
    {
        super(glyph, null, shape, grade);
        this.letter = letter;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // create //
    //--------//
    /**
     * (Try to) create a Plucking inter.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     * @return the created instance or null if failed
     */
    public static PluckingInter create (Glyph glyph,
                                        Shape shape,
                                        double grade)
    {
        return new PluckingInter(glyph, shape, grade, valueOf(shape));
    }

    //-----------------//
    // getSymbolString //
    //-----------------//
    @Override
    public String getSymbolString ()
    {
        return String.valueOf(letter);
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " " + letter;
    }

    //---------//
    // valueOf //
    //---------//
    private static char valueOf (Shape shape)
    {
        switch (shape) {
        case PLUCK_P:
            return 'p';

        case PLUCK_I:
            return 'i';

        case PLUCK_M:
            return 'm';

        case PLUCK_A:
            return 'a';
        }

        throw new IllegalArgumentException("Invalid plucking shape " + shape);
    }
}
