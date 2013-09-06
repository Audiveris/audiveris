//----------------------------------------------------------------------------//
//                                                                            //
//               A b s t r a c t B u f f e r e d I m a g e O p                //
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
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;

/**
 * A convenience class which implements those methods of
 * BufferedImageOp which are rarely changed.
 */
public abstract class AbstractBufferedImageOp
        implements BufferedImageOp
{
    //~ Methods ----------------------------------------------------------------

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
    public Rectangle2D getBounds2D (BufferedImage src)
    {
        return new Rectangle(0, 0, src.getWidth(), src.getHeight());
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

    /**
     * A convenience method for getting ARGB pixels from an image.
     * This tries to avoid the performance penalty of BufferedImage.getRGB
     * unmanaging the image.
     */
    public int[] getRGB (BufferedImage image,
                         int x,
                         int y,
                         int width,
                         int height,
                         int[] pixels)
    {
        int type = image.getType();

        if ((type == BufferedImage.TYPE_INT_ARGB)
            || (type == BufferedImage.TYPE_INT_RGB)) {
            return (int[]) image.getRaster()
                    .getDataElements(x, y, width, height, pixels);
        }

        return image.getRGB(x, y, width, height, pixels, 0, width);
    }

    @Override
    public RenderingHints getRenderingHints ()
    {
        return null;
    }

    /**
     * A convenience method for setting ARGB pixels in an image.
     * This tries to avoid the performance penalty of BufferedImage.setRGB
     * unmanaging the image.
     */
    public void setRGB (BufferedImage image,
                        int x,
                        int y,
                        int width,
                        int height,
                        int[] pixels)
    {
        int type = image.getType();

        if ((type == BufferedImage.TYPE_INT_ARGB)
            || (type == BufferedImage.TYPE_INT_RGB)) {
            image.getRaster()
                    .setDataElements(x, y, width, height, pixels);
        } else {
            image.setRGB(x, y, width, height, pixels, 0, width);
        }
    }
}
