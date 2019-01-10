//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A b s t r a c t T i m e I n t e r                               //
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.score.TimeRational;
import org.audiveris.omr.score.TimeValue;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omrdataset.api.OmrShape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code AbstractTimeInter} represents a time signature, with either one (full)
 * symbol (COMMON, CUT or predefined combo) or a pair of top and bottom numbers.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class AbstractTimeInter
        extends AbstractInter
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(AbstractTimeInter.class);

    /** Collection of default num/den combinations. */
    private static final Set<TimeRational> defaultTimes = new LinkedHashSet<>(
            Arrays.asList(
                    new TimeRational(2, 2), // Duple simple
                    new TimeRational(3, 2), // Triple simple
                    new TimeRational(2, 4), // Duple simple
                    new TimeRational(3, 4), // Triple simple
                    new TimeRational(4, 4), // Duple simple
                    new TimeRational(5, 4), // Asymmetrical simple
                    ///new TimeRational(7, 4), // Asymmetrical simple
                    new TimeRational(3, 8), // Triple compound
                    new TimeRational(6, 8), // Duple compound
                    new TimeRational(9, 8), // Triple compound
                    new TimeRational(12, 8) // Triple compound
            ));

    /** Collection of optional num/den combinations. */
    private static final List<TimeRational> optionalTimes = TimeRational.parseValues(
            constants.optionalTimes.getValue());

    /** Rational value of each (full) time sig shape. */
    private static final Map<Shape, TimeRational> rationals = new EnumMap<>(Shape.class);

    static {
        for (Shape s : ShapeSet.WholeTimes) {
            TimeRational nd = rationalOf(s);

            if (nd == null) {
                logger.error("Rational for {} is not defined", s);
            } else {
                rationals.put(s, nd);
            }
        }
    }

    // Persistent data
    //----------------
    //
    /** TimeRational components. */
    @XmlAttribute(name = "time-rational")
    @XmlJavaTypeAdapter(TimeRational.Adapter.class)
    protected TimeRational timeRational;

    /**
     * Creates a new TimeInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape (COMMON_TIME, CUT_TIME or predefined combo like TIME_FOUR_FOUR)
     * @param grade evaluation grade
     */
    public AbstractTimeInter (Glyph glyph,
                              Shape shape,
                              double grade)
    {
        super(glyph, null, shape, grade);
        timeRational = rationalOf(shape);
    }

    /**
     * Creates a new TimeInter object.
     *
     * @param glyph        underlying glyph
     * @param bounds       bounding bounds
     * @param timeRational the pair of num and den numbers
     * @param grade        evaluation grade
     */
    public AbstractTimeInter (Glyph glyph,
                              Rectangle bounds,
                              TimeRational timeRational,
                              double grade)
    {
        super(glyph, bounds, null, grade);
        this.timeRational = timeRational;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private AbstractTimeInter ()
    {
        super(null, null, null, null);
    }

    //-----------//
    // replicate //
    //-----------//
    /**
     * Use this AbstractTimeInter instance as a template for creating another one.
     * NOTA: Its bounds should be updated to the target location.
     *
     * @param targetStaff the target staff
     * @return the duplicate (not inserted in sig)
     */
    public abstract AbstractTimeInter replicate (Staff targetStaff);

    //----------------//
    // getDenominator //
    //----------------//
    /**
     * Report the bottom part of the time signature.
     *
     * @return the bottom part
     */
    public int getDenominator ()
    {
        return (getTimeRational() != null) ? getTimeRational().den : (-1);
    }

    //--------------//
    // getNumerator //
    //--------------//
    /**
     * Report the top part of the time signature.
     *
     * @return the top part
     */
    public int getNumerator ()
    {
        return (getTimeRational() != null) ? getTimeRational().num : (-1);
    }

    //-----------------//
    // getTimeRational //
    //-----------------//
    /**
     * @return the timeRational
     */
    public TimeRational getTimeRational ()
    {
        return timeRational;
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Report the time value represented by this Inter instance
     *
     * @return the time value
     */
    public TimeValue getValue ()
    {
        getTimeRational();

        if (ShapeSet.SingleWholeTimes.contains(shape)) {
            // COMMON_TIME or CUT_TIME only
            return new TimeValue(shape, timeRational);
        } else {
            if (timeRational != null) {
                return new TimeValue(null, timeRational);
            } else {
                return null;
            }
        }
    }

    //--------//
    // modify //
    //--------//
    /**
     * Modify in situ this time signature using provided shape and rational value.
     *
     * @param shape        the shape (perhaps null) of correct signature
     * @param timeRational the new sig rational value
     */
    public void modify (Shape shape,
                        TimeRational timeRational)
    {
        if (shape == null) {
            shape = predefinedShape(timeRational);

            if (shape == null) {
                shape = Shape.CUSTOM_TIME;
            }
        }

        logger.debug("{} assigned to {}", shape, this);

        this.shape = shape;
        this.timeRational = timeRational;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        TimeValue timeValue = getValue();

        if (timeValue != null) {
            return super.internals() + " " + timeValue;
        } else {
            return super.internals() + " NO_VALUE";
        }
    }

    //------------//
    // rationalOf //
    //------------//
    /**
     * Report the num/den pair of predefined time signature shapes.
     *
     * @param shape the queried shape
     * @return the related num/den or null
     */
    public static TimeRational rationalOf (Shape shape)
    {
        if (shape == null) {
            return null;
        }

        switch (shape) {
        case COMMON_TIME:
        case TIME_FOUR_FOUR:
            return new TimeRational(4, 4);

        case CUT_TIME:
        case TIME_TWO_TWO:
            return new TimeRational(2, 2);

        case TIME_TWO_FOUR:
            return new TimeRational(2, 4);

        case TIME_THREE_FOUR:
            return new TimeRational(3, 4);

        case TIME_FIVE_FOUR:
            return new TimeRational(5, 4);

        case TIME_THREE_EIGHT:
            return new TimeRational(3, 8);

        case TIME_SIX_EIGHT:
            return new TimeRational(6, 8);

        default:
            return null;
        }
    }

    //------------//
    // rationalOf //
    //------------//
    /**
     * Report the num/den pair of predefined time signature shapes.
     *
     * @param omrShape the queried shape
     * @return the related num/den or null
     */
    public static TimeRational rationalOf (OmrShape omrShape)
    {
        if (omrShape == null) {
            return null;
        }

        switch (omrShape) {
        case timeSigCommon:
        case timeSig4over4:
            return new TimeRational(4, 4);

        case timeSigCutCommon:
        case timeSig2over2:
            return new TimeRational(2, 2);

        case timeSig2over4:
            return new TimeRational(2, 4);

        case timeSig3over2:
            return new TimeRational(3, 2);

        case timeSig3over4:
            return new TimeRational(3, 4);

        case timeSig3over8:
            return new TimeRational(3, 8);

        case timeSig5over4:
            return new TimeRational(5, 4);

        case timeSig5over8:
            return new TimeRational(5, 8);

        case timeSig6over4:
            return new TimeRational(6, 4);

        case timeSig6over8:
            return new TimeRational(6, 8);

        case timeSig7over8:
            return new TimeRational(7, 8);

        case timeSig9over8:
            return new TimeRational(9, 8);

        case timeSig12over8:
            return new TimeRational(12, 8);

        default:
            return null;
        }
    }

    //-------------//
    // isSupported //
    //-------------//
    /**
     * Tell whether the provided TimeRational value is among the supported ones.
     *
     * @param tr provided value to check
     * @return true if so
     */
    public static boolean isSupported (TimeRational tr)
    {
        return defaultTimes.contains(tr) || optionalTimes.contains(tr);
    }

    //-----------------//
    // predefinedShape //
    //-----------------//
    /**
     * Look for a predefined shape, if any, that would correspond to the current
     * {@code num} and {@code den} values of this time sig.
     *
     * @return the shape found or null
     */
    private static Shape predefinedShape (TimeRational timeRational)
    {
        if (timeRational == null) {
            return null; // Safer
        }

        for (Shape s : ShapeSet.WholeTimes) {
            TimeRational nd = rationals.get(s);

            if (timeRational.equals(nd)) {
                return s;
            }
        }

        return null;
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.String optionalTimes = new Constant.String(
                "6/4, 7/8",
                "Optional time sigs");
    }
}
