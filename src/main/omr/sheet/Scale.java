//----------------------------------------------------------------------------//
//                                                                            //
//                                 S c a l e                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.constant.Constant;

import omr.log.Logger;

import omr.step.StepException;

import omr.util.DoubleValue;

import javax.xml.bind.annotation.*;

/**
 * Class <code>Scale</code> encapsulates what drives the scale of a given sheet,
 * namely the main lengths of foreground and background vertical lags (which are
 * staff line thickness and white interval between staff lines respectively),
 * and the sum of both which represents the main interline value.
 *
 * <p>This class also provides methods for converting values based on what the
 * interline is actually worth. There are two different measurements :
 *
 * <dl> <dt> <b>pixel</b> </dt> <dd> This is simply an absolute number of
 * pixels, so generally an integer. One pixel is worth 1 pixel (sic) </dd>
 *
 * <dt> <b>(interline) fraction</b> </dt> <dd> This is a number of interlines,
 * so generally a fraction which is implemented as a double. One interline is
 * worth whatever the scale is, generally something around 20 pixels </dd>
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

    /** The utility to compute the scale */
    private ScaleBuilder builder;

    /** Most frequent vertical distance in pixels from one line to the other*/
    @XmlElement
    private int interline;

    /** Most frequent background height */
    @XmlElement
    private int mainBack;

    /** Most frequent run lengths for foreground & background runs. */
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
     * Create a scale entity, this is meant for a specific staff.
     *
     * @param interline the interline value (for this staff)
     * @param mainFore  the line thickness (for this staff)
     */
    public Scale (int interline,
                  int mainFore)
    {
        this.mainFore = mainFore;
        this.interline = interline;
        mainBack = interline - mainFore;
    }

    //-------//
    // Scale //
    //-------//
    /**
     * Create a scale entity, by analyzing the provided sheet picture.
     *
     * @param sheet the sheet to process
     * @throws StepException thrown if either background or foreground runs
     * could not be measured
     */
    public Scale (Sheet sheet)
        throws StepException
    {
        builder = new ScaleBuilder(sheet);
        mainBack = builder.getMainBack();
        mainFore = builder.getMainFore();

        interline = mainFore + mainBack;

        sheet.getBench()
             .recordScale(this);

        builder.checkInterline(interline);
    }

    //-------//
    // Scale //
    //-------//
    /** Needed by JAXB */
    private Scale ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // displayChart //
    //--------------//
    /**
     * Build and display the histogram of run lengths
     */
    public void displayChart ()
    {
        if (builder != null) {
            builder.displayChart();
        } else {
            logger.warning("Data from scale builder is not available");
        }
    }

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
    // mainBack //
    //----------//
    /**
     * Report the white space between two lines
     *
     * @return the number of white pixels between two staff lines
     */
    public int mainBack ()
    {
        return mainBack;
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
    public double pixelsToFrac (int pixels)
    {
        return (double) pixels / (double) interline;
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
     * interline provided, according to the scale.
     *
     * @param frac a measure based on interline (1 = one interline)
     *
     * @return the actual number of pixels with the current scale
     */
    public int toPixels (InterlineFraction frac)
    {
        return (int) Math.rint(toPixelsDouble(frac));
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
     * Same as toPixels, but the result is a double instead of a rounded int.
     *
     * @param frac the interline fraction
     * @return the equivalent in number of pixels
     * @see #toPixels
     */
    public double toPixelsDouble (InterlineFraction frac)
    {
        return (double) interline * frac.doubleValue();
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
        return toPixelsDouble(frac.getWrappedValue());
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Scale")
          .append(" interline=")
          .append(interline)
          .append(" mainBack=")
          .append(mainBack)
          .append(" mainFore=")
          .append(mainFore)
          .append("}");

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
     * interline.
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

        //~ Methods ------------------------------------------------------------

        @Override
        public void setValue (double val)
        {
            setTuple(
                java.lang.Double.toString(val),
                new InterlineFraction(val));
        }

        @Override
        public InterlineFraction getWrappedValue ()
        {
            return (InterlineFraction) getCachedValue();
        }

        @Override
        protected InterlineFraction decode (java.lang.String str)
        {
            return new InterlineFraction(java.lang.Double.valueOf(str));
        }
    }

    //-------------------//
    // InterlineFraction //
    //-------------------//
    /**
     * Class meant to host a double value, specified in fraction of interline.
     */
    public static class InterlineFraction
        extends DoubleValue
    {
        //~ Constructors -------------------------------------------------------

        public InterlineFraction (double val)
        {
            super(val);
        }

        // Meant for JAXB
        private InterlineFraction ()
        {
            super(0d);
        }

        //~ Methods ------------------------------------------------------------

        //--------//
        // equals //
        //--------//
        @Override
        public boolean equals (Object obj)
        {
            if (obj instanceof InterlineFraction) {
                return ((InterlineFraction) obj).value == value;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode ()
        {
            int hash = 7;

            return hash;
        }
    }
}
