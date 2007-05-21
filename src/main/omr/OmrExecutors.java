//----------------------------------------------------------------------------//
//                                                                            //
//                          O m r E x e c u t o r s                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class <code>OmrExecutors</code> handles the two pools of threads
 * provided to the omr package, each pool containing a number of threads
 * equal to the machine number of processors plus one.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class OmrExecutors
{
    //~ Static fields/initializers ---------------------------------------------

    /** Number of processors available */
    private static final int cpuNb = Runtime.getRuntime()
                                            .availableProcessors();

    /** Pool with high priority */
    private static Executor highExecutor;

    /** Pool with low priority */
    private static Executor lowExecutor;

    //~ Constructors -----------------------------------------------------------

    /** Not meant to be instantiated */
    private OmrExecutors ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Return the (single) pool of high priority threads
     *
     * @return the high pool, allocated if needed
     */
    public static synchronized Executor getHighExecutor ()
    {
        if (highExecutor == null) {
            highExecutor = Executors.newFixedThreadPool(
                cpuNb + 1,
                new HighFactory());
        }

        return highExecutor;
    }

    /**
     * Return the (single) pool of low priority threads
     *
     * @return the low pool, allocated if needed
     */
    public static synchronized Executor getLowExecutor ()
    {
        if (lowExecutor == null) {
            lowExecutor = Executors.newFixedThreadPool(
                cpuNb + 1,
                new LowFactory());
        }

        return lowExecutor;
    }

    //~ Inner Classes ----------------------------------------------------------

    private abstract static class Factory
        implements ThreadFactory
    {
        protected final ThreadGroup group;

        Factory ()
        {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup()
                    : Thread.currentThread()
                            .getThreadGroup();
        }

        public Thread newThread (Runnable r)
        {
            Thread t = new Thread(group, r, getThreadName(), 0);

            if (t.isDaemon()) {
                t.setDaemon(false);
            }

            return t;
        }

        protected abstract String getThreadName ();
    }

    private static class HighFactory
        extends Factory
    {
        private final AtomicInteger highThreadNumber = new AtomicInteger(1);

        public Thread newThread (Runnable r)
        {
            Thread t = super.newThread(r);

            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }

            return t;
        }

        protected String getThreadName ()
        {
            return "high-thread-" + highThreadNumber.getAndIncrement();
        }
    }

    private static class LowFactory
        extends Factory
    {
        private final AtomicInteger lowThreadNumber = new AtomicInteger(1);

        public Thread newThread (Runnable r)
        {
            Thread t = super.newThread(r);

            if (t.getPriority() != Thread.MIN_PRIORITY) {
                t.setPriority(Thread.MIN_PRIORITY);
            }

            return t;
        }

        protected String getThreadName ()
        {
            return "low-thread-" + lowThreadNumber.getAndIncrement();
        }
    }
}
