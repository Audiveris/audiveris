//----------------------------------------------------------------------------//
//                                                                            //
//                          G l o b a l F i l t e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import net.jcip.annotations.ThreadSafe;

/**
 * Class {@code GlobalFilter} implements Interface
 * {@code PixelFilter} by using a global threshold on pixel value.
 *
 * @author Hervé Bitteur
 */
@ThreadSafe
public class GlobalFilter
        extends SourceWrapper
        implements PixelFilter
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    //~ Instance fields --------------------------------------------------------
    //
    /** Global threshold. */
    private final int threshold;

    //~ Constructors -----------------------------------------------------------
    //
    //--------------//
    // GlobalFilter //
    //--------------//
    /**
     * Create a binary wrapper on a raw pixel source.
     *
     * @param source    the underlying source of raw pixels
     * @param threshold maximum gray level of foreground pixel
     */
    public GlobalFilter (PixelSource source,
                         int threshold)
    {
        super(source);
        this.threshold = threshold;
    }

    //~ Methods ----------------------------------------------------------------
    //----------------------//
    // getDefaultDescriptor //
    //----------------------//
    public static FilterDescriptor getDefaultDescriptor ()
    {
        return GlobalDescriptor.getDefault();
    }

    //---------------------//
    // getDefaultThreshold //
    //---------------------//
    public static int getDefaultThreshold ()
    {
        return constants.defaultThreshold.getValue();
    }

    //---------------------//
    // setDefaultThreshold //
    //---------------------//
    public static void setDefaultThreshold (int threshold)
    {
        constants.defaultThreshold.setValue(threshold);
    }

    //
    //------------//
    // getContext //
    //------------//
    @Override
    public Context getContext (int x,
                               int y)
    {
        return new Context(threshold);
    }

    //
    // -------//
    // isFore //
    // -------//
    @Override
    public boolean isFore (int x,
                           int y)
    {
        return source.getPixel(x, y) <= threshold;
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Integer defaultThreshold = new Constant.Integer(
                "GrayLevel",
                140,
                "Default threshold value (in 0..255)");

    }
}
