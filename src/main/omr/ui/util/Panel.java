//-----------------------------------------------------------------------//
//                                                                       //
//                               P a n e l                               //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
package omr.ui.util;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.util.Logger;

import com.jgoodies.forms.debug.FormDebugPanel;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Class <code>Panel</code> is a JPanel with specific features to define
 * and position label and text fields.
 *
 * <P><B>note</B> : To actually display the cell limits as a debugging aid
 * to refine the panel layout, you have to edit this class and make it
 * extend FormDebugPanel, instead of JPanel. There is also a line to
 * uncomment in both methods : the constructor and the paintComponent
 * method.
 *
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Panel
    extends JPanel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Panel.class);

    /** Default Insets */
    public static Insets DEFAULT_INSETS;

    //~ Instance fields --------------------------------------------------------

    /** Room for potential specific insets */
    private Insets insets;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new Panel object.
     */
    public Panel ()
    {
        // XXX Uncomment following line for FormDebugPanel XXX
        //setPaintInBackground(constants.paintGrid.getValue());
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Selector to the default button width
     *
     * @return default distance in JGoodies/Forms units (such as DLUs)
     */
    public static String getButtonWidth ()
    {
        return constants.buttonWidth.getValue();
    }

    /**
     * Selector to the default vertical interval between two rows
     *
     * @return default distance in JGoodies/Forms units (such as DLUs)
     */
    public static String getFieldInterline ()
    {
        return constants.fieldInterline.getValue();
    }

    /**
     * Selector to the default interval between two label/field
     *
     * @return default distance in JGoodies/Forms units (such as DLUs)
     */
    public static String getFieldInterval ()
    {
        return constants.fieldInterval.getValue();
    }

    /**
     * Selector to the default label width
     *
     * @return default distance in JGoodies/Forms units (such as DLUs)
     */
    public static String getFieldWidth ()
    {
        return constants.fieldWidth.getValue();
    }

    //-----------//
    // setInsets //
    //-----------//
    /**
     * Set the panel insets (in number of pixels) on the four directions
     *
     * @param top inset on top side
     * @param left inset on the left side
     * @param bottom inset on the bottom side
     * @param right inset on the right side
     */
    public void setInsets (int top,
                           int left,
                           int bottom,
                           int right)
    {
        insets = new Insets(top, left, bottom, right);
    }

    //-----------//
    // getInsets //
    //-----------//
    /**
     * By this way, Swing will paint the component with its specific inset
     * values
     *
     * @return the panel insets
     */
    @Override
    public Insets getInsets ()
    {
        if (insets != null) {
            return insets;
        } else {
            return getDefaultInsets();
        }
    }

    /**
     * Selector to the default label - field interval
     *
     * @return default distance in JGoodies/Forms units (such as DLUs)
     */
    public static String getLabelInterval ()
    {
        return constants.labelInterval.getValue();
    }

    /**
     * Selector to the default label width
     *
     * @return default distance in JGoodies/Forms units (such as DLUs)
     */
    public static String getLabelWidth ()
    {
        return constants.labelWidth.getValue();
    }

    //-------------//
    // setNoInsets //
    //-------------//
    /**
     * A convenient method to set all 4 insets values to zero
     */
    public void setNoInsets ()
    {
        insets = new Insets(0, 0, 0, 0);
    }

    /**
     * Selector to the default vertical interval between two Panels
     *
     * @return default distance in JGoodies/Forms units (such as DLUs)
     */
    public static String getPanelInterline ()
    {
        return constants.panelInterline.getValue();
    }

    //----------------//
    // makeFormLayout //
    //----------------//
    /**
     * Build a JGoodies Forms layout, based on the provided number of rows
     * and number of columns, using default values for label alignment, and
     * for widths of labels and fields
     *
     * @param rows number of logical rows (not counting the interlines)
     * @param cols number of logical columns (counting the combination of a
     * label and its related field as just one column)
     * @return the FormLayout ready to use
     */
    public static FormLayout makeFormLayout (int rows,
                                             int cols)
    {
        return makeFormLayout(
            rows,
            cols,
            "right:",
            Panel.getLabelWidth(),
            Panel.getFieldWidth());
    }

    //----------------//
    // makeFormLayout //
    //----------------//
    /**
     * A more versatile way to prepare a JGoodies FormLayout
     *
     * @param rows number of rows
     * @param cols number of columns
     * @param labelAlignment horizontal alignment for labels
     * @param labelWidth width for labels
     * @param fieldWidth width for text fields
     * @return the FormLayout ready to use
     */
    public static FormLayout makeFormLayout (int    rows,
                                             int    cols,
                                             String labelAlignment,
                                             String labelWidth,
                                             String fieldWidth)
    {
        final String labelInterval = Panel.getLabelInterval();
        final String fieldInterval = Panel.getFieldInterval();
        final String fieldInterline = Panel.getFieldInterline();

        // Columns
        StringBuffer sbc = new StringBuffer();

        for (int i = cols - 1; i >= 0; i--) {
            sbc.append(labelAlignment)
               .append(labelWidth)
               .append(",")
               .append(labelInterval)
               .append(",")
               .append(fieldWidth);

            if (i != 0) {
                sbc.append(",")
                   .append(fieldInterval)
                   .append(",");
            }
        }

        // Rows
        StringBuffer sbr = new StringBuffer();

        for (int i = rows - 1; i >= 0; i--) {
            sbr.append("pref");

            if (i != 0) {
                sbr.append(",")
                   .append(fieldInterline)
                   .append(",");
            }
        }

        if (logger.isFineEnabled()) {
            logger.fine("sbc=" + sbc);
            logger.fine("sbr=" + sbr);
        }

        return new FormLayout(sbc.toString(), sbr.toString());
    }

    //----------------//
    // paintComponent //
    //----------------//
    /**
     * This method is redefined to give a chance to draw the cell
     * boundaries if so desired
     *
     * @param g the graphic context
     */
    @Override
    protected void paintComponent (Graphics g)
    {
        // XXX Uncomment following line for FormDebugPanel XXX
        //setPaintInBackground(constants.paintGrid.getValue());
        super.paintComponent(g);
    }

    //------------------//
    // getDefaultInsets //
    //------------------//
    private Insets getDefaultInsets ()
    {
        if (DEFAULT_INSETS == null) {
            DEFAULT_INSETS = new Insets(
                constants.insetTop.getValue(),
                constants.insetLeft.getValue(),
                constants.insetBottom.getValue(),
                constants.insetRight.getValue());
        }

        return DEFAULT_INSETS;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        Constant.String  buttonWidth = new Constant.String(
            "45dlu",
            "Width of a standard button");
        Constant.String  fieldInterline = new Constant.String(
            "1dlu",
            "Vertical Gap between two lines");
        Constant.String  fieldInterval = new Constant.String(
            "3dlu",
            "Horizontal gap between two fields");
        Constant.String  fieldWidth = new Constant.String(
            "35dlu",
            "Width of a field value");
        Constant.Integer insetBottom = new Constant.Integer(
            6,
            "Value of Bottom inset");
        Constant.Integer insetLeft = new Constant.Integer(
            6,
            "Value of Left inset");
        Constant.Integer insetRight = new Constant.Integer(
            6,
            "Value of Right inset");
        Constant.Integer insetTop = new Constant.Integer(
            6,
            "Value of Top inset");
        Constant.String  labelInterval = new Constant.String(
            "1dlu",
            "Gap between a field label and its field value");
        Constant.String  labelWidth = new Constant.String(
            "25dlu",
            "Width of the label of a field");
        Constant.String  panelInterline = new Constant.String(
            "6dlu",
            "Vertical Gap between two panels");
    }
}
