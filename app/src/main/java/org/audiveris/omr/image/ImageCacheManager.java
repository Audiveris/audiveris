//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   I m a g e C a c h e M a n a g e r                            //
//------------------------------------------------------------------------------------------------//
package org.audiveris.omr.image;

import org.audiveris.omr.config.AudiverisProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Singleton that monitors memory and releases least-recently-used CachedImages
 * when heap pressure exceeds the configured threshold.
 */
public class ImageCacheManager
{
    private static final ImageCacheManager INSTANCE = new ImageCacheManager();

    private final List<CachedImage> registeredImages = new ArrayList<>();

    private final Timer timer;

    private final double threshold;

    private ImageCacheManager ()
    {
        threshold = AudiverisProperties.getMemoryUnloadThreshold();
        timer = new Timer("ImageCacheManager", true);
        timer.schedule(new TimerTask()
        {
            @Override
            public void run ()
            {
                checkMemory();
            }
        }, 10_000, 10_000);
    }

    public static ImageCacheManager getInstance ()
    {
        return INSTANCE;
    }

    public synchronized void register (CachedImage image)
    {
        if (!registeredImages.contains(image)) {
            registeredImages.add(image);
        }
    }

    public synchronized void unregister (CachedImage image)
    {
        registeredImages.remove(image);
    }

    private synchronized void checkMemory ()
    {
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        long max = rt.maxMemory();
        if (max <= 0) {
            return;
        }
        if ((double) used / max < threshold) {
            return;
        }

        CachedImage candidate = null;
        long oldest = Long.MAX_VALUE;
        for (CachedImage img : registeredImages) {
            if (img.isLoaded() && img.getLastAccessTime() < oldest) {
                candidate = img;
                oldest = img.getLastAccessTime();
            }
        }

        if (candidate != null) {
            candidate.release();
        }
    }

    public void shutdown ()
    {
        timer.cancel();
    }
}
