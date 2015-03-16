//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S c a l e                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.constant.Constant;

import omr.util.DoubleValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code Scale} encapsulates what drives the scale of a sheet,
 * namely the main lengths of foreground and background vertical runs
 * (which are staff line thickness and white interval between staff
 * lines respectively), and the sum of these two lengths which
 * represents the main interline value.
 * <p>
 * Primary informations: This data is always detected, otherwise the current
 * page is detected as not being a music page.<ul>
 * <li><b>Staff line thickness</b> (fore): main, max.</li>
 * <li><b>Staff interline</b>: min, main, max.</li>
 * </ul>
 * <p>
 * Secondary informations: This data is always made available, either based on
 * detected value or derived from other information.<ul>
 * <li><b>Beam thickness</b>: main. A second peak in the histogram of vertical
 * foreground runs signals the presence of beams.
 * Otherwise it is computed as a ratio of interline.</li>
 * <li><b>Stem thickness</b>: main. A suitable peak in the histogram of
 * horizontal foreground runs signals the presence of stems.
 * Otherwise it is computed as a ratio of line thickness.</li>
 * </ul>
 * <p>
 * Optional informations: Some of this data may be detected, according to the
 * page at hand.<ul>
 * <li><b>Staff 2nd interline</b>: min, main, max. A second peak in the
 * histogram of vertical background runs signals the presence of staves with a
 * different interline value.</li>
 * </ul>
 * <p>
 * This class also provides methods for converting values based on what the
 * interline and the line thickness are actually worth.
 * There are two different measurements, pixels and fractions:
 *
 * <dl>
 * <dt><b>pixel</b></dt>
 * <dd> This is simply an absolute number of pixels, so generally an
 * integer.</dd>
 * <dt><b>(interline) Fraction</b></dt>
 * <dd> This is a number (or fraction) of interlines.
 * Typical unit value for interline is around 20 pixels.</dd>
 * <dt><b>(interline) AreaFraction</b></dt>
 * <dd> This is a number (or fraction) of square interlines, meant to measure
 * glyph area or weight.
 * Typical unit value for interline area is around 400 pixels.</dd>
 * <dt> <b>LineFraction</b></dt>
 * <dd> This is a number (or fraction) of line thickness.
 * Typical unit value for line is around 4 pixels.</dd>
 * </dl>
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "scale")
public class Scale
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Scale.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Line thickness range. */
    private final Range lineRange;

    /** Main interline range. */
    private final Range interlineRange;

    /** Beam thickness (detected or computed). */
    private final int beamValue;

    /** Second interline range, if any. */
    private final Range secondInterlineRange;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a degenerated scale entity, meant for computing
     * scale-dependent parameters.
     *
     * @param interline the score interline value.
     */
    public Scale (int interline)
    {
        this(interline, -1);
    }

    //-------//
    // Scale //
    //-------//
    /**
     * Create a scale entity, meant for a staff.
     *
     * @param interline the interline value
     * @param mainFore  the line thickness
     */
    public Scale (int interline,
                  int mainFore)
    {
        this(new Range(-1, mainFore, -1), new Range(-1, interline, -1), -1,  null);
    }

    //-------//
    // Scale //
    //-------//
    /**
     * Create a scale entity, meant for a whole sheet.
     *
     * @param lineRange            range of line thickness
     * @param interlineRange       range of interline
     * @param beamValue            beam thickness
     * @param secondInterlineRange range of secondInterline
     */
    public Scale (Range lineRange,
                  Range interlineRange,
                  int beamValue,
                  Range secondInterlineRange)
    {
        this.lineRange = lineRange;
        this.interlineRange = interlineRange;
        this.beamValue = beamValue;
        this.secondInterlineRange = secondInterlineRange;
    }

    //-------//
    // Scale //
    //-------//
    /** No-arg constructor, needed by JAXB. */
    private Scale ()
    {
        this(null, null, -1,  null);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // fracToPixels //
    //--------------//
    /**
     * Convert a fraction of interline to a number of pixels.
     *
     * @param val the fraction value
     * @return the (double) number of pixels
     */
    public double fracToPixels (double val)
    {
        return interlineRange.best * val;
    }

    //--------------//
    // getInterline //
    //--------------//
    /**
     * Report the interline value this scale is based upon.
     *
     * @return the number of pixels (black + white) from one line to the other.
     */
    public int getInterline ()
    {
        return interlineRange.best;
    }

    //-------------//
    // getMainBeam //
    //-------------//
    /**
     * Report the main beam thickness.
     *
     * @return the main beam thickness, either the detected value if any or a
     *         default value computed from interline value if no beam value was
     *         detected.
     */
    public int getMainBeam ()
    {
        return beamValue;
    }

    //-------------//
    // getMainFore //
    //-------------//
    /**
     * Report the line thickness this scale is based upon.
     *
     * @return the number of black pixels in a staff line
     */
    public int getMainFore ()
    {
        return lineRange.best;
    }

    //------------//
    // getMaxFore //
    //------------//
    /**
     * Report the maximum line thickness (using standard percentile).
     *
     * @return the maxFore value
     */
    public int getMaxFore ()
    {
        return lineRange.max;
    }

    //-----------------//
    // getMaxInterline //
    //-----------------//
    /**
     * Report the maximum interline (using standard percentile).
     *
     * @return the maxInterline
     */
    public int getMaxInterline ()
    {
        return interlineRange.max;
    }

    //-----------------------//
    // getMaxSecondInterline //
    //-----------------------//
    /**
     * Report the maximum second interline (using standard percentile).
     *
     * @return the maxSecondInterline if any, otherwise null
     */
    public Integer getMaxSecondInterline ()
    {
        if (secondInterlineRange != null) {
            return secondInterlineRange.max;
        } else {
            return null;
        }
    }

    //-----------------//
    // getMinInterline //
    //-----------------//
    /**
     * Report the minimum interline (using standard percentile).
     *
     * @return the minInterline
     */
    public int getMinInterline ()
    {
        return interlineRange.min;
    }

    //-----------------------//
    // getMinSecondInterline //
    //-----------------------//
    /**
     * Report the minimum second interline (using standard percentile).
     *
     * @return the minSecondInterline if any, otherwise null
     */
    public Integer getMinSecondInterline ()
    {
        if (secondInterlineRange != null) {
            return secondInterlineRange.min;
        } else {
            return null;
        }
    }

    //--------------------//
    // getSecondInterline //
    //--------------------//
    /**
     * Report the second interline value this scale is based upon.
     *
     * @return the second number if any of pixels (black + white) from one line
     *         to the other, otherwise null.
     */
    public Integer getSecondInterline ()
    {
        if (secondInterlineRange != null) {
            return secondInterlineRange.best;
        } else {
            return null;
        }
    }

    //------------------//
    // pixelsToAreaFrac //
    //------------------//
    /**
     * Compute the interline area fraction that corresponds to the
     * given number of pixels.
     *
     * @param pixels the equivalent in number of pixels
     * @return the interline area fraction
     * @see #toPixels
     */
    public double pixelsToAreaFrac (double pixels)
    {
        return pixels / (interlineRange.best * interlineRange.best);
    }

    //--------------//
    // pixelsToFrac //
    //--------------//
    /**
     * Compute the interline fraction that corresponds to the given
     * number of pixels.
     *
     * @param pixels the equivalent in number of pixels
     * @return the interline fraction
     * @see #toPixels
     */
    public double pixelsToFrac (double pixels)
    {
        return pixels / interlineRange.best;
    }

    //------------------//
    // pixelsToLineFrac //
    //------------------//
    /**
     * Compute the line fraction that corresponds to the given
     * number of pixels.
     *
     * @param pixels the equivalent in number of pixels
     * @return the line fraction
     * @see #toPixels
     */
    public double pixelsToLineFrac (double pixels)
    {
        return pixels / lineRange.best;
    }

    //----------//
    // toPixels //
    //----------//
    /**
     * Compute the number of pixels that corresponds to the fraction of
     * interline provided, according to the scale.
     *
     * @param frac a measure based on interline (1 = one interline)
     * @return the actual number of pixels with the current scale
     */
    public int toPixels (Fraction frac)
    {
        return (int) Math.rint(toPixelsDouble(frac));
    }

    //----------//
    // toPixels //
    //----------//
    /**
     * Compute the number of pixels that corresponds to the fraction of
     * line thickness provided, according to the scale.
     *
     * @param lineFrac a measure based on line thickness (1 = one line height)
     * @return the actual number of pixels with the current scale
     */
    public int toPixels (LineFraction lineFrac)
    {
        return (int) Math.rint(toPixelsDouble(lineFrac));
    }

    //----------//
    // toPixels //
    //----------//
    /**
     * Compute the squared-normalized number of pixels, according to
     * the scale.
     *
     * @param areaFrac a measure based on interline (1 = one interline square)
     * @return the actual squared number of pixels with the current scale
     */
    public int toPixels (AreaFraction areaFrac)
    {
        return (int) Math.rint(interlineRange.best * interlineRange.best * areaFrac.getValue());
    }

    //----------------//
    // toPixelsDouble //
    //----------------//
    /**
     * Convenient method, working directly on a constant of interline
     * fraction.
     * Same as toPixels, but the result is a double instead of a rounded int.
     *
     * @param frac the interline fraction constant
     * @return the equivalent in number of pixels
     * @see #toPixels
     */
    public double toPixelsDouble (Fraction frac)
    {
        return fracToPixels(frac.getWrappedValue().doubleValue());
    }

    //----------------//
    // toPixelsDouble //
    //----------------//
    /**
     * Convenient method, working directly on a constant of line
     * fraction.
     * Same as toPixels, but the result is a double instead of a rounded int.
     *
     * @param lineFrac the line fraction constant
     * @return the equivalent in number of pixels
     * @see #toPixels
     */
    public double toPixelsDouble (LineFraction lineFrac)
    {
        return (double) lineRange.best * lineFrac.getWrappedValue().doubleValue();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Scale");

        sb.append(" line: ").append(lineRange);

        sb.append(" interline: ").append(interlineRange);

        sb.append(" beam: ").append(beamValue);

        if (secondInterlineRange != null) {
            sb.append(" secondInterline: ").append(secondInterlineRange);
        }

        return sb.toString();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------------//
    // AreaFraction //
    //--------------//
    /**
     * A subclass of Constant.Double, meant to store a fraction of
     * interline-based area.
     */
    public static class AreaFraction
            extends Constant.Double
    {
        //~ Constructors ---------------------------------------------------------------------------

        /**
         * Specific constructor, where 'unit' and 'name' are assigned
         * later.
         *
         * @param defaultValue the (double) default value
         * @param description  the semantic of the constant
         */
        public AreaFraction (double defaultValue,
                             java.lang.String description)
        {
            super("Interline**2", defaultValue, description);
        }
    }

    //----------//
    // Fraction //
    //----------//
    /**
     * A subclass of Constant.Double, meant to store a fraction of
     * interline, since many distances on a music sheet are expressed
     * in fraction of staff interline (as opposed to
     * {@link Scale.LineFraction} which stores a fraction of line
     * thickness).
     */
    public static class Fraction
            extends Constant.Double
    {
        //~ Static fields/initializers -------------------------------------------------------------

        public static final Fraction ZERO = new Fraction(0, "zero");

        static {
            ZERO.setUnitAndName(Scale.class.getName(), "ZERO");
        }

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Specific constructor, where 'unit' and 'name' are assigned
         * later.
         *
         * @param defaultValue the (double) default value
         * @param description  the semantic of the constant
         */
        public Fraction (double defaultValue,
                         java.lang.String description)
        {
            super("Interline", defaultValue, description);
        }

        // Meant for JAXB
        private Fraction ()
        {
            this(0d, null);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public DoubleValue getWrappedValue ()
        {
            return (DoubleValue) getCachedValue();
        }

        @Override
        public void setValue (double val)
        {
            setTuple(java.lang.Double.toString(val), new DoubleValue(val));
        }

        @Override
        protected DoubleValue decode (java.lang.String str)
        {
            return new DoubleValue(java.lang.Double.valueOf(str));
        }
    }

    //--------------//
    // LineFraction //
    //--------------//
    /**
     * A subclass of Constant.Double, meant to store a fraction of line
     * thickness (as opposed to {@link Scale.Fraction} which stores a
     * fraction of interline).
     */
    public static class LineFraction
            extends Constant.Double
    {
        //~ Constructors ---------------------------------------------------------------------------

        /**
         * Specific constructor, where 'unit' and 'name' are assigned
         * later.
         *
         * @param defaultValue the (double) default value
         * @param description  the semantic of the constant
         */
        public LineFraction (double defaultValue,
                             java.lang.String description)
        {
            super("Line", defaultValue, description);
        }

        // Meant for JAXB
        private LineFraction ()
        {
            this(0d, null);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public DoubleValue getWrappedValue ()
        {
            return (DoubleValue) getCachedValue();
        }

        @Override
        public void setValue (double val)
        {
            setTuple(java.lang.Double.toString(val), new DoubleValue(val));
        }

        @Override
        protected DoubleValue decode (java.lang.String str)
        {
            return new DoubleValue(java.lang.Double.valueOf(str));
        }
    }

    //-------//
    // Range //
    //-------//
    /**
     * Handles a range of values using a (min, best, max) triplet.
     */
    public static class Range
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Value at beginning of range */
        public final int min;

        /** Value at highest point in range */
        public final int best;

        /** Value at end of range */
        public final int max;

        //~ Constructors ---------------------------------------------------------------------------
        public Range (int min,
                      int best,
                      int max)
        {
            this.min = min;
            this.best = best;
            this.max = max;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            return "(" + min + "," + best + "," + max + ")";
        }
    }
}
