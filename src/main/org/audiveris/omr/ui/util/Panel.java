//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           P a n e l                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.ui.util;

import com.jgoodies.forms.layout.FormLayout;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.ui.PixelCount;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JPanel;

/**
 * Class {@code Panel} is a JPanel with specific features to define and position label
 * and text fields.
 * <P>
 * <B>note</B> : To actually display the cell limits as a debugging aid to refine the panel layout,
 * you have to edit this class and make it extend FormDebugPanel, instead of JPanel. There is also a
 * line to uncomment in the paintComponent method.
 *
 * @author Hervé Bitteur
 */
public class Panel
        extends JPanel ///FormDebugPanel

{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Panel.class);

    /** Default Insets */
    private static Insets DEFAULT_INSETS;

    //~ Instance fields ----------------------------------------------------------------------------
    /** Room for potential specific insets */
    private Insets insets;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new Panel object.
     */
    public Panel ()
    {
        setBorder(null);
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Selector to the default button width.
     *
     * @return default distance in JGoodies/Forms units (such as DLUs)
     */
    public static String getButtonWidth ()
    {
        return constants.buttonWidth.getValue();
    }

    /**
     * Selector to the default vertical interval between two rows.
     *
     * @return default distance in JGoodies/Forms units (such as DLUs)
     */
    public static String getFieldInterline ()
    {
        return constants.fieldInterline.getValue();
    }

    /**
     * Selector to the default interval between two label/field.
     *
     * @return default distance in JGoodies/Forms units (such as DLUs)
     */
    public static String getFieldInterval ()
    {
        return constants.fieldInterval.getValue();
    }

    /**
     * Selector to the default label width.
     *
     * @return default distance in JGoodies/Forms units (such as DLUs)
     */
    public static String getFieldWidth ()
    {
        return constants.fieldWidth.getValue();
    }

    /**
     * Selector to the default label - field interval.
     *
     * @return default distance in JGoodies/Forms units (such as DLUs)
     */
    public static String getLabelInterval ()
    {
        return constants.labelInterval.getValue();
    }

    /**
     * Selector to the default label width.
     *
     * @return default distance in JGoodies/Forms units (such as DLUs)
     */
    public static String getLabelWidth ()
    {
        return constants.labelWidth.getValue();
    }

    /**
     * Selector to the default vertical interval between two Panels.
     *
     * @return default distance in JGoodies/Forms units (such as DLUs)
     */
    public static String getPanelInterline ()
    {
        return constants.panelInterline.getValue();
    }

    //-------------//
    // makeColumns //
    //-------------//
    public static String makeColumns (int cols)
    {
        return makeColumns(cols, "right:", Panel.getLabelWidth(), Panel.getFieldWidth());
    }

    //-------------//
    // makeColumns //
    //-------------//
    public static String makeColumns (int cols,
                                      String labelAlignment,
                                      String labelWidth,
                                      String fieldWidth)
    {
        final String labelInterval = Panel.getLabelInterval();
        final String fieldInterval = Panel.getFieldInterval();

        StringBuilder sbc = new StringBuilder();

        for (int i = 0; i < cols; i++) {
            if (i != 0) {
                sbc.append(",").append(fieldInterval).append(",");
            }

            sbc.append(labelAlignment).append(labelWidth).append(",").append(labelInterval)
                    .append(",").append(fieldWidth);
        }

        return sbc.toString();
    }

    //----------------//
    // makeFormLayout //
    //----------------//
    /**
     * Build a JGoodies Forms layout, based on the provided number of
     * rows and number of columns, using default values for label
     * alignment, and for widths of labels and fields.
     *
     * @param rows number of logical rows (not counting the interlines)
     * @param cols number of logical columns (counting the combination of a
     *             label and its related field as just one column)
     * @return the FormLayout ready to use
     */
    public static FormLayout makeFormLayout (int rows,
                                             int cols)
    {
        return makeFormLayout(rows, cols, "right:", Panel.getLabelWidth(), Panel.getFieldWidth());
    }

    //----------------//
    // makeFormLayout //
    //----------------//
    /**
     * A more versatile way to prepare a JGoodies FormLayout.
     *
     * @param rows           number of rows
     * @param cols           number of columns
     * @param labelAlignment horizontal alignment for labels
     * @param labelWidth     width for labels
     * @param fieldWidth     width for text fields
     * @return the FormLayout ready to use
     */
    public static FormLayout makeFormLayout (int rows,
                                             int cols,
                                             String labelAlignment,
                                             String labelWidth,
                                             String fieldWidth)
    {
        return new FormLayout(
                makeColumns(cols, labelAlignment, labelWidth, fieldWidth),
                makeRows(rows));
    }

    //-------------------//
    // makeLabelsColumns //
    //-------------------//
    public static String makeLabelsColumns (int cols,
                                            String labelInterval,
                                            String labelWidth)
    {
        StringBuilder sbc = new StringBuilder();

        for (int i = 0; i < cols; i++) {
            if (i != 0) {
                sbc.append(",").append(labelInterval).append(",");
            }

            sbc.append(labelWidth);
        }

        return sbc.toString();
    }

    //------------------//
    // makeLabelsLayout //
    //------------------//
    /**
     * A more versatile way to prepare a JGoodies FormLayout for labels.
     *
     * @param rows          number of rows
     * @param cols          number of columns
     * @param labelInterval horizontal gap between labels
     * @param labelWidth    width for labels
     * @return the FormLayout ready to use
     */
    public static FormLayout makeLabelsLayout (int rows,
                                               int cols,
                                               String labelInterval,
                                               String labelWidth)
    {
        return new FormLayout(makeLabelsColumns(cols, labelInterval, labelWidth), makeRows(rows));
    }

    //----------//
    // makeRows //
    //----------//
    /**
     * Build the row spec for 'rows' separated by standard field interline.
     *
     * @param rows number of rows
     * @return row spec
     */
    public static String makeRows (int rows)
    {
        return makeRows(rows, getFieldInterline());
    }

    //----------//
    // makeRows //
    //----------//
    /**
     * Build the row spec for 'rows' separated by 'interline' spec.
     *
     * @param rows      number of rows
     * @param interline interline value
     * @return row spec
     */
    public static String makeRows (int rows,
                                   String interline)
    {
        final StringBuilder sbr = new StringBuilder();

        for (int i = 0; i < rows; i++) {
            if (i != 0) {
                sbr.append(",").append(interline).append(",");
            }

            sbr.append("pref");
        }

        return sbr.toString();
    }

    //-----------//
    // getInsets //
    //-----------//
    /**
     * By this way, Swing will paint the component with its specific inset values.
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

    //-----------//
    // setInsets //
    //-----------//
    /**
     * Set the panel insets (in number of pixels) on the four directions.
     *
     * @param top    inset on top side
     * @param left   inset on the left side
     * @param bottom inset on the bottom side
     * @param right  inset on the right side
     */
    public void setInsets (int top,
                           int left,
                           int bottom,
                           int right)
    {
        insets = new Insets(top, left, bottom, right);
    }

    //-------------//
    // setNoInsets //
    //-------------//
    /**
     * A convenient method to set all 4 insets values to zero.
     */
    public void setNoInsets ()
    {
        insets = new Insets(0, 0, 0, 0);
    }

    //----------------//
    // paintComponent //
    //----------------//
    /**
     * This method is redefined to give a chance to draw the cell boundaries
     * if so desired.
     *
     * @param g the graphic context
     */
    @Override
    protected void paintComponent (Graphics g)
    {
        // Note: Uncomment following line for FormDebugPanel
        ///setPaintInBackground(true);
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

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.String buttonWidth = new Constant.String(
                "45dlu",
                "Width of a standard button");

        private final Constant.String fieldInterline = new Constant.String(
                "1dlu",
                "Vertical gap between two lines");

        private final Constant.String fieldInterval = new Constant.String(
                "3dlu",
                "Horizontal gap between two fields");

        private final Constant.String fieldWidth = new Constant.String(
                "35dlu",
                "Width of a field value");

        private final PixelCount insetBottom = new PixelCount(6, "Value of Bottom inset");

        private final PixelCount insetLeft = new PixelCount(6, "Value of Left inset");

        private final PixelCount insetRight = new PixelCount(6, "Value of Right inset");

        private final PixelCount insetTop = new PixelCount(6, "Value of Top inset");

        private final Constant.String labelInterval = new Constant.String(
                "1dlu",
                "Horizontal gap between a field label and its field value");

        private final Constant.String labelWidth = new Constant.String(
                "25dlu",
                "Width of the label of a field");

        private final Constant.String panelInterline = new Constant.String(
                "6dlu",
                "Vertical gap between two panels");
    }
}
