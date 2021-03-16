//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            T a b l e                                           //
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
package org.audiveris.omr.util;

import java.awt.Rectangle;
import java.util.Arrays;

/**
 * Interface {@code Table} provides a rectangular table implemented as a single array
 * of desired type (byte, short, int).
 *
 * @author Hervé Bitteur
 */
public interface Table
{

    /**
     * Dump the table content, with a title.
     *
     * @param title a title for the dump
     */
    void dump (String title);

    /**
     * Fill the whole table with the provided value.
     *
     * @param val the value to fill with
     */
    void fill (int val);

    /**
     * Make a (sub) copy of a table.
     * Modifications on the copy are NOT performed on the original table.
     *
     * @param roi the rectangular limits
     * @return the (sub)Table copy
     */
    Table getCopy (Rectangle roi);

    /**
     * Report table height.
     *
     * @return the table height
     */
    int getHeight ();

    /**
     * Report value at (x,y) location.
     * Returned value is positive for UnsignedByte (0..255)
     *
     * @param x abscissa
     * @param y ordinate
     * @return the value at location
     */
    int getValue (int x,
                  int y);

    /**
     * Report value at index location in table data
     *
     * @param index position in table data
     * @return the value at index
     */
    int getValue (int index);

    /**
     * Define a sub-table view on a table.
     * Modification on the view are performed on the underlying table.
     *
     * @param roi the rectangular limits
     * @return the (sub)Table view
     */
    Table getView (Rectangle roi);

    /**
     * Report the table width
     *
     * @return the table width
     */
    int getWidth ();

    /**
     * Assign value at index location in table data
     *
     * @param index position in table data
     * @param val   the value to assign at index
     */
    void setValue (int index,
                   int val);

    /**
     * Assign the provided value at given location
     *
     * @param x   abscissa
     * @param y   ordinate
     * @param val the new value
     */
    void setValue (int x,
                   int y,
                   int val);

    //----------//
    // Abstract //
    //----------//
    public abstract class Abstract
            implements Table
    {

        /** Width of the whole data. */
        protected final int width;

        /** Height of the whole data. */
        protected final int height;

        /** Rectangular region of interest, if any. */
        protected final Rectangle roi;

        protected Abstract (int width,
                            int height,
                            Rectangle roi)
        {
            this.width = width;
            this.height = height;
            this.roi = (roi != null) ? new Rectangle(roi) : null;
        }

        @Override
        public void dump (String title)
        {
            if (title != null) {
                System.out.println(title);
            }

            final String yFormat = printAbscissae(4);

            for (int y = 0; y < height; y++) {
                System.out.printf(yFormat, y);

                for (int x = 0; x < width; x++) {
                    System.out.printf("%4d", getValue(x, y));
                }

                System.out.println();
            }

        }

        @Override
        public void fill (int val)
        {
            if (roi == null) {
                throw new IllegalArgumentException("Abstract fill() needs non-null roi");
            }

            // Not very efficient implementation...
            for (int y = 0; y < roi.height; y++) {
                for (int x = 0; x < roi.width; x++) {
                    setValue(x, y, val);
                }
            }
        }

        @Override
        public int getHeight ()
        {
            if (roi == null) {
                return height;
            } else {
                return roi.height;
            }
        }

        @Override
        public int getWidth ()
        {
            if (roi == null) {
                return width;
            } else {
                return roi.width;
            }
        }

        protected final void checkRoi (Rectangle roi)
        {
            if ((roi.x < 0) || ((roi.x + roi.width) > width)) {
                throw new IllegalArgumentException(
                        "Illegal abscissa range " + roi + " width:" + width);
            }

            if ((roi.y < 0) || ((roi.y + roi.height) > height)) {
                throw new IllegalArgumentException(
                        "Illegal ordinate range " + roi + " height:" + height);
            }
        }

        /**
         * Print the lines of abscissa values for the table.
         *
         * @param cell cell width
         * @return the format string for printing ordinate values
         */
        protected String printAbscissae (
                int cell)
        {
            // # of x digits
            final int wn = Math.max(1, (int) Math.ceil(Math.log10(width)));

            // # of y digits
            final int hn = Math.max(1, (int) Math.ceil(Math.log10(height)));
            final String margin = "%" + hn + "s ";
            final String dFormat = "%" + cell + "d";
            final String sFormat = "%" + cell + "s";

            // Abscissae
            for (int i = wn - 1; i >= 0; i--) {
                int mod = (int) Math.pow(10, i);
                System.out.printf(margin, "");

                for (int x = 0; x < width; x++) {
                    if ((x % 10) == 0) {
                        int d = (x / mod) % 10;
                        System.out.printf(dFormat, d);
                    } else if (i == 0) {
                        System.out.printf(dFormat, x % 10);
                    } else {
                        System.out.printf(sFormat, "");
                    }
                }

                System.out.println();
            }

            System.out.printf(margin, "");

            for (int x = 0; x < width; x++) {
                System.out.printf(sFormat, "-");
            }

            System.out.println();

            return "%" + hn + "d:";
        }
    }

