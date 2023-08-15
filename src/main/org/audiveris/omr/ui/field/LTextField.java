//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      L T e x t F i e l d                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.ui.field;

import javax.swing.JTextField;

/**
 * Class <code>LTextField</code> is a {@link LField}, where the field is a text field.
 *
 * @author Hervé Bitteur
 */
public class LTextField
        extends LField<JTextField>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Default number of characters in the text field : {@value} */
    private static final int FIELD_WIDTH = 6;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new LTextField object.
     *
     * @param editable Specifies whether this field will be editable
     * @param label    the string to be used as label text
     * @param tip      the related tool tip text
     */
    public LTextField (boolean editable,
                       String label,
                       String tip)
    {
        this(editable, label, tip, FIELD_WIDTH);
    }

    /**
     * Creates a new LTextField object.
     *
     * @param editable Specifies whether this field will be editable
     * @param label    the string to be used as label text
     * @param tip      the related tool tip text
     * @param width    the field width in characters
     */
    public LTextField (boolean editable,
                       String label,
                       String tip,
                       int width)
    {
        super(label, tip, new JTextField(width));

        JTextField textField = getField();
        textField.setEditable(editable);

        if (!editable) {
            textField.setFocusable(false);
            textField.setBorder(null);
        }

        textField.setHorizontalAlignment(JTextField.CENTER);
    }

    /**
     * Creates a new non-editable LTextField object.
     *
     * @param label the string to be used as label text
     * @param tip   the related tool tip text
     */
    public LTextField (String label,
                       String tip)
    {
        this(false, label, tip);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // getText //
    //---------//
    /**
     * Report the current content of the field
     *
     * @return the field content
     */
    public String getText ()
    {
        return getField().getText();
    }

    //---------//
    // setText //
    //---------//
    /**
     * Modify the content of the field
     *
     * @param text new text to set
     */
    public void setText (String text)
    {
        getField().setText(text);
    }
}
