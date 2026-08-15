//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       C a c h e d I m a g e                                    //
//------------------------------------------------------------------------------------------------//
package org.audiveris.omr.image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

/**
 * A BufferedImage wrapper that stores the image in a SoftReference,
 * reloading from disk when needed.
 */
public class CachedImage
{
    private final Path filePath;

    private SoftReference<BufferedImage> imageRef;

    private long lastAccessTime;

    public CachedImage (Path filePath)
    {
        this.filePath = filePath;
    }

    public synchronized BufferedImage get ()
            throws IOException
    {
        BufferedImage img = (imageRef != null) ? imageRef.get() : null;
        if (img == null) {
            try (InputStream in = Files.newInputStream(filePath)) {
                img = ImageIO.read(in);
            }
            imageRef = new SoftReference<>(img);
        }
        lastAccessTime = System.nanoTime();
        ImageCacheManager.getInstance().register(this);
        return img;
    }

    public synchronized void release ()
    {
        if (imageRef != null) {
            imageRef.clear();
            imageRef = null;
        }
    }

    public synchronized boolean isLoaded ()
    {
        return imageRef != null && imageRef.get() != null;
    }

    public long getLastAccessTime ()
    {
        return lastAccessTime;
    }

    public Path getFilePath ()
    {
        return filePath;
    }
}
