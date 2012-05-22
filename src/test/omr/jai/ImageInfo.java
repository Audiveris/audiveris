//----------------------------------------------------------------------------//
//                                                                            //
//                             I m a g e I n f o                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.jai;

import java.awt.Transparency;
import java.awt.image.*;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.util.Arrays;

import javax.media.jai.*;

/**
 * DOCUMENT ME!
 *
 * @author TBD
 * @version TBD
 */
public class ImageInfo
{
    //~ Methods ----------------------------------------------------------------

    public static void main (String[] args)
    {
        String name = "../bnf/manuscrit/2Partitions/00398006/T0000012.tif"; //args[0];
        // Open the image (using the name passed as a command line parameter)
        PlanarImage pi = JAI.create("fileload", name); 
        
        // Get the image file size (non-JAI related).
        File image = new File(name); 
        System.out.println("Image file size: " + image.length() + " bytes.");

        print(pi);
    }

    public static void print (PlanarImage pi)
    {
        // Show the image dimensions and coordinates.
        System.out.print("Image Dimensions: ");
        System.out.print(pi.getWidth() + "x" + pi.getHeight() + " pixels");

        // Remember getMaxX and getMaxY return the coordinate of the next point!
        System.out.println(
            " (from " + pi.getMinX() + "," + pi.getMinY() + " to " +
            (pi.getMaxX() - 1) + "," + (pi.getMaxY() - 1) + ")");

        if ((pi.getNumXTiles() != 1) || (pi.getNumYTiles() != 1)) { // Is it tiled?
                                                                    // Tiles number, dimensions and coordinates.
            System.out.print("Tiles: ");
            System.out.print(
                pi.getTileWidth() + "x" + pi.getTileHeight() + " pixels" +
                " (" + pi.getNumXTiles() + "x" + pi.getNumYTiles() + " tiles)");
            System.out.print(
                " (from " + pi.getMinTileX() + "," + pi.getMinTileY() + " to " +
                pi.getMaxTileX() + "," + pi.getMaxTileY() + ")");
            System.out.println(
                " offset: " + pi.getTileGridXOffset() + "," +
                pi.getTileGridXOffset());
        }

        // Display info about the SampleModel of the image.
        SampleModel sm = pi.getSampleModel();
        System.out.println("Number of bands: " + sm.getNumBands());
        System.out.print("Data type: ");

        switch (sm.getDataType()) {
        case DataBuffer.TYPE_BYTE :
            System.out.println("byte");

            break;

        case DataBuffer.TYPE_SHORT :
            System.out.println("short");

            break;

        case DataBuffer.TYPE_USHORT :
            System.out.println("ushort");

            break;

        case DataBuffer.TYPE_INT :
            System.out.println("int");

            break;

        case DataBuffer.TYPE_FLOAT :
            System.out.println("float");

            break;

        case DataBuffer.TYPE_DOUBLE :
            System.out.println("double");

            break;

        case DataBuffer.TYPE_UNDEFINED :
            System.out.println("undefined");

            break;
        }

        // Display info about the ColorModel of the image.
        ColorModel cm = pi.getColorModel();

        if (cm != null) {
            System.out.println(
                "Number of color components: " + cm.getNumComponents());
            System.out.println("Bits per pixel: " + cm.getPixelSize());
            System.out.print("Image Transparency: ");

            switch (cm.getTransparency()) {
            case Transparency.OPAQUE :
                System.out.println("opaque");

                break;

            case Transparency.BITMASK :
                System.out.println("bitmask");

                break;

            case Transparency.TRANSLUCENT :
                System.out.println("translucent");

                break;
            }
        } else {
            System.out.println("No color model.");
        }

        // Set up the parameters for the Histogram object.
        int[]          bins = { 256, 256, 256 }; // The number of bins.
        double[]       low = { 0.0D, 0.0D, 0.0D }; // The low value.
        double[]       high = { 256.0D, 256.0D, 256.0D }; // The high value.
                                                          // Construct the Histogram object.

        ///Histogram      hist = new Histogram(bins, low, high);

        // Create the parameter block.
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(pi); // Specify the source image
                          //pb.add(hist); // Specify the histogram

        pb.add(null); // No ROI
        pb.add(1); // Sampling
        pb.add(1); // periods
        pb.add(bins);
        pb.add(low);
        pb.add(high);

        // Perform the histogram operation.
        PlanarImage dst = (PlanarImage) JAI.create("histogram", pb, null);

        // Retrieve the histogram data.
        Histogram hist = (Histogram) dst.getProperty("histogram");

        // Print 1-band histogram.
        for (int i = 0; i < hist.getNumBins(0); i++) {
            System.out.println(hist.getBinSize(0, i));
        }

        double[] res = hist.getEntropy();
        System.out.println("getEntropy: " + Arrays.toString(res));

        res = hist.getIterativeThreshold();
        System.out.println("getIterativeThreshold: " + Arrays.toString(res));

        res = hist.getMaxEntropyThreshold();
        System.out.println("getMaxEntropyThreshold: " + Arrays.toString(res));

        res = hist.getMaxVarianceThreshold();
        System.out.println("getMaxVarianceThreshold: " + Arrays.toString(res));

        res = hist.getMinErrorThreshold();
        System.out.println("getMinErrorThreshold: " + Arrays.toString(res));

        res = hist.getMinFuzzinessThreshold();
        System.out.println("getMinFuzzinessThreshold: " + Arrays.toString(res));

        res = hist.getModeThreshold(0.5d);
        System.out.println("getMinErrorThreshold-0.5: " + Arrays.toString(res));

        res = hist.getModeThreshold(1d);
        System.out.println("getMinErrorThreshold-1: " + Arrays.toString(res));

        res = hist.getModeThreshold(2d);
        System.out.println("getMinErrorThreshold-2: " + Arrays.toString(res));

        res = hist.getModeThreshold(5d);
        System.out.println("getMinErrorThreshold-5: " + Arrays.toString(res));

        res = hist.getModeThreshold(10d);
        System.out.println("getMinErrorThreshold-10: " + Arrays.toString(res));

        res = hist.getLowValue();
        System.out.println("getLowValue: " + Arrays.toString(res));

        res = hist.getHighValue();
        System.out.println("getHighValue: " + Arrays.toString(res));

        res = hist.getMean();
        System.out.println("getMean: " + Arrays.toString(res));

        res = hist.getStandardDeviation();
        System.out.println("getStandardDeviation: " + Arrays.toString(res));
    }

    private ImageInfo ()
    {
    }
}
