//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    D i s t a n c e T a b l e                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import static omr.image.ChamferDistance.VALUE_TARGET;
import static omr.image.ChamferDistance.VALUE_UNKNOWN;

import omr.ui.Colors;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

/**
 * Interface {@code DistanceTable}
 *
 * @author Hervé Bitteur
 */
public interface DistanceTable
        extends Table
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report an image built with distance data.
     *
     * @param maxDistance upper bound for distance to be represented on 0..255 scale, all higher
     *                    distance values are shown in white.
     * @return the buffered image
     */
    BufferedImage getImage (int maxDistance);

    /**
     * Report the normalizing value by which each raw distance data should be divided.
     *
     * @return the normalizing value
     */
    int getNormalizer ();

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------//
    // Abstract //
    //----------//
    public abstract class Abstract
            implements DistanceTable
    {
        //~ Instance fields ------------------------------------------------------------------------

        protected final int normalizer;

        //~ Constructors ---------------------------------------------------------------------------
        public Abstract (int normalizer)
        {
            this.normalizer = normalizer;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void dump (String title)
        {
            getTable().dump(title);
        }

        @Override
        public void fill (int val)
        {
            getTable().fill(val);
        }

        @Override
        public int getHeight ()
        {
            return getTable().getHeight();
        }

        //----------//
        // getImage //
        //----------//
        @Override
        public BufferedImage getImage (int maxDistance)
        {
            final BufferedImage img = new BufferedImage(
                    getWidth(),
                    getHeight(),
                    BufferedImage.TYPE_INT_ARGB);

            // Built a LUT (biased one cell to make room for VALUE_UNKNOWN)
            final int rawDistMax = maxDistance * normalizer;
            final int[] lut = getLut(rawDistMax);

            // Process image data, pixel by pixel
            final int[] data = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
            Arrays.fill(data, Color.WHITE.getRGB()); // All white by default

            for (int i = (getWidth() * getHeight()) - 1; i >= 0; i--) {
                final int val = getValue(i);

                if (val < rawDistMax) {
                    data[i] = lut[1 + val]; // LUT is biased
                }
            }

            return img;
        }

        //---------------//
        // getNormalizer //
        //---------------//
        @Override
        public int getNormalizer ()
        {
            return normalizer;
        }

        @Override
        public int getValue (int index)
        {
            return getTable().getValue(index);
        }

        @Override
        public int getValue (int x,
                             int y)
        {
            return getTable().getValue(x, y);
        }

        @Override
        public int getWidth ()
        {
            return getTable().getWidth();
        }

        @Override
        public void setValue (int index,
                              int value)
        {
            getTable().setValue(index, value);
        }

        @Override
        public void setValue (int x,
                              int y,
                              int val)
        {
            getTable().setValue(x, y, val);
        }

        //----------//
        // getTable //
        //----------//
        protected abstract Table getTable ();

        //--------//
        // getLut //
        //--------//
        /**
         * Built a LUT (biased one cell to make room for VALUE_UNKNOWN==-1)
         *
         * @param rawDistMax maximum distance for non-white cell
         * @return the biased LUT
         */
        private int[] getLut (int rawDistMax)
        {
            final int[] lut = new int[1 + rawDistMax];
            lut[1 + VALUE_UNKNOWN] = Colors.DISTANCE_UNKNOWN.getRGB();
            lut[1 + VALUE_TARGET] = Colors.DISTANCE_TARGET.getRGB();

            final int stdR = Colors.DISTANCE_STANDARD.getRed();
            final int stdG = Colors.DISTANCE_STANDARD.getGreen();
            final int stdB = Colors.DISTANCE_STANDARD.getBlue();

            for (int i = rawDistMax - 1; i > 0; i--) {
                //                int alpha = Math.max(0, (int) Math.rint(255 * (1 - (i / (double) rawDistMax))));
                //                lut[1 + i] = new Color(stdR, stdG, stdB, alpha).getRGB();
                ///int alpha = Math.max(0, (int) Math.rint(255 * (1 - (i / (double) rawDistMax))));
                lut[1 + i] = Color.WHITE.getRGB();
            }

            return lut;
        }
    }

    //---------//
    // Integer //
    //---------//
    public static class Integer
            extends Abstract
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Table.Integer table;

        //~ Constructors ---------------------------------------------------------------------------
        public Integer (int width,
                        int height,
                        int normalizer)
        {
            super(normalizer);
            table = new Table.Integer(width, height);
        }

        protected Integer (Table.Integer table,
                           int normalizer)
        {
            super(normalizer);
            this.table = table;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public DistanceTable.Integer getCopy (Rectangle roi)
        {
            return new Integer(table.getCopy(roi), normalizer);
        }

        @Override
        public DistanceTable.Integer getView (Rectangle roi)
        {
            return new Integer(table.getView(roi), normalizer);
        }

        @Override
        protected final Table getTable ()
        {
            return table;
        }
    }

    //-------//
    // Short //
    //-------//
    public static class Short
            extends Abstract
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Table.Short table;

        //~ Constructors ---------------------------------------------------------------------------
        public Short (int width,
                      int height,
                      int normalizer)
        {
            super(normalizer);
            table = new Table.Short(width, height);
        }

        protected Short (Table.Short table,
                         int normalizer)
        {
            super(normalizer);
            this.table = table;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public DistanceTable.Short getCopy (Rectangle roi)
        {
            return new Short(table.getCopy(roi), normalizer);
        }

        @Override
        public DistanceTable.Short getView (Rectangle roi)
        {
            return new Short(table.getView(roi), normalizer);
        }

        @Override
        protected final Table getTable ()
        {
            return table;
        }
    }
}
