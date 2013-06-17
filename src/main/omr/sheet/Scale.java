//----------------------------------------------------------------------------//
//                                                                            //
//                                 S c a l e                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
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
 * lines respectively), and the sum of these lengths which represents
 * the main interline value.
 *
 * <p>This class also provides methods for converting values based on what the
 * interline and the line thickness are actually worth.
 * There are two different measurements, pixels and fractions :
 *
 * <dl> <dt> <b>pixel</b> </dt> <dd> This is simply an absolute number of
 * pixels, so generally an integer. One pixel is worth 1 pixel (sic) </dd>
 *
 * <dt> <b>(interline) fraction</b> </dt> <dd> This is a number of interlines,
 * so generally a fraction which is implemented as a double. One interline is
 * worth whatever the scale is, generally something around 20 pixels </dd>
 *
 * <dt> <b>line fraction</b> </dt> <dd> This is a number of line thickness,
 * so generally a line fraction which is implemented as a double. One line is
 * worth whatever the scale is, generally something around 4 pixels </dd>
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "scale")
public class Scale
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Scale.class);

    //~ Instance fields --------------------------------------------------------
    /** Line thickness range */
    private final Range lineRange;

    /** Main interline range */
    private final Range interlineRange;

    /** Beam thickness, if any */
    private final Integer beamValue;

    /** Second interline range, if any */
    private final Range secondInterlineRange;

    //~ Constructors -----------------------------------------------------------
    //-------//
    // Scale //
    //-------//
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
        this(
                new Range(-1, mainFore, -1),
                new Range(-1, interline, -1),
                null,
                null);
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
                  Integer beamValue,
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
        this(null, null, null, null);
    }

    //~ Methods ----------------------------------------------------------------
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
     * Report the main beam thickness, if any.
     *
     * @return the main beam thickness, or null
     */
    public Integer getMainBeam ()
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
    public Integer getMaxFore ()
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
    public Integer getMaxInterline ()
    {
        return interlineRange.max;
    }

    //-----------------------//
    // getMaxSecondInterline //
    //-----------------------//
    /**
     * Report the maximum second interline (using standard percentile).
     *
     * @return the maxSecondInterline
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
    public Integer getMinInterline ()
    {
        return interlineRange.min;
    }

    //-----------------------//
    // getMinSecondInterline //
    //-----------------------//
    /**
     * Report the minimum second interline (using standard percentile).
     *
     * @return the minSecondInterline
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
        return (int) Math.rint(
                interlineRange.best * interlineRange.best * areaFrac.getValue());
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
        return (double) lineRange.best * lineFrac.getWrappedValue()
                .doubleValue();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Scale");

        sb.append(" line:")
                .append(lineRange);

        sb.append(" interline:")
                .append(interlineRange);

        if (beamValue != null) {
            sb.append(" beam:")
                    .append(beamValue);
        }

        if (secondInterlineRange != null) {
            sb.append(" secondInterline:")
                    .append(secondInterlineRange);
        }

        sb.append("}");

        return sb.toString();
    }

    //~ Inner Classes ----------------------------------------------------------
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
        //~ Constructors -------------------------------------------------------

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
        //~ Constructors -------------------------------------------------------

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

        //~ Methods ------------------------------------------------------------
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
        //~ Constructors -------------------------------------------------------

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

        //~ Methods ------------------------------------------------------------
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
    public static class Range
    {
        //~ Instance fields ----------------------------------------------------

        /** Value at beginning of range */
        public final int min;

        /** Value at highest point in range */
        public final int best;

        /** Value at end of range */
        public final int max;

        //~ Constructors -------------------------------------------------------
        public Range (int min,
                      int best,
                      int max)
        {
            this.min = min;
            this.best = best;
            this.max = max;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public String toString ()
        {
            return "(" + min + "," + best + "," + max + ")";
        }
    }
}
