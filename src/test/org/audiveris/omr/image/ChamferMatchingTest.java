//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              C h a m f e r M a t c h i n g T e s t                             //
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
package org.audiveris.omr.image;

import ij.process.ByteProcessor;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.TableUtil;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

/**
 * Class {@code ChamferMatchingTest}
 *
 * @author Hervé Bitteur
 */
public class ChamferMatchingTest
{

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

    /**
     * Test of matchAll method, of class DistanceMatching.
     */
    @Test
    public void testMatch ()
    {
        System.out.println("match");

        Template template = TemplateFactory.getInstance().getCatalog(56)
                .getTemplate(Shape.NOTEHEAD_BLACK);
        template.dump();

        ByteProcessor image = createImage(imageRows);
        TableUtil.dump("Image:", image);

        DistanceTable distances = new ChamferDistance.Short().computeToFore(image);
        TableUtil.dump("Distances:", distances);

        DistanceMatching instance = new DistanceMatching(distances);
        List<PixelDistance> locs = instance.matchAll(template, Double.MAX_VALUE);

        ///assertArrayEquals(expResult, result);
        printBest(locs);
    }

    private ByteProcessor createImage (String[] rows)
    {
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

    private void printBest (List<PixelDistance> locs)
    {
        System.out.println();
        System.out.println("Best matches:");
        Collections.sort(locs, PixelDistance.byValue);

        for (PixelDistance loc : locs) {
            System.out.println(loc.toString());
        }
    }
}
