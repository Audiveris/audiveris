//-----------------------------------------------------------------------//
//                                                                       //
//                           U n i t M o d e l                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.constant;

import omr.Main;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.ui.treetable.AbstractTreeTableModel;
import omr.ui.treetable.TreeTableModel;
import omr.util.Logger;

import javax.swing.*;

/**
 * Class <code>UnitModel</code> implements a data model for units suitable
 * for use in a JTreeTable.
 *
 * <p>A row in the UnitModel can any instance of the 3 following types:
 * <ul>
 *
 * <li><b>PackageNode</b> to represent a package. Its children rows can be
 * either (sub) PackageNodes or UnitNodes.
 *
 * <li><b>UnitNode</b> to represent a class that contains a ConstantSet or
 * a Logger or both. Its parent node is a PackageNode. Its children rows
 * (if any) are the Constants of its ConstantSet.
 *
 * <li><b>Constant</b> to represent a constant within a ConstantSet. In
 * that case, its parent node is a UnitNode. It has no children rows. </ul>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class UnitModel
    extends AbstractTreeTableModel
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(UnitModel.class);

    /**
     * Enumeration type to describe each column of the JTreeTable
     */
    enum Column
    {
        /**
         * The left column, assigned to tree structure, allows expansion
         * and collapsing of sub-tree portions
         */
        TREE   ("Unit", 250, TreeTableModel.class),

        /**
         * Editable column dedicated to {@link omr.util.Logger} entity if
         * any
         */
        LOGGER ("Logger", 30, String.class),

        /**
         * Editable column for constant current value, with related tool
         * tip retrieved from the constant declaration
         */
        VALUE  ("Value", 100, String.class),

        /**
         * Column that recalls the constant type, and thus the possible
         * range of valued
         */
        TYPE   ("Type", 40, String.class),

        /**
         * Column relevant only for constants which are fractions of
         * interline, as defined by {@link omr.sheet.Scale.Fraction} : the
         * equivalent number of pixels is displayed, according to the scale
         * of the currently selected Sheet. If there is no current Sheet,
         * then just a question mark (?)  is displayed
         */
        PIXEL  ("Pixels", 20, String.class),

        /**
         * Column containing a flag to indicate whether the constant value
         * has been modified or not. If modified, a click in this column
         * resets the constant to its original value
         */
        MODIF  ("Modif", 20, Boolean.class);

        /**
         * Header for the column
         */
        final String header;

        /**
         * Width for column display
         */
        final int width;

        /**
         * Java class to handle column content
         */
        final Class type;

        //--------//
        // Column //
        //--------//
        Column (String header,
                int width,
                Class type)
        {
            this.header = header;
            this.width = width;
            this.type = type;
        }
    }

    //~ Constructors ------------------------------------------------------

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

    //~ Methods -----------------------------------------------------------

    //----------------//
    // isCellEditable //
    //----------------//
    @Override
    public boolean isCellEditable (Object node,
                                   int column)
    {
        return
                (column != Column.PIXEL.ordinal()) &&
                (column != Column.TYPE.ordinal());
    }

    //----------//
    // getChild //
    //----------//
    /**
     * Returns the child of <code>parent</code> at index <code>index</code>
     * in the parent's child array.
     *
     * @param parent a node in the tree, obtained from this data source
     *
     * @return the child of <code>parent</code> at index <code>index</code>
     */
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

        System.err.println("*** getChild. Unexpected node " + parent
                           + ", type=" + parent.getClass().getName());

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
            UnitNode unit = (UnitNode) parent;
            ConstantSet set = unit.getConstantSet();
            if (set != null) {
                return set.size();
            } else {
                return 0;       // A unit with Logger, but w/o ConstantSet
            }
        }

        if (parent instanceof Constant) {
            return 0;
        }

        System.err.println("*** getChildCount. Unexpected node " + parent
                           + ", type=" + parent.getClass().getName());

        return 0;
    }

    //----------------//
    // getColumnClass //
    //----------------//
    @Override
    public Class getColumnClass (int column)
    {
        return Column.values()[column].type;
    }

    //----------------//
    // getColumnCount //
    //----------------//
    public int getColumnCount ()
    {
        return Column.values().length;
    }

    //---------------//
    // getColumnName //
    //---------------//
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

        return false;           // By default
    }

    //------------//
    // setValueAt //
    //------------//
    @Override
    public void setValueAt (Object value,
                            Object node,
                            int col)
    {
        if (node instanceof UnitNode) {
            UnitNode unit = (UnitNode) node;
            Logger logger = unit.getLogger();
            if (logger != null) {
                logger.setLevel((String) value);
            }
        }

        if (node instanceof Constant) {

            Constant constant = (Constant) node;
            Column column = Column.values()[col];

            switch (column) {
                case VALUE:

                    try {
                        constant.setValue(value.toString());

                        // Forward modif to the modif status column and to
                        // the pixel column (brute force!)
                        fireTreeNodesChanged
                                (this,
                                 new Object[]{getRoot()},
                                 null, null);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog
                            (null,
                             "Illegal number format");
                    }

                    break;

                case MODIF:

                    if (!((Boolean) value).booleanValue()) {
                        constant.reset();
                        fireTreeNodesChanged
                                (this,
                                 new Object[]{getRoot()},
                                 null, null);
                    }
                    break;
            }
        }
    }

    //------------//
    // getValueAt //
    //------------//
    public Object getValueAt (Object node,
                              int col)
    {
        Column column = Column.values()[col];

        switch (column) {
            case LOGGER:
                if (node instanceof UnitNode) {
                    UnitNode unit = (UnitNode) node;
                    Logger logger = unit.getLogger();
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

            case VALUE:
                if (node instanceof Constant) {
                    Constant constant = (Constant) node;
                    if (constant instanceof Constant.Boolean) {
                        Constant.Boolean cb = (Constant.Boolean) constant;
                        return Boolean.valueOf (cb.getValue());
                    } else {
                        return constant.currentString();
                    }
                } else {
                    return "";
                }

            case TYPE:
                if (node instanceof Constant) {
                    Constant constant = (Constant) node;
                    String typeName = constant.getClass().getName();
                    int dollar = typeName.lastIndexOf('$');

                    if (dollar != -1) {
                        return typeName.substring(dollar + 1);
                    } else {
                        return typeName;
                    }
                } else {
                    return "";
                }

            case PIXEL:
                if (node instanceof Constant) {
                    Constant constant = (Constant) node;
                    if (constant instanceof Scale.Fraction) {
                        // Compute the equivalent in pixels of this
                        // interline fraction, provided that we have a
                        // current sheet and its scale is available.
                        Sheet sheet = Main.getJui().sheetPane.getCurrentSheet();

                        if ((sheet != null) && sheet.SCALE.isDone()) {
                            Scale scale = sheet.getScale();
                            Scale.Fraction fraction = (Scale.Fraction) constant;

                            return new Integer(scale.toPixels(fraction));
                        } else {
                            return "?"; // Cannot compute the value
                        }
                    }
                }
                return "";

            case MODIF:
                if (node instanceof Constant) {
                    Constant constant = (Constant) node;
                    return Boolean.valueOf (constant.isModified());
                } else {
                    return null;
                }
        }

        return null; // For the compiler
    }
}
