//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      T a s k H i s t o r y                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code TaskHistory} handles a history of UITaskList instances, with the
 * ability to add, undo and redo.
 * <p>
 * Within an UITaskList, all tasks are handled as a whole, to cope with dependent tasks.
 *
 * @author Hervé Bitteur
 */
class TaskHistory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(TaskHistory.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** History of action sequences. */
    private final List<UITaskList> sequences = new ArrayList<UITaskList>();

    /** Current position in history, always pointing to sequence just done. */
    private int cursor = -1;

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Register an action sequence.
     *
     * @param tasks one (or several related) task(s)
     * @return the action sequence
     */
    public UITaskList add (UITaskList seq)
    {
        sequences.add(cursor + 1, seq);
        cursor++;

        // Delete trailing sequences if any
        for (int i = cursor + 1; i < sequences.size(); i++) {
            sequences.remove(i);
        }

        return seq;
    }

    /**
     * Tell if a redo is possible.
     *
     * @return true if OK
     */
    public boolean canRedo ()
    {
        return cursor < (sequences.size() - 1);
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
     * Clear history.
     */
    public void clear ()
    {
        sequences.clear();
        cursor = -1;
    }

    /**
     * Report the cancelled action sequence.
     *
     * @return the task sequence to redo
     */
    public UITaskList toRedo ()
    {
        UITaskList seq = sequences.get(cursor + 1);
        cursor++;

        return seq;
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("TaskHistory{");
        sb.append("c:").append(cursor);

        sb.append(" ").append(sequences);

        sb.append("}");

        return sb.toString();
    }

    /**
     * Report the action sequence to cancel.
     *
     * @return the task sequence to undo
     */
    public UITaskList toUndo ()
    {
        UITaskList seq = sequences.get(cursor);
        cursor--;

        return seq;
    }
}
