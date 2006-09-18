//----------------------------------------------------------------------------//
//                                                                            //
//                         U n i t T r e e T a b l e                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.constant;

import omr.ui.treetable.JTreeTable;

import omr.util.Logger;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreePath;

/**
 * Class <code>UnitTreeTable</code> is a user interface that combines a tree to
 * display the hierarchy of Units, that containt ConstantSets and/or Loggers,
 * and a table to display and edit the various Constants in each ConstantSet as
 * well as the logger level of units.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class UnitTreeTable
    extends JTreeTable
{
    //~ Instance fields --------------------------------------------------------

    private TableCellRenderer loggerRenderer = new LoggerRenderer();
    private TableCellRenderer valueRenderer = new ValueRenderer();

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // UnitTreeTable //
    //---------------//
    /**
     * Create a User Interface JTreeTable dedicated to the handling of unit
     * constants and loggers.
     *
     * @param model the corresponding data model
     */
    public UnitTreeTable (UnitModel model)
    {
        super(model);

        // Specify column widths
        adjustColumns();

        // Customize the tree aspect
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);

        // Pre-expand all package nodes
        preExpandPackages();
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // getCellRenderer //
    //-----------------//
    /**
     * Used by the UI to get the proper renderer for each given cell in the
     * table. For the 'MODIF' column for example, we are able to differentiate
     * rows for Constant (where this Modif flag makes sense) from any other row
     * where Modif is irrelevant and thus left totally blank.
     *
     * @param row row in the table
     * @param col column in the table
     *
     * @return the best renderer for the cell.
     */
    @Override
    public TableCellRenderer getCellRenderer (int row,
                                              int col)
    {
        UnitModel.Column column = UnitModel.Column.values()[col];

        switch (column) {
        case LOGGER :
            return loggerRenderer;

        case VALUE :

            //             Object obj = getModel().getValueAt (row, col);
            //             if (obj instanceof Boolean)
            //                 return getDefaultRenderer(Boolean.class);
            //             else
            return valueRenderer;

        case MODIF :

            TreePath tp = tree.getPathForRow(row);
            Node     node = UnitManager.getInstance()
                                       .getNode(buildKey(tp));

            if (node instanceof Node) {
                return getDefaultRenderer(Object.class);
            } else {
                return getDefaultRenderer(Boolean.class);
            }

        default :
            return getDefaultRenderer(getColumnClass(col));
        }
    }

    //---------------//
    // adjustColumns //
    //---------------//
    /**
     * Allows to adjust the related columnModel, for each and every column
     *
     * @param cModel the proper table column model
     */
    private void adjustColumns ()
    {
        TableColumnModel cModel = getColumnModel();

        // Columns widths
        for (UnitModel.Column c : UnitModel.Column.values()) {
            cModel.getColumn(c.ordinal())
                  .setPreferredWidth(c.width);
        }
    }

    //----------//
    // buildKey //
    //----------//
    /**
     * Given a TreePath, concatenate the various elements to form a key
     * string.
     *
     * @param tp the provided TreePath
     *
     * @return the key string ready to use
     */
    private static String buildKey (TreePath tp)
    {
        if (tp == null) {
            return "";
        }

        StringBuffer sb = new StringBuffer(128);
        int          count = tp.getPathCount();

        for (int i = 1; i < count; i++) { // Start from 1, since 0 = <root>

            if (i > 1) {
                sb.append(".");
            }

            sb.append(tp.getPathComponent(i).toString());
        }

        return sb.toString();
    }

    //-------------------//
    // preExpandPackages //
    //-------------------//
    /**
     * Before displaying the tree, expand all nodes that correspond to packages
     * (PackageNode).
     */
    private void preExpandPackages ()
    {
        for (int row = 0; row < tree.getRowCount(); row++) {
            if (tree.isCollapsed(row)) {
                TreePath tp = tree.getPathForRow(row);
                Object   obj = UnitManager.getInstance()
                                          .getNode(buildKey(tp));

                if (obj instanceof PackageNode) {
                    tree.expandRow(row);
                }
            }
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //----------------//
    // LoggerRenderer //
    //----------------//
    private static class LoggerRenderer
        extends DefaultTableCellRenderer
    {
        //-------------------------------//
        // getTableCellRendererComponent //
        //-------------------------------//
        @Override
        public Component getTableCellRendererComponent (JTable  table,
                                                        Object  value,
                                                        boolean isSelected,
                                                        boolean hasFocus,
                                                        int     row,
                                                        int     column)
        {
            super.getTableCellRendererComponent(
                table,
                value,
                isSelected,
                hasFocus,
                row,
                column);

            // Display the proper tip text
            //            TreePath tp = tree.getPathForRow(row);
            //            Node node = UnitManager.getInstance().getNode(buildKey(tp));
            //            if (node instanceof UnitNode) {
            //                UnitNode unit = (UnitNode) node;
            //                Logger logger = unit.getLogger();
            //                if (logger != null) {
            //                     Logger.Level level = (Logger.Level) logger.getLevel();
            //                     if (level != null) {
            //                         setFont(table.getFont().deriveFont(Font.BOLD).deriveFont(12.0f));
            //                     }
            //                }
            //            }
            return this;
        }
    }

    //---------------//
    // ValueRenderer //
    //---------------//
    private class ValueRenderer
        extends DefaultTableCellRenderer
    {
        //-------------------------------//
        // getTableCellRendererComponent //
        //-------------------------------//
        @Override
        public Component getTableCellRendererComponent (JTable  table,
                                                        Object  value,
                                                        boolean isSelected,
                                                        boolean hasFocus,
                                                        int     row,
                                                        int     column)
        {
            super.getTableCellRendererComponent(
                table,
                value,
                isSelected,
                hasFocus,
                row,
                column);

            // Display the proper tip text
            TreePath tp = tree.getPathForRow(row);
            Node     node = UnitManager.getInstance()
                                       .getNode(buildKey(tp));

            if (node == null) { // A Constant row

                Node        parent = UnitManager.getInstance()
                                                .getNode(
                    buildKey(tp.getParentPath()));
                UnitNode    unit = (UnitNode) parent;
                ConstantSet set = unit.getConstantSet();
                Constant    constant = set.getConstant(
                    tp.getLastPathComponent().toString());
                setToolTipText(constant.getDescription());

                // Use a bold font
                setFont(
                    table.getFont().deriveFont(Font.BOLD).deriveFont(12.0f));
            } else {
                setToolTipText(null);
            }

            return this;
        }
    }
}
