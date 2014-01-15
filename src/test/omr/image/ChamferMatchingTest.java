//----------------------------------------------------------------------------//
//                                                                            //
//                    C h a m f e r M a t c h i n g T e s t                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import omr.glyph.Shape;

import omr.math.TableUtil;

import org.junit.Test;

import java.awt.Dimension;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code ChamferMatchingTest}
 *
 * @author Hervé Bitteur
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
     * Test of matchAll method, of class DistanceMatching.
     */
    @Test
    public void testMatch ()
    {
        System.out.println("match");

        Template template = TemplateFactory.getInstance()
                .getCatalog(14)
                .getTemplate(
                        new Template.Key(Shape.NOTEHEAD_BLACK, false));
        template.dump();

        PixelBuffer image = createImage(imageRows);
        TableUtil.dump("Image:", image);

        DistanceTable distances = new ChamferDistance.Short().computeToFore(
                image);
        TableUtil.dump("Distances:", distances);

        DistanceMatching instance = new DistanceMatching(distances);
        List<PixelDistance> locs = instance.matchAll(
                template,
                Double.MAX_VALUE);

        ///assertArrayEquals(expResult, result);
        printBest(locs);
    }

    private PixelBuffer createImage (String[] rows)
    {
        final int width = rows[0].length();
        final int height = rows.length;
        final PixelBuffer img = new PixelBuffer(new Dimension(width, height));

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                char c = rows[y].charAt(x);
                img.setValue(x, y, (c == 'X') ? 0 : 255);
            }
        }

        return img;
    }

    private void printBest (List<PixelDistance> locs)
    {
        System.out.println();
        System.out.println("Best matches:");
        Collections.sort(locs);

        for (PixelDistance loc : locs) {
            System.out.println(loc.toString());
        }
    }
}
