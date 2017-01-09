//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S c r i p t T a s k                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.script;

import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.util.VoidTask;

import org.jdesktop.application.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code ScriptTask} is the root class of all possible tasks within a book script.
 * <p>
 * The processing of any task is defined by its {@link #core} method. In order to factorize pre- and
 * post- processing, a subclass may also override the {@link #prolog} and {@link #epilog} methods
 * respectively.</p>
 * <p>
 * The whole processing of a task is run synchronously by the {@link #run} method, and this is what
 * the calling {@link Script} engine does. To run a task asynchronously, use the {@link #launch}
 * method, and this is what any UI module should do.</p>
 *
 * @author Hervé Bitteur
 */
public abstract class ScriptTask
{
    //~ Static fields/initializers -----------------------------------------------------------------

    protected static final Logger logger = LoggerFactory.getLogger(ScriptTask.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new ScriptTask object.
     */
    protected ScriptTask ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----//
    // run //
    //-----//
    /**
     * Run this task synchronously (prolog + core + epilog).
     * <p>
     * This is meant to be called by the script engine, to ensure that every task is completed
     * before the next is run.
     * This method is final, subclasses should define core() and potentially customize prolog() and
     * epilog().
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
    }

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
     * This is meant to be called by UI code, for maximum responsiveness of the user interface.
     *
     * @param sheet the sheet to run this task against
     * @return the launched SAF task
     */
    public Task<Void, Void> launch (final Sheet sheet)
    {
        VoidTask task = new VoidTask()
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

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Task");
        sb.append(internals());
        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // internals //
    //-----------//
    /**
     * Return the string of the internals of this class, typically for inclusion
     * in a toString().
     * <p>
     * The overriding methods should comply with the following rule: append either a totally empty
     * string, or a string that begins with a " " followed by some content.
     *
     * @return the string of internals
     */
    protected String internals ()
    {
        return "";
    }
}
