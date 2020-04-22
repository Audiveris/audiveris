//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        T a b l e U t i l                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.math;

import ij.process.ByteProcessor;

import org.audiveris.omr.image.ImageUtil;
import org.audiveris.omr.util.Table;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

/**
 * Class {@code TableUtil} gathers utilities to dump basic table content.
 *
 * @author Hervé Bitteur
 */
public abstract class TableUtil
{

    /**
     * Print out a table of int values.
     *
     * @param title a title for the print
     * @param table the input table
     */
    public static void dump (String title,
                             int[][] table)
    {
        final int width = table.length;
        final int height = table[0].length;

        if (title != null) {
            System.out.println(title);
        }

        final String yFormat = printAbscissae(width, height, 4);

        for (int y = 0; y < height; y++) {
            System.out.printf(yFormat, y);

            for (int x = 0; x < width; x++) {
                System.out.printf("%4d", table[x][y]);
            }

            System.out.println();
        }
    }

    /**
     * Print out a table of boolean values.
     *
     * @param title a title for the print
     * @param table the input table
     */
    public static void dump (String title,
                             boolean[][] table)
    {
        final int width = table.length;
        final int height = table[0].length;

        if (title != null) {
            System.out.println(title);
        }

        final String yFormat = printAbscissae(width, height, 3);

        for (int y = 0; y < height; y++) {
            System.out.printf(yFormat, y);

            for (int x = 0; x < width; x++) {
                System.out.printf("%3s", table[x][y] ? "X" : " ");
            }

            System.out.println();
        }
    }

    /**
     * Print out a table of double values.
     *
     * @param title a title for the print
     * @param table the input table
     */
    public static void dump (String title,
                             double[][] table)
    {
        final int width = table.length;
        final int height = table[0].length;

        if (title != null) {
            System.out.println(title);
        }

        final String yFormat = printAbscissae(width, height, 6);

        for (int y = 0; y < height; y++) {
            System.out.printf(yFormat, y);

            for (int x = 0; x < width; x++) {
                System.out.printf("%6.3f", table[x][y]);
            }

            System.out.println();
        }
    }

    //    /**
    //     * Print out a PixelBuffer.
    //     *
    //     * @param title a title for the print
    //     * @param buf   the input buffer
    //     */
    //    public static void dump (String title,
    //                             PixelBuffer buf)
    //    {
    //        final int width = buf.getWidth();
    //        final int height = buf.getHeight();
    //
    //        if (title != null) {
    //            System.out.println(title);
    //        }
    //
    //        final String yFormat = printAbscissae(width, height, 4);
    //
    //        for (int y = 0; y < height; y++) {
    //            System.out.printf(yFormat, y);
    //
    //            for (int x = 0; x < width; x++) {
    //                System.out.printf("%4d", buf.getValue(x, y));
    //            }
    //
    //            System.out.println();
    //        }
    //    }
    /**
     * Print out a ByteProcessor.
     *
     * @param title a title for the print
     * @param buf   the input buffer
     */
    public static void dump (String title,
                             ByteProcessor buf)
    {
        final int width = buf.getWidth();
        final int height = buf.getHeight();

        if (title != null) {
            System.out.println(title);
        }

        final String yFormat = printAbscissae(width, height, 4);

        for (int y = 0; y < height; y++) {
            System.out.printf(yFormat, y);

            for (int x = 0; x < width; x++) {
                System.out.printf("%4d", buf.get(x, y));
            }

            System.out.println();
        }
    }

    /**
     * Print out a Table.
     *
     * @param title a title for the print
     * @param table the table to print
     */
    public static void dump (String title,
                             Table table)
    {
        final int width = table.getWidth();
        final int height = table.getHeight();

        if (title != null) {
            System.out.println(title);
        }

        final String yFormat = printAbscissae(width, height, 4);

        for (int y = 0; y < height; y++) {
            System.out.printf(yFormat, y);

            for (int x = 0; x < width; x++) {
                System.out.printf("%4d", table.getValue(x, y));
            }

            System.out.println();
        }
    }

    /**
     * Print the lines of abscissae values for a table, knowing its
     * dimension
     *
     * @param width  table width
     * @param height table height
     * @param cell   cell width
     * @return the format string for printing ordinate values
     */
    public static String printAbscissae (int width,
                                         int height,
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

    //-------//
    // store //
    //-------//
    /**
     * Store the provided table as a BufferedImage to disk.
     *
     * @param id    a distinguished name
     * @param table the table to store
     */
    public static void store (String id,
                              short[][] table)
    {
        final int width = table.length;
        final int height = table[0].length;

        // Retrieve max value
        int max = 0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                max = Math.max(max, table[x][y]);
            }
        }

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        int[] pix = new int[1];
        WritableRaster raster = img.getRaster();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int val = table[x][y];
                val = (val * 255) / max;
                pix[0] = val;
                raster.setPixel(x, y, pix);
            }
        }

        ImageUtil.saveOnDisk(img, id);
    }

    //-------//
    // toInt //
    //-------//
    private static int[][] toInt (double[][] doubles)
    {
        final int width = doubles.length;
        final int height = doubles[0].length;
        double max = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                max = Math.max(max, doubles[x][y]);
            }
        }

        int[][] ints = new int[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                ints[x][y] = (int) Math.rint((doubles[x][y] * 255) / max);
            }
        }

        return ints;
    }

    private TableUtil ()
    {
    }
}
