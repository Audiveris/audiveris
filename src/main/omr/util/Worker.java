//----------------------------------------------------------------------------//
//                                                                            //
//                                W o r k e r                                 //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.util;

import omr.log.Logger;

/**
 * Class <code>Worker</code> is a simple way to delegate processing to a worker
 * thread, and synchronize at the end of the work. This is actually derived from
 * the standard SwingWorker class, with the Swing part removed.
 *
 * <p>Usage:<ol>
 * <li>To get a concrete Worker, you only have to subclass this abstract class
 * with a concrete definition of the construct() method.</li>
 * <li>Then use start() to actually start the Worker, and use get() to [wait for
 * completion if needed and] retrieve the result of the work if any.</li>
 * <li>The interrupt() method allows to interrupt the worker.</li>
 * </ol>
 *
 * @param <T> the type used to convey the result of the work
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public abstract class Worker<T>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    protected static final Logger logger = Logger.getLogger(Worker.class);

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
     * Prepare a thread that will call the <code>construct</code> method
     * and then exit. You need to start() this worker.
     */
    public Worker ()
    {
        Runnable doConstruct = new Runnable() {
            public void run ()
            {
                try {
                    setValue(construct());
                } finally {
                    threadVar.clear();

                    if (logger.isFineEnabled()) {
                        logger.fine(
                            Worker.this.getClass().getName() +
                            " finished after " +
                            (System.currentTimeMillis() - startTime) + " ms");
                    }
                }
            }
        };

        Thread t = new Thread(doConstruct);
        threadVar = new ThreadVar(t);

        if (logger.isFineEnabled()) {
            logger.fine(getClass().getName() + " created");
        }
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // construct //
    //-----------//
    /**
     * Compute the value to be returned by the <code>get</code> method.
     * @return the work result, if any
     */
    public abstract T construct ();

    //-----//
    // get //
    //-----//
    /**
     * Return the value created by the <code>construct</code> method.
     * Returns null if either the constructing thread or the current
     * thread was interrupted before a value was produced.
     *
     * @return the value created by the <code>construct</code> method
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

                if (logger.isFineEnabled()) {
                    logger.fine(getClass().getName() + " interrupted");
                }

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
        if (logger.isFineEnabled()) {
            logger.fine(getClass().getName() + " interrupt");
        }

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

            if (logger.isFineEnabled()) {
                logger.fine(getClass().getName() + " started");
            }
        }
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Get the value produced by the worker thread, or null if it hasn't been
     * constructed yet.
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
