/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.audiveris.omr.math;

import org.junit.Test;

/**
 *
 * @author herve
 */
public class ImageUtilTest
{

    /**
     * Test of dump method, of class TableUtil.
     */
    @Test
    public void testDump ()
    {
        System.out.println("dump");

        String title = "Essai:";

        ///int[][] img = createImage();
        int[][] img = new int[300][20];
        TableUtil.dump(title, img);
    }

    private int[][] createImage ()
    {
        String[] rows = new String[]{
            "                              ", "              XXXXXXX         ",
            "           XXXXXXXXXXXXX      ", "         XXXXXXXXXXXXXXXX     ",
            "       XXXXXXXXXXXXXXXXXX     ", "      XXXXXXXXXXXXXXXXXXX     ",
            "     XXXXXXXXXXXXXXXXXXXXX    ", "     XXXXXXXXXXXXXXXXXXXXX    ",
            "     XXXXXXXXXXXXXXXXXXXX     ", "     XXXXXXXXXXXXXXXXXXXX     ",
            "    XXXXXXXXXXXXXXXXXXXXX     ", "     XXXXXXXXXXXXXXXXXXX      ",
            "     XXXXXXXXXXXXXXXXXX       ", "      XXXXXXXXXXXXXXXX        ",
            "         XXXXXXXXXXXXXX       ", "         XXXXXXXXXXXXXXX      ",
            "       XXXXXXXXXXXXXXXXXX     ", "      XXXXXXXXXXXXXXXXXXX     ",
            "     XXXXXXXXXXXXXXXXXXXXX    ", "     XXXXXXXXXXXXXXXXXXXXX    ",
            "    XXXXXXXXXXXXXXXXXXXXXXX   ", "    XXXXXXXXXXXXXXXXXXXXXXX   ",
            "     XXXXXXXXXXXXXXXXXXXXXX   ", "      XXXXXXXXXXXXXXXXXXXXX   ",
            "       XXXXXXXXXXXXXXXXXXX    ", "        XXXXXXXXXXXXXXXXX     ",
            "         XXXXXXXXXXXXXX       ", "           XXXXXXXXX          "
        };
        final int width = rows[0].length();
        final int height = rows.length;
        final int[][] img = new int[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                char c = rows[y].charAt(x);
                img[x][y] = (c == 'X') ? 1 : 0;
            }
        }

        return img;
    }
}