    //---------//
    // Integer //
    //---------//
    public static class Integer
            extends Abstract
    {

        /** Underlying array. */
        private final int[] data;

        public Integer (int width,
                        int height)
        {
            super(width, height, null);
            data = new int[width * height];
        }

        protected Integer (Integer table,
                           Rectangle roi)
        {
            super(table.width, table.height, roi);
            data = table.data;
        }

        @Override
        public void fill (int val)
        {
            if (roi == null) {
                Arrays.fill(data, val);
            } else {
                super.fill(val);
            }
        }

        @Override
        public Integer getCopy (Rectangle roi)
        {
            Integer copy;

            if (roi == null) {
                copy = new Integer(width, height);
                System.arraycopy(data, 0, copy.data, 0, data.length);
            } else {
                checkRoi(roi);

                copy = new Integer(roi.width, roi.height);

                for (int y = 0; y < roi.height; y++) {
                    int p = ((y + roi.y) * width) + roi.x;
                    System.arraycopy(data, p, copy.data, y * roi.width, roi.width);
                }
            }

            return copy;
        }

        @Override
        public int getValue (int x,
                             int y)
        {
            if (roi != null) {
                x += roi.x;
                y += roi.y;
            }

            return data[(y * width) + x];
        }

        @Override
        public int getValue (int index)
        {
            return data[index];
        }

        public int[] getValues ()
        {
            if (roi != null) {
                throw new UnsupportedOperationException("getValues() not supported on view");
            }

            return data;
        }

        @Override
        public Integer getView (Rectangle roi)
        {
            checkRoi(roi);

            return new Integer(this, roi);
        }

        @Override
        public void setValue (int index,
                              int val)
        {
            data[index] = val;
        }

        @Override
        public void setValue (int x,
                              int y,
                              int val)
        {
            if (roi != null) {
                x += roi.x;
                y += roi.y;
            }

            data[(y * width) + x] = val;
        }
    }

    //-------//
    // Short //
    //-------//
    public static class Short
            extends Abstract
    {

        /** Underlying array. */
        private final short[] data;

        public Short (int width,
                      int height)
        {
            super(width, height, null);
            data = new short[width * height];
        }

        protected Short (Short table,
                         Rectangle roi)
        {
            super(table.width, table.height, roi);
            data = table.data;
        }

        @Override
        public void fill (int val)
        {
            if (roi == null) {
                Arrays.fill(data, (short) val);
            } else {
                super.fill(val);
            }
        }

        @Override
        public Short getCopy (Rectangle roi)
        {
            Short copy;

            if (roi == null) {
                copy = new Short(width, height);
                System.arraycopy(data, 0, copy.data, 0, data.length);
            } else {
                checkRoi(roi);

                copy = new Short(roi.width, roi.height);

                for (int y = 0; y < roi.height; y++) {
                    int p = ((y + roi.y) * width) + roi.x;
                    System.arraycopy(data, p, copy.data, y * roi.width, roi.width);
                }
            }

            return copy;
        }

        @Override
        public int getValue (int x,
                             int y)
        {
            if (roi != null) {
                x += roi.x;
                y += roi.y;
            }

            return data[(y * width) + x];
        }

        @Override
        public int getValue (int index)
        {
            return data[index];
        }

        public short[] getValues ()
        {
            if (roi != null) {
                throw new UnsupportedOperationException("getValues() not supported on view");
            }

            return data;
        }

        @Override
        public Short getView (Rectangle roi)
        {
            checkRoi(roi);

            return new Short(this, roi);
        }

        @Override
        public void setValue (int index,
                              int val)
        {
            data[index] = (short) val;
        }

        @Override
        public void setValue (int x,
                              int y,
                              int val)
        {
            if (roi != null) {
                x += roi.x;
                y += roi.y;
            }

            data[(y * width) + x] = (short) val;
        }
    }

    //--------------//
    // UnsignedByte //
    //--------------//
    public static class UnsignedByte
            extends Abstract
    {

        /** Underlying array. */
        protected final byte[] data;

        public UnsignedByte (int width,
                             int height)
        {
            super(width, height, null);
            data = new byte[width * height];
        }

        protected UnsignedByte (UnsignedByte table,
                                Rectangle roi)
        {
            super(table.width, table.height, roi);
            data = table.data;
        }

        @Override
        public void fill (int val)
        {
            if (roi == null) {
                Arrays.fill(data, (byte) val);
            } else {
                super.fill(val);
            }
        }

        @Override
        public UnsignedByte getCopy (Rectangle roi)
        {
            UnsignedByte copy;

            if (roi == null) {
                copy = new UnsignedByte(width, height);
                System.arraycopy(data, 0, copy.data, 0, data.length);
            } else {
                checkRoi(roi);

                copy = new UnsignedByte(roi.width, roi.height);

                for (int y = 0; y < roi.height; y++) {
                    int p = ((y + roi.y) * width) + roi.x;
                    System.arraycopy(data, p, copy.data, y * roi.width, roi.width);
                }
            }

            return copy;
        }

        @Override
        public int getValue (int x,
                             int y)
        {
            if (roi != null) {
                x += roi.x;
                y += roi.y;
            }

            return data[(y * width) + x] & 0xff;
        }

        @Override
        public int getValue (int index)
        {
            return data[index] & 0xff;
        }

        public byte[] getValues ()
        {
            if (roi != null) {
                throw new UnsupportedOperationException("getValues() not supported on view");
            }

            return data;
        }

        @Override
        public UnsignedByte getView (Rectangle roi)
        {
            checkRoi(roi);

            return new UnsignedByte(this, roi);
        }

        @Override
        public void setValue (int index,
                              int val)
        {
            data[index] = (byte) val;
        }

        @Override
        public void setValue (int x,
                              int y,
                              int val)
        {
            if (roi != null) {
                x += roi.x;
                y += roi.y;
            }

            data[(y * width) + x] = (byte) val;
        }
    }
}
