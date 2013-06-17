//----------------------------------------------------------------------------//
//                                                                            //
//                        T r e e T a b l e M o d e l                         //
//                                                                            //
//----------------------------------------------------------------------------//
//      $Id$
package omr.ui.treetable;


/*
 * TreeTableModel.java
 *
 * Copyright (c) 1998 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of Sun
 * Microsystems, Inc. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Sun.
 *
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
 * SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 */
import javax.swing.tree.TreeModel;

/**
 * TreeTableModel is the model used by a JTreeTable. It extends TreeModel to
 * add methods for getting information about the set of columns each node in
 * the TreeTableModel may have. Each column, like a column in a TableModel,
 * has a name and a type associated with it. Each node in the TreeTableModel
 * can return a value for each of the columns and set that value if
 * isCellEditable() returns true.
 *
 * @author Philip Milne
 * @author Scott Violet
 */
public interface TreeTableModel
        extends TreeModel
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Returns the type for column number {@code column}.
     */
    public Class<?> getColumnClass (int column);

    /**
     * Returns the number ofs availible column.
     */
    public int getColumnCount ();

    /**
     * Returns the name for column number {@code column}.
     */
    public String getColumnName (int column);

    /**
     * Returns the value to be displayed for node {@code node}, at column
     * number {@code column}.
     */
    public Object getValueAt (Object node,
                              int column);

    /**
     * Indicates whether the the value for node {@code node}, at column
     * number {@code column} is editable.
     */
    public boolean isCellEditable (Object node,
                                   int column);

    /**
     * Sets the value for node {@code node}, at column number
     * {@code column}.
     */
    public void setValueAt (Object aValue,
                            Object node,
                            int column);
}
