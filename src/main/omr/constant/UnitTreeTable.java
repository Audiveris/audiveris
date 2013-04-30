//----------------------------------------------------------------------------//
//                                                                            //
//                         U n i t T r e e T a b l e                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.constant;

import omr.ui.treetable.JTreeTable;
import omr.ui.treetable.TreeTableModelAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreePath;

/**
 * Class {@code UnitTreeTable} is a user interface that combines a tree
 * to display the hierarchy of Units, that contains ConstantSets, 
 * and a table to display and edit the various Constants in
 * each ConstantSet.
 *
 * @author Hervé Bitteur
 */
public class UnitTreeTable
        extends JTreeTable
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(UnitTreeTable.class);

    /** Alternate color for zebra appearance */
    private static final Color zebraColor = new Color(248, 248, 255);

    //~ Instance fields --------------------------------------------------------
    private TableCellRenderer valueRenderer = new ValueRenderer();

    private TableCellRenderer pixelRenderer = new PixelRenderer();

    //~ Constructors -----------------------------------------------------------
    //---------------//
    // UnitTreeTable //
    //---------------//
    /**
     * Create a User Interface JTreeTable dedicated to the handling of
     * unit constants.
     *
     * @param model the corresponding data model
     */
    public UnitTreeTable (UnitModel model)
    {
        super(model);

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
        case MODIF: {
            Object node = nodeForRow(row);

            if (node instanceof Constant) {
                return getDefaultEditor(Boolean.class);
            }
        }

        break;

        case VALUE: {
            Object obj = getModel().getValueAt(row, col);

            if (obj instanceof Boolean) {
                return getDefaultEditor(Boolean.class);
            }
        }

        break;

        default:
        }

        // Default cell editor (determined by column class)
        return super.getCellEditor(row, col);
    }

    //-----------------//
    // getCellRenderer //
    //-----------------//
    /**
     * Used by the UI to get the proper renderer for each given cell in
     * the table.
     *
     * @param row row in the table
     * @param col column in the table
     * @return the best renderer for the cell.
     */
    @Override
    public TableCellRenderer getCellRenderer (int row,
                                              int col)
    {
        UnitModel.Column column = UnitModel.Column.values()[col];

        switch (column) {
        case MODIF: {
            Object obj = getModel().getValueAt(row, col);

            if (obj instanceof Boolean) {
                // A constant => Modif flag
                return getDefaultRenderer(Boolean.class);
            } else {
                // A node (unit or package)
                return getDefaultRenderer(Object.class);
            }
        }

        case VALUE: {
            Object obj = getModel().getValueAt(row, col);

            if (obj instanceof Boolean) {
                return getDefaultRenderer(Boolean.class);
            } else {
                return valueRenderer;
            }
        }

        case PIXEL:
            return pixelRenderer;

        default:
            return getDefaultRenderer(getColumnClass(col));
        }
    }

    //--------------------//
    // scrollRowToVisible //
    //--------------------//
    /**
     * Scroll so that the provided row gets visible
     *
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

    //-------------------//
    // setNodesSelection //
    //-------------------//
    /**
     * Select the rows that correspond to the provided nodes
     *
     * @param matches the nodes to select
     * @return the relevant rows
     */
    public List<Integer> setNodesSelection (Collection<Object> matches)
    {
        List<TreePath> paths = new ArrayList<>();

        for (Object object : matches) {
            if (object instanceof Constant) {
                Constant constant = (Constant) object;
                TreePath path = getPath(constant, constant.getQualifiedName());
                paths.add(path);
            } else if (object instanceof Node) {
                Node node = (Node) object;
                TreePath path = getPath(node, node.getName());
                paths.add(path);
            }
        }

        // Selection on tree side
        tree.setSelectionPaths(paths.toArray(new TreePath[paths.size()]));

        // Selection on table side
        clearSelection();

        List<Integer> rows = new ArrayList<>();

        for (TreePath path : paths) {
            int row = tree.getRowForPath(path);

            if (row != -1) {
                rows.add(row);
                addRowSelectionInterval(row, row);
            }
        }

        Collections.sort(rows);

        return rows;
    }

    //---------------//
    // adjustColumns //
    //---------------//
    /**
     * Allows to adjust the related columnModel, for each and every
     * column
     *
     * @param cModel the proper table column model
     */
    private void adjustColumns ()
    {
        TableColumnModel cModel = getColumnModel();

        // Columns widths
        for (UnitModel.Column c : UnitModel.Column.values()) {
            cModel.getColumn(c.ordinal()).setPreferredWidth(c.getWidth());
        }
    }

    //---------//
    // getPath //
    //---------//
    private TreePath getPath (Object object,
                              String fullName)
    {
        UnitManager unitManager = UnitManager.getInstance();
        List<Object> objects = new ArrayList<>();
        objects.add(unitManager.getRoot());

        int dotPos = -1;

        while ((dotPos = fullName.indexOf('.', dotPos + 1)) != -1) {
            String path = fullName.substring(0, dotPos);
            objects.add(unitManager.getNode(path));
        }

        objects.add(object);

        logger.debug("path to {} objects:{}", fullName, objects);

        return new TreePath(objects.toArray());
    }

    //------------//
    // nodeForRow //
    //------------//
    /**
     * Return the tree node facing the provided table row
     *
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
     * Before displaying the tree, expand all nodes that correspond to
     * packages (PackageNode).
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

    //---------------//
    // PixelRenderer //
    //---------------//
    private static class PixelRenderer
            extends DefaultTableCellRenderer
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public Component getTableCellRendererComponent (JTable table,
                                                        Object value,
                                                        boolean isSelected,
                                                        boolean hasFocus,
                                                        int row,
                                                        int column)
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
    private static class ValueRenderer
            extends DefaultTableCellRenderer
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public Component getTableCellRendererComponent (JTable table,
                                                        Object value,
                                                        boolean isSelected,
                                                        boolean hasFocus,
                                                        int row,
                                                        int column)
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
