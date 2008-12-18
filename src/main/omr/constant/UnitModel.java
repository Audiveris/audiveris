//----------------------------------------------------------------------------//
//                                                                            //
//                             U n i t M o d e l                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.constant;

import omr.log.Logger;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SheetManager;

import omr.ui.treetable.AbstractTreeTableModel;
import omr.ui.treetable.TreeTableModel;

import javax.swing.*;

/**
 * Class <code>UnitModel</code> implements a data model for units suitable for
 * use in a JTreeTable.
 *
 * <p>A row in the UnitModel can any instance of the 3 following types: <ul>
 *
 * <li><b>PackageNode</b> to represent a package. Its children rows can be
 * either (sub) PackageNodes or UnitNodes.
 *
 * <li><b>UnitNode</b> to represent a class that contains a ConstantSet or a
 * Logger or both. Its parent node is a PackageNode. Its children rows (if any)
 * are the Constants of its ConstantSet.
 *
 * <li><b>Constant</b> to represent a constant within a ConstantSet. In that
 * case, its parent node is a UnitNode. It has no children rows. </ul>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class UnitModel
    extends AbstractTreeTableModel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(UnitModel.class);

    //~ Enumerations -----------------------------------------------------------

    /**
     * Enumeration type to describe each column of the JTreeTable
     */
    public static enum Column {
        //~ Enumeration constant initializers ----------------------------------


        /**
         * The left column, assigned to tree structure, allows expansion and
         * collapsing of sub-tree portions
         */
        TREE("Unit", true, 250, TreeTableModel.class),

        /**
         * Editable column dedicated to {@link omr.log.Logger} entity if any
         */
        LOGGER("Logger", true, 30, String.class), 

        /**
         * Editable column for constant current value, with related tool tip
         * retrieved from the constant declaration
         */
        VALUE("Value", true, 100, String.class), 

        /**
         * Column that recalls the constant type, and thus the possible range of
         * values
         */
        TYPE("Type", false, 40, String.class), 

        /**
         * Column for the units, if any, used for the value
         */
        UNIT("Unit", false, 40, String.class), 

        /**
         * Column relevant only for constants which are fractions of interline,
         * as defined by {@link omr.sheet.Scale.Fraction} : the equivalent
         * number of pixels is displayed, according to the scale of the
         * currently selected Sheet. If there is no current Sheet, then just a
         * question mark (?)  is displayed
         */
        PIXEL("Pixels", false, 20, String.class), 

        /**
         * Column containing a flag to indicate whether the constant value has
         * been modified or not. If modified, a click in this column resets the
         * constant to its original value
         */
        MODIF("Modif", false, 20, Boolean.class);
        //~ Instance fields ----------------------------------------------------

        /**
         * Java class to handle column content
         */
        final Class type;

        /**
         * Is this column user editable?
         */
        final boolean editable;

        /**
         * Header for the column
         */
        final String header;

        /**
         * Width for column display
         */
        final int width;

        //~ Constructors -------------------------------------------------------

        //--------//
        // Column //
        //--------//
        Column (String  header,
                boolean editable,
                int     width,
                Class   type)
        {
            this.header = header;
            this.editable = editable;
            this.width = width;
            this.type = type;
        }
    }

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // UnitModel //
    //-----------//
    /**
     * Builds the model
     */
    public UnitModel ()
    {
        super(UnitManager.getInstance().getRoot());
    }

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // isCellEditable //
    //----------------//
    /**
     * Predicate on cell being editable
     *
     * @param node the related tree node
     * @param column the related table column
     *
     * @return true if editable
     */
    @Override
    public boolean isCellEditable (Object node,
                                   int    column)
    {
        Column col = Column.values()[column];

        return col.editable;
    }

    //----------//
    // getChild //
    //----------//
    /**
     * Returns the child of <code>parent</code> at index <code>index</code> in
     * the parent's child array.
     *
     * @param parent a node in the tree, obtained from this data source
     *
     * @return the child of <code>parent</code> at index <code>index</code>
     */
    public Object getChild (Object parent,
                            int    i)
    {
        if (parent instanceof PackageNode) {
            PackageNode pNode = (PackageNode) parent;

            return pNode.getChild(i);
        }

        if (parent instanceof UnitNode) {
            UnitNode    unit = (UnitNode) parent;
            ConstantSet set = unit.getConstantSet();

            if (set != null) {
                return set.getConstant(i);
            }
        }

        System.err.println(
            "*** getChild. Unexpected node " + parent + ", type=" +
            parent.getClass().getName());

        return null;
    }

    //---------------//
    // getChildCount //
    //---------------//
    /**
     * Returns the number of children of <code>parent</code>.
     *
     * @param parent a node in the tree, obtained from this data source
     *
     * @return the number of children of the node <code>parent</code>
     */
    public int getChildCount (Object parent)
    {
        if (parent instanceof PackageNode) {
            return ((PackageNode) parent).getChildCount();
        }

        if (parent instanceof UnitNode) {
            UnitNode    unit = (UnitNode) parent;
            ConstantSet set = unit.getConstantSet();

            if (set != null) {
                return set.size();
            } else {
                return 0; // A unit with Logger, but w/o ConstantSet
            }
        }

        if (parent instanceof Constant) {
            return 0;
        }

        System.err.println(
            "*** getChildCount. Unexpected node " + parent + ", type=" +
            parent.getClass().getName());

        return 0;
    }

    //----------------//
    // getColumnClass //
    //----------------//
    /**
     * Report the class for instances in the provided column
     *
     * @param column the desired column
     *
     * @return the class for all cells in this column
     */
    @Override
    public Class getColumnClass (int column)
    {
        return Column.values()[column].type;
    }

    //----------------//
    // getColumnCount //
    //----------------//
    /**
     * Report the number of column in the table
     *
     * @return the table number of columns
     */
    public int getColumnCount ()
    {
        return Column.values().length;
    }

    //---------------//
    // getColumnName //
    //---------------//
    /**
     * Report the name of the column at hand
     *
     * @param column the desired column
     *
     * @return the column name
     */
    public String getColumnName (int column)
    {
        return Column.values()[column].header;
    }

    //--------//
    // isLeaf //
    //--------//
    /**
     * Returns <code>true</code> if <code>node</code> is a leaf.
     *
     * @param node a node in the tree, obtained from this data source
     *
     * @return true if <code>node</code> is a leaf
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
     * @param node the target node
     * @param col the related column
     */
    @Override
    public void setValueAt (Object value,
                            Object node,
                            int    col)
    {
        if (node instanceof UnitNode) {
            UnitNode unit = (UnitNode) node;
            Logger   logger = unit.getLogger();

            if (logger != null) {
                logger.setLevel((String) value);
            }
        }

        if (node instanceof Constant) {
            Constant constant = (Constant) node;
            Column   column = Column.values()[col];

            switch (column) {
            case VALUE :

                try {
                    constant.setValue(value.toString());

                    // Forward modif to the modif status column and to the pixel
                    // column (brute force!)
                    fireTreeNodesChanged(
                        this,
                        new Object[] { getRoot() },
                        null,
                        null);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(
                        null,
                        "Illegal number format");
                }

                break;

            case MODIF :

                if (!((Boolean) value).booleanValue()) {
                    constant.reset();
                    fireTreeNodesChanged(
                        this,
                        new Object[] { getRoot() },
                        null,
                        null);
                }

                break;
            }
        }
    }

    //------------//
    // getValueAt //
    //------------//
    /**
     * Report the value of a cell
     *
     * @param node the desired node
     * @param col the related column
     *
     * @return the cell value
     */
    public Object getValueAt (Object node,
                              int    col)
    {
        Column column = Column.values()[col];

        switch (column) {
        case LOGGER :

            if (node instanceof UnitNode) {
                UnitNode unit = (UnitNode) node;
                Logger   logger = unit.getLogger();

                if (logger != null) {
                    return logger.getEffectiveLevel();
                } else {
                    return "";
                }
            } else if (node instanceof PackageNode) {
                return "--";
            } else {
                return "";
            }

        case VALUE :

            if (node instanceof Constant) {
                Constant constant = (Constant) node;

                if (constant instanceof Constant.Boolean) {
                    Constant.Boolean cb = (Constant.Boolean) constant;

                    return Boolean.valueOf(cb.getValue());
                } else {
                    return constant.getCurrentString();
                }
            } else {
                return "";
            }

        case TYPE :

            if (node instanceof Constant) {
                Constant constant = (Constant) node;

                return constant.getShortTypeName();
            } else {
                return "";
            }

        case UNIT :

            if (node instanceof Constant) {
                Constant constant = (Constant) node;

                return (constant.getQuantityUnit() != null)
                       ? constant.getQuantityUnit() : "";
            } else {
                return "";
            }

        case PIXEL :

            if (node instanceof Constant) {
                Constant constant = (Constant) node;

                if (constant instanceof Scale.Fraction ||
                    constant instanceof Scale.AreaFraction) {
                    // Compute the equivalent in pixels of this interline-based
                    // fraction or area fraction, provided that we have a 
                    // current sheet and its scale is available.
                    Sheet sheet = SheetManager.getSelectedSheet();

                    if (sheet != null) {
                        Scale scale = sheet.getScale();

                        if (scale != null) {
                            if (constant instanceof Scale.Fraction) {
                                return Integer.valueOf(
                                    scale.toPixels((Scale.Fraction) constant));
                            }

                            if (constant instanceof Scale.AreaFraction) {
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

        case MODIF :

            if (node instanceof Constant) {
                Constant constant = (Constant) node;

                return Boolean.valueOf(constant.isModified());
            } else {
                return null;
            }
        }

        return null; // For the compiler
    }
}
