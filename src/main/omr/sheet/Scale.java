//----------------------------------------------------------------------------//
//                                                                            //
//                                 S c a l e                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.constant.Constant;

import omr.log.Logger;

import omr.util.DoubleValue;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>Scale</code> encapsulates what drives the scale of a given sheet,
 * namely the main lengths of foreground and background vertical lags (which are
 * staff line thickness and white interval between staff lines respectively),
 * and the sum of both which represents the main interline value.
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
    private static final Logger logger = Logger.getLogger(Scale.class);

    //~ Instance fields --------------------------------------------------------

    /** Most frequent vertical distance in pixels from one line to the other*/
    @XmlElement
    private int interline;

    /** Second most frequent vertical distance in pixels from one line to the other*/
    @XmlElement
    private Integer secondInterline;

    /** Most frequent foreground height */
    @XmlElement
    private int mainFore;

    //~ Constructors -----------------------------------------------------------

    //-------//
    // Scale //
    //-------//
    /**
     * Create a scale entity, this is meant for allocation after a score is
     * read.
     *
     * @param interline the score interline value.
     */
    public Scale (int interline)
    {
        this.interline = interline;
    }

    //-------//
    // Scale //
    //-------//
    /**
     * Create a scale entity, meant for a whole sheet.
     *
     * @param interline the interline value
     * @param mainFore  the line thickness
     * @param secondInterline the second interline value, or null
     */
    public Scale (int     interline,
                  int     mainFore,
                  Integer secondInterline)
    {
        this.mainFore = mainFore;
        this.interline = interline;
        this.secondInterline = secondInterline;
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
        this(interline, mainFore, null);
    }

    //-------//
    // Scale //
    //-------//
    /** Needed by JAXB */
    public Scale ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // interline //
    //-----------//
    /**
     * Report the interline value this scale is based upon
     *
     * @return the number of pixels (black + white) from one line to the other.
     */
    public int interline ()
    {
        return interline;
    }

    //----------//
    // mainFore //
    //----------//
    /**
     * Report the line thickness this scale is based upon
     *
     * @return the number of black pixels in a staff line
     */
    public int mainFore ()
    {
        return mainFore;
    }

    //------------------//
    // pixelsToAreaFrac //
    //------------------//
    /**
     * Compute the interline area fraction that corresponds to the given number
     * of pixels.
     *
     * @param pixels the equivalent in number of pixels
     * @return the interline area fraction
     * @see #toPixels
     */
    public double pixelsToAreaFrac (double pixels)
    {
        return pixels / (interline * interline);
    }

    //--------------//
    // pixelsToFrac //
    //--------------//
    /**
     * Compute the interline fraction that corresponds to the given number of
     * pixels.
     *
     * @param pixels the equivalent in number of pixels
     * @return the interline fraction
     * @see #toPixels
     */
    public double pixelsToFrac (double pixels)
    {
        return pixels / interline;
    }

    //-----------------//
    // secondInterline //
    //-----------------//
    /**
     * Report the second interline value this scale is based upon
     *
     * @return the second number if any of pixels (black + white) from one line
     * to the other, otherwise null.
     */
    public Integer secondInterline ()
    {
        return secondInterline;
    }

    //----------//
    // toPixels //
    //----------//
    /**
     * Compute the number of pixels that corresponds to the fraction of
     * interline provided, according to the scale.
     *
     * @param frac a measure based on interline (1 = one interline)
     *
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
     * @param lFrac a measure based on line thickness (1 = one line height)
     *
     * @return the actual number of pixels with the current scale
     */
    public int toPixels (LineFraction lFrac)
    {
        return (int) Math.rint(toPixelsDouble(lFrac));
    }

    //----------//
    // toPixels //
    //----------//
    /**
     * Compute the squared-normalized number of pixels, according to the scale.
     *
     * @param areaFrac a measure based on interline (1 = one interline square)
     *
     * @return the actual squared number of pixels with the current scale
     */
    public int toPixels (AreaFraction areaFrac)
    {
        return (int) Math.rint(interline * interline * areaFrac.getValue());
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
        return (double) interline * frac.getWrappedValue()
                                        .doubleValue();
    }

    //----------------//
    // toPixelsDouble //
    //----------------//
    /**
     * Convenient method, working directly on a constant of line fraction.
     * Same as toPixels, but the result is a double instead of a rounded int.
     *
     * @param lFrac the line fraction constant
     * @return the equivalent in number of pixels
     * @see #toPixels
     */
    public double toPixelsDouble (LineFraction lFrac)
    {
        return (double) mainFore * lFrac.getWrappedValue()
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
        sb.append(" mainFore=")
          .append(mainFore);
        sb.append(" interline=")
          .append(interline);

        if (secondInterline != null) {
            sb.append(" secondInterline=")
              .append(secondInterline);
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
         * Specific constructor, where 'unit' and 'name' are assigned later
         *
         * @param defaultValue the (double) default value
         * @param description  the semantic of the constant
         */
        public AreaFraction (double           defaultValue,
                             java.lang.String description)
        {
            super("Interline**2", defaultValue, description);
        }
    }

    //----------//
    // Fraction //
    //----------//
    /**
     * A subclass of Constant.Double, meant to store a fraction of interline,
     * since many distances on a music sheet are expressed in fraction of staff
     * interline (as opposed to {@link Scale.LineFraction} which stores a
     * fraction of line thickness)
     */
    public static class Fraction
        extends Constant.Double
    {
        //~ Constructors -------------------------------------------------------

        /**
         * Specific constructor, where 'unit' and 'name' are assigned later
         *
         * @param defaultValue the (double) default value
         * @param description  the semantic of the constant
         */
        public Fraction (double           defaultValue,
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
        public void setValue (double val)
        {
            setTuple(java.lang.Double.toString(val), new DoubleValue(val));
        }

        @Override
        public DoubleValue getWrappedValue ()
        {
            return (DoubleValue) getCachedValue();
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
     * thickness (as opposed to {@link Scale.Fraction} which stores a fraction
     * of interline)
     */
    public static class LineFraction
        extends Constant.Double
    {
        //~ Constructors -------------------------------------------------------

        /**
         * Specific constructor, where 'unit' and 'name' are assigned later
         *
         * @param defaultValue the (double) default value
         * @param description  the semantic of the constant
         */
        public LineFraction (double           defaultValue,
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
        public void setValue (double val)
        {
            setTuple(java.lang.Double.toString(val), new DoubleValue(val));
        }

        @Override
        public DoubleValue getWrappedValue ()
        {
            return (DoubleValue) getCachedValue();
        }

        @Override
        protected DoubleValue decode (java.lang.String str)
        {
            return new DoubleValue(java.lang.Double.valueOf(str));
        }
    }
}
