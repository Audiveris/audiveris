//----------------------------------------------------------------------------//
//                                                                            //
//                             U n i t M o d e l                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.constant;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.ui.SheetsController;

import omr.ui.treetable.AbstractTreeTableModel;
import omr.ui.treetable.TreeTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;

/**
 * Class {@code UnitModel} implements a data model for units suitable
 * for use in a JTreeTable.
 *
 * <p>A row in the UnitModel can be any instance of the 3 following types:
 * <ul>
 *
 * <li><b>PackageNode</b> to represent a package. Its children rows can be
 * either (sub) PackageNodes or UnitNodes.</li>
 *
 * <li><b>UnitNode</b> to represent a class that contains a ConstantSet.
 * Its parent node is a PackageNode. Its children rows (if any)
 * are the Constants of its ConstantSet.</li>
 *
 * <li><b>Constant</b> to represent a constant within a ConstantSet. In that
 * case, its parent node is a UnitNode. It has no children rows.</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class UnitModel
        extends AbstractTreeTableModel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            UnitModel.class);

    //~ Enumerations -----------------------------------------------------------
    /**
     * Enumeration type to describe each column of the JTreeTable
     */
    public static enum Column
    {
        //~ Enumeration constant initializers ----------------------------------

        /**
         * The left column, assigned to tree structure, allows expansion
         * and collapsing of sub-tree portions.
         */
        TREE("Unit", true, 280, TreeTableModel.class),
        /**
         * Editable column with modification flag if node is a constant.
         * Empty if node is a package.
         */
        MODIF("Modif", true, 50, String.class),
        /**
         * Column that recalls the constant type, and thus the possible
         * range of values.
         */
        TYPE("Type", false, 70, String.class),
        /**
         * Column for the units, if any, used for the value.
         */
        UNIT("Unit", false, 70, String.class),
        /**
         * Column relevant only for constants which are fractions of
         * interline, as defined by {@link omr.sheet.Scale.Fraction}.
         * The equivalent number of pixels is displayed, according to the scale
         * of the currently selected Sheet.
         * If there is no current Sheet, then just a question mark (?) is shown
         */
        PIXEL("Pixels", false, 30, String.class),
        /**
         * Editable column for constant current value, with related tool
         * tip retrieved from the constant declaration.
         */
        VALUE("Value", true, 100, String.class),
        /**
         * Column dedicated to constant description.
         */
        DESC("Description", false, 350, String.class);
        //~ Instance fields ----------------------------------------------------

        /** Java class to handle column content. */
        private final Class<?> type;

        /** Is this column user editable?. */
        private final boolean editable;

        /** Header for the column. */
        private final String header;

        /** Width for column display. */
        private final int width;

        //~ Constructors -------------------------------------------------------
        //--------//
        // Column //
        //--------//
        Column (String header,
                boolean editable,
                int width,
                Class<?> type)
        {
            this.header = header;
            this.editable = editable;
            this.width = width;
            this.type = type;
        }

        //~ Methods ------------------------------------------------------------
        //----------//
        // getWidth //
        //----------//
        public int getWidth ()
        {
            return width;
        }
    }

    //~ Constructors -----------------------------------------------------------
    //-----------//
    // UnitModel //
    //-----------//
    /**
     * Builds the model.
     */
    public UnitModel ()
    {
        super(UnitManager.getInstance().getRoot());
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // getChild //
    //----------//
    /**
     * Returns the child of {@code parent} at index {@code index} in
     * the parent's child array.
     *
     * @param parent a node in the tree, obtained from this data source
     * @param i      the child index in parent sequence
     * @return the child of {@code parent</code> at index <code>index}
     */
    @Override
    public Object getChild (Object parent,
                            int i)
    {
        if (parent instanceof PackageNode) {
            PackageNode pNode = (PackageNode) parent;

            return pNode.getChild(i);
        }

        if (parent instanceof UnitNode) {
            UnitNode unit = (UnitNode) parent;
            ConstantSet set = unit.getConstantSet();

            if (set != null) {
                return set.getConstant(i);
            }
        }

        System.err.println(
                "*** getChild. Unexpected node " + parent + ", type="
                + parent.getClass().getName());

        return null;
    }

    //---------------//
    // getChildCount //
    //---------------//
    /**
     * Returns the number of children of {@code parent}.
     *
     * @param parent a node in the tree, obtained from this data source
     * @return the number of children of the node {@code parent}
     */
    @Override
    public int getChildCount (Object parent)
    {
        if (parent instanceof PackageNode) {
            return ((PackageNode) parent).getChildCount();
        }

        if (parent instanceof UnitNode) {
            UnitNode unit = (UnitNode) parent;
            ConstantSet set = unit.getConstantSet();

            return set.size();
        }

        if (parent instanceof Constant) {
            return 0;
        }

        System.err.println(
                "*** getChildCount. Unexpected node " + parent + ", type="
                + parent.getClass().getName());

        return 0;
    }

    //----------------//
    // getColumnClass //
    //----------------//
    /**
     * Report the class for instances in the provided column.
     *
     * @param column the desired column
     * @return the class for all cells in this column
     */
    @Override
    public Class<?> getColumnClass (int column)
    {
        return Column.values()[column].type;
    }

    //----------------//
    // getColumnCount //
    //----------------//
    /**
     * Report the number of column in the table.
     *
     * @return the table number of columns
     */
    @Override
    public int getColumnCount ()
    {
        return Column.values().length;
    }

    //---------------//
    // getColumnName //
    //---------------//
    /**
     * Report the name of the column at hand.
     *
     * @param column the desired column
     * @return the column name
     */
    @Override
    public String getColumnName (int column)
    {
        return Column.values()[column].header;
    }

    //------------//
    // getValueAt //
    //------------//
    /**
     * Report the value of a cell.
     *
     * @param node the desired node
     * @param col  the related column
     * @return the cell value
     */
    @Override
    public Object getValueAt (Object node,
                              int col)
    {
        Column column = Column.values()[col];

        switch (column) {
        case MODIF:

            if (node instanceof PackageNode) {
                return null;
            } else if (node instanceof Constant) {
                Constant constant = (Constant) node;

                return Boolean.valueOf(!constant.isSourceValue());
            }

            return "";

        case VALUE:

            if (node instanceof Constant) {
                Constant constant = (Constant) node;

                if (constant instanceof Constant.Boolean) {
                    Constant.Boolean cb = (Constant.Boolean) constant;

                    return cb.getCachedValue();
                } else {
                    return constant.getCurrentString();
                }
            } else {
                return "";
            }

        case TYPE:

            if (node instanceof Constant) {
                Constant constant = (Constant) node;

                return constant.getShortTypeName();
            } else {
                return "";
            }

        case UNIT:

            if (node instanceof Constant) {
                Constant constant = (Constant) node;

                return (constant.getQuantityUnit() != null)
                        ? constant.getQuantityUnit() : "";
            } else {
                return "";
            }

        case PIXEL:

            if (node instanceof Constant) {
                Constant constant = (Constant) node;

                if (constant instanceof Scale.Fraction
                    || constant instanceof Scale.LineFraction
                    || constant instanceof Scale.AreaFraction) {
                    // Compute the equivalent in pixels of this interline-based
                    // fraction, line or area fraction, provided that we have a
                    // current sheet and its scale is available.
                    Sheet sheet = SheetsController.getCurrentSheet();

                    if (sheet != null) {
                        Scale scale = sheet.getScale();

                        if (scale != null) {
                            if (constant instanceof Scale.Fraction) {
                                return Integer.valueOf(
                                        scale.toPixels((Scale.Fraction) constant));
                            } else if (constant instanceof Scale.LineFraction) {
                                return Integer.valueOf(
                                        scale.toPixels(
                                        (Scale.LineFraction) constant));
                            } else if (constant instanceof Scale.AreaFraction) {
                                return Integer.valueOf(
                                        scale.toPixels(
                                        (Scale.AreaFraction) constant));
                            }
                        }
                    } else {
                        return "?"; // Cannot compute the value
                    }
                }
            }

            return "";

        case DESC:

            if (node instanceof Constant) {
                Constant constant = (Constant) node;

                return constant.getDescription();
            } else {
                return null;
            }
        }

        return null; // For the compiler
    }

    //----------------//
    // isCellEditable //
    //----------------//
    /**
     * Predicate on cell being editable
     *
     * @param node   the related tree node
     * @param column the related table column
     * @return true if editable
     */
    @Override
    public boolean isCellEditable (Object node,
                                   int column)
    {
        Column col = Column.values()[column];

        if (col == Column.MODIF) {
            if (node instanceof Constant) {
                Constant constant = (Constant) node;

                return Boolean.valueOf(!constant.isSourceValue());

                //            } else if (node instanceof UnitNode) {
                //                return true;
            } else {
                return false;
            }
        } else {
            return col.editable;
        }
    }

    //--------//
    // isLeaf //
    //--------//
    /**
     * Returns {@code true} if {@code node} is a leaf.
     *
     * @param node a node in the tree, obtained from this data source
     * @return true if {@code node} is a leaf
     */
    @Override
    public boolean isLeaf (Object node)
    {
        if (node instanceof Constant) {
            return true;
        }

        if (node instanceof UnitNode) {
            UnitNode unit = (UnitNode) node;

            return (unit.getConstantSet() == null);
        }

        return false; // By default
    }

    //------------//
    // setValueAt //
    //------------//
    /**
     * Assign a value to a cell
     *
     * @param value the value to assign
     * @param node  the target node
     * @param col   the related column
     */
    @Override
    public void setValueAt (Object value,
                            Object node,
                            int col)
    {
        if (node instanceof Constant) {
            Constant constant = (Constant) node;
            Column column = Column.values()[col];

            switch (column) {
            case VALUE:

                try {
                    constant.setValue(value.toString());

                    // Forward modif to the modif status column and to the pixel
                    // column (brute force!)
                    fireTreeNodesChanged(
                            this,
                            new Object[]{getRoot()},
                            null,
                            null);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(
                            null,
                            "Illegal number format");
                }

                break;

            case MODIF:

                if (!((Boolean) value).booleanValue()) {
                    constant.reset();
                    fireTreeNodesChanged(
                            this,
                            new Object[]{getRoot()},
                            null,
                            null);
                }

                break;

            default:
            }
        }
    }
}
