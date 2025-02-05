//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        F r e t I n t e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import java.util.Comparator;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>FretInter</code> represents a fret number for guitar left-hand.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "fret")
public class FretInter
        extends AbstractInter
        implements StringSymbolInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /**
     * For comparing FretInter instances by their decreasing length.
     */
    public static final Comparator<FretInter> byDecreasingLength = (f1,
                                                                    f2) //
    -> Integer.compare(f2.getSymbolString().length(), f1.getSymbolString().length());

    //~ Instance fields ----------------------------------------------------------------------------

    /** Fret value. */
    @XmlAttribute
    private Integer value;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    private FretInter ()
    {
        this.value = null;
    }

    /**
     * Creates a new <code>FretInter</code> object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     */
    public FretInter (Glyph glyph,
                      Shape shape,
                      Double grade)
    {
        super(glyph, null, shape, grade);
        this.value = valueOf(shape);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------------//
    // afterUnmarshal //
    //----------------//
    @SuppressWarnings("unused")
    private void afterUnmarshal (Unmarshaller m,
                                 Object parent)
    {
        value = valueOf(shape);
    }

    //-----------------//
    // getSymbolString //
    //-----------------//
    @Override
    public String getSymbolString ()
    {
        return symbolStringOf(value);
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-----------------//
    // getSymbolString //
    //-----------------//
    public static String symbolStringOf (int value)
    {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            case 11 -> "XI";
            case 12 -> "XII";

            default -> null;
        };
    }

    //---------//
    // valueOf //
    //---------//
    public static Integer valueOf (Shape shape)
    {
        return switch (shape) {
            case ROMAN_I -> 1;
            case ROMAN_II -> 2;
            case ROMAN_III -> 3;
            case ROMAN_IV -> 4;
            case ROMAN_V -> 5;
            case ROMAN_VI -> 6;
            case ROMAN_VII -> 7;
            case ROMAN_VIII -> 8;
            case ROMAN_IX -> 9;
            case ROMAN_X -> 10;
            case ROMAN_XI -> 11;
            case ROMAN_XII -> 12;

            default -> null;
        };
    }
}
