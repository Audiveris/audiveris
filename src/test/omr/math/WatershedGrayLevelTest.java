//----------------------------------------------------------------------------//
//                                                                            //
//                   W a t e r s h e d G r a y L e v e l T e s t              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import omr.image.ChamferDistance;
import omr.image.WatershedGrayLevel;
import org.junit.Test;

/**
 *
 * @author Hervé Bitteur
 */
public class WatershedGrayLevelTest
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new WatershedGrayLevelTest object.
     */
    public WatershedGrayLevelTest ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Test of process method, of class WatershedGrayLevel.
     */
    @Test
    public void testProcess ()
    {
        System.out.println("process");

        boolean[][] image = createImage();
        TableUtil.dump("input:", image);

        double[][] doubles = new ChamferDistance().compute(image);
        int[][]    dists = toInt(doubles);
        TableUtil.dump("Normalized:", dists);

        WatershedGrayLevel instance = new WatershedGrayLevel(dists, true);
        int                step = 1;
        boolean[][]        expResult = null;
        boolean[][]        result = instance.process(step);
        TableUtil.dump("watershed:", result);

        // Apply watershed line on initial image
        boolean[][] merge = merge(image, result);
        TableUtil.dump("regions", merge);
    }

    private boolean[][] createImage ()
    {
        String[]          rows = new String[] {
                                     "                              ",
                                     "              XX  XXX         ",
                                     "           XXXXXX XXXXXX      ",
                                     "         XXXXXXXX XXXXXXX     ",
                                     "       XXXXXXXXXX XXXXXXX     ",
                                     "      XXXXXXXXXXXXXXXXXXX     ",
                                     "     XXXXXXXXXXXXXXXXXXXXX    ",
                                     "     XXXXXXXXXXXXXXXXXXXXX    ",
                                     "     XXXXXXXXXXXXXXXXXXXX     ",
                                     "     XXXXXXXXXXXXXXXXXXXX     ",
                                     "    XXXXXXXXXXXXXXXXXXXXX     ",
                                     "     XXXXXXXXXXXXXXXXXXX      ",
                                     "     XXXXXXXXXXXXXXXXXX       ",
                                     "      XXXXXXXXXXXXXXXX        ",
                                     "         XXXXXXXXXXXXXX       ",
                                     "         XXXXXXXXXXXXXXX      ",
                                     "       XXXXXXXXXXXXXXXXXX     ",
                                     "      XXXXXXXXXXXXXXXXXXX     ",
                                     "     XXXXXXXXXXXXXXXXXXXXX    ",
                                     "     XXXXXXXXXXXXXXXXXXXXX    ",
                                     "    XXXXXXXXXXXXXXXXXXXXXXX   ",
                                     "    XXXXXXXXXXXXXXXXXXXXXXX   ",
                                     "     XXXXXXXXXXXXXXXXXXXXXX   ",
                                     "      XXXXXXXXXXXXXXXXXXXXX   ",
                                     "       XXXXXXXXXXXXXXXXXXX    ",
                                     "        XXXXXXXXXXXXXXXXX     ",
                                     "         XXXXXXXXXXXXXX       ",
                                     "           XXXXXXXXX          "
                                 };
        final int         width = rows[0].length();
        final int         height = rows.length;
        final boolean[][] img = new boolean[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                char c = rows[y].charAt(x);
                img[x][y] = !(c == 'X');
            }
        }

        return img;
    }

    private boolean[][] merge (boolean[][] image,
                               boolean[][] lines)
    {
        final int width = lines.length;
        final int height = lines[0].length;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (lines[x][y]) {
                    image[x][y] = true;
                }
            }
        }

        return image;
    }

    private int[][] toInt (double[][] doubles)
    {
        final int width = doubles.length;
        final int height = doubles[0].length;
        double    max = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                max = Math.max(max, doubles[x][y]);
            }
        }

        System.out.println("max = " + max);

        int[][] ints = new int[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                ints[x][y] = (int) Math.rint((doubles[x][y] * 255) / max);
            }
        }

        return ints;
    }
}
