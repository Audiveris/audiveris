//----------------------------------------------------------------------------//
//                                                                            //
//                 T r e e T a b l e M o d e l A d a p t e r                  //
//                                                                            //
//----------------------------------------------------------------------------//
package omr.ui.treetable;


/*
 * @(#)TreeTableModelAdapter.java 1.2 98/10/27
 *
 * Copyright 1997, 1998 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information"). You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.TreePath;

/**
 * This is a wrapper class takes a TreeTableModel and implements the table
 * model interface. The implementation is trivial, with all of the event
 * dispatching support provided by the superclass: the AbstractTableModel.
 *
 * @author Philip Milne
 * @author Scott Violet
 * @version 1.2 10/27/98
 */
public class TreeTableModelAdapter
        extends AbstractTableModel
{
    //~ Instance fields --------------------------------------------------------

    JTree tree;

    TreeTableModel treeTableModel;

    //~ Constructors -----------------------------------------------------------
    //-----------------------//
    // TreeTableModelAdapter //
    //-----------------------//
    /**
     * Creates a new TreeTableModelAdapter object.
     *
     * @param treeTableModel DOCUMENT ME!
     * @param tree           DOCUMENT ME!
     */
    public TreeTableModelAdapter (TreeTableModel treeTableModel,
                                  JTree tree)
    {
        this.tree = tree;
        this.treeTableModel = treeTableModel;

        tree.addTreeExpansionListener(
                new TreeExpansionListener()
        {
            // Don't use fireTableRowsInserted() here; the selection model
            // would get updated twice.
            @Override
            public void treeExpanded (TreeExpansionEvent event)
            {
                fireTableDataChanged();
            }

            @Override
            public void treeCollapsed (TreeExpansionEvent event)
            {
                fireTableDataChanged();
            }
        });

        // Install a TreeModelListener that can update the table when
        // tree changes. We use delayedFireTableDataChanged as we can
        // not be guaranteed the tree will have finished processing
        // the event before us.
        treeTableModel.addTreeModelListener(
                new TreeModelListener()
        {
            @Override
            public void treeNodesChanged (TreeModelEvent e)
            {
                delayedFireTableDataChanged();
            }

            @Override
            public void treeNodesInserted (TreeModelEvent e)
            {
                delayedFireTableDataChanged();
            }

            @Override
            public void treeNodesRemoved (TreeModelEvent e)
            {
                delayedFireTableDataChanged();
            }

            @Override
            public void treeStructureChanged (TreeModelEvent e)
            {
                delayedFireTableDataChanged();
            }
        });
    }

    //~ Methods ----------------------------------------------------------------
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
        return treeTableModel.getColumnClass(column);
    }

    //----------------//
    // getColumnCount //
    //----------------//
    // Wrappers, implementing TableModel interface.
    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    @Override
    public int getColumnCount ()
    {
        return treeTableModel.getColumnCount();
    }

    //---------------//
    // getColumnName //
    //---------------//
    /**
     * DOCUMENT ME!
     *
     * @param column DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    @Override
    public String getColumnName (int column)
    {
        return treeTableModel.getColumnName(column);
    }

    //-------------//
    // getRowCount //
    //-------------//
    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    @Override
    public int getRowCount ()
    {
        return tree.getRowCount();
    }

    //------------//
    // getValueAt //
    //------------//
    /**
     * DOCUMENT ME!
     *
     * @param row    DOCUMENT ME!
     * @param column DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    @Override
    public Object getValueAt (int row,
                              int column)
    {
        return treeTableModel.getValueAt(nodeForRow(row), column);
    }

    //----------------//
    // isCellEditable //
    //----------------//
    /**
     * DOCUMENT ME!
     *
     * @param row    DOCUMENT ME!
     * @param column DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    @Override
    public boolean isCellEditable (int row,
                                   int column)
    {
        return treeTableModel.isCellEditable(nodeForRow(row), column);
    }

    //------------//
    // nodeForRow //
    //------------//
    /**
     * DOCUMENT ME!
     *
     * @param row DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public Object nodeForRow (int row)
    {
        TreePath treePath = tree.getPathForRow(row);

        return treePath.getLastPathComponent();
    }

    //------------//
    // setValueAt //
    //------------//
    /**
     * DOCUMENT ME!
     *
     * @param value  DOCUMENT ME!
     * @param row    DOCUMENT ME!
     * @param column DOCUMENT ME!
     */
    @Override
    public void setValueAt (Object value,
                            int row,
                            int column)
    {
        treeTableModel.setValueAt(value, nodeForRow(row), column);
    }

    //-----------------------------//
    // delayedFireTableDataChanged //
    //-----------------------------//
    /**
     * Invokes fireTableDataChanged after all the pending events have been
     * processed. SwingUtilities.invokeLater is used to handle this.
     */
    protected void delayedFireTableDataChanged ()
    {
        SwingUtilities.invokeLater(
                new Runnable()
        {
            @Override
            public void run ()
            {
                fireTableDataChanged();
            }
        });
    }
}
