//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              A b s t r a c t N u m b e r I n t e r                             //
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
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.NumberSymbol;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omrdataset.api.OmrShape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class <code>AbstractNumberInter</code> is an abstract inter with a integer value.
 * <p>
 * Concrete subclasses are defined for:
 * <ul>
 * <li>{@link TimeNumberInter} value in upper or lower part of a time signature,
 * <li>{@link MeasureCountInter} to specify the count of measures above a {@link MultipleRestInter}
 * or above a {@link MeasureRepeatInter} for 2 or 4 bars.
 * <li>An ending number in a volta (to be confirmed)
 * </ul>
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractNumberInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AbstractNumberInter.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Integer value for the number. */
    @XmlAttribute
    protected Integer value;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new AbstractNumberInter object.
     *
     * @param glyph underlying glyph
     * @param value numerical value
     * @param grade evaluation value
     */
    public AbstractNumberInter (Glyph glyph,
                                Integer value,
                                Double grade)
    {
        super(glyph, null, null, grade);

        if (value != null) {
            this.value = value; // Copy
        }
    }

    /**
     * Creates a new AbstractNumberInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     */
    public AbstractNumberInter (Glyph glyph,
                                Shape shape,
                                Double grade)
    {
        super(glyph, null, shape, grade);
        this.value = (shape != null) ? valueOf(shape) : null;
    }

    /**
     * Creates a new AbstractNumberInter object.
     *
     * @param bounds bounding box of the number
     * @param value  numerical value
     * @param grade  evaluation value
     */
    public AbstractNumberInter (Rectangle bounds,
                                Integer value,
                                Double grade)
    {
        super(null, bounds, null, grade);

        if (value != null) {
            this.value = value; // Copy
        }
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
                                Double grade)
    {
        super(null, bounds, shape, grade);
        this.value = (shape != null) ? valueOf(shape) : null;
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

    //----------------//
    // getShapeString //
    //----------------//
    @Override
    public String getShapeString ()
    {
        return "NUMBER_" + value;
    }

    //----------------//
    // getShapeSymbol //
    //----------------//
    @Override
    public ShapeSymbol getShapeSymbol (MusicFamily family)
    {
        return new NumberSymbol(shape, family, value);
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Report the integer value of this symbol
     *
     * @return the integer value
     */
    public Integer getValue ()
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

    //----------//
    // setValue //
    //----------//
    /**
     * Set a new integer value to this inter.
     *
     * @param value the new value
     */
    public void setValue (Integer value)
    {
        this.value = value;

        // Update containing time pair if any
        if (sig != null) {
            for (Relation rel : sig.getRelations(this, Containment.class)) {
                final Inter ens = sig.getEdgeSource(rel);
                if (ens instanceof TimePairInter pair) {
                    pair.invalidateCache();
                }
            }
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //---------//
    // valueOf //
    //---------//
    protected static int valueOf (OmrShape omrShape)
    {
        return switch (omrShape) {
            case timeSig0 -> 0;
            case timeSig1 -> 1;
            case timeSig2 -> 2;
            case timeSig3 -> 3;
            case timeSig4 -> 4;
            case timeSig5 -> 5;
            case timeSig6 -> 6;
            case timeSig7 -> 7;
            case timeSig8 -> 8;
            case timeSig9 -> 9;
            case timeSig12 -> 12;
            case timeSig16 -> 16;

            default -> throw new IllegalArgumentException(
                    "No integer value defined for " + omrShape);
        };
    }

    //---------//
    // valueOf //
    //---------//
    /**
     * Report the integer value for the provided shape.
     *
     * @param shape shape to test
     * @return supported integer value or null
     */
    public static Integer valueOf (Shape shape)
    {
        return switch (shape) {
            case NUMBER_CUSTOM, TIME_ZERO -> 0;
            case TIME_ONE -> 1;
            case TIME_TWO -> 2;
            case TIME_THREE -> 3;
            case TIME_FOUR -> 4;
            case TIME_FIVE -> 5;
            case TIME_SIX -> 6;
            case TIME_SEVEN -> 7;
            case TIME_EIGHT -> 8;
            case TIME_NINE -> 9;
            case TIME_TWELVE -> 12;
            case TIME_SIXTEEN -> 16;
            case null, default -> null;
        };
    }
}
