//----------------------------------------------------------------------------//
//                                                                            //
//                         T i m e S i g n a t u r e                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.Glyphs;
import omr.glyph.Shape;
import static omr.glyph.Shape.*;
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;

import omr.score.visitor.ScoreVisitor;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class {@code TimeSignature} encapsulates a time signature,
 * which may be composed of one or several glyphs.
 *
 * @author Hervé Bitteur
 */
public class TimeSignature
        extends MeasureNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(TimeSignature.class);

    /** Rational value of each (full) time sig shape */
    private static final Map<Shape, TimeRational> rationals = new EnumMap<>(
            Shape.class);

    static {
        for (Shape s : ShapeSet.FullTimes) {
            if (s != CUSTOM_TIME) {
                TimeRational nd = rationalOf(s);

                if (nd == null) {
                    logger.error("Rational for ''{}'' is not defined", s);
                } else {
                    rationals.put(s, nd);
                }
            }
        }
    }

    /** Set of acceptable N/D values for programmatic recognition */
    private static final Set<TimeRational> acceptables = new HashSet<>();

    static {
        // Predefined
        for (Shape s : ShapeSet.FullTimes) {
            TimeRational nd = rationals.get(s);

            if (nd != null) {
                acceptables.add(nd);
            }
        }

        // A few others
        acceptables.add(new TimeRational(5, 4));
    }

    //~ Instance fields --------------------------------------------------------
    //
    /** Flag a time sig not created out of its glyphs. */
    private final boolean isDummy;

    /**
     * Precise time signature shape (if any, since we may have no
     * predefined shape for complex time signatures).
     *
     * Since a time signature may be composed incrementally through several
     * glyphs, its shape is determined only when needed.
     *
     * In case of failure, a NO_LEGAL_TIME shape is assigned,
     * preventing any further computation until a reset is performed on this
     * item (such a reset is performed for example when an additional glyph is
     * added to the item).
     */
    private Shape shape;

    /** Actual TimeRational components. */
    private TimeRational timeRational;

    //~ Constructors -----------------------------------------------------------
    //
    //---------------//
    // TimeSignature //
    //--------------//
    /**
     * Create a time signature, with containing measure and related staff
     *
     * @param measure the containing measure
     * @param staff   the related staff
     */
    public TimeSignature (Measure measure,
                          Staff staff)
    {
        super(measure);
        isDummy = false;
        setStaff(staff);

        logger.debug("Created TS");
    }

    //---------------//
    // TimeSignature //
    //--------------//
    /**
     * Create a time signature, with containing measure and related
     * staff, and information copied from the provided time signature.
     *
     * @param measure the containing measure
     * @param staff   the related staff
     * @param other   the other time signature to clone
     */
    public TimeSignature (Measure measure,
                          Staff staff,
                          TimeSignature other)
    {
        super(measure);
        isDummy = true;
        setStaff(staff);

        try {
            timeRational = other.timeRational;
            shape = other.getShape();

            if (!staff.isDummy()) {
                setCenter(
                        new Point(
                        other.getCenter().x,
                        other.getCenter().y - other.getStaff().getTopLeft().y
                        + staff.getTopLeft().y));
            }
        } catch (InvalidTimeSignature ex) {
            logger.error("Cannot duplicate TimeSignature", ex);
        }

        logger.debug("Created TS copy");
    }

    //~ Methods ----------------------------------------------------------------
    //
    //--------------//
    // isAcceptable //
    //--------------//
    /**
     * Check whether the provided value is a rather common time signature
     *
     * @param timeRational
     * @return true if the provided sig is acceptable
     */
    public static boolean isAcceptable (TimeRational timeRational)
    {
        if (predefinedShape(timeRational) != null) {
            return true;
        }

        // Check other acceptable rational values
        return acceptables.contains(timeRational);
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //------//
    // copy //
    //------//
    /**
     * Replaces in situ this time signature by the logical information of
     * 'newSig'.
     *
     * @param newSig the correct sig to assign in lieu of this one
     */
    public void copy (TimeSignature newSig)
    {
        try {
            modify(newSig.getShape(), newSig.timeRational);
        } catch (InvalidTimeSignature ex) {
            logger.warn("Invalid time signature", ex);
        }
    }

    //-----------------//
    // createDummyCopy //
    //-----------------//
    public TimeSignature createDummyCopy (Measure measure,
                                          Point center)
    {
        TimeSignature dummy = new TimeSignature(measure, null);
        dummy.setCenter(center);

        dummy.timeRational = timeRational;
        dummy.shape = shape;

        return dummy;
    }

    //----------------//
    // getDenominator //
    //----------------//
    /**
     * Report the bottom part of the time signature.
     *
     * @return the bottom part
     * @throws InvalidTimeSignature
     */
    public Integer getDenominator ()
            throws InvalidTimeSignature
    {
        if (timeRational == null) {
            if (shape == NO_LEGAL_TIME) {
                throw new InvalidTimeSignature();
            } else {
                computeTimeRational();
            }
        }

        if (timeRational != null) {
            return timeRational.den;
        } else {
            return null;
        }
    }

    //----------------------//
    // getDenominatorShapes //
    //----------------------//
    /**
     * Report the sequence of shapes that depict the denominator.
     *
     * @return for example: [1, 6] if denominator is 16
     */
    public List<Shape> getDenominatorShapes ()
    {
        try {
            return getShapes(getDenominator());
        } catch (InvalidTimeSignature ex) {
            return Collections.emptyList();
        }
    }

    //--------------//
    // getNumerator //
    //--------------//
    /**
     * Report the top part of the time signature.
     *
     * @return the top part
     * @throws InvalidTimeSignature
     */
    public Integer getNumerator ()
            throws InvalidTimeSignature
    {
        if (timeRational == null) {
            if (shape == NO_LEGAL_TIME) {
                throw new InvalidTimeSignature();
            } else {
                computeTimeRational();
            }
        }

        if (timeRational != null) {
            return timeRational.num;
        } else {
            return null;
        }
    }

    //--------------------//
    // getNumeratorShapes //
    //--------------------//
    /**
     * Report the sequence of shapes that depict the numerator.
     *
     * @return for example: [1, 2] if numerator is 12
     */
    public List<Shape> getNumeratorShapes ()
    {
        try {
            return getShapes(getNumerator());
        } catch (InvalidTimeSignature ex) {
            return Collections.emptyList();
        }
    }

    //----------//
    // getShape //
    //----------//
    /**
     * Report the shape of this time signature.
     *
     * @return the (lazily determined) shape, which may be null
     * @throws omr.score.entity.TimeSignature.InvalidTimeSignature
     */
    public Shape getShape ()
            throws InvalidTimeSignature
    {
        if (shape == null) {
            computeTimeRational();
        }

        return shape;
    }

    //-----------------//
    // getTimeRational //
    //-----------------//
    /**
     * Report the time signature as a rational
     *
     * @return the num/den time rational, or null
     */
    public TimeRational getTimeRational ()
            throws InvalidTimeSignature
    {
        if (timeRational == null) {
            computeTimeRational();
        }

        return timeRational;
    }

    //---------//
    // isDummy //
    //---------//
    /**
     * Report whether this time signature has been built artificially
     *
     * @return true if artificial
     */
    public boolean isDummy ()
    {
        return isDummy;
    }

    //----------//
    // isManual //
    //----------//
    /**
     * Report whether this time signature is based on manual assignment
     *
     * @return true if manually assigned
     */
    public boolean isManual ()
    {
        return Glyphs.containsManual(getGlyphs());
    }

    //--------//
    // modify //
    //--------//
    /**
     * Modify in situ this time signature using provided shape and
     * rational value.
     * We use the intersected glyphs of the old sig as the glyphs for the newly
     * built signature.
     *
     * @param shape        the shape (perhaps null) of correct signature
     * @param timeRational the new sig rational value
     */
    public void modify (Shape shape,
                        TimeRational timeRational)
    {
        if (!isDummy()) {
            if (shape == null) {
                shape = predefinedShape(timeRational);

                if (shape == null) {
                    shape = Shape.CUSTOM_TIME;
                }
            }

            SystemInfo systemInfo = getSystem().getInfo();
            Glyph compound = systemInfo.buildTransientCompound(
                    getGlyphs());
            compound = systemInfo.addGlyph(compound);
            compound.setShape(shape, Evaluation.ALGORITHM);

            if (shape == Shape.CUSTOM_TIME) {
                compound.setTimeRational(timeRational);
            }

            logger.debug("{} assigned to {}", shape, compound.idString());
        }

        setRational(timeRational, shape);
    }

    //----------//
    // populate //
    //----------//
    /**
     * Populate the score with a time signature built from the provided glyph
     *
     * @param glyph   the source glyph
     * @param measure containing measure
     * @param staff   the related staff
     * @param center  the glyph center wrt system
     * @return true if population is successful, false otherwise
     */
    public static boolean populate (Glyph glyph,
                                    Measure measure,
                                    Staff staff,
                                    Point center)
    {
        // First, some basic tests
        // Horizontal distance since beginning of measure
        int unitDx = center.x - measure.getLeftX();

        if (unitDx < measure.getScale().toPixels(constants.minTimeOffset)) {
            logger.debug("Too small offset for time signature" + " (glyph #{})",
                    glyph.getId());

            return false;
        }

        // Then, processing depends on partial / full time-signature
        Shape shape = glyph.getShape();

        if (ShapeSet.PartialTimes.contains(shape)) {
            return populatePartialTime(glyph, measure, staff);
        } else {
            return populateFullTime(glyph, measure, staff);
        }
    }

    //------------------//
    // populateFullTime //
    //------------------//
    /**
     * We create a full time signature with just the provided glyph.
     * (assumed to be the whole signature, perhaps composed of several digits,
     * for example one digit for the numerator and one digit for the
     * denominator)
     *
     * @param glyph   the provided (multi-digit) glyph
     * @param measure the containing measure
     * @param staff   the related satff
     * @return true if successful
     */
    public static boolean populateFullTime (Glyph glyph,
                                            Measure measure,
                                            Staff staff)
    {
        logger.debug("populateFullTime with {}", glyph);

        TimeSignature ts = measure.getTimeSignature(staff);

        if (ts == null) {
            ts = new TimeSignature(measure, staff);
            ts.addGlyph(glyph);
            glyph.setTranslation(ts);

            return true;
        } else {
            logger.debug("Second whole time signature ({}" + ")"
                         + " in the same measure", glyph.idString());

            return false;
        }
    }

    //------------//
    // rationalOf //
    //------------//
    /**
     * Report the num/den pair of predefined timesig shapes.
     *
     * @param shape the queried shape
     * @return the related num/den or null
     */
    public static TimeRational rationalOf (Shape shape)
    {
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

        case TIME_SIX_EIGHT:
            return new TimeRational(6, 8);

        default:
            return null;
        }
    }

    //-------//
    // reset //
    //-------//
    /**
     * Invalidate cached data, so that it gets lazily recomputed when
     * needed.
     */
    @Override
    public void reset ()
    {
        super.reset();

        shape = null;
        timeRational = null;
    }

    //-------------//
    // setRational //
    //-------------//
    /**
     * Force this time signature to align to the provided rational value.
     *
     * @param timeRational the forced value
     * @param shape        the forced shape, if any
     */
    public void setRational (TimeRational timeRational,
                             Shape shape)
    {
        this.timeRational = timeRational;

        if (shape != null) {
            this.shape = shape;
        } else {
            this.shape = predefinedShape();
        }
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a readable description.
     *
     * @return description
     */
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{TimeSignature");

        try {
            if (getNumerator() != null) {
                sb.append(" ").append(getNumerator()).append("/").append(
                        getDenominator());
            }

            if (getShape() != null) {
                sb.append(" ").append(getShape());
            }

            if (getCenter() != null) {
                sb.append(" center=").append(getCenter());
            }

            if (!getGlyphs().isEmpty()) {
                sb.append(" ").append(Glyphs.toString(getGlyphs()));
            }
        } catch (InvalidTimeSignature e) {
            sb.append(" INVALID");
        }

        sb.append("}");

        return sb.toString();
    }

    //---------------//
    // computeCenter //
    //---------------//
    @Override
    protected void computeCenter ()
    {
        setCenter(computeGlyphsCenter(glyphs));
    }

    //-----------------//
    // getNumericShape //
    //-----------------//
    private static Shape getNumericShape (int val)
    {
        switch (val) {
        case 0:
            return TIME_ZERO;

        case 1:
            return TIME_ONE;

        case 2:
            return TIME_TWO;

        case 3:
            return TIME_THREE;

        case 4:
            return TIME_FOUR;

        case 5:
            return TIME_FIVE;

        case 6:
            return TIME_SIX;

        case 7:
            return TIME_SEVEN;

        case 8:
            return TIME_EIGHT;

        case 9:
            return TIME_NINE;

        case 12:
            return TIME_TWELVE;

        case 16:
            return TIME_SIXTEEN;

        default:
            return null;
        }
    }

    //-----------------//
    // getNumericValue //
    //-----------------//
    private static Integer getNumericValue (Glyph glyph)
    {
        Shape shape = glyph.getShape();

        if (shape != null) {
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
        }

        return null;
    }

    //-----------//
    // getShapes //
    //-----------//
    private List<Shape> getShapes (int val)
    {
        List<Shape> shapes = new ArrayList<>();

        for (; val > 0; val /= 10) {
            int digit = val % 10;
            shapes.add(getNumericShape(digit));
        }

        Collections.reverse(shapes);

        return shapes;
    }

    //---------------------//
    // populatePartialTime //
    //---------------------//
    /**
     * We add the provided glyph to a time signature composed of single
     * digits.
     *
     * @param glyph   the provided (single-digit) glyph
     * @param measure the containing measure
     * @param staff   the related staff
     * @return true if successful
     */
    private static boolean populatePartialTime (Glyph glyph,
                                                Measure measure,
                                                Staff staff)
    {
        logger.debug("populatePartialTime with {}", glyph);

        TimeSignature ts = measure.getTimeSignature(staff);

        if (ts != null) {
            // Check we are not too far from this first time signature part
            Point center = measure.computeGlyphCenter(glyph);
            double dist = center.distance(ts.getCenter());
            double max = measure.getScale().toPixelsDouble(
                    constants.maxTimeDistance);

            if (dist > max) {
                logger.debug("Time signature part ({}" + ")"
                             + " too far from previous one", glyph.idString());

                return false;
            }
        } else {
            // Start a brand new time sig
            ts = new TimeSignature(measure, staff);
        }

        ts.addGlyph(glyph);
        glyph.setTranslation(ts);

        return true;
    }

    //---------------------//
    // computeTimeRational //
    //---------------------//
    /**
     * Compute the actual members of time rational.
     *
     * @throws InvalidTimeSignature
     */
    private void computeTimeRational ()
            throws InvalidTimeSignature
    {
        if (glyphs == null) {
            throw new InvalidTimeSignature();
        }

        if (!glyphs.isEmpty()) {
            if (glyphs.size() == 1) {
                // Just one symbol
                Glyph theGlyph = glyphs.first();
                Shape theShape = theGlyph.getShape();

                if (theShape != null) {
                    if (ShapeSet.FullTimes.contains(theShape)) {
                        TimeRational theRational = (theShape == CUSTOM_TIME)
                                ? theGlyph.getTimeRational()
                                : rationalOf(theShape);

                        if (theRational != null) {
                            shape = theShape;
                            this.timeRational = theRational;
                        }

                        return;
                    }

                    addError(glyphs.first(), "Weird single time component");

                    return;
                }
            } else {
                // Several symbols
                // Dispatch symbols on top and bottom parts
                timeRational = null;

                int numerator = 0;
                int denominator = 0;

                for (Glyph glyph : glyphs) {
                    int pitch = (int) Math.rint(
                            getStaff().pitchPositionOf(computeGlyphCenter(glyph)));
                    Integer value = getNumericValue(glyph);
                    logger.debug("pitch={} value={} glyph={}",
                            pitch, value, glyph);

                    if (value != null) {
                        if (pitch < 0) {
                            numerator = (10 * numerator) + value;
                        } else if (pitch > 0) {
                            denominator = (10 * denominator) + value;
                        } else {
                            addError(
                                    glyph,
                                    "Multi-symbol time signature"
                                    + " with a component of pitch position 0");
                        }
                    } else {
                        addError(
                                glyph,
                                "Time signature component with no numeric value");
                    }
                }

                if ((numerator != 0) && (denominator != 0)) {
                    timeRational = new TimeRational(numerator, denominator);
                }

                // Try to assign a predefined shape
                shape = predefinedShape();
            }

            logger.debug("time rational: {}", timeRational);
        }
    }

    //-----------------//
    // predefinedShape //
    //-----------------//
    /**
     * Look for a predefined shape, if any, that would correspond to the
     * current
     * <code>num</code> and
     * <code>den</code> values of this time
     * sig.
     *
     * @return the shape found or null
     */
    private static Shape predefinedShape (TimeRational timeRational)
    {
        if (timeRational == null) {
            return null; // Safer
        }

        for (Shape s : ShapeSet.FullTimes) {
            TimeRational nd = rationals.get(s);

            if (timeRational.equals(nd)) {
                return s;
            }
        }

        return null;
    }

    //-----------------//
    // predefinedShape //
    //-----------------//
    /**
     * Look for a predefined shape, if any, that would correspond to the
     * current
     * <code>num</code> and
     * <code>den</code> values of this time
     * sig.
     *
     * @return the shape found or null
     */
    private Shape predefinedShape ()
    {
        if (timeRational == null) {
            return null; // Safer
        } else {
            return predefinedShape(timeRational);
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //----------------------//
    // InvalidTimeSignature //
    //----------------------//
    /**
     * Used to signal that a time signature is invalid.
     * (for example because some of its parts are missing or incorrectly
     * recognized)
     */
    public static class InvalidTimeSignature
            extends Exception
    {
        //~ Constructors -------------------------------------------------------

        public InvalidTimeSignature ()
        {
            super("Time signature is invalid");
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction maxTimeDistance = new Scale.Fraction(
                4d,
                "Maximum euclidian distance between two"
                + " parts of a time signature");

        Scale.Fraction minTimeOffset = new Scale.Fraction(
                1d,
                "Minimum horizontal offset for a time"
                + " signature since start of measure");

    }
}
