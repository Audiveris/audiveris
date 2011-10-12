//----------------------------------------------------------------------------//
//                                                                            //
//                            T e s t I m a g e 2                             //
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
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class TestImage2
    extends JPanel
{
    //RenderedImage image;
    RenderedImage image;

    // Affine tranform
    final float ratio = 1f;
    AffineTransform scaleXform = AffineTransform.getScaleInstance(ratio, ratio);

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

    //------------//
    // TestImage2 //
    //------------//
    public TestImage2()
    {
        JFrame frame = new JFrame(getClass().getName());
        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        pane.add(this);

        image = toRenderedImage(new String[] {
                "-##########################################################################-",
                "----------------------------------------------------------------------------",
                "-##########################################################################-",
                "----------------------------------------------------------------------------",
                "-##########################################################################-",
                "----------------------------------------------------------------------------",
                "-##########################################################################-",
                "----------------------------------------------------------------------------",
                "-##########################################################################-",
                "----------------------------------------------------------------------------",
                "-##########################################################################-",
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
                "---#############------#############------#############------#############---",
                "---#############------#############------#############------#############---",
                "---#############------#############------#############------#############---",
                "---#############------#############------#############------#############---",
                "---#############------#############------#############------#############---",
                "---#############------#############------#############------#############---",
                "---#############------#############------#############------#############---",
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
        frame.setSize(200, 200);
        frame.setVisible(true);
    }

    //------//
    // main //
    //------//
    public static void main(String... args)
    {
        new TestImage2();
    }

    //----------------//
    // paintComponent //
    //----------------//
    public void paintComponent(Graphics g)
    {
        // For background
        super.paintComponent(g);

        // Meant for visual check
        if (image != null) {

            Graphics2D g2 = (Graphics2D) g;

            g2.drawRenderedImage (image, scaleXform);
            //g2.drawImage (image, 1, 1, this);
        }
    }

    //-----------------//
    // toRenderedImage //
    //-----------------//
    public static RenderedImage toRenderedImage (String[] rows)
    {
        // Create the DataBuffer to hold the pixel samples
        final int width = rows[0].length();
        final int height = rows.length;
        final int size = width * height;
        byte[] pixels = new byte[size];
        int index = 0;
        for (String row : rows) {
            for (int x = 0; x < width; x++) {
                pixels[index++] = (byte) decodeGray(row.charAt(x));
            }
        }
        DataBuffer dataBuffer = new DataBufferByte(pixels, size);

        // Create Raster
        WritableRaster writableRaster = Raster.createBandedRaster
            (dataBuffer, width, height,
             width,                      // scanlineStride
             new int[] {0},             // bankIndices,
             new int[] {0},             // bandOffsets,
             null);                     // location

        // Create the image
        BufferedImage bufferedImage = new BufferedImage
                (width, height, BufferedImage.TYPE_BYTE_GRAY);
        bufferedImage.setData(writableRaster);

        return bufferedImage;
    }

    //------------//
    // decodeGray //
    //------------//
    private static int decodeGray(char c)
    {
        // Check the char
        if (c == WHITE) {
            return  255;
        } else {
            for (int i = charTable.length -1; i >= 0; i--) {
                if (charTable[i] == c) {
                    int level = 2 + i * 36; // Range 2 .. 254 (not too bad)
                    return level;
                }
            }
        }

        // Unknown -> white
        return 255;
    }
}
