//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             W a t e r s h e d G r a y L e v e l T e s t                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import org.audiveris.omr.math.TableUtil;

import org.junit.Test;

/**
 * @author Hervé Bitteur
 */
public class WatershedGrayLevelTest
{
    @Test
    public void testProcess ()
    {
        System.out.println("process");

        ByteProcessor image = createImage();
        TableUtil.dump("input:", image);

        Table dists = new ChamferDistance.Short().computeToBack(image);
        TableUtil.dump("Dists:", dists);

        WatershedGrayLevel instance = new WatershedGrayLevel(dists, true);
        int step = 1;
        boolean[][] result = instance.process(step);
        TableUtil.dump("watershed:", result);

        // Apply watershed line on initial image
        merge(image, result);
        TableUtil.dump("regions", image);
    }

    private ByteProcessor createImage ()
    {
        String[] rows = new String[]{
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

    private void merge (ByteProcessor img,
                        boolean[][] lines)
    {
        for (int y = 0, h = img.getHeight(); y < h; y++) {
            for (int x = 0, w = img.getWidth(); x < w; x++) {
                if (lines[x][y]) {
                    img.set(x, y, PixelSource.BACKGROUND);
                }
            }
        }
    }
}
