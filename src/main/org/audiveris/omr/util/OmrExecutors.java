//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    O m r E x e c u t o r s                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.util;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.step.ProcessingCancellationException;
import org.audiveris.omr.util.param.Param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class {@code OmrExecutors} handles several pools of threads provided to Audiveris
 * application: <ul>
 * <li>lowExecutor: a fixed nb (#cpu+1) of threads with low priority</li>
 * <li>highExecutor: a fixed nb (#cpu+1) of threads with high priority</li>
 * <li>cachedLowExecutor: a varying nb of threads with low priority</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class OmrExecutors
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(OmrExecutors.class);

    private static final Constants constants = new Constants();

    /** Default parameter. */
    public static final Param<Boolean> defaultParallelism = new Default();

    /** Number of processors available. */
    private static final int cpuCount = Runtime.getRuntime().availableProcessors();

    static {
        if (constants.printEnvironment.isSet()) {
            logger.info(
                    "Environment. CPU count: {}, Use of parallelism: {}",
                    cpuCount,
                    defaultParallelism.getValue());
        }
    }

    // Specific pools
    private static final Pool highs = new Highs();

    private static final Pool lows = new Lows();

    private static final Pool cachedLows = new CachedLows();

    /** To handle all the pools as a whole. */
    private static final Collection<Pool> allPools = Arrays.asList(cachedLows, lows, highs);

    /** To prevent parallel creation of pools when closing. */
    private static volatile boolean creationAllowed = true;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Not meant to be instantiated.
     */
    private OmrExecutors ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
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
        return cpuCount;
    }

    //---------//
    // restart //
    //---------//
    /**
     * (re-)Allow the creation of pools.
     */
    public static void restart ()
    {
        creationAllowed = true;
        logger.debug("OmrExecutors open");
    }

    //----------//
    // shutdown //
    //----------//
    /**
     * Gracefully shut down all the executors launched
     *
     * @return true if OK, false if timeout
     */
    public static boolean shutdown ()
    {
        boolean result = true;
        logger.debug("Closing all pools ...");

        // No creation of pools from now on!
        creationAllowed = false;

        for (Pool pool : allPools) {
            if (pool.isActive()) {
                if (!pool.close()) {
                    result = false;
                }
            } else {
                logger.debug("Pool {} not active", pool.getName());
            }
        }

        logger.debug("OmrExecutors closed");

        return result;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------//
    // Pool //
    //------//
    private abstract static class Pool
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** The underlying pool of threads. */
        protected ExecutorService pool;

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Name the pool.
         */
        public abstract String getName ();

        /**
         * Terminate the pool.
         * <p>
         * BEWARE, doc on shutdownNow says: There are no guarantees beyond best-effort attempts to
         * stop processing actively executing tasks. For example, typical implementations will
         * cancel via {@link Thread#interrupt}, so any task that fails to respond to interrupts may
         * never terminate.
         *
         * @return true if OK, false if timed out
         */
        public synchronized boolean close ()
        {
            boolean result = true;

            if (!isActive()) {
                return result;
            }

            logger.debug("Closing pool {}", getName());
            pool.shutdown(); // Disable new tasks from being submitted

            try {
                // Wait a while for existing tasks to terminate
                if (!pool.awaitTermination(constants.graceDelay.getValue(), TimeUnit.SECONDS)) {
                    logger.warn("Pool {} did not terminate", getName());
                    result = false;

                    // (Try to) cancel currently executing tasks.
                    pool.shutdownNow();
                }
            } catch (InterruptedException ie) {
                // (Re-)Try to cancel if current thread also got interrupted
                pool.shutdownNow();

                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }

            logger.debug("Pool {} closed.", getName());

            // Let garbage collector work
            pool = null;

            return result;
        }

        /**
         * Get the pool ready to use.
         */
        public synchronized ExecutorService getPool ()
        {
            if (!creationAllowed) {
                logger.info("No longer allowed to create pool: {}", getName());

                throw new ProcessingCancellationException("Executor closed");
            }

            if (!isActive()) {
                logger.debug("Creating pool: {}", getName());
                pool = createPool();
            }

            return pool;
        }

        /**
         * Is the pool active?.
         */
        public synchronized boolean isActive ()
        {
            return (pool != null) && !pool.isShutdown();
        }

        /**
         * Needed to create the concrete pool.
         */
        protected abstract ExecutorService createPool ();
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean printEnvironment = new Constant.Boolean(
                false,
                "Should we print out current environment?");

        private final Constant.Boolean useParallelism = new Constant.Boolean(
                false, //true, // Disabled for the time being
                "Should we use parallelism when we have several processors?");

        private final Constant.Integer graceDelay = new Constant.Integer(
                "seconds",
                60,
                "Time to wait for terminating tasks");
    }

    //------------//
    // CachedLows //
    //------------//
    /** Cached pool with low priority. */
    private static class CachedLows
            extends Pool
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public String getName ()
        {
            return "cachedLow";
        }

        @Override
        protected ExecutorService createPool ()
        {
            return Executors.newCachedThreadPool(new Factory(getName(), Thread.MIN_PRIORITY, 0));
        }
    }

    //---------//
    // Default //
    //---------//
    private static class Default
            extends Param<Boolean>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public Boolean getSpecific ()
        {
            if (constants.useParallelism.isSourceValue()) {
                return null;
            } else {
                return constants.useParallelism.getValue();
            }
        }

        @Override
        public Boolean getValue ()
        {
            return constants.useParallelism.getValue();
        }

        @Override
        public boolean isSpecific ()
        {
            return !constants.useParallelism.isSourceValue();
        }

        @Override
        public boolean setSpecific (Boolean specific)
        {
            if (!getValue().equals(specific)) {
                constants.useParallelism.setValue(specific);
                logger.info("Parallelism is {} allowed", specific ? "now" : "no longer");

                return true;
            } else {
                return false;
            }
        }
    }

    //---------//
    // Factory //
    //---------//
    private static class Factory
            implements ThreadFactory
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final ThreadGroup group;

        private final String threadPrefix;

        private final int threadPriority;

        private final long stackSize;

        private final AtomicInteger threadNumber = new AtomicInteger(0);

        //~ Constructors ---------------------------------------------------------------------------
        Factory (String threadPrefix,
                 int threadPriority,
                 long stackSize)
        {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            this.threadPrefix = threadPrefix;
            this.threadPriority = threadPriority;
            this.stackSize = stackSize;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
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
    /** Fixed pool with high priority. */
    private static class Highs
            extends Pool
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public String getName ()
        {
            return "high";
        }

        @Override
        protected ExecutorService createPool ()
        {
            return Executors.newFixedThreadPool(
                    defaultParallelism.getValue() ? (cpuCount + 1) : 1,
                    new Factory(getName(), Thread.NORM_PRIORITY, 0));
        }
    }

    //------//
    // Lows //
    //------//
    /** Fixed pool with low priority. */
    private static class Lows
            extends Pool
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public String getName ()
        {
            return "low";
        }

        @Override
        protected ExecutorService createPool ()
        {
            return Executors.newFixedThreadPool(
                    defaultParallelism.getValue() ? (cpuCount + 1) : 1,
                    new Factory(getName(), Thread.MIN_PRIORITY, 0));
        }
    }
}
