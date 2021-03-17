//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        I m a g e U t i l                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

import org.audiveris.omr.WellKnowns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;

/**
 * Class {@code ImageUtil} gathers convenient static methods working on images.
 * <p>
 * TODO: Perhaps chaining JAI commands into a single operation would be more efficient (memory-wise
 * and performance-wise) that performing each bulk operation one after the other. It would also
 * save multiple calls to "getAsBufferedImage()".
 *
 * @author Hervé Bitteur
 */
public abstract class ImageUtil
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ImageUtil.class);

    //~ Constructors -------------------------------------------------------------------------------
    private ImageUtil ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // invert //
    //--------//
    /**
     * Invert an image.
     *
     * @param image the image to process
     * @return the inverted image
     */
    public static BufferedImage invert (BufferedImage image)
    {
        return JAI.create("Invert", new ParameterBlock().addSource(image).add(null), null)
                .getAsBufferedImage();
    }

    //--------------//
    // maxRgbToGray //
    //--------------//
    /**
     * Take an RGB image and always select the maximum pixel value among R,G and B bands
     * to provide the output gray value.
     *
     * @param image input image with 3 bands RGB
     * @return a gray image
     */
    public static BufferedImage maxRgbToGray (BufferedImage image)
    {
        logger.info("Converting max RGB to gray");

        // We use the max value among the RGB channels
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = img.getRaster();
        Raster source = image.getData();
        int[] levels = new int[3];
        int maxLevel;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                source.getPixel(x, y, levels);
                maxLevel = 0;

                for (int level : levels) {
                    if (maxLevel < level) {
                        maxLevel = level;
                    }
                }

                raster.setSample(x, y, 0, maxLevel);
            }
        }

        return img;
    }

    //---------------//
    // maxRgbaToGray //
    //---------------//
    /**
     * Take an RGBA image and, ignoring the alpha value, always select the maximum pixel
     * value among R,G and B bands to provide the output gray value.
     *
     * @param rgba input image with 3 bands RGB and 1 Alpha channel
     * @return a gray image
     */
    public static BufferedImage maxRgbaToGray (BufferedImage rgba)
    {
        return maxRgbToGray(rgbaToRgb(rgba));
    }

    //-----------//
    // printInfo //
    //-----------//
    /**
     * Convenient method to print some characteristics of the provided image.
     *
     * @param img   the image to query
     * @param title a title to be printed, or null
     */
    public static void printInfo (BufferedImage img,
                                  String title)
    {
        int type = img.getType();
        ColorModel colorModel = img.getColorModel();
        logger.info(
                "{} type:({}={}) cm:({})",
                (title != null) ? title : "",
                type,
                typeOf(type),
                colorModel);
    }

    //-----------//
    // rgbToGray //
    //-----------//
    /**
     * Take an RGB image and combine the R,G and B bands according to standard luminance
     * value to provide the output gray value.
     *
     * @param rgb input image with 3 bands RGB
     * @return a gray image
     */
    public static BufferedImage rgbToGray (BufferedImage rgb)
    {
        logger.info("Converting RGB to gray ...");

        // We use luminance value based on standard RGB combination
        double[][] matrix = {{0.114d, 0.587d, 0.299d, 0.0d}};

        return JAI.create("bandcombine", new ParameterBlock().addSource(rgb).add(matrix), null)
                .getAsBufferedImage();
    }

    //------------//
    // rgbaToGray //
    //------------//
    /**
     * Take an RBGA image and provide an output gray image, using standard luminance.
     * This method assumes that the bands were premultiplied.
     *
     * @param rgba input RGBA image
     * @return output gray image
     */
    public static BufferedImage rgbaToGray (BufferedImage rgba)
    {
        return rgbToGray(rgbaToRgb(rgba));
    }

    //-----------//
    // rgbaToRgb //
    //-----------//
    /**
     * Take an RBGA image and provide an output with the alpha channel removed.
     * This method assumes that the bands were premultiplied.
     *
     * @param rgba input RGBA image
     * @return output RGB image
     */
    public static BufferedImage rgbaToRgb (BufferedImage rgba)
    {
        logger.info("Discarding alpha band ...");

        return JAI.create("bandselect", rgba, new int[]{0, 1, 2}).getAsBufferedImage();
    }

    //------------//
    // saveOnDisk //
    //------------//
    /**
     * Convenient method to save a BufferedImage to disk (in application temp area)
     *
     * @param image the image to save
     * @param name  file name, without extension
     */
    public static void saveOnDisk (BufferedImage image,
                                   String name)
    {
        try {
            final File file = WellKnowns.TEMP_FOLDER.resolve(name + ".png").toFile();
            ImageIO.write(image, "png", file);
            logger.info("Saved {}", file);
        } catch (IOException ex) {
            logger.warn("Error saving " + name, ex);
        }
    }

    //-----//
    // xor //
    //-----//
    /**
     * Xor operation on two images.
     *
     * @param image1 first image to process
     * @param image2 second image to process
     * @return the resulting image
     */
    public static BufferedImage xor (BufferedImage image1,
                                     BufferedImage image2)
    {
        return JAI.create("Xor", image1, image2).getAsBufferedImage();
    }

    //--------//
    // typeOf //
    //--------//
    private static String typeOf (int type)
    {
        switch (type) {
        case BufferedImage.TYPE_CUSTOM:
            return "TYPE_CUSTOM";

        case BufferedImage.TYPE_INT_RGB:
            return "TYPE_INT_RGB";

        case BufferedImage.TYPE_INT_ARGB:
            return "TYPE_INT_ARGB";

        case BufferedImage.TYPE_INT_ARGB_PRE:
            return "TYPE_INT_ARGB_PRE";

        case BufferedImage.TYPE_INT_BGR:
            return "TYPE_INT_BGR";

        case BufferedImage.TYPE_3BYTE_BGR:
            return "TYPE_3BYTE_BGR";

        case BufferedImage.TYPE_4BYTE_ABGR:
            return "TYPE_4BYTE_ABGR";

        case BufferedImage.TYPE_4BYTE_ABGR_PRE:
            return "TYPE_4BYTE_ABGR_PRE";

        case BufferedImage.TYPE_USHORT_565_RGB:
            return "TYPE_USHORT_565_RGB";

        case BufferedImage.TYPE_USHORT_555_RGB:
            return "TYPE_USHORT_555_RGB";

        case BufferedImage.TYPE_BYTE_GRAY:
            return "TYPE_BYTE_GRAY";

        case BufferedImage.TYPE_USHORT_GRAY:
            return "TYPE_USHORT_GRAY";

        case BufferedImage.TYPE_BYTE_BINARY:
            return "TYPE_BYTE_BINARY";

        case BufferedImage.TYPE_BYTE_INDEXED:
            return "TYPE_BYTE_INDEXED";

        default:
            return "?";
        }
    }
}
