//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   L I n t e g e r F i e l d                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;

/**
 * Class <code>LIntegerField</code> is an {@link LTextField}, whose field is meant to handle
 * an integer value.
 *
 * @author Hervé Bitteur
 */
public class LIntegerField
        extends LTextField
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create an integer labeled field
     *
     * @param editable tells whether the field is editable
     * @param label    string to be used as label text
     * @param tip      related tool tip text
     */
    public LIntegerField (boolean editable,
                          String label,
                          String tip)
    {
        super(editable, label, tip);
        setFilter();
    }

    /**
     * Create an integer labeled field
     *
     * @param editable tells whether the field is editable
     * @param label    string to be used as label text
     * @param tip      related tool tip text
     * @param width    field width in characters
     */
    public LIntegerField (boolean editable,
                          String label,
                          String tip,
                          int width)
    {
        super(editable, label, tip, width);
        setFilter();
    }

    /**
     * Create a (constant) integer labeled field
     *
     * @param label string to be used as label text
     * @param tip   related tool tip text
     */
    public LIntegerField (String label,
                          String tip)
    {
        super(true, label, tip);
        setFilter();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // getValue //
    //----------//
    /**
     * Extract the current integer value form the text field.
     *
     * @return current integer value (a blank field is assumed to mean 0).
     */
    public int getValue ()
    {
        String str = getField().getText().trim();

        if (str.length() == 0) {
            return 0;
        } else {
            return Integer.parseInt(str);
        }
    }

    //-----------//
    // setFilter //
    //-----------//
    /**
     * Adds the filter to the input field's document.
     */
    private void setFilter () {
        AbstractDocument doc = (AbstractDocument) getField().getDocument();
        doc.setDocumentFilter(new IntFilter());
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Modify the current field
     *
     * @param val the integer value to be used
     */
    public void setValue (int val)
    {
        getField().setText(Integer.toString(val));
    }


    //~ Inner Classes ------------------------------------------------------------------------------

    /** 
     * Intercepts input in the LIntegerField and disallows input that would
     * result in an invalid int.
     */
    private class IntFilter extends DocumentFilter
    {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
            throws BadLocationException {
            Document doc = fb.getDocument();
            StringBuilder sb = new StringBuilder(doc.getText(0, doc.getLength()));
            sb.insert(offset, string);

            if (test(sb.toString())) {
                super.insertString(fb, offset, string, attr);
            }

        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
          throws BadLocationException {
            Document doc = fb.getDocument();
            StringBuilder sb = new StringBuilder(doc.getText(0, doc.getLength()));
            sb.replace(offset, offset + length, text);

            if (test(sb.toString())) {
                super.replace(fb, offset, length, text, attrs);
            }
            
        }

        private boolean test(String text) {
            try {
                Integer.parseInt(text);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }

        }
    }

}
