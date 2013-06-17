//----------------------------------------------------------------------------//
//                                                                            //
//                                W o r k e r                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code Worker} is a simple way to delegate processing to a
 * worker thread, and synchronize at the end of the work.
 *
 * This is actually derived from the standard SwingWorker class, with the Swing
 * part removed. The worker thread is always assigned the minimum priority.
 *
 * <p>Usage:<ol>
 * <li>To get a concrete Worker, you only have to subclass this abstract class
 * with a concrete definition of the construct() method.</li>
 * <li>Then use start() to actually start the Worker, and use get() to [wait for
 * completion if needed and] retrieve the result of the work if any.</li>
 * <li>The interrupt() method allows to interrupt the worker.</li>
 * </ol>
 *
 * <p>Nota: This class is now <b>deprecated</b> in favor of the use of
 * {@link omr.util.OmrExecutors#getCachedLowExecutor()} which provides similar
 * functionality plus has its lifecycle managed correctly when shutting down the
 * application.</p>
 *
 * @param <T> the type used to convey the result of the work
 *
 * @author Hervé Bitteur
 */
@Deprecated
public abstract class Worker<T>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    protected static final Logger logger = LoggerFactory.getLogger(
            Worker.class);

    //~ Instance fields --------------------------------------------------------
    /** The work result, accessed only via getValue() and setValue() */
    private T value;

    /** Intermediate thread holder */
    private ThreadVar threadVar;

    /** Start time */
    private long startTime = System.currentTimeMillis();

    //~ Constructors -----------------------------------------------------------
    //--------//
    // Worker //
    //--------//
    /**
     * Prepare a Worker with a specified stack size
     *
     * @param stackSize the worker stack size, specified in bytes
     */
    public Worker (long stackSize)
    {
        Runnable doConstruct = new Runnable()
        {
            @Override
            public void run ()
            {
                try {
                    setValue(construct());
                } finally {
                    threadVar.clear();
                    logger.debug(
                            "{} finished after {} ms",
                            Worker.this.getClass().getName(),
                            System.currentTimeMillis() - startTime);
                }
            }
        };

        Thread t = new Thread(null, doConstruct, "Worker", stackSize);
        t.setPriority(Thread.MIN_PRIORITY);
        threadVar = new ThreadVar(t);
        logger.debug("{} created", getClass().getName());
    }

    //--------//
    // Worker //
    //--------//
    /**
     * Prepare a thread that will call the {@code construct} method
     * and then exit. You need to start() this worker.
     * The worker will use the default stakc size.
     */
    public Worker ()
    {
        // Use default stack size
        this(0L);
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // construct //
    //-----------//
    /**
     * Compute the value to be returned by the {@code get} method.
     *
     * @return the work result, if any
     */
    public abstract T construct ();

    //-----//
    // get //
    //-----//
    /**
     * Return the value created by the {@code construct} method.
     * Returns null if either the constructing thread or the current
     * thread was interrupted before a value was produced.
     *
     * @return the value created by the {@code construct} method
     */
    public T get ()
    {
        while (true) {
            Thread t = threadVar.get();

            if (t == null) {
                return getValue();
            }

            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread()
                        .interrupt(); // propagate
                logger.debug("{} interrupted", getClass().getName());

                return null;
            }
        }
    }

    //-----------//
    // interrupt //
    //-----------//
    /**
     * Interrupt the worker thread. Call this method to force the worker to stop
     * what it's doing.
     */
    public void interrupt ()
    {
        logger.debug("{} interrupt", getClass().getName());

        Thread t = threadVar.get();

        if (t != null) {
            t.interrupt();
        }

        threadVar.clear();
    }

    //-------//
    // start //
    //-------//
    /**
     * Start the worker thread.
     */
    public void start ()
    {
        Thread t = threadVar.get();

        if (t != null) {
            t.start();
            logger.debug("{} started", getClass().getName());
        }
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Get the value produced by the worker thread, or null if it hasn't been
     * constructed yet.
     *
     * @return the work result
     */
    protected synchronized T getValue ()
    {
        return value;
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Set the value produced by the worker thread
     */
    private synchronized void setValue (T value)
    {
        this.value = value;
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // ThreadVar //
    //-----------//
    /**
     * Class to maintain reference to current worker thread under separate
     * synchronization control.
     */
    private static class ThreadVar
    {
        //~ Instance fields ----------------------------------------------------

        private Thread thread;

        //~ Constructors -------------------------------------------------------
        ThreadVar (Thread t)
        {
            thread = t;
        }

        //~ Methods ------------------------------------------------------------
        synchronized void clear ()
        {
            thread = null;
        }

        synchronized Thread get ()
        {
            return thread;
        }
    }
}
