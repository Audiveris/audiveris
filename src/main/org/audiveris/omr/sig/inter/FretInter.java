//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        F r e t I n t e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;

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
     */
    public FretInter (Glyph glyph,
                      Shape shape,
                      double grade)
    {
        super(glyph, null, shape, grade);
        this.value = valueOf(shape);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private FretInter ()
    {
        this.value = 0;
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

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " " + shape;
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
