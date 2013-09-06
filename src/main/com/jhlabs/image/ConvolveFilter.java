//----------------------------------------------------------------------------//
//                                                                            //
//                        C o n v o l v e F i l t e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
/*
 ** Copyright 2005 Huxtable.com. All rights reserved.
 */
package com.jhlabs.image;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Kernel;
import java.awt.image.ConvolveOp;

/**
 * A filter which applies a convolution kernel to an image.
 *
 * @author Jerry Huxtable
 */
public class ConvolveFilter
        extends AbstractBufferedImageOp
{
    //~ Static fields/initializers ---------------------------------------------

    static final long serialVersionUID = 2239251672685254626L;

    public static int ZERO_EDGES = 0;

    public static int CLAMP_EDGES = 1;

    public static int WRAP_EDGES = 2;

    //~ Instance fields --------------------------------------------------------
    protected Kernel kernel = null;

    public boolean alpha = true;

    protected int edgeAction = CLAMP_EDGES;

    //~ Constructors -----------------------------------------------------------
    /**
     * Construct a filter with a null kernel. This is only useful if you're
     * going to change the kernel later on.
     */
    public ConvolveFilter ()
    {
        this(new float[9]);
    }

    /**
     * Construct a filter with the given 3x3 kernel.
     *
     * @param matrix an array of 9 floats containing the kernel
     */
    public ConvolveFilter (float[] matrix)
    {
        this(new Kernel(3, 3, matrix));
    }

    /**
     * Construct a filter with the given kernel.
     *
     * @param rows   the number of rows in the kernel
     * @param cols   the number of columns in the kernel
     * @param matrix an array of rows*cols floats containing the kernel
     */
    public ConvolveFilter (int rows,
                           int cols,
                           float[] matrix)
    {
        this(new Kernel(cols, rows, matrix));
    }

    /**
     * Construct a filter with the given 3x3 kernel.
     *
     * @param matrix an array of 9 floats containing the kernel
     */
    public ConvolveFilter (Kernel kernel)
    {
        this.kernel = kernel;
    }

    //~ Methods ----------------------------------------------------------------
    public void convolve (int[] inPixels,
                          int[] outPixels,
                          int width,
                          int height)
    {
        if (kernel.getHeight() == 1) {
            convolveH(inPixels, outPixels, width, height);
        } else if (kernel.getWidth() == 1) {
            convolveV(inPixels, outPixels, width, height);
        } else {
            convolveHV(inPixels, outPixels, width, height);
        }
    }

    @Override
    public BufferedImage createCompatibleDestImage (BufferedImage src,
                                                    ColorModel dstCM)
    {
        if (dstCM == null) {
            dstCM = src.getColorModel();
        }

        return new BufferedImage(
                dstCM,
                dstCM.createCompatibleWritableRaster(
                src.getWidth(),
                src.getHeight()),
                dstCM.isAlphaPremultiplied(),
                null);
    }

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

        convolve(inPixels, outPixels, width, height);

        setRGB(dst, 0, 0, width, height, outPixels);

        return dst;
    }

    @Override
    public Rectangle2D getBounds2D (BufferedImage src)
    {
        return new Rectangle(0, 0, src.getWidth(), src.getHeight());
    }

    public int getEdgeAction ()
    {
        return edgeAction;
    }

    public Kernel getKernel ()
    {
        return kernel;
    }

    @Override
    public Point2D getPoint2D (Point2D srcPt,
                               Point2D dstPt)
    {
        if (dstPt == null) {
            dstPt = new Point2D.Double();
        }

        dstPt.setLocation(srcPt.getX(), srcPt.getY());

        return dstPt;
    }

    @Override
    public RenderingHints getRenderingHints ()
    {
        return null;
    }

    public void setEdgeAction (int edgeAction)
    {
        this.edgeAction = edgeAction;
    }

    public void setKernel (Kernel kernel)
    {
        this.kernel = kernel;
    }

    @Override
    public String toString ()
    {
        return "Blur/Convolve...";
    }

    /**
     * Clamp a value to the range 0..255.
     */
    protected static int clamp (int c)
    {
        if (c < 0) {
            return 0;
        }

        if (c > 255) {
            return 255;
        }

        return c;
    }

    /**
     * Convolve with a kernel consisting of one row.
     */
    private void convolveH (int[] inPixels,
                            int[] outPixels,
                            int width,
                            int height)
    {
        int index = 0;
        float[] matrix = kernel.getKernelData(null);
        int cols = kernel.getWidth();
        int cols2 = cols / 2;

        for (int y = 0; y < height; y++) {
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
                outPixels[index++] = (ia << 24) | (ir << 16) | (ig << 8) | ib;
            }
        }
    }

    /**
     * Convolve with a 2D kernel.
     */
    private void convolveHV (int[] inPixels,
                             int[] outPixels,
                             int width,
                             int height)
    {
        int index = 0;
        float[] matrix = kernel.getKernelData(null);
        int rows = kernel.getHeight();
        int cols = kernel.getWidth();
        int rows2 = rows / 2;
        int cols2 = cols / 2;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float r = 0;
                float g = 0;
                float b = 0;
                float a = 0;

                for (int row = -rows2; row <= rows2; row++) {
                    int iy = y + row;
                    int ioffset;

                    if ((0 <= iy) && (iy < height)) {
                        ioffset = iy * width;
                    } else if (edgeAction == CLAMP_EDGES) {
                        ioffset = y * width;
                    } else if (edgeAction == WRAP_EDGES) {
                        ioffset = ((iy + height) % height) * width;
                    } else {
                        continue;
                    }

                    int moffset = (cols * (row + rows2)) + cols2;

                    for (int col = -cols2; col <= cols2; col++) {
                        float f = matrix[moffset + col];

                        if (f != 0) {
                            int ix = x + col;

                            if (!((0 <= ix) && (ix < width))) {
                                if (edgeAction == CLAMP_EDGES) {
                                    ix = x;
                                } else if (edgeAction == WRAP_EDGES) {
                                    ix = (x + width) % width;
                                } else {
                                    continue;
                                }
                            }

                            int rgb = inPixels[ioffset + ix];
                            a += (f * ((rgb >> 24) & 0xff));
                            r += (f * ((rgb >> 16) & 0xff));
                            g += (f * ((rgb >> 8) & 0xff));
                            b += (f * (rgb & 0xff));
                        }
                    }
                }

                int ia = alpha ? clamp((int) (a + 0.5)) : 0xff;
                int ir = clamp((int) (r + 0.5));
                int ig = clamp((int) (g + 0.5));
                int ib = clamp((int) (b + 0.5));
                outPixels[index++] = (ia << 24) | (ir << 16) | (ig << 8) | ib;
            }
        }
    }

    /**
     * Convolve with a kernel consisting of one column.
     */
    private void convolveV (int[] inPixels,
                            int[] outPixels,
                            int width,
                            int height)
    {
        int index = 0;
        float[] matrix = kernel.getKernelData(null);
        int rows = kernel.getHeight();
        int rows2 = rows / 2;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float r = 0;
                float g = 0;
                float b = 0;
                float a = 0;

                for (int row = -rows2; row <= rows2; row++) {
                    int iy = y + row;
                    int ioffset;

                    if (iy < 0) {
                        if (edgeAction == CLAMP_EDGES) {
                            ioffset = 0;
                        } else if (edgeAction == WRAP_EDGES) {
                            ioffset = ((y + height) % height) * width;
                        } else {
                            ioffset = iy * width;
                        }
                    } else if (iy >= height) {
                        if (edgeAction == CLAMP_EDGES) {
                            ioffset = (height - 1) * width;
                        } else if (edgeAction == WRAP_EDGES) {
                            ioffset = ((y + height) % height) * width;
                        } else {
                            ioffset = iy * width;
                        }
                    } else {
                        ioffset = iy * width;
                    }

                    float f = matrix[row + rows2];

                    if (f != 0) {
                        int rgb = inPixels[ioffset + x];
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
                outPixels[index++] = (ia << 24) | (ir << 16) | (ig << 8) | ib;
            }
        }
    }
}
