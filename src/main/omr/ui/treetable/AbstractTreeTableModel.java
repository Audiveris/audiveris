//----------------------------------------------------------------------------//
//                                                                            //
//                A b s t r a c t T r e e T a b l e M o d e l                 //
//                                                                            //
//----------------------------------------------------------------------------//
package omr.ui.treetable;


/*
 * @(#)AbstractTreeTableModel.java 1.2 98/10/27
 *
 * Copyright 1997, 1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of Sun
 * Microsystems, Inc. ("Confidential Information"). You shall not disclose
 * such Confidential Information and shall use it only in accordance with the
 * terms of the license agreement you entered into with Sun.
 */
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

/**
 * @author Philip Milne
 * @version 1.2 10/27/98 An abstract implementation of the TreeTableModel
 * interface, handling the list of listeners.
 */
public abstract class AbstractTreeTableModel
        implements TreeTableModel
{
    //~ Instance fields --------------------------------------------------------

    /**
     * DOCUMENT ME!
     */
    protected EventListenerList listenerList = new EventListenerList();

    /**
     * DOCUMENT ME!
     */
    protected Object root;

    //~ Constructors -----------------------------------------------------------
    //------------------------//
    // AbstractTreeTableModel //
    //------------------------//
    /**
     * Creates a new AbstractTreeTableModel object.
     *
     * @param root DOCUMENT ME!
     */
    public AbstractTreeTableModel (Object root)
    {
        this.root = root;
    }

    //~ Methods ----------------------------------------------------------------
    //----------------------//
    // addTreeModelListener //
    //----------------------//
    /**
     * DOCUMENT ME!
     *
     * @param l DOCUMENT ME!
     */
    @Override
    public void addTreeModelListener (TreeModelListener l)
    {
        listenerList.add(TreeModelListener.class, l);
    }

    //----------------//
    // getColumnClass //
    //----------------//
    /**
     * DOCUMENT ME!
     *
     * @param column DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    @Override
    public Class<?> getColumnClass (int column)
    {
        return Object.class;
    }

    //-----------------//
    // getIndexOfChild //
    //-----------------//
    // This is not called in the JTree's default mode: use a naive implementation.
    /**
     * DOCUMENT ME!
     *
     * @param parent DOCUMENT ME!
     * @param child  DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    @Override
    public int getIndexOfChild (Object parent,
                                Object child)
    {
        for (int i = 0; i < getChildCount(parent); i++) {
            if (getChild(parent, i)
                    .equals(child)) {
                return i;
            }
        }

        return -1;
    }

    //---------//
    // getRoot //
    //---------//
    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    @Override
    public Object getRoot ()
    {
        return root;
    }

    /**
     * By default, make the column with the Tree in it the only editable one.
     * Making this column editable causes the JTable to forward mouse and
     * keyboard events in the Tree column to the underlying JTree.
     */
    @Override
    public boolean isCellEditable (Object node,
                                   int column)
    {
        return getColumnClass(column) == TreeTableModel.class;
    }

    //--------//
    // isLeaf //
    //--------//
    /**
     * DOCUMENT ME!
     *
     * @param node DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    @Override
    public boolean isLeaf (Object node)
    {
        return getChildCount(node) == 0;
    }

    //-------------------------//
    // removeTreeModelListener //
    //-------------------------//
    /**
     * DOCUMENT ME!
     *
     * @param l DOCUMENT ME!
     */
    @Override
    public void removeTreeModelListener (TreeModelListener l)
    {
        listenerList.remove(TreeModelListener.class, l);
    }

    //------------//
    // setValueAt //
    //------------//
    /**
     * DOCUMENT ME!
     *
     * @param aValue DOCUMENT ME!
     * @param node   DOCUMENT ME!
     * @param column DOCUMENT ME!
     */
    @Override
    public void setValueAt (Object aValue,
                            Object node,
                            int column)
    {
    }

    //---------------------//
    // valueForPathChanged //
    //---------------------//
    /**
     * DOCUMENT ME!
     *
     * @param path     DOCUMENT ME!
     * @param newValue DOCUMENT ME!
     */
    @Override
    public void valueForPathChanged (TreePath path,
                                     Object newValue)
    {
    }

    /*
     * Notify all listeners that have registered interest for
     * notification on this event type. The event instance
     * is lazily created using the parameters passed into
     * the fire method.
     * @see EventListenerList
     */
    protected void fireTreeNodesChanged (Object source,
                                         Object[] path,
                                         int[] childIndices,
                                         Object[] children)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;

        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TreeModelListener.class) {
                // Lazily create the event:
                if (e == null) {
                    e = new TreeModelEvent(
                            source,
                            path,
                            childIndices,
                            children);
                }

                ((TreeModelListener) listeners[i + 1]).treeNodesChanged(e);
            }
        }
    }

    /*
     * Notify all listeners that have registered interest for
     * notification on this event type. The event instance
     * is lazily created using the parameters passed into
     * the fire method.
     * @see EventListenerList
     */
    protected void fireTreeNodesInserted (Object source,
                                          Object[] path,
                                          int[] childIndices,
                                          Object[] children)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;

        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TreeModelListener.class) {
                // Lazily create the event:
                if (e == null) {
                    e = new TreeModelEvent(
                            source,
                            path,
                            childIndices,
                            children);
                }

                ((TreeModelListener) listeners[i + 1]).treeNodesInserted(e);
            }
        }
    }

    /*
     * Notify all listeners that have registered interest for
     * notification on this event type. The event instance
     * is lazily created using the parameters passed into
     * the fire method.
     * @see EventListenerList
     */
    protected void fireTreeNodesRemoved (Object source,
                                         Object[] path,
                                         int[] childIndices,
                                         Object[] children)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;

        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TreeModelListener.class) {
                // Lazily create the event:
                if (e == null) {
                    e = new TreeModelEvent(
                            source,
                            path,
                            childIndices,
                            children);
                }

                ((TreeModelListener) listeners[i + 1]).treeNodesRemoved(e);
            }
        }
    }

    /*
     * Notify all listeners that have registered interest for
     * notification on this event type. The event instance
     * is lazily created using the parameters passed into
     * the fire method.
     * @see EventListenerList
     */
    protected void fireTreeStructureChanged (Object source,
                                             Object[] path,
                                             int[] childIndices,
                                             Object[] children)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        TreeModelEvent e = null;

        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TreeModelListener.class) {
                // Lazily create the event:
                if (e == null) {
                    e = new TreeModelEvent(
                            source,
                            path,
                            childIndices,
                            children);
                }

                ((TreeModelListener) listeners[i + 1]).treeStructureChanged(e);
            }
        }
    }
    // Left to be implemented in the subclass:

    /*
     * public Object getChild(Object parent, int index)
     * public int getChildCount(Object parent)
     * public int getColumnCount()
     * public String getColumnName(Object node, int column)
     * public Object getValueAt(Object node, int column)
     */
}
