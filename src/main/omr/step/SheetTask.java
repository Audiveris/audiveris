//----------------------------------------------------------------------------//
//                                                                            //
//                             S h e e t T a s k                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.log.Logger;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.util.Collection;

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

    /** Flag to indicate the task has started */
    protected volatile boolean stepStarted;

    /** Flag to indicate the task has been done */
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
     * @param systems the collection of systems to process
     * @throws StepException raised if processing failed
     */
    public abstract void doit (Collection<SystemInfo> systems)
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
    // isStarted //
    //-----------//
    /**
     * Check whether this task has started
     * @return true if started, false otherwise
     */
    public boolean isStarted ()
    {
        return stepStarted;
    }

    //--------//
    // doStep //
    //--------//
    /**
     * Run the step
     * @param systems systems to process (null means all systems)
     * @throws StepException raised if processing failed
     */
    public void doStep (Collection<SystemInfo> systems)
        throws StepException
    {
        started();
        doit(systems);
        done();
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

    //---------//
    // started //
    //---------//
    /**
     * Flag this step as started
     */
    public void started ()
    {
        if (logger.isFineEnabled()) {
            logger.fine(step + " started ....................................");
        }

        stepStarted = true;
    }
}
