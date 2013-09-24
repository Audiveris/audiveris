//----------------------------------------------------------------------------//
//                                                                            //
//                                  T a b l e                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import omr.math.TableUtil;

import java.util.Arrays;

/**
 * Class {@code Table} provides a rectangular table implemented as a
 * single array of desired type (byte, short, int).
 *
 * @author Hervé Bitteur
 */
public interface Table
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Dump the table content, witha title.
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

    //~ Inner Classes ----------------------------------------------------------
    //----------//
    // Abstract //
    //----------//
    public abstract class Abstract
            implements Table
    {
        //~ Instance fields ----------------------------------------------------

        /** Width of the table. */
        protected final int width;

        /** Height of the table. */
        protected final int height;

        //~ Constructors -------------------------------------------------------
        /**
         * Creates a new Table object.
         *
         * @param width  the table width
         * @param height the table height
         */
        protected Abstract (int width,
                            int height)
        {
            this.width = width;
            this.height = height;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void dump (String title)
        {
            TableUtil.dump(title, this);
        }

        @Override
        public int getHeight ()
        {
            return height;
        }

        @Override
        public int getWidth ()
        {
            return width;
        }
    }

    //---------//
    // Integer //
    //---------//
    public static class Integer
            extends Abstract
    {
        //~ Instance fields ----------------------------------------------------

        /** Underlying array. */
        private final int[] table;

        //~ Constructors -------------------------------------------------------
        public Integer (int width,
                        int height)
        {
            super(width, height);
            table = new int[width * height];
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void fill (int val)
        {
            Arrays.fill(table, val);
        }

        @Override
        public int getValue (int x,
                             int y)
        {
            return table[(y * width) + x];
        }

        @Override
        public void setValue (int x,
                              int y,
                              int val)
        {
            table[(y * width) + x] = val;
        }
    }

    //-------//
    // Short //
    //-------//
    public static class Short
            extends Abstract
    {
        //~ Instance fields ----------------------------------------------------

        /** Underlying array. */
        private final short[] table;

        //~ Constructors -------------------------------------------------------
        public Short (int width,
                      int height)
        {
            super(width, height);
            table = new short[width * height];
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void fill (int val)
        {
            Arrays.fill(table, (short) val);
        }

        @Override
        public int getValue (int x,
                             int y)
        {
            return table[(y * width) + x];
        }

        @Override
        public void setValue (int x,
                              int y,
                              int val)
        {
            table[(y * width) + x] = (short) val;
        }
    }

    //--------------//
    // UnsignedByte //
    //--------------//
    public static class UnsignedByte
            extends Abstract
    {
        //~ Instance fields ----------------------------------------------------

        /** Underlying array. */
        private final byte[] table;

        //~ Constructors -------------------------------------------------------
        public UnsignedByte (int width,
                             int height)
        {
            super(width, height);
            table = new byte[width * height];
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void fill (int val)
        {
            Arrays.fill(table, (byte) val);
        }

        @Override
        public int getValue (int x,
                             int y)
        {
            int val = table[(y * width) + x];

            if (val < 0) {
                val += 256;
            }

            return val;
        }

        @Override
        public void setValue (int x,
                              int y,
                              int val)
        {
            table[(y * width) + x] = (byte) val;
        }
    }
}
