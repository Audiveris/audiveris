//-----------------------------------------------------------------------//
//                                                                       //
//                               S c a l e                               //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.sheet;

import omr.constant.Constant;
import omr.score.PagePoint;
import omr.score.UnitDimension;
import omr.util.Logger;

import static omr.score.ScoreConstants.*;

import java.awt.Rectangle;

/**
 * Class <code>Scale</code> encapsulates what drives the scale of a given
 * sheet, namely the main lengths of foreground and background vertical
 * lags (which are staff line thickness and white interval between staff
 * lines respectively), and the sum of both which represents the main
 * interline value.
 *
 * <p>This class also provides methods for converting values based on what
 * the interline is actually worth. There are three different measurements
 * :
 *
 * <dl> <dt> <b>pixel</b> </dt> <dd> This is simply an absolute number of
 * pixels, so generally an integer. One pixel is worth 1 pixel (sic) </dd>
 *
 * <dt> <b>(interline) fraction</b> </dt> <dd> This is a number of
 * interlines, so generally a fraction which is implemented as a
 * double. One interline is worth whatever the scale is, generally
 * something around 20 pixels </dd>
 *
 * <dt> <b>unit</b> </dt> <dd> This is a number of 1/16th of interline,
 * since the score display is built on this value. One unit is thus
 * generally worth something like 20/16 of pixels </dd> </dl>
 *
 * @see omr.score.ScoreConstants#BASE
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Scale
    implements java.io.Serializable
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(Scale.class);

    //~ Instance variables ------------------------------------------------

    // Most frequent run lengths for foreground & background runs.
    private int mainFore;
    private int mainBack;
    private int interline;

    // The utility to compute the scale
    private transient ScaleBuilder builder;

    //~ Constructors ------------------------------------------------------

    //-------//
    // Scale //
    //-------//
    /**
     * Create a scale entity, this is meant for allocation after a score is
     * read.
     *
     * @param spacing the score spacing value, expressed using BASE
     *                resolution, since this is an int.
     *
     * @see omr.score.ScoreConstants#BASE
     */
    public Scale (int spacing)
    {
        interline = spacing / BASE;
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
     * Create a scale entity, by processing the provided sheet picture.
     *
     * @param sheet the sheet to process
     *
     * @throws omr.ProcessingException
     */
    public Scale (Sheet sheet)
        throws omr.ProcessingException
    {
        builder = new ScaleBuilder(sheet);
        mainFore = builder.getMainFore();
        mainBack = builder.getMainBack();
        interline = mainFore + mainBack;
    }

    //~ Methods -----------------------------------------------------------

    //--------------//
    // displayChart //
    //--------------//
    /**
     * Build and display the histogram of run lengths
     */
    public void displayChart()
    {
        if (builder != null) {
            builder.displayChart();
        } else {
            logger.warning("Data from scale builder is not available");
        }
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

    //----------------//
    // toPixelsDouble //
    //----------------//
    /**
     * Same as toPixels, but the result is a double instead of a
     * rounded int.
     *
     *
     * @param frac the interline fraction
     * @return the equivalent in number of pixels
     * @see #toPixels
     */
    public double toPixelsDouble (Fraction frac)
    {
        return (double) interline * frac.getValue();
    }

    //----------------//
    // toSquarePixels //
    //----------------//
    /**
     * Compute the squared-normalized number of pixels, according to the
     * scale.
     *
     * @param frac a measure based on interline (1 = one interline)
     *
     * @return the actual squared number of pixels with the current scale
     */
    public int toSquarePixels (Fraction frac)
    {
        double val = toPixelsDouble(frac);

        return (int) Math.rint(val * val);
    }

    //---------//
    // toUnits //
    //---------//
    /**
     * Compute the number of units that corresponds to the fraction of
     * interline provided, according to the scale.
     *
     * @param frac a measure based on interline (1 = one interline)
     *
     * @return the actual number of units with the current scale
     */
    public int toUnits (Fraction frac)
    {
        return (int) Math.rint(toUnitsDouble(frac));
    }

    //-------------//
    // unitsToFrac //
    //-------------//
    /**
     * Transtale a number of units to an interline fraction
     *
     * @param d the number of units
     * @return the corresponding interline fraction
     */
    public double unitsToFrac (double d)
    {
        return d / INTER_LINE;
    }

    //---------------//
    // toUnitsDouble //
    //---------------//
    /**
     * Same as toUnits, but the result is a double instead of a
     * rounded int.
     *
     *
     * @param frac the interline fraction
     * @return the equivalent in number of units
     * @see #toUnits
     */
    public double toUnitsDouble (Fraction frac)
    {
        return INTER_LINE * frac.getValue();
    }

    //-----------//
    // interline //
    //-----------//
    /**
     * Report the interline value this scale is based upon
     *
     * @return the number of pixels (black + white) from one line to the
     *         other.
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
     * Compute the interline fraction that corresponds to the given number
     * of pixels.
     *
     *
     * @param pixels the equivalent in number of pixels
     * @return the interline fraction
     * @see #toPixels
     */
    public double pixelsToFrac (int pixels)
    {
        return (double) pixels / (double) interline;
    }

    //-------------//
    // toPagePoint //
    //-------------//
    /**
     * Convert a point whose coordinates are in pixels to a point whose
     * coordinates are in units.
     *
     * @param pixPt point in pixels
     *
     * @return the result in units
     */
    public PagePoint toPagePoint (PixelPoint pixPt)
    {
        return toPagePoint(pixPt, null);
    }

    //-------------//
    // toPagePoint //
    //-------------//
    /**
     * Convert a point whose coordinates are in pixels to a point whose
     * coordinates are in units.
     *
     * @param pixPt point in pixels
     * @param pagPt equivalent point in units, or null if allocation to be
     *              performed by the routine
     *
     * @return the result in units
     */
    public PagePoint toPagePoint (PixelPoint pixPt,
                                  PagePoint pagPt)
    {
        if (pagPt == null) {
            pagPt = new PagePoint();
        }

        pagPt.x = pixelsToUnits(pixPt.x);
        pagPt.y = pixelsToUnits(pixPt.y);

        return pagPt;
    }

    //---------//
    // toUnits //
    //---------//
    /**
     * Convert a dimension whose compoents are in pixels to a dimension
     * whose components are in units.
     *
     * @param pixelDim dimension in pixels
     *
     * @return the result in units
     */
    public UnitDimension toUnits (PixelDimension pixelDim)
    {
        return toUnits(pixelDim, null);
    }

    //---------//
    // toUnits //
    //---------//
    /**
     * Convert a dimension whose compoents are in pixels to a dimension
     * whose components are in units.
     *
     * @param pixelDim dimension in pixels
     * @param unitDim equivalent dimension in units, or null if allocation to be
     *                performed by the routine
     *
     * @return the result in units
     */
    public UnitDimension toUnits (PixelDimension pixelDim,
                                  UnitDimension  unitDim)
    {
        if (unitDim == null) {
            unitDim = new UnitDimension();
        }

        unitDim.width = pixelsToUnits(pixelDim.width);
        unitDim.height = pixelsToUnits(pixelDim.height);

        return unitDim;
    }

    //---------------//
    // pixelsToUnits //
    //---------------//
    /**
     * Same as pixelsToUnitsDouble, but result is rounded to nearest int.
     *
     * @param pixels number of pixels
     *
     * @return integer number of units
     * @see #pixelsToUnitsDouble
     */
    public int pixelsToUnits (int pixels)
    {
        return (int) Math.rint(pixelsToUnitsDouble(pixels));
    }

    //---------------------//
    // pixelsToUnitsDouble //
    //---------------------//
    /**
     * Convert a number of pixels to its equivalent in units (1/16th of
     * interline)
     *
     * @param pixels number of pixels
     *
     * @return number (may be fraction) of units
     */
    public double pixelsToUnitsDouble (double pixels)
    {
        return (pixels * INTER_LINE) / (double) interline;
    }

    //---------//
    // spacing //
    //---------//
    /**
     * Report a measure of spacing, based on the interline
     *
     * @return an int value, which is the interline value (expressed in
     *         pixels) times the BASE resolution
     * @see omr.score.ScoreConstants#BASE
     */
    public int spacing ()
    {
        return interline * BASE;
    }

    //--------------//
    // toPixelPoint //
    //--------------//
    /**
     * Convert a point with coordinates in units into its equivalent with
     * coordinates in pixels. Reverse function of pixelsToUnits
     *
     * @param pagPt point in units
     *
     * @return the computed point in pixels
     */
    public PixelPoint toPixelPoint (PagePoint pagPt)
    {
        return toPixelPoint(pagPt, null);
    }

    //--------------//
    // toPixelPoint //
    //--------------//
    /**
     * Convert a point with coordinates in units into its equivalent with
     * coordinates in pixels. Reverse function of pixelsToUnits
     *
     * @param pagPt point in units
     * @param pixPt point in pixels, or null if allocation to be made by
     *              the routine
     *
     * @return the computed point in pixels
     */
    public PixelPoint toPixelPoint (PagePoint pagPt,
                                    PixelPoint pixPt)
    {
        if (pixPt == null) {
            pixPt = new PixelPoint();
        }

        pixPt.x = unitsToPixels(pagPt.x);
        pixPt.y = unitsToPixels(pagPt.y);

        return pixPt;
    }

    //----------//
    // toPixels //
    //----------//
    /**
     * Convert a dimension with components in units into its equivalent with
     * components in pixels. Reverse function of pixelsToUnits
     *
     * @param unitDim  dimension in units
     * @param pixelDim dimension in pixels, or null if allocation to be made by
     *                 the routine
     *
     * @return the computed point in pixels
     */
    public PixelDimension toPixels (UnitDimension  unitDim,
                                    PixelDimension pixelDim)
    {
        if (pixelDim == null) {
            pixelDim = new PixelDimension();
        }

        pixelDim.width  = unitsToPixels(unitDim.width);
        pixelDim.height = unitsToPixels(unitDim.height);

        return pixelDim;
    }

    //----------//
    // toPixels //
    //----------//
    /**
     * Convert a dimension with components in units into its equivalent with
     * components in pixels. Reverse function of pixelsToUnits
     *
     * @param unitDim  dimension in units
     *
     * @return the computed point in pixels
     */
    public PixelDimension toPixels (UnitDimension  unitDim)
    {
        return toPixels(unitDim, null);
    }

    //----------//
    // toPixels //
    //----------//
    /**
     * Convert a rectangle with coordinates in units into its equivalent with
     * coordinates in pixels. Reverse function of pixelsToUnits
     *
     * @param pagRect rectangle in units
     *
     * @return the computed rectangle in pixels
     */
    public Rectangle toPixels (Rectangle pagRect)
    {
        return toPixels(pagRect, null);
    }

    //----------//
    // toPixels //
    //----------//
    /**
     * Convert a rectangle with coordinates in units into its equivalent with
     * coordinates in pixels. Reverse function of pixelsToUnits
     *
     * @param pagRect rectangle in units
     * @param pixRect rectangle in pixels, or null if allocation to be made
     *              by the routine
     *
     * @return the computed rectangle in pixels
     */
    public Rectangle toPixels (Rectangle pagRect,
                               Rectangle pixRect)
    {
        if (pixRect == null) {
            pixRect = new Rectangle();
        }

        pixRect.x = unitsToPixels(pagRect.x);
        pixRect.y = unitsToPixels(pagRect.y);
        pixRect.width = unitsToPixels(pagRect.width);
        pixRect.height= unitsToPixels(pagRect.height);

        return pixRect;
    }

    //---------------//
    // unitsToPixels //
    //---------------//
    /**
     * Converts a number of units into its equivalent in pixels
     *
     * @param units number of units
     *
     * @return equivalent number of pixels
     */
    private int unitsToPixels (int units)
    {
        return (units * interline) / INTER_LINE;
    }

    //~ Classes -----------------------------------------------------------

    //----------//
    // Fraction //
    //----------//
    /**
     * A subclass of Double, meant to store a fraction of interline, since
     * many distances on a music sheet are expressed in fraction of staff
     * interline.
     */
    public static class Fraction
        extends Constant.Double
    {
        //~ Constructors --------------------------------------------------

        /**
         * Normal constructor, with a double type for default value
         *
         * @param unit         the enclosing unit
         * @param name         the constant name
         * @param defaultValue the default (double) value
         * @param description  the semantic of the constant
         */
        public Fraction (java.lang.String unit,
                         java.lang.String name,
                         double defaultValue,
                         java.lang.String description)
        {
            super(unit, name, defaultValue, description);
        }

        /**
         * Specific constructor, where 'unit' and 'name' are assigned later
         *
         * @param defaultValue the (double) default value
         * @param description  the semantic of the constant
         */
        public Fraction (double defaultValue,
                         java.lang.String description)
        {
            super(defaultValue, description);
        }
    }
}
