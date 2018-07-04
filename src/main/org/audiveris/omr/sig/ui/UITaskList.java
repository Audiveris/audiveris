//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       U I T a s k L i s t                                      //
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

import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.relation.Relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

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

    //~ Enumerations -------------------------------------------------------------------------------
    /** Possible options. */
    public static enum Option
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** User has validated the choice. */
        VALIDATED,
        /** Measures are to be updated. */
        UPDATE_MEASURES;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Sequence of related actions. */
    private final List<UITask> list = new ArrayList<UITask>();

    /** Options for the actions list. */
    private final Set<Option> options = new HashSet<Option>();

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
    //-----//
    // add //
    //-----//
    /**
     * Add a task to the task sequence
     *
     * @param task related task to add
     */
    public void add (UITask task)
    {
        list.add(task);
    }

    //-----------//
    // getInters //
    //-----------//
    /**
     * Report the list of Inters found in seq, that match the provided classes if any.
     *
     * @param classes provided inter classes
     * @return the matching inters in seq
     */
    public List<Inter> getInters (Class... classes)
    {
        List<Inter> found = new ArrayList<Inter>();

        for (UITask task : list) {
            if (task instanceof InterTask) {
                final Inter inter = ((InterTask) task).getInter();

                if (classes.length == 0) {
                    found.add(inter);
                } else {
                    final Class interClass = inter.getClass();

                    for (Class cl : classes) {
                        if (cl.isAssignableFrom(interClass)) {
                            found.add(inter);
                        }
                    }
                }
            }
        }

        return found;
    }

    //--------------//
    // getRelations //
    //--------------//
    /**
     * Report the list of Relations found in seq, that match the provided classes if any.
     *
     * @param classes provided relation classes
     * @return the matching relations in seq
     */
    public List<Relation> getRelations (Class... classes)
    {
        List<Relation> found = new ArrayList<Relation>();

        for (UITask task : list) {
            if (task instanceof RelationTask) {
                final Relation relation = ((RelationTask) task).getRelation();

                if (classes.length == 0) {
                    found.add(relation);
                } else {
                    final Class relationClass = relation.getClass();

                    for (Class cl : classes) {
                        if (cl.isAssignableFrom(relationClass)) {
                            found.add(relation);
                        }
                    }
                }
            }
        }

        return found;
    }

    //--------//
    // getSig //
    //--------//
    public SIGraph getSig ()
    {
        for (UITask task : list) {
            return task.getSig();
        }

        return null;
    }

    //----------//
    // getTasks //
    //----------//
    public List<UITask> getTasks ()
    {
        return list;
    }

    //-------------//
    // isOptionSet //
    //-------------//
    public boolean isOptionSet (Option key)
    {
        return options.contains(key);
    }

    //-----------//
    // performDo //
    //-----------//
    public void performDo ()
    {
        logger.debug("  do {}", this);

        for (UITask task : list) {
            task.performDo();
        }
    }

    //-------------//
    // performUndo //
    //-------------//
    public void performUndo ()
    {
        logger.debug("undo {}", this);

        // Perform Undo in reverse list order
        for (ListIterator<UITask> it = list.listIterator(list.size()); it.hasPrevious();) {
            UITask task = it.previous();
            task.performUndo();
        }
    }

    //------------//
    // setOptions //
    //------------//
    public void setOptions (Option... keys)
    {
        options.addAll(Arrays.asList(keys));
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("seq[");

        for (UITask task : list) {
            sb.append("\n   ").append(task);
        }

        sb.append("]");

        return sb.toString();
    }

    //--------------//
    // unsetOptions //
    //--------------//
    public void unsetOptions (Option... keys)
    {
        options.removeAll(Arrays.asList(keys));
    }
}
