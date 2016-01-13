//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A b s t r a c t T i m e I n t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;

import omr.score.TimeRational;
import omr.score.TimeValue;

import omr.sheet.Staff;

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
import javax.xml.bind.annotation.XmlElement;
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
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Collection of default num/den combinations. */
    private static final Set<TimeRational> defaultTimes = new LinkedHashSet<TimeRational>(
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

    private static final Constants constants = new Constants();

    /** Collection of optional num/den combinations. */
    private static final List<TimeRational> optionalTimes = TimeRational.parseValues(
            constants.optionalTimes.getValue());

    private static final Logger logger = LoggerFactory.getLogger(AbstractTimeInter.class);

    /** Rational value of each (full) time sig shape. */
    private static final Map<Shape, TimeRational> rationals = new EnumMap<Shape, TimeRational>(
            Shape.class);

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

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** TimeRational components. */
    @XmlElement(name = "time-rational")
    @XmlJavaTypeAdapter(TimeRational.Adapter.class)
    protected TimeRational timeRational;

    //~ Constructors -------------------------------------------------------------------------------
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

    //~ Methods ------------------------------------------------------------------------------------
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

    //--------//
    // create //
    //--------//
    public static List<Inter> create (Shape shape,
                                      Glyph glyph,
                                      double grade)
    {
        throw new UnsupportedOperationException("Not supported yet.");
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

        case TIME_SIX_EIGHT:
            return new TimeRational(6, 8);

        default:
            return null;
        }
    }

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
        return timeRational.den;
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
        return timeRational.num;
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
        return new TimeValue(timeRational);
    }

    //-------------//
    // isSupported //
    //-------------//
    public static boolean isSupported (TimeRational tr)
    {
        return defaultTimes.contains(tr) || optionalTimes.contains(tr);
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
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(' ').append(getValue());

        return sb.toString();
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

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.String optionalTimes = new Constant.String(
                "6/4, 7/8",
                "Time sigs besides " + defaultTimes);
    }
}
