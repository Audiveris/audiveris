
package omr.util;

import java.util.concurrent.Future;

/**
 * Class <code>JaiLoader</code> is designed to speed up the load time
 * of the first <code>Picture</code> by allowing the <code>JAI</code> class to be
 * preloaded. Because, in some implementations, the <code>JAI</code> class must
 * load a number of renderers, its static initialization can take a long time.
 * On the first call to any static method in this class, this initialization
 * will begin in a low-priority thread, and any call to <code>ensureLoaded</code>
 * is guaranteed to block until the initialization is complete.
 *
 * @author Brenton Partridge
 * @version $Id$
 */
public class JaiLoader
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(JaiLoader.class);

    /** A future which reflects whether JAI has been initialized **/
    private static final Future<?> loading = OmrExecutors.getLowExecutor()
                                                         .submit(
        new JaiLoaderRunnable());

    //~ Constructors -----------------------------------------------------------

    private JaiLoader ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Blocks until the JAI class has been initialized.
     * If initialization has not yet begun, begins initialization.
     *
     */
    public static void ensureLoaded ()
    {
        try {
            loading.get();
        } catch (Exception e) {
            logger.severe("JAI loading failed", e);
        }
    }

    /**
     * On the first call, starts the initialization.
     *
     */
    public static void preload ()
    {
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * Actually initializes the <code>JAI</code> class.
     */
    private static class JaiLoaderRunnable
        implements Runnable
    {
        //~ Constructors -------------------------------------------------------

        private JaiLoaderRunnable ()
        {
        }

        //~ Methods ------------------------------------------------------------

        public void run ()
        {
            long startTime = System.currentTimeMillis();
            javax.media.jai.JAI.getBuildVersion();

            if (logger.isFineEnabled()) {
                logger.fine(
                    "Loaded JAI in " +
                    (System.currentTimeMillis() - startTime) + "ms");
            }
        }
    }
}
