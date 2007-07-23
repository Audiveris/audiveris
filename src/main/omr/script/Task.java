//----------------------------------------------------------------------------//
//                                                                            //
//                                  T a s k                                   //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.script;

import omr.sheet.Sheet;

import omr.step.StepException;

import omr.util.Logger;

import javax.xml.bind.annotation.*;

/**
 * Class <code>Task</code> is meant for all possible tasks within a score script
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public abstract class Task
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    protected static final Logger logger = Logger.getLogger(Task.class);

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new Task object.
     */
    protected Task ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //-----//
    // run //
    //-----//
    /**
     * Actually run this task
     *
     * @param sheet the sheet to run this task against
     * @exception StepException raised if processing error occurs
     */
    public abstract void run (Sheet sheet)
        throws StepException;

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
     * Return the string of the internals of this class, for inclusion in a
     * toString
     *
     * @return the string of internals
     */
    protected abstract String internalsString ();
}
