/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package omr.math;

import omr.image.ChamferDistance;
import org.junit.Test;

/**
 *
 * @author herve
 */
public class ChamferDistanceTest
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ChamferDistanceTest object.
     */
    public ChamferDistanceTest ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    /**
     * Test of compute method, of class ChamferDistance.
     */
    @Test
    public void testCompute ()
    {
        System.out.println("compute");

        boolean[][] input = createImage();
        TableUtil.dump("Initial:", input);

        ChamferDistance instance = new ChamferDistance();
        double[][] expResult = null;
        double[][] result = instance.compute(input);
        TableUtil.dump("Distances:", result);
    }

    private boolean[][] createImage ()
    {
        String[] rows = new String[]{
            "                              ",
            "              XXXXXXX         ",
            "           XXXXXXXXXXXXX      ",
            "         XXXXXXXXXXXXXXXX     ",
            "       XXXXXXXXXXXXXXXXXX     ",
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
        final int width = rows[0].length();
        final int height = rows.length;
        final boolean[][] img = new boolean[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                char c = rows[y].charAt(x);
                img[x][y] = !(c == 'X');
            }
        }

        return img;
    }
}
