//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       T e s t I m a g e                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.jai;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.image.MemoryImageSource;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * DOCUMENT ME!
 *
 * @author TBD
 * @version TBD
 */
public class TestImage
        extends JPanel
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static char WHITE = '-'; // And transparent

    private static char[] charTable = new char[]{
        '#', // 0 Black
        '$', // 1
        '*', // 2
        '0', // 3
        'o', // 4
        '+', // 5
        '.', // 6
        WHITE // 7
    };

    //~ Instance fields ----------------------------------------------------------------------------
    //RenderedImage image;
    Image image;

    AffineTransform scaleXform = AffineTransform.getScaleInstance(1f, 1f);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new TestImage object.
     */
    public TestImage ()
    {
        JFrame frame = new JFrame(getClass().toString());
        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        pane.add(this);

        image = decodeImage(
                new String[]{
                    "----------------------------------------------------------------------------",
                    "----------------------------------------------------------------------------",
                    "----------------------------------------------------------------------------",
                    "----------------------------------------------------------------------------",
                    "----####---------------####---------------####---------------####-----------",
                    "--------##-----------------##-----------------##-----------------##---------",
                    "----------####---------------####---------------####---------------####-----",
                    "--------------#------------------#------------------#------------------#----",
                    "--------------#------------------#------------------#------------------#----",
                    "----------------------------------------------------------------------------",
                    "---#############------#############------#############------#############---",
                    "---#############------#############------#############------#############---",
                    "----------------------------------------------------------------------------",
                    "----------------------------------------------------------------------------",
                    "----####---------------####---------------####---------------####-----------",
                    "--------##-----------------##-----------------##-----------------##---------",
                    "----------####---------------####---------------####---------------####-----",
                    "--------------#------------------#------------------#------------------#----",
                    "--------------#------------------#------------------#------------------#----",
                    "----------------------------------------------------------------------------",
                    "---#############------#############------#############------#############---",
                    "---#############------#############------#############------#############---",
                    "----------------------------------------------------------------------------",
                    "----------------------------------------------------------------------------",
                    "----####---------------####---------------####---------------####-----------",
                    "--------##-----------------##-----------------##-----------------##---------",
                    "----------####---------------####---------------####---------------####-----",
                    "--------------#------------------#------------------#------------------#----",
                    "--------------#------------------#------------------#------------------#----",
                    "----------------------------------------------------------------------------",
                    "---#############------#############------#############------#############---",
                    "---#############------#############------#############------#############---",
                    "----------------------------------------------------------------------------",
                    "----------------------------------------------------------------------------"
                });

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocation(100, 100);
        frame.pack();
        frame.setSize(100, 100);
        frame.setVisible(true);
    }

    //~ Methods ------------------------------------------------------------------------------------
    public static Image decodeImage (String[] rows)
    {
        final int width = rows[0].length();
        final int height = rows.length;
        int[] pix = new int[width * height];
        int index = 0;

        for (String row : rows) {
            for (int x = 0; x < width; x++) {
                pix[index++] = decodeARGB(row.charAt(x));
            }
        }

        // Create the proper image icon
        Toolkit tk = Toolkit.getDefaultToolkit();

        return tk.createImage(new MemoryImageSource(width, height, pix, 0, width));
    }

    public static void main (String... args)
    {
        new TestImage();
    }

    public void paintComponent (Graphics g)
    {
        // For background
        super.paintComponent(g);

        // Meant for visual check
        if (image != null) {
            Graphics2D g2 = (Graphics2D) g;

            //g2.drawRenderedImage (image, scaleXform);
            g2.drawImage(image, 1, 1, this);
        }
    }

    private static int decodeARGB (char c)
    {
        // Check the char
        if (c == WHITE) {
            return 0;
        } else {
            for (int i = charTable.length - 1; i >= 0; i--) {
                if (charTable[i] == c) {
                    int level = 3 + (i * 36); // Range 3 .. 255 (not too bad)

                    return (255 << 24)
                           | // Alpha (opaque)
                            (level << 16)
                           | // R
                            (level << 8)
                           | // G
                            level; // B
                }
            }
        }

        return 0;
    }
}
