//----------------------------------------------------------------------------//
//                                                                            //
//                             S h e e t T a s k                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Class {@code SheetTask} defines the task for a step at the whole
 * sheet level. This is meant for steps where the systems have not yet been
 * retrieved.
 *
 * @author Hervé Bitteur
 */
public abstract class SheetTask
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            SheetTask.class);

    //~ Instance fields --------------------------------------------------------
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
     *
     * @param step the step performed by the task
     */
    protected SheetTask (Step step)
    {
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

    //--------//
    // doStep //
    //--------//
    /**
     * Run the step
     *
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
    // doit //
    //------//
    /**
     * Actually perform the step
     *
     * @param systems the collection of systems to process
     * @throws StepException raised if processing failed
     */
    public abstract void doit (Collection<SystemInfo> systems)
            throws StepException;

    //------//
    // done //
    //------//
    /**
     * Flag this step as done
     */
    public void done ()
    {
        logger.debug("{} done", step);

        stepDone = true;
    }

    //--------//
    // isDone //
    //--------//
    /**
     * Check whether this task has been done
     *
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
     *
     * @return true if started, false otherwise
     */
    public boolean isStarted ()
    {
        return stepStarted;
    }

    //---------//
    // started //
    //---------//
    /**
     * Flag this step as started
     */
    public void started ()
    {
        logger.debug("{} started ....................................", step);

        stepStarted = true;
    }
}
