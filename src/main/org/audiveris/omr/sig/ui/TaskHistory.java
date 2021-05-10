//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      T a s k H i s t o r y                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
    private final List<UITaskList> sequences = new ArrayList<>();

    /** Current position in history, always pointing to sequence just done. */
    private int cursor = -1;

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Register an action sequence.
     *
     * @param seq one (or several related) task(s)
     * @return the action sequence
     */
    public synchronized UITaskList add (UITaskList seq)
    {
        sequences.add(cursor + 1, seq);
        cursor++;

        // Delete trailing sequences if any
        for (int i = cursor + 1; i < sequences.size(); i++) {
            sequences.remove(i);
        }

        logger.debug("TaskHistory seqs:{} cursor:{} added {}", sequences.size(), cursor, seq);

        return seq;
    }

    /**
     * Tell if a redo is possible.
     *
     * @return true if OK
     */
    public synchronized boolean canRedo ()
    {
        return cursor < (sequences.size() - 1);
    }

    /**
     * Tell if an undo is possible.
     *
     * @return true if OK
     */
    public synchronized boolean canUndo ()
    {
        return cursor >= 0;
    }

    /**
     * Clear history.
     */
    public synchronized void clear ()
    {
        sequences.clear();
        cursor = -1;
    }

    /**
     * Report the cancelled action sequence.
     *
     * @return the task sequence to redo
     */
    public synchronized UITaskList toRedo ()
    {
        if (cursor < (sequences.size() - 1)) {
            UITaskList seq = sequences.get(cursor + 1);
            cursor++;

            logger.debug("TaskHistory seqs:{} cursor:{} redo {}", sequences.size(), cursor, seq);

            return seq;
        } else {
            return null;
        }
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
    public synchronized UITaskList toUndo ()
    {
        if (cursor >= 0) {
            UITaskList seq = sequences.get(cursor);
            cursor--;

            logger.debug("TaskHistory seqs:{} cursor:{} undo {}", sequences.size(), cursor, seq);

            return seq;
        } else {
            return null;
        }
    }
}
