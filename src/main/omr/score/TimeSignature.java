//----------------------------------------------------------------------------//
//                                                                            //
//                         T i m e S i g n a t u r e                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import static omr.glyph.Shape.*;

import omr.score.visitor.ScoreVisitor;

import omr.sheet.Scale;

import omr.util.Logger;

import java.util.*;

/**
 * Class <code>TimeSignature</code> encapsulates a time signature, which may be
 * composed of one or several sticks.
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

    //~ Instance fields --------------------------------------------------------

    /** Rational component : numerator */
    private Integer numerator;

    /** Rational component : denominator */
    private Integer denominator;

    /**
     * Precise time signature shape (if any, since we may have no predefined
     * shape for complex time signatures). Since a time signature may be
     * composed incrementally through several glyphs, its shape is determined
     * only when needed. In case of failure, a NO_LEGAL_SHAPE is assigned,
     * preventing any further computation until a reset is performed on this
     * item (such a reset is performed for example when an additional glyph is
     * added to the item).
     */
    private Shape shape;

    /**
     * The glyph(s) that compose the time signature, a collection which is kept
     * sorted on glyph abscissa. This can be just one : e.g. TIME_SIX_EIGHT for
     * 6/8, or several : e.g. TIME_SIX + TIME_TWELVE for 6/12
     */
    private SortedSet<Glyph> glyphs = new TreeSet<Glyph>();

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
        setStaff(staff);
    }

    //~ Methods ----------------------------------------------------------------

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

    //----------------//
    // getDenominator //
    //----------------//
    /**
     * Report the bottom part of the time signature
     *
     * @return the bottom part
     * @throws omr.score.TimeSignature.InvalidTimeSignature
     */
    public Integer getDenominator ()
        throws InvalidTimeSignature
    {
        if (denominator == null) {
            if (shape == NO_LEGAL_SHAPE) {
                throw new InvalidTimeSignature();
            } else {
                computeRational();
            }
        }

        return denominator;
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
     * @throws omr.score.TimeSignature.InvalidTimeSignature
     */
    public Integer getNumerator ()
        throws InvalidTimeSignature
    {
        if (numerator == null) {
            if (shape == NO_LEGAL_SHAPE) {
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
     * @throws omr.score.TimeSignature.InvalidTimeSignature
     */
    public Shape getShape ()
        throws InvalidTimeSignature
    {
        if (shape == null) {
            computeRational();
        }

        return shape;
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

            sb.append(" shape=")
              .append(getShape());

            sb.append(" center=")
              .append(getCenter());

            if (getGlyphs() != null) {
                sb.append(" glyphs[");

                for (Glyph glyph : getGlyphs()) {
                    sb.append("#")
                      .append(glyph.getId());
                }

                sb.append("]");
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
    static boolean populate (Glyph       glyph,
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

        if (SingleTimes.contains(shape)) {
            return populateSingleTime(glyph, measure, staff);
        } else {
            return populateMultiTime(glyph, measure, staff);
        }
    }

    //----------------//
    // assignRational //
    //----------------//
    private void assignRational (Shape shape,
                                 int   numerator,
                                 int   denominator)
    {
        this.shape = shape;
        this.numerator = numerator;
        this.denominator = denominator;
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

        if (glyphs.size() > 0) {
            if (glyphs.size() == 1) {
                // Just one symbol
                Shape shape = glyphs.first()
                                    .getShape();

                if (shape != null) {
                    switch (shape) {
                    case TIME_FOUR_FOUR :
                        assignRational(shape, 4, 4);

                        return;

                    case TIME_TWO_TWO :
                        assignRational(shape, 2, 2);

                        return;

                    case TIME_TWO_FOUR :
                        assignRational(shape, 2, 4);

                        return;

                    case TIME_THREE_FOUR :
                        assignRational(shape, 3, 4);

                        return;

                    case TIME_SIX_EIGHT :
                        assignRational(shape, 6, 8);

                        return;

                    case COMMON_TIME :
                        assignRational(shape, 4, 4);

                        return;

                    case CUT_TIME :
                        assignRational(shape, 2, 2);

                        return;

                    default :
                        addError(glyphs.first(), "Weird single time component");
                        shape = NO_LEGAL_SHAPE;
                        throw new InvalidTimeSignature();
                    }
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
            }

            if (logger.isFineEnabled()) {
                logger.fine(
                    "numerator=" + numerator + " denominator=" + denominator);
            }
        }
    }

    //-----------------//
    // getNumericValue //
    //-----------------//
    private Integer getNumericValue (Glyph glyph)
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

    //-------------------//
    // populateMultiTime //
    //-------------------//
    private static boolean populateMultiTime (Glyph   glyph,
                                              Measure measure,
                                              Staff   staff)
    {
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
    private static boolean populateSingleTime (Glyph   glyph,
                                               Measure measure,
                                               Staff   staff)
    {
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
            ts = new TimeSignature(measure, staff);
        }

        ts.addGlyph(glyph);
        glyph.setTranslation(ts);

        return true;
    }

    //~ Inner Classes ----------------------------------------------------------

    //----------------------//
    // InvalidTimeSignature //
    //----------------------//
    public static class InvalidTimeSignature
        extends Exception
    {
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
        /**
         * Maximum euclidian distance between two parts
         * of a time signature
         */
        Scale.Fraction maxTimeDistance = new Scale.Fraction(
            4d,
            "Maximum euclidian distance between two" +
            " parts of a time signature");

        /**
         * Minimum horizontal offset for a time
         * signature since start of measure
         */
        Scale.Fraction minTimeOffset = new Scale.Fraction(
            1d,
            "Minimum horizontal offset for a time" +
            " signature since start of measure");
    }
}
