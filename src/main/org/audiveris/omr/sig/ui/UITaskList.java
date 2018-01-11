//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       U I T a s k L i s t                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright ©  Audiveris 2017. All rights reserved.
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
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

/**
 * Class {@code UITaskList} is a sequence of {@link UITask} instances, meant to
 * be handled as a whole.
 *
 * @author Hervé Bitteur
 */
public class UITaskList
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(UITaskList.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Sequence of related actions. */
    private final List<UITask> list = new ArrayList<UITask>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code InterTaskList} object.
     *
     * @param tasks sequence of related tasks
     */
    public UITaskList (UITask... tasks)
    {
        this(Arrays.asList(tasks));
    }

    /**
     * Creates a new {@code InterTaskList} object.
     *
     * @param tasks sequence of related tasks
     */
    public UITaskList (List<UITask> tasks)
    {
        list.addAll(tasks);
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Add a task to the task sequence
     *
     * @param task related task to add
     */
    public void add (UITask task)
    {
        list.add(task);
    }

    public InterTask getFirstInterTask ()
    {
        for (UITask task : getTasks()) {
            if (task instanceof InterTask) {
                return (InterTask) task;
            }
        }

        return null;
    }

    public UITask getFirstTask ()
    {
        return list.get(0);
    }

    public UITask getLastTask ()
    {
        return list.get(list.size() - 1);
    }

    public List<UITask> getTasks ()
    {
        return list;
    }

    public void performDo ()
    {
        logger.info("  do {}", this);

        for (UITask task : list) {
            task.performDo();
        }
    }

    public void performUndo ()
    {
        logger.info("undo {}", this);

        // Perform Undo in reverse list order
        for (ListIterator<UITask> it = list.listIterator(list.size()); it.hasPrevious();) {
            UITask task = it.previous();
            task.performUndo();
        }
    }

    @Override
    public String toString ()
    {
        return list.toString();
    }
}
