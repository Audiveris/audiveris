//----------------------------------------------------------------------------//
//                                                                            //
//                         T i m e S i g n a t u r e                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Glyphs;
import omr.glyph.Shape;
import static omr.glyph.Shape.*;
import omr.glyph.ShapeRange;

import omr.log.Logger;

import omr.score.common.SystemPoint;
import omr.score.visitor.ScoreVisitor;

import omr.sheet.Scale;

import java.util.*;

/**
 * Class <code>TimeSignature</code> encapsulates a time signature, which may be
 * composed of one or several glyphs.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
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
    private static Map<Shape, Rational> rationals = new HashMap<Shape, Rational>();

    static {
        for (Shape s : ShapeRange.MultiTimes) {
            Rational nd = rationalOf(s);

            if (nd == null) {
                logger.severe("Rational for '" + s + "' is not defined");
            } else {
                rationals.put(s, nd);
            }
        }
    }

    //~ Instance fields --------------------------------------------------------

    /** Rational component : numerator */
    private Integer numerator;

    /** Rational component : denominator */
    private Integer denominator;

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

    /**
     * The glyph(s) that compose the time signature, a set which is kept
     * sorted on glyph abscissa. This can be just one : e.g. TIME_SIX_EIGHT for
     * 6/8, or several : e.g. TIME_SIX + TIME_TWELVE for 6/12
     */
    private SortedSet<Glyph> glyphs = Glyphs.set();

    /** Flag a time sig not created out of its glyphs */
    private final boolean isDummy;

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
            numerator = new Integer(other.getNumerator());
            denominator = new Integer(other.getDenominator());
            shape = other.getShape();
            setCenter(
                new SystemPoint(
                    other.getCenter().x,
                    other.getCenter().y - other.getStaff().getPageTopLeft().y +
                    staff.getPageTopLeft().y));
        } catch (InvalidTimeSignature ex) {
            logger.severe("Cannot duplicate TimeSignature", ex);
        }
    }

    //~ Methods ----------------------------------------------------------------

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
        if (denominator == null) {
            if (shape == NO_LEGAL_TIME) {
                throw new InvalidTimeSignature();
            } else {
                computeRational();
            }
        }

        return denominator;
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

    //-----------//
    // getGlyphs //
    //-----------//
    public Collection<Glyph> getGlyphs ()
    {
        return glyphs;
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
        if (numerator == null) {
            if (shape == NO_LEGAL_TIME) {
                throw new InvalidTimeSignature();
            } else {
                computeRational();
            }
        }

        return numerator;
    }

    //----------//
    // getShape //
    //----------//
    /**
     * Report the shape of this time signature
     *
     * @return the (lazily determined) shape
     * @throws omr.score.entity.TimeSignature.InvalidTimeSignature
     */
    public Shape getShape ()
        throws InvalidTimeSignature
    {
        if (shape == null) {
            computeRational();
        }

        return shape;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //----------//
    // addGlyph //
    //----------//
    /**
     * Add a new glyph as part of this time signature
     *
     * @param glyph the new component glyph
     */
    public void addGlyph (Glyph glyph)
    {
        glyphs.add(glyph);
        reset();
    }

    //-----------------//
    // createDummyCopy //
    //-----------------//
    public TimeSignature createDummyCopy (Measure     measure,
                                          SystemPoint center)
    {
        TimeSignature dummy = new TimeSignature(measure, null);
        dummy.setCenter(center);

        dummy.numerator = numerator;
        dummy.denominator = denominator;
        dummy.shape = shape;

        return dummy;
    }

    //----------//
    // deassign //
    //----------//
    public void deassign ()
    {
        for (Glyph glyph : glyphs) {
            glyph.setShape(null);
        }
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
    public static boolean populate (Glyph       glyph,
                                    Measure     measure,
                                    Staff       staff,
                                    SystemPoint center)
    {
        // First, some basic tests
        // Horizontal distance since beginning of measure
        int unitDx = center.x - measure.getLeftX();

        if (unitDx < measure.getScale()
                            .toUnits(constants.minTimeOffset)) {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "Too small offset for time signature" + " (glyph #" +
                    glyph.getId() + ")");
            }

            return false;
        }

        // Then, processing depends on single/multi time signature
        Shape shape = glyph.getShape();

        if (ShapeRange.SingleTimes.contains(shape)) {
            return populateSingleTime(glyph, measure, staff);
        } else {
            return populateMultiTime(glyph, measure, staff);
        }
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
        return Glyphs.containsManualShape(getGlyphs());
    }

    //-------//
    // reset //
    //-------//
    /**
     * Invalidate cached data, so that it gets lazily recomputed when needed
     */
    public void reset ()
    {
        setCenter(null);
        shape = null;
        numerator = null;
        denominator = null;
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

    //-----------------//
    // computeRational //
    //-----------------//
    private void computeRational ()
        throws InvalidTimeSignature
    {
        if (glyphs == null) {
            throw new InvalidTimeSignature();
        }

        if (!glyphs.isEmpty()) {
            if (glyphs.size() == 1) {
                // Just one symbol
                Shape theShape = glyphs.first()
                                       .getShape();

                if (theShape != null) {
                    if (ShapeRange.MultiTimes.contains(theShape)) {
                        Rational nd = rationalOf(theShape);
                        shape = theShape;
                        numerator = nd.num;
                        denominator = nd.den;

                        return;
                    }

                    addError(glyphs.first(), "Weird single time component");

                    return;
                }
            } else {
                // Several symbols
                // Dispatch symbols on top and bottom parts
                numerator = denominator = null;

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
                            if (numerator == null) {
                                numerator = 0;
                            }

                            numerator = (10 * numerator) + value;
                        } else if (pitch > 0) {
                            if (denominator == null) {
                                denominator = 0;
                            }

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

                // Try to assign a predefined shape
                shape = predefinedShape();
            }

            if (logger.isFineEnabled()) {
                logger.fine(
                    "numerator=" + numerator + " denominator=" + denominator);
            }
        }
    }

    //-------------------//
    // populateMultiTime //
    //-------------------//
    /**
     * We create a full time signature with just the provided glyph (assumed to
     * be the whole signature, perhaps composed of several digits, for example
     * one digit for the numerator and one digit for the denominator)
     * @param glyph the provided (multi-digit) glyph
     * @param measure the containing measure
     * @param staff the related satff
     * @return true if successful
     */
    private static boolean populateMultiTime (Glyph   glyph,
                                              Measure measure,
                                              Staff   staff)
    {
        if (logger.isFineEnabled()) {
            logger.fine("populateMultiTime with " + glyph);
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

    //--------------------//
    // populateSingleTime //
    //--------------------//
    /**
     * We add the provided glyph to a time signature composed of single digits
     * @param glyph the provided (single-digit) glyph
     * @param measure the containing measure
     * @param staff the related staff
     * @return true if successful
     */
    private static boolean populateSingleTime (Glyph   glyph,
                                               Measure measure,
                                               Staff   staff)
    {
        if (logger.isFineEnabled()) {
            logger.fine("populateSingleTime with " + glyph);
        }

        TimeSignature ts = measure.getTimeSignature(staff);

        if (ts != null) {
            // Check we are not too far from this first time signature part
            SystemPoint center = measure.computeGlyphCenter(glyph);
            double      unitDist = center.distance(ts.getCenter());
            double      unitMax = measure.getScale()
                                         .toUnitsDouble(
                constants.maxTimeDistance);

            if (unitDist > unitMax) {
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
        if ((numerator == null) || (denominator == null)) {
            return null; // Safer
        }

        for (Shape s : ShapeRange.MultiTimes) {
            Rational nd = rationals.get(s);

            if ((nd != null) &&
                (nd.num == numerator) &&
                (nd.den == denominator)) {
                return s;
            }
        }

        return null;
    }

    //------------//
    // rationalOf //
    //------------//
    /**
     * Report the num/den pair of predefined timesig shapes
     * @param shape the queried shape
     * @return the related num/den or null
     */
    private static Rational rationalOf (Shape shape)
    {
        switch (shape) {
        case TIME_FOUR_FOUR :
            return new Rational(4, 4);

        case TIME_TWO_TWO :
            return new Rational(2, 2);

        case TIME_TWO_FOUR :
            return new Rational(2, 4);

        case TIME_THREE_FOUR :
            return new Rational(3, 4);

        case TIME_SIX_EIGHT :
            return new Rational(6, 8);

        case COMMON_TIME :
            return new Rational(4, 4);

        case CUT_TIME :
            return new Rational(2, 2);

        default :
            return null;
        }
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

    //----------//
    // Rational //
    //----------//
    /**
     * Meant to host a pair numerator and denominator
     */
    private static class Rational
    {
        //~ Instance fields ----------------------------------------------------

        final int num;
        final int den;

        //~ Constructors -------------------------------------------------------

        public Rational (int num,
                         int den)
        {
            this.num = num;
            this.den = den;
        }
    }
}
