//----------------------------------------------------------------------------//
//                                                                            //
//                             S h e e t T a s k                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.step;

import omr.sheet.Sheet;

import omr.util.Logger;

/**
 * Class <code>SheetTask</code> defines the task for a step at the whole
 * sheet level. This is meant for steps where the systems have not yet been
 * retrieved.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public abstract class SheetTask
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SheetTask.class);

    //~ Instance fields --------------------------------------------------------

    /** The related sheet */
    protected final Sheet sheet;

    /** The related step for this task */
    protected final Step step;

    /** Flag to indicate the task has been done (actually launched) */
    protected volatile boolean stepDone;

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // SheetTask //
    //-----------//
    /**
     * Creates a task at sheet level
     * @param sheet the processed sheet
     * @param step the step performed by the task
     */
    protected SheetTask (Sheet sheet,
                         Step  step)
    {
        this.sheet = sheet;
        this.step = step;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // displayUI //
    //-----------//
    /**
     * Make the related user interface visible for this step
     */
    public void displayUI ()
    {
        // Void by default
    }

    //------//
    // doit //
    //------//
    /**
     * Actually perform the step
     * @throws StepException raised if processing failed
     */
    public abstract void doit ()
        throws StepException;

    //--------//
    // isDone //
    //--------//
    /**
     * Check whether this task has been done
     * @return true if started/done, false otherwise
     */
    public boolean isDone ()
    {
        return stepDone;
    }

    //-----------//
    // getResult //
    //-----------//
    /**
     * Make sure this step has been run (at least started)
     * @throws StepException raised if processing failed
     */
    public void getResult ()
        throws StepException
    {
        if (!isDone()) {
            if (logger.isFineEnabled()) {
                logger.fine(step + " doit ...");
            }

            doit();

            if (logger.isFineEnabled()) {
                logger.fine(step + " doit end");
            }
        }
    }

    //------//
    // done //
    //------//
    /**
     * Flag this step as done
     */
    public void done ()
    {
        if (logger.isFineEnabled()) {
            logger.fine(step + " done");
        }

        stepDone = true;
    }
}
