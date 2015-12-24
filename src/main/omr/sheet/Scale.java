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
import omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code Scale} encapsulates what drives the scale of a sheet, namely the main
 * lengths of foreground and background vertical runs (which are staff line thickness
 * and white interval between staff lines respectively), and the sum of these two
 * lengths which represents the main interline value.
 * <p>
 * Primary informations: This data is always detected, otherwise the current page is detected as not
 * being a music page.<ul>
 * <li><b>Staff line thickness</b> (fore): main, max.</li>
 * <li><b>Staff interline</b>: min, main, max.</li>
 * </ul>
 * <p>
 * Secondary informations: This data is always made available, either based on detected value or
 * derived from other information.<ul>
 * <li><b>Beam thickness</b>: main. A second peak in the histogram of vertical foreground runs
 * signals the presence of beams.
 * Otherwise it is computed as a ratio of main background length between staff lines.</li>
 * <li><b>Stem thickness</b>: main. A suitable peak in the histogram of horizontal foreground runs
 * signals the presence of stems.
 * Otherwise it is computed as a ratio of line thickness.</li>
 * </ul>
 * <p>
 * Optional informations: Some of this data may be detected, according to the page at hand.<ul>
 * <li><b>Staff 2nd interline</b>: min, main, max. A second peak in the histogram of vertical
 * background runs signals the presence of staves with a different interline value.</li>
 * </ul>
 * <p>
 * This class also provides methods for converting values based on what the interline and the line
 * thickness are actually worth.
 * There are two different measurements, pixels and fractions:
 *
 * <dl>
 * <dt><b>pixel</b></dt>
 * <dd> This is simply an absolute number of pixels, so generally an integer.</dd>
 * <dt><b>(interline) Fraction</b></dt>
 * <dd> This is a number (or fraction) of interlines.
 * Typical unit value for interline is around 20 pixels.</dd>
 * <dt><b>(interline) AreaFraction</b></dt>
 * <dd> This is a number (or fraction) of square interlines, meant to measure glyph area or weight.
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

    private static final Logger logger = LoggerFactory.getLogger(
            Scale.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Line thickness range. */
    @XmlElement(name = "line")
    private final Range lineRange;

    /** Main interline range. */
    @XmlElement(name = "interline")
    private final Range interlineRange;

    /** Second interline range, if any. */
    @XmlElement(name = "second-interline")
    private final Range secondInterlineRange;

    /** Beam information. */
    @XmlElement(name = "beam")
    private final BeamScale beamScale;

    /** Stem information. */
    @XmlElement(name = "stem")
    private StemScale stemScale;

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
        this(new Range(-1, mainFore, -1), new Range(-1, interline, -1), null, null);
    }

    //-------//
    // Scale //
    //-------//
    /**
     * Create a scale entity, meant for a whole sheet.
     *
     * @param lineRange            range of line thickness
     * @param interlineRange       range of interline
     * @param secondInterlineRange range of secondInterline
     */
    public Scale (Range lineRange,
                  Range interlineRange,
                  Range secondInterlineRange,
                  BeamScale beamScale)
    {
        this.lineRange = lineRange;
        this.interlineRange = interlineRange;
        this.secondInterlineRange = secondInterlineRange;
        this.beamScale = beamScale;
    }

    //-------//
    // Scale //
    //-------//
    /** No-arg constructor, needed by JAXB. */
    private Scale ()
    {
        this(null, null, null, null);
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
        return interlineRange.main * val;
    }

    //---------------------//
    // getBeamMeanDistance //
    //---------------------//
    /**
     * Report the mean value for vertical distance between grouped beams.
     *
     * @return mean value for vertical beam gap
     */
    public Double getBeamMeanDistance ()
    {
        return beamScale.getDistanceMean();
    }

    //----------------------//
    // getBeamSigmaDistance //
    //----------------------//
    /**
     * Report the standard deviation for vertical distance between grouped beams.
     *
     * @return standard deviation for vertical beam gap
     */
    public Double getBeamSigmaDistance ()
    {
        return beamScale.getDistanceSigma();
    }

    //----------------------//
    // getBeamThicknessMain //
    //----------------------//
    public int getBeamThicknessMain ()
    {
        Objects.requireNonNull(beamScale, "This scale instance has no beam information");

        return beamScale.getMain();
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
        return interlineRange.main;
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
        return lineRange.main;
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

    //------------//
    // getMaxStem //
    //------------//
    /**
     * @return the reasonable maximum stem thickness
     */
    public int getMaxStem ()
    {
        return stemScale.getMax();
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
            return secondInterlineRange.main;
        } else {
            return null;
        }
    }

    //----------------------//
    // getStemMainThickness //
    //----------------------//
    /**
     * @return the most frequent stem thickness
     */
    public int getStemMainThickness ()
    {
        return stemScale.getMain();
    }

    //--------------------//
    // isBeamExtrapolated //
    //--------------------//
    public boolean isBeamExtrapolated ()
    {
        Objects.requireNonNull(beamScale, "This scale instance has no beam information");

        return beamScale.isExtrapolated();
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
        return pixels / (interlineRange.main * interlineRange.main);
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
        return pixels / interlineRange.main;
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
        return pixels / lineRange.main;
    }

    //-----------------//
    // setBeamDistance //
    //-----------------//
    /**
     * Remember vertical distance between grouped beams
     *
     * @param meanValue
     * @param standardDeviation
     */
    public void setBeamDistance (double meanValue,
                                 double standardDeviation)
    {
        beamScale.setDistance(meanValue, standardDeviation);
    }

    //--------------//
    // setStemScale //
    //--------------//
    /**
     * Remember stem scaling information.
     *
     * @param stemScale stem scaling
     */
    public void setStemScale (StemScale stemScale)
    {
        this.stemScale = stemScale;
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
        return (int) Math.rint(interlineRange.main * interlineRange.main * areaFrac.getValue());
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
        return (double) lineRange.main * lineFrac.getWrappedValue().doubleValue();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");

        sb.append("line: ").append(lineRange);
        sb.append(" interline: ").append(interlineRange);

        if (secondInterlineRange != null) {
            sb.append(" secondInterline: ").append(secondInterlineRange);
        }

        if (beamScale != null) {
            sb.append(" ").append(beamScale);
        }

        if (stemScale != null) {
            sb.append(" ").append(stemScale);
        }

        sb.append("}");

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

    //-----------//
    // BeamScale //
    //-----------//
    /**
     * Class {@code BeamScale} keeps scaling information about beams in a sheet.
     * <p>
     * Generally, there are enough beam-based vertical runs in a sheet to produce a visible peak in
     * the sheet histogram of vertical run lengths.
     * In some cases, however, there is just a few beam-based vertical runs, and so the sheet
     * histogram reveals no beam peak. The main value for beam thickness is then extrapolated as a
     * pre-defined fraction of sheet interline, and flagged as such in this BeamScale instance.
     * <p>
     * When beams are actually retrieved, the vertical distance between beams of the same group is
     * also measured and recorded in this BeamScale as mean value and standard deviation.
     */
    public static class BeamScale
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Most frequent beam thickness. */
        @XmlAttribute(name = "main-thickness")
        private final int main;

        /** Measured or extrapolated. */
        @XmlAttribute(name = "extra")
        private final Boolean extra;

        /** Mean vertical distance (center to center) between beams of the same group. */
        @XmlAttribute(name = "mean-distance")
        @XmlJavaTypeAdapter(Jaxb.Double1Adapter.class)
        private Double distanceMean;

        /** Standard deviation for vertical distance between beams of the same group. */
        @XmlAttribute(name = "sigma-distance")
        @XmlJavaTypeAdapter(Jaxb.Double1Adapter.class)
        private Double distanceSigma;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new {@code BeamScale} object.
         *
         * @param main         most frequent beam thickness
         * @param extrapolated true if not enough measurements are available
         */
        public BeamScale (int main,
                          boolean extrapolated)
        {
            this.main = main;

            this.extra = extrapolated ? true : null;
        }

        /**
         * No-arg constructor needed for JAXB.
         */
        private BeamScale ()
        {
            this.main = 0;
            this.extra = null;
        }

        //~ Methods --------------------------------------------------------------------------------
        /**
         * @return the distanceMean
         */
        public Double getDistanceMean ()
        {
            return distanceMean;
        }

        /**
         * @return the distanceSigma
         */
        public Double getDistanceSigma ()
        {
            return distanceSigma;
        }

        public int getMain ()
        {
            return main;
        }

        public boolean isExtrapolated ()
        {
            return extra != null;
        }

        /**
         * @param distanceMean  the distanceMean to set
         * @param distanceSigma the distanceSigma to set
         */
        public void setDistance (double distanceMean,
                                 double distanceSigma)
        {
            this.distanceMean = distanceMean;
            this.distanceSigma = distanceSigma;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("beam");
            sb.append('{');
            sb.append("main:").append(main);

            if (extra != null) {
                sb.append(" extra");
            }

            if (distanceMean != null) {
                sb.append(" gapMean:").append(distanceMean);
                sb.append(" gapSigma:").append(distanceSigma);
            }

            sb.append('}');

            return sb.toString();
        }
    }

    //----------//
    // Fraction //
    //----------//
    /**
     * A subclass of Constant.Double, meant to store a fraction of interline, since many
     * distances on a music sheet are expressed as fraction of staff interline (as
     * opposed to {@link Scale.LineFraction} which stores a fraction of line thickness).
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
         * Specific constructor, where 'unit' and 'name' are assigned later.
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
     * A subclass of Constant.Double, meant to store a fraction of line thickness (as
     * opposed to {@link Scale.Fraction} which stores a fraction of interline).
     */
    public static class LineFraction
            extends Constant.Double
    {
        //~ Constructors ---------------------------------------------------------------------------

        /**
         * Specific constructor, where 'unit' and 'name' are assigned later.
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
     * Handles a range of values using a (min, main, max) triplet.
     */
    @XmlAccessorType(XmlAccessType.NONE)
    @XmlRootElement(name = "range")
    public static class Range
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Value at beginning of range. */
        @XmlAttribute
        public final int min;

        /** Value at highest point in range. */
        @XmlAttribute
        public final int main;

        /** Value at end of range. */
        @XmlAttribute
        public final int max;

        //~ Constructors ---------------------------------------------------------------------------
        public Range (int min,
                      int main,
                      int max)
        {
            this.min = min;
            this.main = main;
            this.max = max;
        }

        public Range ()
        {
            this.min = 0;
            this.main = 0;
            this.max = 0;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            return "(" + min + "," + main + "," + max + ")";
        }
    }

    //-----------//
    // StemScale //
    //-----------//
    /**
     * Class {@code StemScale} keeps scaling information about stems in a sheet.
     * <p>
     * It handles main and max values for stem thickness.
     * <p>
     * TODO: It could also handle stem length, which can be interesting for stem candidates (at
     * least their tail for those free of beam and flag).
     */
    public static class StemScale
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Most frequent stem thickness. */
        @XmlAttribute(name = "main-thickness")
        private final int main;

        /** Maximum stem thickness. */
        @XmlAttribute(name = "max-thickness")
        private final int max;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new {@code StemScale} object.
         *
         * @param main most frequent thickness
         * @param max  max thickness
         */
        public StemScale (int main,
                          int max)
        {
            this.main = main;
            this.max = max;
        }

        /**
         * No-arg constructor needed for JAXB.
         */
        private StemScale ()
        {
            this.main = 0;
            this.max = 0;
        }

        //~ Methods --------------------------------------------------------------------------------
        //---------//
        // getMain //
        //---------//
        /**
         * @return the main
         */
        public int getMain ()
        {
            return main;
        }

        //--------//
        // getMax //
        //--------//
        /**
         * @return the max
         */
        public int getMax ()
        {
            return max;
        }

        //----------//
        // toString //
        //----------//
        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder(getClass().getSimpleName());
            sb.append('{');
            sb.append("main:").append(main);
            sb.append(" max:").append(max);
            sb.append('}');

            return sb.toString();
        }
    }
}
