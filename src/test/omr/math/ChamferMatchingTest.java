/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package omr.math;

import omr.image.ChamferDistance;
import omr.image.ChamferMatching;
import omr.image.PixDistance;
import omr.image.Template;

import org.junit.Test;

import java.util.ArrayList;

///import static org.junit.Assert.*;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author herve
 */
public class ChamferMatchingTest
{
    //~ Static fields/initializers ---------------------------------------------

    private static final String[] imageRows = new String[]{
        "                                    ",
        "                                    ",
        "                                    ",
        "                                    ",
        "                    XXXXXXX         ",
        "                 XXXXXXXXXXXXX      ",
        "               XXXXXXXXXXXXXXXX     ",
        "             XXXXXXXXXXXXXXXXXX     ",
        "            XXXXXXXXXXXXXXXXXXX     ",
        "           XXXXXXXXXXXXXXXXXXXXX    ",
        "           XXXXXXXXXXXXXXXXXXXXX    ",
        "           XXXXXXXXXXXXXXXXXXXX     ",
        "           XXXXXXXXXXXXXXXXXXXX     ",
        "          XXXXXXXXXXXXXXXXXXXXX     ",
        "           XXXXXXXXXXXXXXXXXXX      ",
        "           XXXXXXXXXXXXXXXXXX       ",
        "            XXXXXXXXXXXXXXXX        ",
        "               XXXXXXXXXXXXXX       ",
        "               XXXXXXXXXXXXXXX      ",
        "             XXXXXXXXXXXXXXXXXX     ",
        "            XXXXXXXXXXXXXXXXXXX     ",
        "           XXXXXXXXXXXXXXXXXXXXX    ",
        "           XXXXXXXXXXXXXXXXXXXXX    ",
        "          XXXXXXXXXXXXXXXXXXXXXXX   ",
        "          XXXXXXXXXXXXXXXXXXXXXXX   ",
        "           XXXXXXXXXXXXXXXXXXXXXX   ",
        "            XXXXXXXXXXXXXXXXXXXXX   ",
        "             XXXXXXXXXXXXXXXXXXX    ",
        "              XXXXXXXXXXXXXXXXX     ",
        "               XXXXXXXXXXXXXX       ",
        "                 XXXXXXXXX          "
    };

    private static final String[] templateRows = new String[]{
        "          XXXXXXX     ",
        "       XXXXXXXXXXXXX  ",
        "     XXXXXXXXXXXXXXXX ",
        "   XXXXXXXXXXXXXXXXXX ",
        "  XXXXXXXXXXXXXXXXXXX ",
        " XXXXXXXXXXXXXXXXXXXXX",
        " XXXXXXXXXXXXXXXXXXXXX",
        " XXXXXXXXXXXXXXXXXXXX ",
        " XXXXXXXXXXXXXXXXXXXX ",
        "XXXXXXXXXXXXXXXXXXXXX ",
        " XXXXXXXXXXXXXXXXXXX  ",
        " XXXXXXXXXXXXXXXXXX   ",
        "  XXXXXXXXXXXXXXXX    ",
        "     XXXXXXXXXXXX     ",
        "       XXXXXXXXX      "
    };

    //~ Methods ----------------------------------------------------------------
    /**
     * Test of matchAll method, of class ChamferMatching.
     */
    @Test
    public void testMatch ()
    {
        System.out.println("match");

        Template template = createTemplate(templateRows);
        template.dump();

        boolean[][] image = createImage(imageRows);
        TableUtil.dump("Image:", image);

        double[][] distances = new ChamferDistance().compute(image);
        TableUtil.dump("Distances:", distances);

        ChamferMatching instance = new ChamferMatching(distances);
        List<PixDistance> locs = instance.matchAll(template, Double.MAX_VALUE);

        ///assertArrayEquals(expResult, result);
        printBest(locs);
    }

    private boolean[][] createImage (String[] rows)
    {
        final int width = rows[0].length();
        final int height = rows.length;
        final boolean[][] img = new boolean[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                char c = rows[y].charAt(x);
                img[x][y] = c == 'X';
            }
        }

        return img;
    }

    private Template createTemplate (String[] rows)
    {
        final int width = rows[0].length();
        final int height = rows.length;
        final List<PixDistance> keys = new ArrayList<PixDistance>();
        final boolean[][] img = new boolean[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                char c = rows[y].charAt(x);
                img[x][y] = !(c == 'X');

                //
                //                if (c == 'X') {
                //                    keys.add(new Point(x, y));
                //                }
            }
        }

        ChamferDistance instance = new ChamferDistance();
        double[][] dists = instance.compute(img);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                keys.add(new PixDistance(x, y, dists[x][y]));
            }
        }

        return new Template("test-template", width, height, keys, null);
    }

    private void printBest (List<PixDistance> locs)
    {
        System.out.println();
        System.out.println("Best matches:");
        Collections.sort(locs);

        for (PixDistance loc : locs) {
            System.out.println(loc.toString());
        }
    }
}
