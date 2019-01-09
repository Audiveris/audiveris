//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              C h a m f e r D i s t a n c e T e s t                             //
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
 *
 * @author Hervé Bitteur
 */
public class ChamferDistanceTest
{

    /**
     * Creates a new ChamferDistanceTest object.
     */
    public ChamferDistanceTest ()
    {
    }

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
