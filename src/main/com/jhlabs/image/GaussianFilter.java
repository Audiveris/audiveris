//----------------------------------------------------------------------------//
//                                                                            //
//                        G a u s s i a n F i l t e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
/*
 ** Copyright 2005 Huxtable.com. All rights reserved.
 */
package com.jhlabs.image;

import java.awt.image.BufferedImage;
import java.awt.image.Kernel;
import java.awt.image.WritableRaster;

/**
 * A filter which applies Gaussian blur to an image.
 * This is a subclass of ConvolveFilter which simply creates a kernel with
 * a Gaussian distribution for blurring.
 *
 * @author Jerry Huxtable
 */
public class GaussianFilter
        extends ConvolveFilter
{
    //~ Static fields/initializers ---------------------------------------------

    static final long serialVersionUID = 5377089073023183684L;

    //~ Instance fields --------------------------------------------------------
    /** Radius of kernel. */
    protected float radius;

    //~ Constructors -----------------------------------------------------------
    //----------------//
    // GaussianFilter //
    //----------------//
    /**
     * Construct a Gaussian filter with a radius value of 2.
     */
    public GaussianFilter ()
    {
        this(2);
    }

    //----------------//
    // GaussianFilter //
    //----------------//
    /**
     * Construct a Gaussian filter wth a specified radius value.
     *
     * @param radius blur radius in pixels
     */
    public GaussianFilter (float radius)
    {
        setRadius(radius);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // makeKernel //
    //------------//
    /**
     * Make a Gaussian blur kernel.
     *
     * @param radius desired kernel radius specified in pixels around center
     */
    public static Kernel makeKernel (float radius)
    {
        final int r = (int) Math.ceil(radius);
        final int rows = (r * 2) + 1;
        final float[] matrix = new float[rows];
        final float sigma = 1f; //HB: was radius / 3;
        final float sigmaSq2 = 2 * sigma * sigma;
        final float radiusSq = radius * radius;

        float total = 0;
        int index = 0;

        for (int row = -r; row <= r; row++) {
            float distanceSq = row * row;

            if (distanceSq > radiusSq) {
                matrix[index] = 0;
            } else {
                matrix[index] = (float) Math.exp(-distanceSq / sigmaSq2);
            }

            total += matrix[index];
            index++;
        }

        // Normalize all matrix items
        for (int i = 0; i < rows; i++) {
            matrix[i] /= total;
        }

        return new Kernel(rows, 1, matrix);
    }

    //--------//
    // filter //
    //--------//
    /**
     * Run this Gaussian filter on the provided source image.
     *
     * @param src the provided source image
     * @param dst the destination image (allocated if null)
     * @return the destination image
     */
    @Override
    public BufferedImage filter (BufferedImage src,
                                 BufferedImage dst)
    {
        int width = src.getWidth();
        int height = src.getHeight();

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        int[] inPixels = new int[width * height];
        int[] outPixels = new int[width * height];
        getRGB(src, 0, 0, width, height, inPixels);

        convolveAndTranspose(inPixels, outPixels, width, height);
        convolveAndTranspose(outPixels, inPixels, height, width);

        final BufferedImage img = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_BYTE_GRAY);
        final WritableRaster raster = img.getRaster();
        final int[] pixel = new int[1];

        for (int y = 0; y < height; y++) {
            final int offset = y*width;
            for (int x = 0; x < width; x++) {
                pixel[0] = inPixels[offset + x];
                raster.setPixel(x, y, pixel);
            }
        }
        

        return img;
    }

    //-----------//
    // getRadius //
    //-----------//
    /**
     * Get the radius of the kernel.
     *
     * @return the radius
     */
    public float getRadius ()
    {
        return radius;
    }

    //-----------//
    // setRadius //
    //-----------//
    /**
     * Set the radius of the kernel, and hence the amount of blur.
     * The bigger the radius, the longer this filter will take.
     *
     * @param radius the radius of the blur in pixels.
     */
    public void setRadius (float radius)
    {
        this.radius = radius;
        kernel = makeKernel(radius);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "Blur/Gaussian Blur...";
    }

    //----------------------//
    // convolveAndTranspose //
    //----------------------//
    protected void convolveAndTranspose (int[] inPixels,
                                         int[] outPixels,
                                         int width,
                                         int height)
    {
        float[] matrix = kernel.getKernelData(null);
        int cols = kernel.getWidth();
        int cols2 = cols / 2;

        for (int y = 0; y < height; y++) {
            int index = y;
            int ioffset = y * width;

            for (int x = 0; x < width; x++) {
                float r = 0;
                float g = 0;
                float b = 0;
                float a = 0;
                int moffset = cols2;

                for (int col = -cols2; col <= cols2; col++) {
                    float f = matrix[moffset + col];

                    if (f != 0) {
                        int ix = x + col;

                        if (ix < 0) {
                            if (edgeAction == CLAMP_EDGES) {
                                ix = 0;
                            } else if (edgeAction == WRAP_EDGES) {
                                ix = (x + width) % width;
                            }
                        } else if (ix >= width) {
                            if (edgeAction == CLAMP_EDGES) {
                                ix = width - 1;
                            } else if (edgeAction == WRAP_EDGES) {
                                ix = (x + width) % width;
                            }
                        }

                        int rgb = inPixels[ioffset + ix];
                        a += (f * ((rgb >> 24) & 0xff));
                        r += (f * ((rgb >> 16) & 0xff));
                        g += (f * ((rgb >> 8) & 0xff));
                        b += (f * (rgb & 0xff));
                    }
                }

                int ia = alpha ? clamp((int) (a + 0.5)) : 0xff;
                int ir = clamp((int) (r + 0.5));
                int ig = clamp((int) (g + 0.5));
                int ib = clamp((int) (b + 0.5));
                outPixels[index] = (ia << 24) | (ir << 16) | (ig << 8) | ib;
                index += height;
            }
        }
    }
}
