//----------------------------------------------------------------------------//
//                                                                            //
//                          O m r E x e c u t o r s                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class <code>OmrExecutors</code> handles several pools of threads
 * provided to the Audiveris application: <ul>
 * <li>lowExecutor: a fixed nb (cpu+1) of threads with low priority</li>
 * <li>highExecutor: a fixed nb (cpu+1) of threads with high priority</li>
 * <li>cachedLowExecutor: a varying nb of threads with low priority</li>
 * <li>ocrExecutor: one thread with high priority and large stack size</li>
 * </ul>
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class OmrExecutors
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(OmrExecutors.class);

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Number of processors available */
    private static final int cpuNb = Runtime.getRuntime()
                                            .availableProcessors();
    private static Pool highs = new Highs();
    private static Pool lows = new Lows();
    private static Pool cachedLows = new CachedLows();
    private static Pool ocrs = new Ocrs();

    //~ Constructors -----------------------------------------------------------

    /** Not meant to be instantiated */
    private OmrExecutors ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //----------------------//
    // getCachedLowExecutor //
    //----------------------//
    /**
     * Return the (single) pool of cached low priority threads
     *
     * @return the cached low pool, allocated if needed
     */
    public static ExecutorService getCachedLowExecutor ()
    {
        return cachedLows.getPool();
    }

    //-----------------//
    // getHighExecutor //
    //-----------------//
    /**
     * Return the (single) pool of high priority threads
     *
     * @return the high pool, allocated if needed
     */
    public static ExecutorService getHighExecutor ()
    {
        return highs.getPool();
    }

    //----------------//
    // getLowExecutor //
    //----------------//
    /**
     * Return the (single) pool of low priority threads
     *
     * @return the low pool, allocated if needed
     */
    public static ExecutorService getLowExecutor ()
    {
        return lows.getPool();
    }

    //-----------------//
    // getNumberOfCpus //
    //-----------------//
    /**
     * Report the number of "processors" available
     *
     * @return the number of CPUs
     */
    public static int getNumberOfCpus ()
    {
        return cpuNb;
    }

    //----------------//
    // getOcrExecutor //
    //----------------//
    /**
     * Return the (single) pool of OCR threads
     *
     * @return the OCR pool, allocated if needed
     */
    public static ExecutorService getOcrExecutor ()
    {
        return ocrs.getPool();
    }

    //----------//
    // shutdown //
    //----------//
    /**
     * Gracefully shut down  all the executors launched
     */
    public static void shutdown ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("Closing all pools");
        }

        for (Pool pool : Arrays.asList(cachedLows, lows, highs, ocrs)) {
            if (pool.isActive()) {
                pool.close();
            } else {
                if (logger.isFineEnabled()) {
                    logger.fine("Pool " + pool.getName() + " not active");
                }
            }
        }
    }

    //----------------//
    // useParallelism //
    //----------------//
    /**
     * Report whether we should try to use parallelism as much as possible
     *
     * @return true for parallel
     */
    public static boolean useParallelism ()
    {
        return constants.useParallelism.getValue();
    }

    //~ Inner Classes ----------------------------------------------------------

    //------//
    // Pool //
    //------//
    private abstract static class Pool
    {
        //~ Instance fields ----------------------------------------------------

        /** The underlying pool of threads */
        protected ExecutorService pool;

        //~ Methods ------------------------------------------------------------

        /** Name the pool */
        public abstract String getName ();

        /** Is the pool active? */
        public boolean isActive ()
        {
            return (pool != null) && !pool.isShutdown();
        }

        /** Get the pool ready to use */
        public synchronized ExecutorService getPool ()
        {
            if (!isActive()) {
                if (logger.isFineEnabled()) {
                    logger.fine("Creating pool: " + getName());
                }

                pool = createPool();
            }

            return pool;
        }

        /** Terminate the pool */
        public synchronized void close ()
        {
            if (!isActive()) {
                return;
            }

            if (logger.isFineEnabled()) {
                logger.fine("Closing pool: " + getName());
            }

            pool.shutdown(); // Disable new tasks from being submitted

            try {
                // Wait a while for existing tasks to terminate
                if (!pool.awaitTermination(
                    constants.graceDelay.getValue(),
                    TimeUnit.SECONDS)) {
                    // Cancel currently executing tasks
                    pool.shutdownNow();

                    // Wait a while for tasks to respond to being cancelled
                    if (!pool.awaitTermination(
                        constants.graceDelay.getValue(),
                        TimeUnit.SECONDS)) {
                        logger.warning(
                            "Pool " + getName() + " did not terminate");
                    }
                }
            } catch (InterruptedException ie) {
                // (Re-)Cancel if current thread also interrupted
                pool.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread()
                      .interrupt();
            }
        }

        /** Needed to create the concrete pool */
        protected abstract ExecutorService createPool ();
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean useParallelism = new Constant.Boolean(
            true,
            "Should we use parallelism when we have several processors?");

        //
        Constant.Integer graceDelay = new Constant.Integer(
            "seconds",
            15,
            "Time to wait for terminating tasks");
    }

    //------------//
    // CachedLows //
    //------------//
    /** Cached pool with low priority */
    private static class CachedLows
        extends Pool
    {
        //~ Methods ------------------------------------------------------------

        public String getName ()
        {
            return "cachedLow";
        }

        @Override
        protected ExecutorService createPool ()
        {
            return Executors.newCachedThreadPool(
                new Factory(getName(), Thread.MIN_PRIORITY, 0));
        }
    }

    //---------//
    // Factory //
    //---------//
    private static class Factory
        implements ThreadFactory
    {
        //~ Instance fields ----------------------------------------------------

        private final ThreadGroup   group;
        private final String        threadPrefix;
        private final int           threadPriority;
        private final long          stackSize;
        private final AtomicInteger threadNumber = new AtomicInteger(0);

        //~ Constructors -------------------------------------------------------

        Factory (String threadPrefix,
                 int    threadPriority,
                 long   stackSize)
        {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup()
                    : Thread.currentThread()
                            .getThreadGroup();
            this.threadPrefix = threadPrefix;
            this.threadPriority = threadPriority;
            this.stackSize = stackSize;
        }

        //~ Methods ------------------------------------------------------------

        @Implement(ThreadFactory.class)
        public Thread newThread (Runnable r)
        {
            Thread t = new Thread(group, r, getOneThreadName(), stackSize);

            if (t.isDaemon()) {
                t.setDaemon(false);
            }

            if (t.getPriority() != threadPriority) {
                t.setPriority(threadPriority);
            }

            return t;
        }

        private String getOneThreadName ()
        {
            return threadPrefix + "-thread-" + threadNumber.incrementAndGet();
        }
    }

    //-------//
    // Highs //
    //-------//
    /** Fixed pool with high priority */
    private static class Highs
        extends Pool
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public String getName ()
        {
            return "high";
        }

        @Override
        protected ExecutorService createPool ()
        {
            return Executors.newFixedThreadPool(
                useParallelism() ? (cpuNb + 1) : 1,
                new Factory(getName(), Thread.NORM_PRIORITY, 0));
        }
    }

    //------//
    // Lows //
    //------//
    /** Fixed pool with low priority */
    private static class Lows
        extends Pool
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public String getName ()
        {
            return "low";
        }

        @Override
        protected ExecutorService createPool ()
        {
            return Executors.newFixedThreadPool(
                useParallelism() ? (cpuNb + 1) : 1,
                new Factory(getName(), Thread.MIN_PRIORITY, 0));
        }
    }

    //------//
    // Ocrs //
    //------//
    /** One-thread pool with high priority */
    private static class Ocrs
        extends Pool
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public String getName ()
        {
            return "ocr";
        }

        @Override
        protected ExecutorService createPool ()
        {
            return Executors.newFixedThreadPool(
                1,
                new Factory(getName(), Thread.NORM_PRIORITY, 10000000L));
        }
    }
}
