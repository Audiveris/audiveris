//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S c a l e                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.math.Range;
import org.audiveris.omr.util.DoubleValue;
import org.audiveris.omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import javax.xml.bind.Unmarshaller;
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
 * <li><b>Staff line thickness</b> (fore): min, main, max.</li>
 * <li><b>Staff interline</b>: min, main, max. (Vertical distance measured from line center to line
 * center)</li>
 * </ul>
 * Secondary informations: This data is always made available, either based on detected value or
 * derived from other information.<ul>
 * <li><b>Beam thickness</b>: main. A second peak in the histogram of vertical foreground runs
 * signals the presence of beams.
 * Otherwise it is computed as a ratio of main background length between staff lines.</li>
 * <li><b>Stem thickness</b>: main, max. These values are computed during STEM_SEEDS step.</li>
 * </ul>
 * Optional informations: This data may exist or not, according to the sheet at hand.<ul>
 * <li><b>Small staff interline</b>: min, main, max.
 * If a second peak is detected in the histogram of background runs, it signals the presence of
 * staves with a different interline value.
 * In that case, "small interline" will point to the smaller and "interline" to the larger.</li>
 * </ul>
 * This class also provides methods for converting values based on what the interline and the line
 * thickness are actually worth.
 * There are two different measurements, pixels and fractions:
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
    /** Line thickness scale. */
    @XmlElement(name = "line")
    private final LineScale lineScale;

    /** Large interline scale. */
    @XmlElement(name = "interline")
    private InterlineScale interlineScale;

    /** Small interline scale, if any. */
    @XmlElement(name = "small-interline")
    private InterlineScale smallInterlineScale;

    /** Second interline scale, if any. Temporarily kept for compatibility. */
    @XmlElement(name = "second-interline")
    private InterlineScale interlineScale2;

    /** Beam scale. */
    @XmlElement(name = "beam")
    private final BeamScale beamScale;

    /** Stem scale. */
    @XmlElement(name = "stem")
    private StemScale stemScale;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a scale entity, meant for a whole sheet.
     *
     * @param lineScale           scale of line thickness
     * @param interlineScale      scale of (large) interline
     * @param smallInterlineScale scale of small interline, perhaps null
     * @param beamScale           scale of beam
     */
    public Scale (LineScale lineScale,
                  InterlineScale interlineScale,
                  InterlineScale smallInterlineScale,
                  BeamScale beamScale)
    {
        this.lineScale = lineScale;
        this.interlineScale = interlineScale;
        this.smallInterlineScale = smallInterlineScale;
        this.beamScale = beamScale;

        interlineScale2 = null;
    }

    /** No-arg constructor, needed by JAXB. */
    private Scale ()
    {
        this(null, null, null, null);
    }

    //~ Methods ------------------------------------------------------------------------------------
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

    //------------------//
    // getBeamThickness //
    //------------------//
    /**
     * Report the main thickness for beams.
     *
     * @return main beam thickness
     */
    public int getBeamThickness ()
    {
        Objects.requireNonNull(beamScale, "This scale instance has no beam information");

        return beamScale.getMain();
    }

    //---------//
    // getFore //
    //---------//
    /**
     * Report the line thickness this scale is based upon.
     *
     * @return the number of black pixels in a staff line
     */
    public int getFore ()
    {
        return lineScale.main;
    }

    //--------------//
    // getInterline //
    //--------------//
    /**
     * Report the main interline value this scale is based upon.
     *
     * @return the number of pixels (black + white) from one line to the other.
     */
    public int getInterline ()
    {
        return interlineScale.main;
    }

    //--------------//
    // getInterline //
    //--------------//
    /**
     * Report the main interline value within the small or large family
     *
     * @param small true for small family, false for large family
     * @return the smaller interline value
     */
    public int getInterline (boolean small)
    {
        if (!small) {
            return interlineScale.main;
        }

        if (smallInterlineScale != null) {
            return smallInterlineScale.main;
        }

        throw new IllegalArgumentException("No small interline value");
    }

    //-------------------//
    // getInterlineScale //
    //-------------------//
    /**
     * Report the (large) interline scale.
     *
     * @return the interlineScale
     */
    public InterlineScale getInterlineScale ()
    {
        return interlineScale;
    }

    //-------------------//
    // getInterlineScale //
    //-------------------//
    /**
     * Report the large or small scale, according to boolean value.
     *
     * @param small true for getting small scale, false for getting large scale
     * @return the desired interlineScale, perhaps null if no small exists
     */
    public InterlineScale getInterlineScale (boolean small)
    {
        return small ? smallInterlineScale : interlineScale;
    }

    //-------------------//
    // getInterlineScale //
    //-------------------//
    /**
     * Report the scale that applies for the provided (staff) interline value.
     *
     * @param interline the provided (staff specific) interline
     * @return the desired interlineScale
     */
    public InterlineScale getInterlineScale (int interline)
    {
        if (interlineScale.main == interline) {
            return interlineScale;
        }

        if ((smallInterlineScale != null) && (smallInterlineScale.main == interline)) {
            return smallInterlineScale;
        }

        throw new IllegalArgumentException("No interline scale for provided value " + interline);
    }

    //------------------------//
    // getLargeInterlineScale //
    //------------------------//
    /**
     * Same as {@link #getInterlineScale()}, method defined for completeness.
     *
     * @return the (large) interlineScale
     */
    public InterlineScale getLargeInterlineScale ()
    {
        return interlineScale;
    }

    //------------//
    // getMaxFore //
    //------------//
    /**
     * Report the maximum line thickness (using standard percentile).
     *
     * @return the max fore value
     */
    public int getMaxFore ()
    {
        return lineScale.max;
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
        return interlineScale.max;
    }

    //----------------------//
    // getMaxSmallInterline //
    //----------------------//
    /**
     * Report the maximum value of small interline if any (using standard percentile).
     *
     * @return the max smallInterline if any, otherwise null
     */
    public Integer getMaxSmallInterline ()
    {
        if (smallInterlineScale != null) {
            return smallInterlineScale.max;
        } else {
            return null;
        }
    }

    //------------//
    // getMaxStem //
    //------------//
    /**
     * Report the reasonable maximum stem width within the sheet.
     *
     * @return the reasonable maximum stem thickness
     */
    public int getMaxStem ()
    {
        return stemScale.getMax();
    }

    //------------//
    // getMinFore //
    //------------//
    /**
     * Report the minimum line thickness (using standard percentile).
     *
     * @return the min fore value
     */
    public int getMinFore ()
    {
        return lineScale.min;
    }

    //-----------------//
    // getMinInterline //
    //-----------------//
    /**
     * Report the minimum (large) interline (using standard percentile).
     *
     * @return the minInterline
     */
    public int getMinInterline ()
    {
        return interlineScale.min;
    }

    //----------------------//
    // getMinSmallInterline //
    //----------------------//
    /**
     * Report the minimum value of small interline (using standard percentile).
     *
     * @return the min of smallInterline if any, otherwise null
     */
    public Integer getMinSmallInterline ()
    {
        if (smallInterlineScale != null) {
            return smallInterlineScale.min;
        } else {
            return null;
        }
    }

    //-------------------//
    // getSmallInterline //
    //-------------------//
    /**
     * Report the small interline value, if any.
     *
     * @return the small interline, perhaps null.
     */
    public Integer getSmallInterline ()
    {
        if (smallInterlineScale == null) {
            return null;
        }

        return smallInterlineScale.main;
    }

    //------------------------//
    // getSmallInterlineScale //
    //------------------------//
    /**
     * Report the small interline scale if any.
     *
     * @return the smallInterlineScale, perhaps null
     */
    public InterlineScale getSmallInterlineScale ()
    {
        return smallInterlineScale;
    }

    //------------------//
    // getStemThickness //
    //------------------//
    /**
     * Report the most frequent stem thickness (width).
     *
     * @return the most frequent stem thickness
     */
    public int getStemThickness ()
    {
        return stemScale.getMain();
    }

    //--------------------//
    // isBeamExtrapolated //
    //--------------------//
    /**
     * Tell whether beam scaling info is extrapolated (rather than measured).
     *
     * @return true for extrapolated, false for measured
     */
    public boolean isBeamExtrapolated ()
    {
        Objects.requireNonNull(beamScale, "This scale instance has no beam information");

        return beamScale.isExtrapolated();
    }

    //------------------//
    // pixelsToAreaFrac //
    //------------------//
    /**
     * Compute the interline area fraction that corresponds to the given number of pixels.
     *
     * @param pixels the equivalent in number of pixels
     * @return the interline area fraction
     * @see #toPixels
     */
    public double pixelsToAreaFrac (double pixels)
    {
        return pixels / (interlineScale.main * interlineScale.main);
    }

    //--------------//
    // pixelsToFrac //
    //--------------//
    /**
     * Compute the interline fraction that corresponds to the given number of pixels.
     *
     * @param pixels the equivalent in number of pixels
     * @return the interline fraction
     * @see #toPixels
     */
    public double pixelsToFrac (double pixels)
    {
        return pixels / interlineScale.main;
    }

    //------------------//
    // pixelsToLineFrac //
    //------------------//
    /**
     * Compute the line fraction that corresponds to the given number of pixels.
     *
     * @param pixels the equivalent in number of pixels
     * @return the line fraction
     * @see #toPixels
     */
    public double pixelsToLineFrac (double pixels)
    {
        return pixels / lineScale.main;
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
     * Compute the number of pixels that corresponds to the fraction of interline
     * provided, according to the scale.
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
     * Compute the number of pixels that corresponds to the fraction of line thickness
     * provided, according to the scale.
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
     * Compute the squared-normalized number of pixels, according to the scale.
     *
     * @param areaFrac a measure based on interline (1 = one interline square)
     * @return the actual squared number of pixels with the current scale
     */
    public int toPixels (AreaFraction areaFrac)
    {
        return InterlineScale.toPixels(interlineScale.main, areaFrac);
    }

    //----------------//
    // toPixelsDouble //
    //----------------//
    /**
     * Convenient method, working directly on a constant of interline fraction.
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
        return (double) lineScale.main * lineFrac.getWrappedValue().doubleValue();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");

        sb.append("line").append(lineScale);
        sb.append(" interline").append(interlineScale);

        if (smallInterlineScale != null) {
            sb.append(" smallInterline").append(smallInterlineScale);
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

    //----------------//
    // afterUnmarshal //
    //----------------//
    /**
     * Called after all the properties (except IDREF) are unmarshalled for this object,
     * but before this object is set to the parent object.
     */
    @SuppressWarnings("unused")
    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        // Migrate old interline / interline2 to (large) interline / small interline
        if (interlineScale2 != null) {
            InterlineScale larger = (interlineScale.main > interlineScale2.main) ? interlineScale
                    : interlineScale2;
            InterlineScale smaller = (interlineScale.main < interlineScale2.main) ? interlineScale
                    : interlineScale2;
            interlineScale = larger;
            smallInterlineScale = smaller;
            interlineScale2 = null;
        }
    }

    //--------------//
    // fracToPixels //
    //--------------//
    /**
     * Convert a fraction of interline to a number of pixels.
     *
     * @param val the fraction value
     * @return the (double) number of pixels
     */
    private double fracToPixels (double val)
    {
        return interlineScale.main * val;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------------//
    // AreaFraction //
    //--------------//
    /**
     * A subclass of Constant.Double, meant to store a fraction of interline-based area.
     */
    public static class AreaFraction
            extends Constant.Double
    {
        //~ Constructors ---------------------------------------------------------------------------

        /**
         * Specific constructor, where 'unit' and 'name' are assigned later.
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
     * When beams are actually retrieved (during BEAMS step), the vertical distance between beams of
     * the same group is also measured and recorded in this BeamScale as mean value and standard
     * deviation.
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
         * Report the average distance (center to center) between beams of the same group.
         *
         * @return the distanceMean
         */
        public Double getDistanceMean ()
        {
            return distanceMean;
        }

        /**
         * Report the standard deviation of beam distance.
         *
         * @return the distanceSigma
         */
        public Double getDistanceSigma ()
        {
            return distanceSigma;
        }

        /**
         * Report the most frequent beam distance
         *
         * @return the most frequent beam distance
         */
        public int getMain ()
        {
            return main;
        }

        /**
         * Tell whether beam thickness is extrapolated (rather than measured)
         *
         * @return true for extrapolated, false for measured
         */
        public boolean isExtrapolated ()
        {
            return extra != null;
        }

        /**
         * Record mean and sigma values for inter-beam distance
         *
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
            sb.append('(');
            sb.append(main);

            if (extra != null) {
                sb.append(" extra");
            }

            if (distanceMean != null) {
                sb.append(String.format(" gapMean:%.1f", distanceMean));
                sb.append(String.format(" gapStd:%.1f", distanceSigma));
            }

            sb.append(')');

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

    //----------------//
    // InterlineScale //
    //----------------//
    /**
     * Range of values for staff line vertical distance (center to center).
     */
    public static class InterlineScale
            extends Range
    {
        //~ Constructors ---------------------------------------------------------------------------

        public InterlineScale (Range range)
        {
            this(range.min, range.main, range.max);
        }

        public InterlineScale (int min,
                               int main,
                               int max)
        {
            super(min, main, max);
        }

        /** Meant for JAXB. */
        protected InterlineScale ()
        {
            this(0, 0, 0);
        }

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Compute the interline fraction that corresponds to the given number of pixels.
         *
         * @param pixels the equivalent in number of pixels
         * @return the interline fraction
         * @see #toPixels
         */
        public double pixelsToFrac (double pixels)
        {
            return pixels / main;
        }

        public static int toPixels (int interline,
                                    Fraction frac)
        {
            return (int) Math.rint(toPixelsDouble(interline, frac));
        }

        /**
         * Compute the squared-normalized number of pixels, according to the provided
         * interline.
         *
         * @param interline provided interline value
         * @param areaFrac  a measure based on interline (1 = one interline square)
         * @return the actual squared number of pixels with the current scale
         */
        public static int toPixels (int interline,
                                    AreaFraction areaFrac)
        {
            return (int) Math.rint(interline * interline * areaFrac.getValue());
        }

        public static double toPixelsDouble (int interline,
                                             Fraction frac)
        {
            return interline * frac.getWrappedValue().doubleValue();
        }

        /**
         * Compute the squared-normalized number of pixels.
         *
         * @param areaFrac a measure based on interline (1 = one interline square)
         * @return the actual squared number of pixels with the current scale
         */
        public int toPixels (AreaFraction areaFrac)
        {
            return toPixels(main, areaFrac);
        }

        /**
         * Compute the number of pixels that corresponds to the fraction of interline
         * provided, according to the scale.
         *
         * @param frac a measure based on interline (1 = one interline)
         * @return the actual number of pixels with the current scale
         */
        public int toPixels (Fraction frac)
        {
            return toPixels(main, frac);
        }

        /**
         * Convenient method, working directly on a constant of interline fraction.
         * Same as toPixels, but the result is a double instead of a rounded integer.
         *
         * @param frac the interline fraction constant
         * @return the equivalent in number of pixels
         * @see #toPixels
         */
        public double toPixelsDouble (Fraction frac)
        {
            return toPixelsDouble(main, frac);
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

    //-----------//
    // LineScale //
    //-----------//
    /**
     * Range of values for staff line thickness.
     */
    public static class LineScale
            extends Range
    {
        //~ Constructors ---------------------------------------------------------------------------

        public LineScale (Range range)
        {
            this(range.min, range.main, range.max);
        }

        public LineScale (int min,
                          int main,
                          int max)
        {
            super(min, main, max);
        }

        /** Meant for JAXB. */
        protected LineScale ()
        {
            this(0, 0, 0);
        }

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Compute the number of pixels that corresponds to the fraction of line
         * thickness provided, according to the scale.
         *
         * @param lineFrac a measure based on line thickness (1 = one line height)
         * @return the actual number of pixels with the current scale
         */
        public int toPixels (LineFraction lineFrac)
        {
            return (int) Math.rint(toPixelsDouble(lineFrac));
        }

        /**
         * Convenient method, working directly on a constant of line fraction.
         * Same as toPixels, but the result is a double instead of a rounded integer.
         *
         * @param lineFrac the line fraction constant
         * @return the equivalent in number of pixels
         * @see #toPixels
         */
        public double toPixelsDouble (LineFraction lineFrac)
        {
            return (double) main * lineFrac.getWrappedValue().doubleValue();
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
        /**
         * Report the most frequent stem thickness
         *
         * @return the main value
         */
        public int getMain ()
        {
            return main;
        }

        /**
         * Report the reasonable maximum stem thickness.
         *
         * @return the max
         */
        public int getMax ()
        {
            return max;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("stem");
            sb.append('(');
            sb.append(main);
            sb.append(" max:").append(max);
            sb.append(')');

            return sb.toString();
        }
    }
}
