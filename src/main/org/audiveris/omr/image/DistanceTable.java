//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    D i s t a n c e T a b l e                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.image;

import static org.audiveris.omr.image.ChamferDistance.VALUE_TARGET;
import static org.audiveris.omr.image.ChamferDistance.VALUE_UNKNOWN;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.util.Table;

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

    //----------//
    // Abstract //
    //----------//
    public abstract class Abstract
            implements DistanceTable
    {

        protected final int normalizer;

        public Abstract (int normalizer)
        {
            this.normalizer = normalizer;
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

            for (int i = rawDistMax - 1; i > 0; i--) {
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

        private final Table.Integer table;

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

        @Override
        public DistanceTable.Integer getCopy (Rectangle roi)
        {
            return new DistanceTable.Integer(table.getCopy(roi), normalizer);
        }

        @Override
        public DistanceTable.Integer getView (Rectangle roi)
        {
            return new DistanceTable.Integer(table.getView(roi), normalizer);
        }

        @Override
        public void dump (String title)
        {
            table.dump(title);
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

        private final Table.Short table;

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

        @Override
        public DistanceTable.Short getCopy (Rectangle roi)
        {
            return new DistanceTable.Short(table.getCopy(roi), normalizer);
        }

        @Override
        public DistanceTable.Short getView (Rectangle roi)
        {
            return new DistanceTable.Short(table.getView(roi), normalizer);
        }

        @Override
        public void dump (String title)
        {
            table.dump(title);
        }

        @Override
        protected final Table getTable ()
        {
            return table;
        }
    }
}
