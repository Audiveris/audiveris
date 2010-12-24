//----------------------------------------------------------------------------//
//                                                                            //
//                         U n i t T r e e T a b l e                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.constant;

import omr.log.Logger;

import omr.ui.treetable.JTreeTable;
import omr.ui.treetable.TreeTableModelAdapter;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.tree.TreePath;

/**
 * Class <code>UnitTreeTable</code> is a user interface that combines a tree to
 * display the hierarchy of Units, that contains ConstantSets and/or Loggers,
 * and a table to display and edit the various Constants in each ConstantSet as
 * well as the logger level of units.
 *
 * @author Hervé Bitteur
 */
public class UnitTreeTable
    extends JTreeTable
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(UnitTreeTable.class);

    /** Alternate color for zebra appearance */
    private static final Color zebraColor = new Color(248, 248, 255);

    //~ Instance fields --------------------------------------------------------

    private TableCellRenderer loggerRenderer = new LoggerRenderer();
    private TableCellRenderer valueRenderer = new ValueRenderer();
    private TableCellRenderer pixelRenderer = new PixelRenderer();
    private JComboBox         loggerCombo = new JComboBox();
    private TableCellEditor   loggerEditor = new DefaultCellEditor(loggerCombo);

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

        loggerCombo.addItem("INFO");
        loggerCombo.addItem("FINEST");

        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Zebra
        UIManager.put("Table.alternateRowColor", zebraColor);
        setFillsViewportHeight(true);

        // Show grid?
        //setShowGrid(true);

        // Specify column widths
        adjustColumns();

        // Customize the tree aspect
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);

        // Pre-expand all package nodes
        preExpandPackages();
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // getCellEditor //
    //---------------//
    @Override
    public TableCellEditor getCellEditor (int row,
                                          int col)
    {
        UnitModel.Column column = UnitModel.Column.values()[col];

        switch (column) {
        case LOGMOD : {
            Object node = nodeForRow(row);

            if (node instanceof UnitNode) { // LOGGER

                UnitNode unit = (UnitNode) node;
                Logger   logger = unit.getLogger();

                if (logger != null) {
                    Level level = logger.getEffectiveLevel();
                    loggerCombo.setSelectedItem(level.toString());

                    return loggerEditor;
                }
            } else if (node instanceof Constant) { // MODIF

                return getDefaultEditor(Boolean.class);
            }
        }

        break;

        case VALUE : {
            Object obj = getModel()
                             .getValueAt(row, col);

            if (obj instanceof Boolean) {
                return getDefaultEditor(Boolean.class);
            }
        }

        break;

        default :
        }

        // Default cell editor (determined by column class)
        return super.getCellEditor(row, col);
    }

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
        case LOGMOD : {
            Object obj = getModel()
                             .getValueAt(row, col);

            if (obj instanceof Boolean) {
                // A constant => Modif flag
                return getDefaultRenderer(Boolean.class);
            } else if (obj instanceof Level) {
                // A logger level
                return loggerRenderer;
            } else {
                // A node (unit or package)
                return getDefaultRenderer(Object.class);
            }
        }

        case VALUE : {
            Object obj = getModel()
                             .getValueAt(row, col);

            if (obj instanceof Boolean) {
                return getDefaultRenderer(Boolean.class);
            } else {
                return valueRenderer;
            }
        }

        case PIXEL :
            return pixelRenderer;

        default :
            return getDefaultRenderer(getColumnClass(col));
        }
    }

    //-----------------------//
    // setConstantsSelection //
    //-----------------------//
    /**
     * Select the rows that correspond to the provided constants
     * @param constants the constants to select
     * @return the relevant rows
     */
    public int[] setConstantsSelection (Collection<Constant> constants)
    {
        List<TreePath> paths = new ArrayList<TreePath>();

        for (Constant constant : constants) {
            TreePath path = getPath(constant);
            paths.add(path);
        }

        tree.setSelectionPaths(paths.toArray(new TreePath[0]));

        int[] rows = tree.getSelectionRows();

        if (rows != null) {
            Arrays.sort(rows);
        }

        return rows;
    }

    //---------//
    // getPath //
    //---------//
    public TreePath getPath (Constant constant)
    {
        UnitManager  unitManager = UnitManager.getInstance();
        String       fullName = constant.getQualifiedName();
        List<Object> objects = new ArrayList<Object>();
        objects.add(unitManager.getRoot());

        int dotPos = -1;

        while ((dotPos = fullName.indexOf('.', dotPos + 1)) != -1) {
            String path = fullName.substring(0, dotPos);
            objects.add(unitManager.getNode(path));
        }

        objects.add(constant);

        if (logger.isFineEnabled()) {
            logger.fine("constant:" + fullName + " objects:" + objects);
        }

        return new TreePath(objects.toArray());
    }

    //--------------------//
    // scrollRowToVisible //
    //--------------------//
    /**
     * Scroll so that the provided row gets visible
     * @param row the provided row
     */
    public void scrollRowToVisible (int row)
    {
        final int height = tree.getRowHeight();
        Rectangle rect = new Rectangle(0, row * height, 0, 0);

        if (getParent() instanceof JComponent) {
            JComponent comp = (JComponent) getParent();
            rect.grow(0, comp.getHeight() / 2);
        } else {
            rect.grow(0, height);
        }

        scrollRectToVisible(rect);
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

    //------------//
    // nodeForRow //
    //------------//
    /**
     * Return the tree node facing the provided table row
     * @param row the provided row
     * @return the corresponding tree node
     */
    private Object nodeForRow (int row)
    {
        return ((TreeTableModelAdapter) getModel()).nodeForRow(row);
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
                tree.expandRow(row);
            }
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //----------------//
    // LoggerRenderer //
    //----------------//
    private class LoggerRenderer
        extends DefaultTableCellRenderer
    {
        //~ Methods ------------------------------------------------------------

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

            Object node = nodeForRow(row);

            if (node instanceof UnitNode) {
                UnitNode unit = (UnitNode) nodeForRow(row);
                Logger   logger = unit.getLogger();

                if (logger != null) {
                    Level level = logger.getEffectiveLevel();

                    if (level != Level.INFO) {
                        setBackground(Color.LIGHT_GRAY);
                        setForeground(Color.WHITE);

                        return this;
                    }
                }
            }

            setBackground(Color.WHITE);
            setForeground(Color.BLACK);

            return this;
        }
    }

    //---------------//
    // PixelRenderer //
    //---------------//
    private class PixelRenderer
        extends DefaultTableCellRenderer
    {
        //~ Methods ------------------------------------------------------------

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

            // Use right alignment
            setHorizontalAlignment(SwingConstants.RIGHT);

            return this;
        }
    }

    //---------------//
    // ValueRenderer //
    //---------------//
    private class ValueRenderer
        extends DefaultTableCellRenderer
    {
        //~ Methods ------------------------------------------------------------

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

            // Use a bold font
            setFont(table.getFont().deriveFont(Font.BOLD).deriveFont(12.0f));

            // Use center alignment
            setHorizontalAlignment(SwingConstants.CENTER);

            return this;
        }
    }
}
