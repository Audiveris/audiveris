//----------------------------------------------------------------------------//
//                                                                            //
//                            S c r i p t T a s k                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.sheet.Sheet;

import omr.util.BasicTask;

import org.jdesktop.application.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code ScriptTask} is the root class of all possible tasks
 * within a score script.
 *
 * <p>The processing of any task is defined by its {@link #core} method. In
 * order to factorize pre and post processing, a subclass may also redefine the
 * {@link #prolog} and {@link #epilog} methods respectively.</p>
 *
 * <p>The whole processing of a task is run synchronously by the {@link #run}
 * method, and this is what the calling {@link Script} engine does. To run a
 * task asynchronously, use the {@link #launch} method, and this is what any
 * UI module should do.</p>
 *
 * <p>Running a task has the side-effect of writing this task in the current
 * score script, unless the task is defined as not recordable.</p>
 *
 * @author Hervé Bitteur
 */
public abstract class ScriptTask
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    protected static final Logger logger = LoggerFactory.getLogger(
            ScriptTask.class);

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new ScriptTask object.
     */
    protected ScriptTask ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //------//
    // core //
    //------//
    /**
     * Run the core of this task.
     *
     * @param sheet the sheet to run this task against
     * @exception Exception
     */
    public abstract void core (Sheet sheet)
            throws Exception;

    //--------//
    // epilog //
    //--------//
    /**
     * Epilog if any, to be called after the run() method
     *
     * @param sheet the sheet to run this task against
     */
    public void epilog (Sheet sheet)
    {
        // Empty by default
    }

    //--------//
    // launch //
    //--------//
    /**
     * Launch this task asynchronously (prolog + core + epilog).
     * This is meant to be called by UI code, for maximum responsiveness of the
     * user interface.
     *
     * @param sheet the sheet to run this task against
     * @return the launched SAF task
     */
    public Task<Void, Void> launch (final Sheet sheet)
    {
        Task<Void, Void> task = new BasicTask()
        {
            @Override
            protected Void doInBackground ()
                    throws Exception
            {
                ScriptTask.this.run(sheet);

                return null;
            }
        };

        task.execute();

        return task;
    }

    //--------//
    // prolog //
    //--------//
    /**
     * Prolog if any, to be called before the run() method
     *
     * @param sheet the sheet to run this task against
     */
    public void prolog (Sheet sheet)
    {
        // Empty by default
    }

    //-----//
    // run //
    //-----//
    /**
     * Run this task synchronously (prolog + core + epilog)
     * This is meant to be called by the script engine, to ensure that
     * every task is completed before the next is run.
     * This method is final, subclasses should define core() and potentially
     * customize prolog() and epilog().
     *
     * @param sheet the sheet to run this task against
     * @exception Exception
     */
    public final void run (Sheet sheet)
            throws Exception
    {
        prolog(sheet);
        core(sheet);
        epilog(sheet);

        // Record the task instance in the current script?
        if (isRecordable()) {
            sheet.getScore()
                    .getScript()
                    .addTask(this);
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Task");
        sb.append(internalsString());
        sb.append("}");

        return sb.toString();
    }

    //-----------------//
    // internalsString //
    //-----------------//
    /**
     * Return the string of the internals of this class, typically for inclusion
     * in a toString. The overriding methods should comply with the following
     * rule: return either a totally empty string, or a string that begins with
     * a " " followed by some content.
     *
     * @return the string of internals
     */
    protected String internalsString ()
    {
        return "";
    }

    //--------------//
    // isRecordable //
    //--------------//
    /**
     * Report whether this task should be written in the current script
     *
     * @return true if recordable
     */
    boolean isRecordable ()
    {
        return true;
    }
}
