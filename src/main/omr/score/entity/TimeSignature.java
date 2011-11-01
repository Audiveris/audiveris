//----------------------------------------------------------------------------//
//                                                                            //
//                         T i m e S i g n a t u r e                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
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
import omr.glyph.ShapeRange;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.visitor.ScoreVisitor;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class <code>TimeSignature</code> encapsulates a time signature, which may be
 * composed of one or several glyphs.
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
    private static final Logger logger = Logger.getLogger(TimeSignature.class);

    /** Rational value of each (full) time sig shape */
    private static final Map<Shape, TimeRational> rationals = new EnumMap<Shape, TimeRational>(
        Shape.class);

    static {
        for (Shape s : ShapeRange.FullTimes) {
            if (s != CUSTOM_TIME_SIGNATURE) {
                TimeRational nd = rationalOf(s);

                if (nd == null) {
                    logger.severe("Rational for '" + s + "' is not defined");
                } else {
                    rationals.put(s, nd);
                }
            }
        }
    }

    /** Set of acceptable N/D values for programmatic recognition */
    private static final Set<TimeRational> acceptables = new HashSet<TimeRational>();

    static {
        // Predefined
        for (Shape s : ShapeRange.FullTimes) {
            TimeRational nd = rationals.get(s);

            if (nd != null) {
                acceptables.add(nd);
            }
        }

        // A few others
        acceptables.add(new TimeRational(5, 4));
    }

    //~ Instance fields --------------------------------------------------------

    /** Flag a time sig not created out of its glyphs */
    private final boolean isDummy;

    /**
     * Precise time signature shape (if any, since we may have no predefined
     * shape for complex time signatures). Since a time signature may be
     * composed incrementally through several glyphs, its shape is determined
     * only when needed. In case of failure, a NO_LEGAL_TIME shape is assigned,
     * preventing any further computation until a reset is performed on this
     * item (such a reset is performed for example when an additional glyph is
     * added to the item).
     */
    private Shape shape;

    /** Actual TimeRational components */
    private TimeRational timeRational;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // TimeSignature //
    //--------------//
    /**
     * Create a time signature, with containing measure and related staff
     *
     * @param measure the containing measure
     * @param staff the related staff
     */
    public TimeSignature (Measure measure,
                          Staff   staff)
    {
        super(measure);
        isDummy = false;
        setStaff(staff);

        if (logger.isFineEnabled()) {
            logger.fine("Created TS");
        }
    }

    //---------------//
    // TimeSignature //
    //--------------//
    /**
     * Create a time signature, with containing measure and related staff, and
     * information copied from the provided time signature
     *
     * @param measure the containing measure
     * @param staff the related staff
     * @param other the other time signature to clone
     */
    public TimeSignature (Measure       measure,
                          Staff         staff,
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
                    new PixelPoint(
                        other.getCenter().x,
                        other.getCenter().y - other.getStaff().getTopLeft().y +
                        staff.getTopLeft().y));
            }
        } catch (InvalidTimeSignature ex) {
            logger.severe("Cannot duplicate TimeSignature", ex);
        }

        if (logger.isFineEnabled()) {
            logger.fine("Created TS copy");
        }
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // isAcceptable //
    //--------------//
    /**
     * Check whether the provided value is a rather common time signature
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

    //----------------//
    // getDenominator //
    //----------------//
    /**
     * Report the bottom part of the time signature
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
     * Report the sequence of shapes that depict the denominator
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

    //---------//
    // isDummy //
    //---------//
    /**
     * Report whether this time signature has been built artificially
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
     * @return true if manually assigned
     */
    public boolean isManual ()
    {
        return Glyphs.containsManual(getGlyphs());
    }

    //--------------//
    // getNumerator //
    //--------------//
    /**
     * Report the top part of the time signature
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
     * Report the sequence of shapes that depict the numerator
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

    //-------------//
    // setTimeRational //
    //-------------//
    /**
     * Force this time signature to align to the provided rational value
     * @param timeRational the forced value
     */
    public void setRational (TimeRational timeRational)
    {
        this.timeRational = timeRational;
        shape = predefinedShape();
    }

    //----------//
    // getShape //
    //----------//
    /**
     * Report the shape of this time signature
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
     * Replaces in situ this time signature by the logical information
     * of 'newSig'.
     * @param newSig the correct sig to assign in lieu of this one
     */
    public void copy (TimeSignature newSig)
    {
        try {
            modify(newSig.getShape(), newSig.timeRational);
        } catch (InvalidTimeSignature ex) {
            logger.warning("Invalid time signature", ex);
        }
    }

    //-----------------//
    // createDummyCopy //
    //-----------------//
    public TimeSignature createDummyCopy (Measure    measure,
                                          PixelPoint center)
    {
        TimeSignature dummy = new TimeSignature(measure, null);
        dummy.setCenter(center);

        dummy.timeRational = timeRational;
        dummy.shape = shape;

        return dummy;
    }

    //----------//
    // populate //
    //----------//
    /**
     * Populate the score with a time signature built from the provided glyph
     *
     * @param glyph the source glyph
     * @param measure containing measure
     * @param staff the related staff
     * @param center the glyph center wrt system
     *
     * @return true if population is successful, false otherwise
     */
    public static boolean populate (Glyph      glyph,
                                    Measure    measure,
                                    Staff      staff,
                                    PixelPoint center)
    {
        // First, some basic tests
        // Horizontal distance since beginning of measure
        int unitDx = center.x - measure.getLeftX();

        if (unitDx < measure.getScale()
                            .toPixels(constants.minTimeOffset)) {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "Too small offset for time signature" + " (glyph #" +
                    glyph.getId() + ")");
            }

            return false;
        }

        // Then, processing depends on partial / full time-signature
        Shape shape = glyph.getShape();

        if (ShapeRange.PartialTimes.contains(shape)) {
            return populatePartialTime(glyph, measure, staff);
        } else {
            return populateFullTime(glyph, measure, staff);
        }
    }

    //------------------//
    // populateFullTime //
    //------------------//
    /**
     * We create a full time signature with just the provided glyph (assumed to
     * be the whole signature, perhaps composed of several digits, for example
     * one digit for the numerator and one digit for the denominator)
     * @param glyph the provided (multi-digit) glyph
     * @param measure the containing measure
     * @param staff the related satff
     * @return true if successful
     */
    public static boolean populateFullTime (Glyph   glyph,
                                            Measure measure,
                                            Staff   staff)
    {
        if (logger.isFineEnabled()) {
            logger.fine("populateFullTime with " + glyph);
        }

        TimeSignature ts = measure.getTimeSignature(staff);

        if (ts == null) {
            ts = new TimeSignature(measure, staff);
            ts.addGlyph(glyph);
            glyph.setTranslation(ts);

            return true;
        } else {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "Second whole time signature" + " (glyph#" + glyph.getId() +
                    ")" + " in the same measure");
            }

            return false;
        }
    }

    //------------//
    // rationalOf //
    //------------//
    /**
     * Report the num/den pair of predefined timesig shapes
     * @param shape the queried shape
     * @return the related num/den or null
     */
    public static TimeRational rationalOf (Shape shape)
    {
        switch (shape) {
        case COMMON_TIME :
        case TIME_FOUR_FOUR :
            return new TimeRational(4, 4);

        case CUT_TIME :
        case TIME_TWO_TWO :
            return new TimeRational(2, 2);

        case TIME_TWO_FOUR :
            return new TimeRational(2, 4);

        case TIME_THREE_FOUR :
            return new TimeRational(3, 4);

        case TIME_SIX_EIGHT :
            return new TimeRational(6, 8);

        default :
            return null;
        }
    }

    //--------//
    // modify //
    //--------//
    /**
     * Modify in situ this time signature using provided shape and
     * rational. We use the intersected glyphs of the old sig as the glyphs
     * for the newly built signature.
     * @param shape the shape (perhaps null) of correct signature
     * @param timeRational the new sig rational value
     */
    public void modify (Shape        shape,
                        TimeRational timeRational)
    {
        if (!isDummy()) {
            if (shape == null) {
                shape = predefinedShape(timeRational);

                if (shape == null) {
                    shape = Shape.CUSTOM_TIME_SIGNATURE;
                }
            }

            SystemInfo systemInfo = getSystem()
                                        .getInfo();
            Glyph      compound = systemInfo.buildTransientCompound(
                getGlyphs());
            systemInfo.computeGlyphFeatures(compound);
            compound = systemInfo.addGlyph(compound);

            compound.setShape(shape, Evaluation.ALGORITHM);

            if (shape == Shape.CUSTOM_TIME_SIGNATURE) {
                compound.setTimeRational(timeRational);
            }

            if (logger.isFineEnabled()) {
                logger.fine(shape + " assigned to glyph#" + compound.getId());
            }
        }

        setRational(timeRational);
    }

    //-------//
    // reset //
    //-------//
    /**
     * Invalidate cached data, so that it gets lazily recomputed when needed
     */
    @Override
    public void reset ()
    {
        super.reset();

        shape = null;
        timeRational = null;
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a readable description
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
                sb.append(" ")
                  .append(getNumerator())
                  .append("/")
                  .append(getDenominator());
            }

            if (getShape() != null) {
                sb.append(" ")
                  .append(getShape());
            }

            sb.append(" center=")
              .append(getCenter());

            if (!getGlyphs()
                     .isEmpty()) {
                sb.append(" ")
                  .append(Glyphs.toString(getGlyphs()));
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
        case 0 :
            return TIME_ZERO;

        case 1 :
            return TIME_ONE;

        case 2 :
            return TIME_TWO;

        case 3 :
            return TIME_THREE;

        case 4 :
            return TIME_FOUR;

        case 5 :
            return TIME_FIVE;

        case 6 :
            return TIME_SIX;

        case 7 :
            return TIME_SEVEN;

        case 8 :
            return TIME_EIGHT;

        case 9 :
            return TIME_NINE;

        case 12 :
            return TIME_TWELVE;

        case 16 :
            return TIME_SIXTEEN;

        default :
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
            case TIME_ZERO :
                return 0;

            case TIME_ONE :
                return 1;

            case TIME_TWO :
                return 2;

            case TIME_THREE :
                return 3;

            case TIME_FOUR :
                return 4;

            case TIME_FIVE :
                return 5;

            case TIME_SIX :
                return 6;

            case TIME_SEVEN :
                return 7;

            case TIME_EIGHT :
                return 8;

            case TIME_NINE :
                return 9;

            case TIME_TWELVE :
                return 12;

            case TIME_SIXTEEN :
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
        List<Shape> shapes = new ArrayList<Shape>();

        for (; val > 0; val /= 10) {
            int digit = val % 10;
            shapes.add(getNumericShape(digit));
        }

        Collections.reverse(shapes);

        return shapes;
    }

    //---------------------//
    // computeTimeRational //
    //---------------------//
    /**
     * Compute the actual members of time rational
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
                    if (ShapeRange.FullTimes.contains(theShape)) {
                        TimeRational theRational = (theShape == CUSTOM_TIME_SIGNATURE)
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
                    int     pitch = (int) Math.rint(
                        getStaff().pitchPositionOf(computeGlyphCenter(glyph)));
                    Integer value = getNumericValue(glyph);

                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "pitch=" + pitch + " value=" + value + " glyph=" +
                            glyph);
                    }

                    if (value != null) {
                        if (pitch < 0) {
                            numerator = (10 * numerator) + value;
                        } else if (pitch > 0) {
                            denominator = (10 * denominator) + value;
                        } else {
                            addError(
                                glyph,
                                "Multi-symbol time signature" +
                                " with a component of pitch position 0");
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

            if (logger.isFineEnabled()) {
                logger.fine("time rational: " + timeRational);
            }
        }
    }

    //---------------------//
    // populatePartialTime //
    //---------------------//
    /**
     * We add the provided glyph to a time signature composed of single digits
     * @param glyph the provided (single-digit) glyph
     * @param measure the containing measure
     * @param staff the related staff
     * @return true if successful
     */
    private static boolean populatePartialTime (Glyph   glyph,
                                                Measure measure,
                                                Staff   staff)
    {
        if (logger.isFineEnabled()) {
            logger.fine("populatePartialTime with " + glyph);
        }

        TimeSignature ts = measure.getTimeSignature(staff);

        if (ts != null) {
            // Check we are not too far from this first time signature part
            PixelPoint center = measure.computeGlyphCenter(glyph);
            double     dist = center.distance(ts.getCenter());
            double     max = measure.getScale()
                                    .toPixelsDouble(constants.maxTimeDistance);

            if (dist > max) {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Time signature part" + " (glyph#" + glyph.getId() +
                        ")" + " too far from previous one");
                }

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

    //-----------------//
    // predefinedShape //
    //-----------------//
    /**
     * Look for a predefined shape, if any, that would correspond to the current
     * num and den values of this time sig
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

    //-----------------//
    // predefinedShape //
    //-----------------//
    /**
     * Look for a predefined shape, if any, that would correspond to the current
     * num and den values of this time sig
     * @return the shape found or null
     */
    private static Shape predefinedShape (TimeRational timeRational)
    {
        if (timeRational == null) {
            return null; // Safer
        }

        for (Shape s : ShapeRange.FullTimes) {
            TimeRational nd = rationals.get(s);

            if (timeRational.equals(nd)) {
                return s;
            }
        }

        return null;
    }

    //~ Inner Classes ----------------------------------------------------------

    //----------------------//
    // InvalidTimeSignature //
    //----------------------//
    /**
     * Used to signal that a time signature is invalid (for example because
     * some of its parts are missing or incorrectly recognized)
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
            "Maximum euclidian distance between two" +
            " parts of a time signature");
        Scale.Fraction minTimeOffset = new Scale.Fraction(
            1d,
            "Minimum horizontal offset for a time" +
            " signature since start of measure");
    }
}
