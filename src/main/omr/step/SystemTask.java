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

import omr.log.Logger;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

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

    //------//
    // doit //
    //------//
    /**
     * Actually perform the step.
     * This method is run when this step is explicitly selected
     * @param systems systems to process (null means all systems)
     * @throws StepException raised if processing failed
     */
    public void doit (Collection<SystemInfo> systems)
        throws StepException
    {
        // Preliminary actions
        doProlog(systems);

        // Processing system per system
        doitPerSystem(systems);

        // Final actions
        doEpilog(systems);
    }

    //----------//
    // doEpilog //
    //----------//
    /**
     * Final processing for this step, once all systems have been processed
     * @throws StepException raised if processing failed
     */
    protected void doEpilog (Collection<SystemInfo> systems)
        throws StepException
    {
        // Empty by default
    }

    //----------//
    // doProlog //
    //----------//
    /**
     * Do preliminary common work before all systems processings are launched in
     * parallel
     */
    protected void doProlog (Collection<SystemInfo> systems)
        throws StepException
    {
        // Empty by default
    }

    //---------------//
    // doitPerSystem //
    //---------------//
    private void doitPerSystem (Collection<SystemInfo> systems)
    {
        try {
            Collection<Callable<Void>> tasks = new ArrayList<Callable<Void>>();

            if (systems == null) {
                systems = sheet.getSystems();
            }

            for (SystemInfo info : systems) {
                final SystemInfo system = info;
                tasks.add(
                    new Callable<Void>() {
                            public Void call ()
                                throws Exception
                            {
                                try {
                                    if (logger.isFineEnabled()) {
                                        logger.fine(
                                            step + " doSystem #" +
                                            system.getId());
                                    }

                                    doSystem(system);
                                } catch (Exception ex) {
                                    logger.warning(
                                        "Interrupt on " + system,
                                        ex);
                                }

                                return null;
                            }
                        });
            }

            // Launch all system tasks in parallel and wait for their completion
            OmrExecutors.getLowExecutor()
                        .invokeAll(tasks);
        } catch (InterruptedException ex) {
            logger.warning("doitPerSystem got interrupted", ex);
        }
    }
}
