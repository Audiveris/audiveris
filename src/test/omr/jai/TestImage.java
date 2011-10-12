//----------------------------------------------------------------------------//
//                                                                            //
//                             T e s t I m a g e                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.jai;

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

public class TestImage
    extends JPanel
{
    //RenderedImage image;
    Image image;
    AffineTransform scaleXform = AffineTransform.getScaleInstance(1f, 1f);

    private static char WHITE = '-';    // And transparent
    private static char[] charTable = new char[]
    {
        '#',  // 0 Black
        '$',  // 1
        '*',  // 2
        '0',  // 3
        'o',  // 4
        '+',  // 5
        '.',  // 6
        WHITE // 7
    };

    public TestImage()
    {
        JFrame frame = new JFrame(getClass().toString());
        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        pane.add(this);

        image = decodeImage(new String[] {
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

    public static void main(String... args)
    {
        new TestImage();
    }

    public void paintComponent(Graphics g)
    {
        // For background
        super.paintComponent(g);

        // Meant for visual check
        if (image != null) {

            Graphics2D g2 = (Graphics2D) g;

            //g2.drawRenderedImage (image, scaleXform);
            g2.drawImage (image, 1, 1, this);
        }
    }

    public static Image decodeImage (String[] rows)
    {
        final int width = rows[0].length();
        final int height = rows.length;
        int[] pix = new int[width * height];
        int index = 0;
        for (String row : rows) {
            for (int x = 0; x < width; x++) {
                pix[index++] = decodeARGB (row.charAt (x));
            }
        }

        // Create the proper image icon
        Toolkit tk = Toolkit.getDefaultToolkit ();
        return tk.createImage
            (new MemoryImageSource (width, height, pix, 0, width));
    }

    private static int decodeARGB (char c)
    {
        // Check the char
        if (c == WHITE) {
            return 0;
        } else {
            for (int i = charTable.length -1; i >= 0; i--) {
                if (charTable[i] == c) {
                    int level = 3 + i * 36; // Range 3 .. 255 (not too bad)
                    return
                        255   << 24 |      // Alpha (opaque)
                        level << 16 |      // R
                        level <<  8 |      // G
                        level;             // B
                }
            }
        }

        return 0;
    }

}
