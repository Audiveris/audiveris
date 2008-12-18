//----------------------------------------------------------------------------//
//                                                                            //
//                                 S c a l e                                  //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.constant.Constant;

import omr.log.Logger;

import omr.score.common.PagePoint;
import omr.score.common.PageRectangle;
import omr.score.common.PixelDimension;
import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.common.UnitDimension;
import static omr.score.ui.ScoreConstants.*;

import omr.step.StepException;

import java.awt.Rectangle;

/**
 * Class <code>Scale</code> encapsulates what drives the scale of a given sheet,
 * namely the main lengths of foreground and background vertical lags (which are
 * staff line thickness and white interval between staff lines respectively),
 * and the sum of both which represents the main interline value.
 *
 * <p>This class also provides methods for converting values based on what the
 * interline is actually worth. There are three different measurements :
 *
 * <dl> <dt> <b>pixel</b> </dt> <dd> This is simply an absolute number of
 * pixels, so generally an integer. One pixel is worth 1 pixel (sic) </dd>
 *
 * <dt> <b>(interline) fraction</b> </dt> <dd> This is a number of interlines,
 * so generally a fraction which is implemented as a double. One interline is
 * worth whatever the scale is, generally something around 20 pixels </dd>
 *
 * <dt> <b>unit</b> </dt> <dd> This is a number of 1/16th of interline, since
 * the score display is built on this value. One unit is thus generally worth
 * something like 20/16 of pixels </dd> </dl>
 *
 * @see omr.score.ui.ScoreConstants#BASE
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Scale
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Scale.class);

    //~ Instance fields --------------------------------------------------------

    /** The utility to compute the scale */
    private ScaleBuilder builder;

    /** Most frequent vertical distance in pixels from one line to the other*/
    private int interline;

    /** Most frequent background height */
    private int mainBack;

    /** Most frequent run lengths for foreground & background runs. */
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
     * Create a scale entity, by processing the provided sheet picture.
     *
     * @param sheet the sheet to process
     *
     * @throws StepException
     */
    public Scale (Sheet sheet)
        throws StepException
    {
        builder = new ScaleBuilder(sheet);
        mainFore = builder.getMainFore();
        mainBack = builder.getMainBack();
        interline = mainFore + mainBack;
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
                                  PagePoint  pagPt)
    {
        if (pagPt == null) {
            pagPt = new PagePoint();
        }

        pagPt.x = pixelsToUnits(pixPt.x);
        pagPt.y = pixelsToUnits(pixPt.y);

        return pagPt;
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
     * @param pixPt point in pixels, or null if allocation to be made by the
     *              routine
     *
     * @return the computed point in pixels
     */
    public PixelPoint toPixelPoint (PagePoint  pagPt,
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

        pixelDim.width = unitsToPixels(unitDim.width);
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
    public PixelDimension toPixels (UnitDimension unitDim)
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
     * @param pixRect rectangle in pixels, or null if allocation to be made by
     *              the routine
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
        pixRect.height = unitsToPixels(pagRect.height);

        return pixRect;
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
    public double toPixelsDouble (Fraction frac)
    {
        return (double) interline * frac.getValue();
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

    //---------//
    // toUnits //
    //---------//
    /**
     * Compute the number of units that corresponds to the fraction of interline
     * provided, according to the scale.
     *
     * @param frac a measure based on interline (1 = one interline)
     *
     * @return the actual number of units with the current scale
     */
    public int toUnits (Fraction frac)
    {
        return (int) Math.rint(toUnitsDouble(frac));
    }

    //---------//
    // toUnits //
    //---------//
    /**
     * Convert a dimension whose compoents are in pixels to a dimension whose
     * components are in units.
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
     * Convert a dimension whose compoents are in pixels to a dimension whose
     * components are in units.
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

    //---------//
    // toUnits //
    //---------//
    /**
     * Convert a rectangle whose components are in pixels to a rectangle whose
     * components are in units.
     *
     * @param pixelRect rectangle in pixels
     *
     * @return the result in units
     */
    public PageRectangle toUnits (PixelRectangle pixelRect)
    {
        return toUnits(pixelRect, null);
    }

    //---------//
    // toUnits //
    //---------//
    /**
     * Convert a rectangle whose components are in pixels to a rectangle whose
     * components are in units.
     *
     * @param pixelRect rectangle in pixels
     * @param unitRect equivalent rectangle in units, or null if allocation to be
     *                performed by the routine
     *
     * @return the result in units
     */
    public PageRectangle toUnits (PixelRectangle pixelRect,
                                  PageRectangle  unitRect)
    {
        if (unitRect == null) {
            unitRect = new PageRectangle();
        }

        unitRect.x = pixelsToUnits(pixelRect.x);
        unitRect.y = pixelsToUnits(pixelRect.y);
        unitRect.width = pixelsToUnits(pixelRect.width);
        unitRect.height = pixelsToUnits(pixelRect.height);

        return unitRect;
    }

    //---------------//
    // toUnitsDouble //
    //---------------//
    /**
     * Same as toUnits, but the result is a double instead of a rounded int.
     *
     * @param frac the interline fraction
     * @return the equivalent in number of units
     * @see #toUnits
     */
    public double toUnitsDouble (Fraction frac)
    {
        return INTER_LINE * frac.getValue();
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

    //~ Inner Classes ----------------------------------------------------------

    //--------------//
    // AreaFraction //
    //--------------//
    /**
     * A subclass of Double, meant to store a fraction of interline-based area.
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
     * A subclass of Double, meant to store a fraction of interline, since many
     * distances on a music sheet are expressed in fraction of staff interline.
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
    }
}
