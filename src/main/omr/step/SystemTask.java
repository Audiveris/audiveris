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

import omr.ProcessingException;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.Logger;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Class <code>SystemTask</code> defines the ask for a step at system level
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

    /** Flag which systems have been done */
    private final SortedMap<SystemInfo, Boolean> systemDone;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // SystemTask //
    //------------//
    SystemTask (Sheet sheet,
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
     * @throws ProcessingException raised if processing failed
     */
    public abstract void doSystem (SystemInfo system)
        throws ProcessingException;

    //---------//
    // doFinal //
    //---------//
    /**
     * Final processing for this step, once all systems have been processed
     * @throws ProcessingException raised if processing failed
     */
    public void doFinal ()
        throws ProcessingException
    {
        // Empty by default
    }

    //------//
    // doit //
    //------//
    /**
     * Actually perform the step.
     * This method is run when this step is explicitly selected
     * @throws ProcessingException raised if processing failed
     */
    public void doit ()
        throws ProcessingException
    {
        logger.fine(step + " SystemTask doit ...");

        // Launch parallel processing per system
        // TBD
        for (SystemInfo system : sheet.getSystems()) {
            // TBD
            done(system);
            doSystem(system);
        }

        // Final latch here
        // TBD
        doFinal();
        logger.fine(step + " SystemTask doit end");
    }

    //------//
    // done //
    //------//
    /**
     * Flag this system as done
     * @param system the provided system
     */
    public void done (SystemInfo system)
    {
        logger.fine(
            step + " done for system #" + system.getScoreSystem().getId());
        done();
        systemDone.put(system, Boolean.valueOf(true));
    }

    //-----------//
    // getResult //
    //-----------//
    /**
     * Make sure this step has been run for the given system
     * @param system the system for which processing may be required
     * @throws ProcessingException raised if processing failed
     */
    public void getResult (SystemInfo system)
        throws ProcessingException
    {
        if (!isDone(system)) {
            doSystem(system);
        }
    }

    //--------//
    // isDone //
    //--------//
    /**
     * Check whether processing for this system has been done/started
     * @param system the provided system
     * @return true if done/started, false otherwise
     */
    public boolean isDone (SystemInfo system)
    {
        Boolean result = systemDone.get(system);

        return (result != null) && result;
    }
}
