//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    P l u c k i n g I n t e r                                   //
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

    /**
     * No-arg constructor meant for JAXB.
     */
    private PluckingInter ()
    {
        this.letter = 0;
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
