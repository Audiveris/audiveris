//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              C h a m f e r D i s t a n c e T e s t                             //
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

import ij.process.ByteProcessor;

import org.junit.Test;

import java.awt.Dimension;

/**
 *
 * @author Hervé Bitteur
 */
public class ChamferDistanceTest
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new ChamferDistanceTest object.
     */
    public ChamferDistanceTest ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Test of compute method, of class ChamferDistance.
     */
    @Test
    public void testCompute ()
    {
        System.out.println("compute");

        ByteProcessor input = createImage();
        TableUtil.dump("Initial:", input);

        ChamferDistance instance = new ChamferDistance.Integer();

        Table toFore = instance.computeToFore(input);
        TableUtil.dump("Distances to fore:", toFore);

        Table toBack = instance.computeToBack(input);
        TableUtil.dump("Distances to back:", toBack);
    }

    private ByteProcessor createImage ()
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
        final ByteProcessor img = new ByteProcessor(width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                char c = rows[y].charAt(x);
                img.set(x, y, (c == 'X') ? 0 : 255);
            }
        }

        return img;
    }
}
