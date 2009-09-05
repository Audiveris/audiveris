//----------------------------------------------------------------------------//
//                                                                            //
//                            S c r i p t T a s k                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.script;

import omr.log.Logger;

import omr.sheet.Sheet;

import omr.step.StepException;

/**
 * Class <code>Task</code> is meant for all possible tasks within a score script
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public abstract class ScriptTask
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    protected static final Logger logger = Logger.getLogger(ScriptTask.class);

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new Task object.
     */
    protected ScriptTask ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // prolog //
    //--------//
    /**
     * Prolog if any, to be called before the run() method
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
     * Actually run this task
     *
     * @param sheet the sheet to run this task against
     * @exception Exception
     */
    public abstract void run (Sheet sheet)
        throws Exception;

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Task ");
        sb.append(internalsString());
        sb.append("}");

        return sb.toString();
    }

    //-----------------//
    // internalsString //
    //-----------------//
    /**
     * Return the string of the internals of this class, for inclusion in a
     * toString
     *
     * @return the string of internals
     */
    protected abstract String internalsString ();
}
