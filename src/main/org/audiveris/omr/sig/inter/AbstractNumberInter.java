//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              A b s t r a c t N u m b e r I n t e r                             //
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

import java.awt.Rectangle;

import javax.xml.bind.annotation.XmlAttribute;

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

    /** Integer value for the number. */
    @XmlAttribute
    protected final int value;

    /**
     * Creates a new AbstractNumberInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     */
    public AbstractNumberInter (Glyph glyph,
                                Shape shape,
                                double grade)
    {
        super(glyph, null, shape, grade);
        this.value = (shape != null) ? valueOf(shape) : (-1);
    }

    /**
     * Creates a new AbstractNumberInter object.
     *
     * @param bounds bounding box of the number
     * @param shape  precise shape
     * @param grade  evaluation value
     */
    public AbstractNumberInter (Rectangle bounds,
                                Shape shape,
                                double grade)
    {
        super(null, bounds, shape, grade);
        this.value = (shape != null) ? valueOf(shape) : (-1);
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Report the integer value of this symbol
     *
     * @return the integer value
     */
    public int getValue ()
    {
        return value;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " " + value;
    }

    //---------//
    // valueOf //
    //---------//
    /**
     * Report the integer value for the provided shape
     *
     * @param shape shape to test
     * @return supported integer value or IllegalArgumentException is thrown
     */
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

        throw new IllegalArgumentException("No integer value defined for " + shape);
    }
}
