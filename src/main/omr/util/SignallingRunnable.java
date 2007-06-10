//----------------------------------------------------------------------------//
//                                                                            //
//                    S i g n a l l i n g R u n n a b l e                     //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.util;

import java.util.concurrent.CountDownLatch;

/**
 * Class <code>SignallingRunnable</code> is a wrapper around a given
 * Runnable, which is meant to decrement a provided latch, to help handle
 * the completion of several tasks.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SignallingRunnable
    implements Runnable
{
    //~ Instance fields --------------------------------------------------------

    /** The latch to decrement on end of processing */
    private final CountDownLatch doneSignal;

    /** The actual task to perform */
    private final Runnable task;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new instance of SignallingRunnable
     *
     * @param doneSignal the latch to decrement when processing is done
     * @param task the actual task to perform
     */
    public SignallingRunnable (CountDownLatch doneSignal,
                               Runnable       task)
    {
        this.doneSignal = doneSignal;
        this.task = task;
    }

    //~ Methods ----------------------------------------------------------------

    //-----//
    // run //
    //-----//
    /**
     * The actual processing, which decrements the latch when work is over
     */
    @Implement(Runnable.class)
    public void run ()
    {
        try {
            task.run();
        } finally {
            doneSignal.countDown();
        }
    }
}
