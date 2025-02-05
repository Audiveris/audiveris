//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        I m a g e U t i l                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

/**
 * Class <code>ImageUtil</code> gathers convenient static methods working on images.
 * <p>
 * NOTA: Most of its methods have been re-written in order to remove all former dependencies on JAI.
 * They may not be as efficient as their JAI counterparts, but this is a first implementation.
 *
 * @author Hervé Bitteur
 */
public abstract class ImageUtil
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ImageUtil.class);

    //~ Constructors -------------------------------------------------------------------------------

    /** No constructor needed, this is just a functional class. */
    private ImageUtil ()
    {
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-----------------//
    // grayAlphaToGray //
    //-----------------//
    /**
     * Take a 2-band image (gray + alpha) and provide the output gray value.
     *
     * @param image input image with 2 bands (gray band and alpha band)
     * @return a gray image
     */
    public static BufferedImage grayAlphaToGray (BufferedImage image)
    {
        logger.info("Converting gray+alpha to gray");

        final int width = image.getWidth();
        final int height = image.getHeight();
        final Raster source = image.getData();
        final int numBands = image.getSampleModel().getNumBands(); // Including alpha
        final int[] levels = new int[numBands];

        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        final WritableRaster raster = img.getRaster();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                source.getPixel(x, y, levels);
                raster.setSample(x, y, 0, levels[0]);
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

    //--------------//
    // maxRgbToGray //
    //--------------//
    /**
     * Take an RGB image and always select the maximum pixel value among R, G and B bands
     * to provide the output gray value.
     *
     * @param image input image with 3 bands RGB
     * @return a gray image
     */
    public static BufferedImage maxRgbToGray (BufferedImage image)
    {
        logger.info("Converting max RGB to gray");

        final int width = image.getWidth();
        final int height = image.getHeight();
        final Raster source = image.getData();

        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        final WritableRaster raster = img.getRaster();

        // We use the max value among the RGB channels
        final int[] levels = new int[3];
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
        final int type = img.getType();
        final ColorModel colorModel = img.getColorModel();
        logger.info(
                "{} type:({}={}) cm:({})",
                (title != null) ? title : "",
                type,
                typeOf(type),
                colorModel);
    }

    //------------//
    // rgbaToGray //
    //------------//
    /**
     * Take an RBGA image and provide an output gray image, using standard luminance.
     * <p>
     * This method assumes that the bands were pre-multiplied.
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
     * <p>
     * This method assumes that the bands were pre-multiplied.
     *
     * @param rgba input RGBA image
     * @return output RGB image
     */
    public static BufferedImage rgbaToRgb (BufferedImage rgba)
    {
        logger.info("Discarding alpha band ...");

        final int width = rgba.getWidth();
        final int height = rgba.getHeight();
        final Raster source = rgba.getData();
        final int numBands = rgba.getColorModel().getNumComponents(); // Including alpha
        final int[] levels = new int[numBands];

        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final WritableRaster raster = img.getRaster();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                source.getPixel(x, y, levels);
                raster.setPixel(x, y, levels);
            }
        }

        return img;
    }

    //-----------//
    // rgbToGray //
    //-----------//
    /**
     * Take an RGB image and combine the R, G and B bands according to standard luminance
     * value to provide the output gray value.
     *
     * @param rgb input image with 3 bands RGB
     * @return a gray image
     */
    public static BufferedImage rgbToGray (BufferedImage rgb)
    {
        logger.info("Converting RGB to gray ...");

        final int width = rgb.getWidth();
        final int height = rgb.getHeight();
        final Raster source = rgb.getData();
        final int numBands = rgb.getColorModel().getNumComponents();
        final int[] inLevels = new int[numBands];

        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        final WritableRaster raster = img.getRaster();

        // We use luminance value based on standard RGB combination
        final double[] weights =
        { 0.114d, 0.587d, 0.299d };

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                source.getPixel(x, y, inLevels);

                double val = 0;
                for (int i = 0; i < 3; i++) {
                    val += weights[i] * inLevels[i];
                }

                raster.setSample(x, y, 0, (int) Math.rint(val));
            }
        }

        return img;
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
        saveOnDisk(image, "", name);
    }

    //------------//
    // saveOnDisk //
    //------------//
    /**
     * Convenient method to save a BufferedImage to disk (in application temp area)
     *
     * @param s     applied scaling
     * @param image the image to save
     * @param name  file name, without extension
     */
    public static void saveOnDisk (int s,
                                   BufferedImage image,
                                   String name)
    {
        final BufferedImage scaledImg = new BufferedImage(
                s * image.getWidth(),
                s * image.getHeight(),
                image.getType());
        final AffineTransform at = AffineTransform.getScaleInstance(s, s);
        AffineTransformOp scaleOp = new AffineTransformOp(at, null);
        scaleOp.filter(image, scaledImg);
        saveOnDisk(scaledImg, "", name);
    }

    //------------//
    // saveOnDisk //
    //------------//
    /**
     * Convenient method to save a BufferedImage to disk,
     * in a subdirectory of application temp area)
     *
     * @param image the image to save
     * @param dirs  the intermediate sub-directories, perhaps an empty string
     * @param name  file name, without extension
     */
    public static void saveOnDisk (BufferedImage image,
                                   String dirs,
                                   String name)
    {
        try {
            final Path dir = Files.createDirectories(WellKnowns.TEMP_FOLDER.resolve(dirs));
            final File file = dir.resolve(name + ".png").toFile();
            ImageIO.write(image, "png", file);
            logger.info("Saved {}", file);
        } catch (IOException ex) {
            logger.warn("Error saving " + name, ex);
        }
    }

    //--------//
    // typeOf //
    //--------//
    private static String typeOf (int type)
    {
        return switch (type) {
            case BufferedImage.TYPE_CUSTOM -> "TYPE_CUSTOM";
            case BufferedImage.TYPE_INT_RGB -> "TYPE_INT_RGB";
            case BufferedImage.TYPE_INT_ARGB -> "TYPE_INT_ARGB";
            case BufferedImage.TYPE_INT_ARGB_PRE -> "TYPE_INT_ARGB_PRE";
            case BufferedImage.TYPE_INT_BGR -> "TYPE_INT_BGR";
            case BufferedImage.TYPE_3BYTE_BGR -> "TYPE_3BYTE_BGR";
            case BufferedImage.TYPE_4BYTE_ABGR -> "TYPE_4BYTE_ABGR";
            case BufferedImage.TYPE_4BYTE_ABGR_PRE -> "TYPE_4BYTE_ABGR_PRE";
            case BufferedImage.TYPE_USHORT_565_RGB -> "TYPE_USHORT_565_RGB";
            case BufferedImage.TYPE_USHORT_555_RGB -> "TYPE_USHORT_555_RGB";
            case BufferedImage.TYPE_BYTE_GRAY -> "TYPE_BYTE_GRAY";
            case BufferedImage.TYPE_USHORT_GRAY -> "TYPE_USHORT_GRAY";
            case BufferedImage.TYPE_BYTE_BINARY -> "TYPE_BYTE_BINARY";
            case BufferedImage.TYPE_BYTE_INDEXED -> "TYPE_BYTE_INDEXED";
            default -> "?";
        };
    }
}
