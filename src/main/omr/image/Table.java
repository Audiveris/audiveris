//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            T a b l e                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import omr.math.TableUtil;

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
    //~ Methods ------------------------------------------------------------------------------------

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
     * Assign the provided value at given location
     *
     * @param x   abscissa
     * @param y   ordinate
     * @param val the new value
     */
    void setValue (int x,
                   int y,
                   int val);

    //~ Inner Classes ------------------------------------------------------------------------------

    //----------//
    // Abstract //
    //----------//
    public abstract class Abstract
        implements Table
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Width of the whole data. */
        protected final int width;

        /** Height of the whole data. */
        protected final int height;

        /** Rectangular region of interest, if any. */
        protected final Rectangle roi;

        //~ Constructors ---------------------------------------------------------------------------

        protected Abstract (int       width,
                            int       height,
                            Rectangle roi)
        {
            this.width = width;
            this.height = height;
            this.roi = (roi != null) ? new Rectangle(roi) : null;
        }

        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void dump (String title)
        {
            TableUtil.dump(title, this);
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
    }

    //---------//
    // Integer //
    //---------//
    public static class Integer
        extends Abstract
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Underlying array. */
        private final int[] data;

        //~ Constructors ---------------------------------------------------------------------------

        public Integer (int width,
                        int height)
        {
            super(width, height, null);
            data = new int[width * height];
        }

        protected Integer (Integer   table,
                           Rectangle roi)
        {
            super(table.width, table.height, roi);
            data = table.data;
        }

        //~ Methods --------------------------------------------------------------------------------

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
        //~ Instance fields ------------------------------------------------------------------------

        /** Underlying array. */
        private final short[] data;

        //~ Constructors ---------------------------------------------------------------------------

        public Short (int width,
                      int height)
        {
            super(width, height, null);
            data = new short[width * height];
        }

        protected Short (Short     table,
                         Rectangle roi)
        {
            super(table.width, table.height, roi);
            data = table.data;
        }

        //~ Methods --------------------------------------------------------------------------------

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
        //~ Instance fields ------------------------------------------------------------------------

        /** Underlying array. */
        protected final byte[] data;

        //~ Constructors ---------------------------------------------------------------------------

        public UnsignedByte (int width,
                             int height)
        {
            super(width, height, null);
            data = new byte[width * height];
        }

        protected UnsignedByte (UnsignedByte table,
                                Rectangle    roi)
        {
            super(table.width, table.height, roi);
            data = table.data;
        }

        //~ Methods --------------------------------------------------------------------------------

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
