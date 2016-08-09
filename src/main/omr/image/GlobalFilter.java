//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    G l o b a l F i l t e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.image;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import ij.process.ByteProcessor;

import net.jcip.annotations.ThreadSafe;

/**
 * Class {@code GlobalFilter} implements Interface {@code PixelFilter} by using a
 * global threshold on pixel value.
 *
 * @author Hervé Bitteur
 */
@ThreadSafe
public class GlobalFilter
        extends SourceWrapper
        implements PixelFilter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    //~ Instance fields ----------------------------------------------------------------------------
    /** Global threshold. */
    private final int threshold;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a binary wrapper on a raw pixel source.
     *
     * @param source    the underlying source of raw pixels
     * @param threshold maximum gray level of foreground pixel
     */
    public GlobalFilter (ByteProcessor source,
                         int threshold)
    {
        super(source);
        this.threshold = threshold;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // filteredImage //
    //---------------//
    @Override
    public ByteProcessor filteredImage ()
    {
        ByteProcessor ip = new ByteProcessor(source.getWidth(), source.getHeight());

        for (int y = 0, h = ip.getHeight(); y < h; y++) {
            for (int x = 0, w = ip.getWidth(); x < w; x++) {
                if (isFore(x, y)) {
                    ip.set(x, y, FOREGROUND);
                } else {
                    ip.set(x, y, BACKGROUND);
                }
            }
        }

        return ip;
    }

    //------------//
    // getContext //
    //------------//
    @Override
    public Context getContext (int x,
                               int y)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

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

    // -------//
    // isFore //
    // -------//
    @Override
    public boolean isFore (int x,
                           int y)
    {
        return source.get(x, y) <= threshold;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Integer defaultThreshold = new Constant.Integer(
                "GrayLevel",
                140,
                "Default threshold value (in 0..255)");
    }
}
