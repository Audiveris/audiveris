//----------------------------------------------------------------------------//
//                                                                            //
//                            S y s t e m T a s k                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.step;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.Logger;
import omr.util.OmrExecutors;

import java.util.*;
import java.util.concurrent.*;

/**
 * Class <code>SystemTask</code> defines the task for a step at system level for
 * a given sheet
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public abstract class SystemTask
    extends SheetTask
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SystemTask.class);

    //~ Instance fields --------------------------------------------------------

    /** Flag which systems have been processed by this step */
    private final SortedMap<SystemInfo, Boolean> systemDone;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // SystemTask //
    //------------//
    /**
     * Create a system-based task for a given sheet
     * @param sheet the related sheet
     * @param step the step that governs this task
     */
    protected SystemTask (Sheet sheet,
                          Step  step)
    {
        super(sheet, step);
        systemDone = new TreeMap<SystemInfo, Boolean>();
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // doSystem //
    //----------//
    /**
     * Actually perform the step on the given system
     * @param system the system on which the step must be performed
     * @throws StepException raised if processing failed
     */
    public abstract void doSystem (SystemInfo system)
        throws StepException;

    //--------//
    // isDone //
    //--------//
    /**
     * Check whether processing for this system has been done/started
     * @param system the provided system
     * @return true if done/started, false otherwise
     */
    public synchronized boolean isDone (SystemInfo system)
    {
        Boolean result = systemDone.get(system);

        return (result != null) && result;
    }

    //-----------//
    // getResult //
    //-----------//
    /**
     * Make sure this step has been run for the given system
     * @param system the system for which processing may be required
     * @throws StepException raised if processing failed
     */
    public void getResult (SystemInfo system)
        throws StepException
    {
        if (!isDone(system)) {
            doSystem(system);
        }
    }

    //---------//
    // doFinal //
    //---------//
    /**
     * Final processing for this step, once all systems have been processed
     * @throws StepException raised if processing failed
     */
    public void doFinal ()
        throws StepException
    {
        // Empty by default
    }

    //------//
    // doit //
    //------//
    /**
     * Actually perform the step.
     * This method is run when this step is explicitly selected
     * @throws StepException raised if processing failed
     */
    public void doit ()
        throws StepException
    {
        if (logger.isFineEnabled()) {
            logger.fine(step + " SystemTask doit ...");
        }

        // Processing per system
        if (OmrExecutors.useParallelism() &&
            (OmrExecutors.getNumberOfCpus() > 1)) {
            doitParallel();
        } else {
            doitSerial();
        }

        // Final actions
        doFinal();

        if (logger.isFineEnabled()) {
            logger.fine(step + " SystemTask doit end");
        }
    }

    //------//
    // done //
    //------//
    /**
     * Flag this system as done
     * @param system the provided system
     */
    public synchronized void done (SystemInfo system)
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                step + " done system #" + system.getScoreSystem().getId());
        }

        done();
        systemDone.put(system, Boolean.valueOf(true));
    }

    //--------------//
    // doitParallel //
    //--------------//
    private void doitParallel ()
    {
        try {
            Collection<Callable<Void>> tasks = new ArrayList<Callable<Void>>(
                sheet.getSystems().size());

            for (SystemInfo info : sheet.getSystems()) {
                final SystemInfo system = info;
                tasks.add(
                    new Callable<Void>() {
                            public Void call ()
                                throws Exception
                            {
                                try {
                                    doSystem(system);
                                } catch (StepException ex) {
                                    logger.warning(
                                        "Step aborted on system",
                                        ex);
                                }

                                return null;
                            }
                        });
            }

            OmrExecutors.getLowExecutor()
                        .invokeAll(tasks);
        } catch (InterruptedException ex) {
            logger.warning("doitParallel got interrupted", ex);
        }
    }

    //------------//
    // doitSerial //
    //------------//
    private void doitSerial ()
        throws StepException
    {
        for (SystemInfo system : sheet.getSystems()) {
            if (logger.isFineEnabled()) {
                logger.info(
                    this.getClass().getSimpleName() + " doSystem #" +
                    system.getId());
            }

            doSystem(system);
        }
    }
}
