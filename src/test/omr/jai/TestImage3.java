//----------------------------------------------------------------------------//
//                                                                            //
//                            T e s t I m a g e 3                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.jai;

import omr.jai.ImageInfo;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.util.Arrays;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.InterpolationBilinear;

public class TestImage3
    extends JPanel
{
    //RenderedImage image;
    PlanarImage image;

    // Affine tranform
    final float ratio = 4f;
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
    // TestImage3 //
    //------------//
    public TestImage3()
    {
        JFrame frame = new JFrame(getClass().getName());
        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        pane.add(this);

        image = decodeImage(new String[] {
                "#-###############",
                "-----------------",
                "#################",
                "-----------------",
                "#################",
                "-----------------",
                "#################",
                "-----------------",
                "#################",
                "-----------------",
                "#################",
                "-----------------",
                "-----------------",
                "-----------------",
                "---####----------",
                "-------##--------",
                "---------####----",
                "-------------#---",
                "-------------#---",
                "-----------------",
                "--#############--",
                "--#############--",
                "--#############--",
                "--#############--",
                "--#############--",
                "--#############--",
                "--#############--",
                "--#############--",
                "--#############--",
                "--#############--",
                "--#############--",
                "-----------------",
                "-----------------",
                "---####----------",
                "-------##--------",
                "---------####----",
                "-------------#---",
                "-------------#---",
                "-----------------",
                "--#############--",
                "--#############--",
                "-----------------",
                "-----------------"
        });

        //        checkImageFormat();

        ImageInfo.print(image);

        // Scaling
        final float scale = 1f;
        ParameterBlock pb = new ParameterBlock()
            .addSource(image)
            .add(scale)
            .add(scale)
            .add(0f)
            .add(0f)
            .add(new InterpolationNearest());
        image = JAI.create("scale", pb);
        dumpPixels(0, 0, 5, 7);

        if (false) {
            System.out.println("\nBand Selection");
            image = JAI.create("bandselect",image,new int[] {0, 1, 2});
            ImageInfo.print(image);
            dumpPixels(0, 0, 5, 7);
        }

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocation(100, 100);
        frame.pack();
        frame.setSize(100, 250);
        frame.setVisible(true);
    }

    private void dumpPixels (int x0,
                             int y0,
                             int w,
                             int h)
    {
        Raster raster = image.getData();
        int[] pixel = null;
        System.out.print("pixels=");
        for (int y = y0; y < y0+h; y++) {
            System.out.println();
            for (int x = x0; x <= x0+w; x++) {
                pixel = raster.getPixel(x, y, pixel);
                System.out.print(" [");
                for (int i = 0; i < pixel.length; i++) {
                    System.out.print(String.format("%3x", pixel[i]));
                }
                System.out.print("]");
            }
        }
        System.out.println();
    }


    //------//
    // main //
    //------//
    public static void main(String... args)
    {
        new TestImage3();
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

    //-------------//
    // decodeImage //
    //-------------//
    public static PlanarImage decodeImage (String[] rows)
    {
        // Create the DataBuffer to hold the pixel samples
        final int width = rows[0].length();
        final int height = rows.length;

        // Create Raster
        Raster raster;
        if (true) {
            raster = Raster.createPackedRaster
            (DataBuffer.TYPE_INT, width, height,
             new int[] {0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000},// bandMasks RGBA
             null);
        } else {
            raster = Raster.createInterleavedRaster
                (DataBuffer.TYPE_BYTE, width, height,
                 4,// num of bands
                 null);
        }

        // Populate the data buffer
        DataBuffer dataBuffer = raster.getDataBuffer();
        int index = 0;
        for (String row : rows) {
            for (int x = 0; x < width; x++) {
                int argb = toARGB(row.charAt(x));
                dataBuffer.setElem(index, argb);
                index++;
            }
        }

        // Dump
//         final int size = width * height;
//         System.out.println("DataBuffer :");
//         for (int i = 0; i < size; i++) {
//             if (i % width == 0) {
//                 System.out.println();
//             }
//             System.out.print(String.format("%8x ", dataBuffer.getElem(i)));
//         }
//         System.out.println();

        // Create the image
        BufferedImage bufferedImage = new BufferedImage
                (width, height, BufferedImage.TYPE_INT_ARGB);
        bufferedImage.setData(raster);

        // Dump
//         System.out.println("BufferedImage :");
//         for (int y = 0; y < height; y++) {
//             System.out.println();
//             for (int x = 0; x < width; x++) {
//                 System.out.print(String.format("%8x ", bufferedImage.getRGB(x, y)));
//             }
//         }
//         System.out.println();

        return PlanarImage.wrapRenderedImage(bufferedImage);
    }

    //--------//
    // toARGB //
    //--------//
    /**
     * Compute the ARGB pixel that corresponds to the given char
     *
     * @param c the char
     * @return the corresponding pixel value (ARGB format)
     */
    private static int toARGB (char c)
    {
        // Check the char
        if (c == WHITE) {
            return 0x00ffffff;      // Totally transparent / white
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

        return 0x00ffffff;      // Totally transparent / white
    }

    //------------------//
    // checkImageFormat //
    //------------------//
    /**
     * Check if the image format (and especially its color model) is
     * properly handled by Audiveris.
     *
     * @throws ImageFormatException is the format is not supported
     */
    private void checkImageFormat()
    {
        // Check nb of bands
        int numBands = image.getSampleModel().getNumBands();
        if (numBands != 1) {
            if (numBands == 3) {
                image = colorToGray(image);
            } else {
                throw new RuntimeException
                    ("Unsupported sample model" +
                     " numBands=" + numBands);
            }
        }

        // Check pixel size
        ColorModel colorModel = image.getColorModel();
        int pixelSize = colorModel.getPixelSize();
        if (pixelSize != 8) {
            System.out.println("pixelSize=" + pixelSize +
                    " colorModel=" + colorModel);
            image = grayToGray256(image);
        }
    }

    //--------//
    // invert //
    //--------//
    private static PlanarImage invert (PlanarImage image)
    {
        return JAI.create("Invert",
                          new ParameterBlock()
                          .addSource(image)
                          .add(null)
                          .add(null)
                          .add(null)
                          .add(null)
                          .add(null),
                          null);
    }

    //-------------//
    // colorToGray //
    //-------------//
    private static PlanarImage colorToGray (PlanarImage image)
    {
        System.out.println("Converting color image to gray ...");
        double[][] matrix = { {0.114d, 0.587d, 0.299d, 0.0d} };

        return JAI.create("bandcombine",
                          new ParameterBlock()
                          .addSource(image)
                          .add(matrix),
                          null);
    }

    //---------------//
    // grayToGray256 //
    //---------------//
    private static PlanarImage grayToGray256 (PlanarImage image)
    {
        System.out.println("Converting gray image to gray-256 ...");

        ColorSpace colorSpace = ColorSpace.getInstance
            (java.awt.color.ColorSpace.CS_GRAY);

//        int[] bits = new int[]{8};
//        int opaque = Transparency.OPAQUE;
//        int dataType = DataBuffer.TYPE_BYTE;
//        ColorModel colorModel = new ComponentColorModel
//            (colorSpace, bits, false, false, opaque, dataType);

        return JAI.create("colorConvert", image, colorSpace, null);
    }

}
