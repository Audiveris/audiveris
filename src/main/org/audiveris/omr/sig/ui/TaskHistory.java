//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      T a s k H i s t o r y                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.sig.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code TaskHistory} handles a history of InterTasks, with the ability to add,
 * undo and redo.
 *
 * @author Hervé Bitteur
 */
class TaskHistory
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** History of actions. */
    private final List<InterTask> tasks = new ArrayList<InterTask>();

    /** Current position in history, always pointing to action just done. */
    private int cursor = -1;

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Register an action.
     *
     * @param task the task at hand
     * @return the task to do
     */
    public InterTask add (InterTask task)
    {
        tasks.add(cursor + 1, task);
        cursor++;

        // Delete trailing tasks if any
        for (int i = cursor + 1; i < tasks.size(); i++) {
            tasks.remove(i);
        }

        return task;
    }

    /**
     * Tell if a redo is possible.
     *
     * @return true if OK
     */
    public boolean canRedo ()
    {
        return cursor < (tasks.size() - 1);
    }

    /**
     * Tell if an undo is possible.
     *
     * @return true if OK
     */
    public boolean canUndo ()
    {
        return cursor >= 0;
    }

    /**
     * Redo the cancelled action.
     *
     * @return the task to redo
     */
    public InterTask redo ()
    {
        InterTask task = tasks.get(cursor + 1);
        cursor++;

        return task;
    }

    /**
     * Cancel the previous action.
     *
     * @return the task to undo
     */
    public InterTask undo ()
    {
        InterTask task = tasks.get(cursor);
        cursor--;

        return task;
    }
}
